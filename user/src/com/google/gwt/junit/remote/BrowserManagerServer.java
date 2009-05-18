/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.junit.remote;

import com.google.gwt.junit.remote.BrowserManagerProcess.ProcessExitCb;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages instances of a web browser as child processes. This class is
 * experimental and unsupported. An instance of this class can create browser
 * windows using one specific shell-level command. It performs process
 * management (baby sitting) on behalf of a remote client. This can be useful
 * for running a GWTTestCase on a browser that cannot be run on the native
 * platform. For example, a GWTTestCase test running on Linux could use a remote
 * call to a Windows machine to test with Internet Explorer.
 * 
 * <p>
 * Calling {@link #main(String[])} can instantiate and register multiple
 * instances of this class at given RMI namespace locations.
 * </p>
 * 
 * <p>
 * This system has been tested on Internet Explorer 6 & 7. Firefox does not work
 * in the general case; if an existing Firefox process is already running, new
 * processes simply delegate to the existing process and terminate, which breaks
 * the model. A shell script that sets MOZNOREMOTE=1 and cleans up
 * locks/sessions is needed. Safari on MacOS requires very special treatment
 * given Safari's poor command line support, but that is beyond the scope of
 * this documentation.
 * </p>
 * 
 * <p>
 * TODO(scottb): We technically need a watchdog thread to slurp up stdout and
 * stderr from the child processes, or they might block. However, most browsers
 * never write to stdout and stderr, so this is low priority. (There is now a
 * thread that is spawned for each task to wait for an exit value - this might
 * be adapted for that purpose one day.)
 * </p>
 * 
 * <p>
 * This class is not actually serializable as-is, because timer is not
 * serializable.
 * </p>
 * 
 * see http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4062587
 */
public class BrowserManagerServer extends UnicastRemoteObject implements
    BrowserManager {

  /**
   * Implementation notes: <code>processByToken</code> must be locked before
   * performing any state-changing operations.
   */

  /**
   * Entry in the launchCommandQueue to use when tasks are serialized.
   */
  private static class LaunchCommand {
    long keepAliveMsecs;
    int token;
    String url;

    LaunchCommand(int tokenIn) {
      this(tokenIn, null, 0);
    }

    LaunchCommand(int tokenIn, String urlIn, long keepAliveMsecsIn) {
      token = tokenIn;
      url = urlIn;
      keepAliveMsecs = keepAliveMsecsIn;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof LaunchCommand && ((LaunchCommand) obj).token == token) {
        return true;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return token;
    }
  }

  private static final Logger logger = Logger.getLogger(BrowserManagerServer.class.getName());

  /**
   * Starts up and registers one or more browser servers. Command-line entry
   * point.
   */
  public static void main(String[] args) throws Exception {

    // Startup logic has been delegated to BrowserManagerServerLauncher
    // class to facilitate use of the ToolBase class for
    // argument handling.
    BrowserManagerServerLauncher serverMain = new BrowserManagerServerLauncher();
    if (serverMain.doProcessArgs(args)) {
      serverMain.run();
    }
  }

  /**
   * Receives an event when a child process exits.
   */
  private final ProcessExitCb childExitCallback = new ProcessExitCb() {
    /**
     * Called back from BrowserManagerProcess in a DIFFERENT THREAD than the
     * main thread.
     * 
     * @param token token value of browser that exited.
     * @param exitValue exit status of the browser.
     */
    public void childExited(int token, int exitValue) {
      synchronized (processByToken) {
        processByToken.remove(token);
        // Start up any commands that were delayed.
        launchDelayedCommand();
      }
    }
  };

  /**
   * The shell command to launch when a new browser is requested.
   */
  private final String launchCmd;

  /**
   * A queue of delayed commands. This is used if the serialized option is
   * turned on.
   */
  private Queue<LaunchCommand> launchCommandQueue = new LinkedList<LaunchCommand>();

  /**
   * The next token that will be returned from
   * {@link #launchNewBrowser(String, long)}.
   */
  private int nextToken = 1;

  /**
   * Master map of tokens onto ProcessManagers managing live processes. Also
   * serves as a lock that must be held before any state-changing operations on
   * this class may be performed.
   */
  private final Map<Integer, BrowserManagerProcess> processByToken = new HashMap<Integer, BrowserManagerProcess>();

  /**
   * Flag that is set if the serialized option is turned on.
   */
  private final boolean serializeFlag;

  /**
   * A single shared Timer used by all instances of
   * {@link BrowserManagerProcess}.
   */
  private final Timer timer = new Timer();

  /**
   * Constructs a manager for a particular shell command. The specified launch
   * command should be a path to a browser's executable, suitable for passing to
   * {@link Runtime#exec(java.lang.String)}. It may also include newline
   * delimited arguments to pass to that executable. The invoked process must
   * accept a URL as the final command line argument.
   * 
   * @param launchCmd a command to launch a browser executable
   * @param serializeFlag if <code>true</code>, serialize instance of browser
   *          processes to only run one at a time
   */
  BrowserManagerServer(String launchCmd, boolean serializeFlag)
      throws RemoteException {
    // TODO: It would be nice to test to see if this file exists, but
    // currently this mechanism allows you to pass in command line arguments
    // and it will be a pain to accommodate this.
    this.launchCmd = launchCmd;
    this.serializeFlag = serializeFlag;
  }

  /**
   * @see BrowserManager#keepAlive(int, long)
   */
  public void keepAlive(int token, long keepAliveMs) {

    if (keepAliveMs <= 0) {
      throw new IllegalArgumentException();
    }

    synchronized (processByToken) {
      // Is the token one we've issued?
      if (token < 0 || token >= nextToken) {
        throw new IllegalArgumentException();
      }
      BrowserManagerProcess process = processByToken.get(token);
      if (process != null) {
        if (process.keepAlive(keepAliveMs)) {
          // The process was successfully kept alive.
          return;
        }
      } else if (launchCommandQueue.contains(new LaunchCommand(token))) {
        // Nothing to do, the command hasn't started yet.
        return;
      }

      // The process is already dead. Fall through to failure.
    }

    throw new IllegalStateException("Process " + token + " already dead");
  }

  /**
   * @see BrowserManager#killBrowser(int)
   */
  public void killBrowser(int token) {

    synchronized (processByToken) {
      // Is the token one we've issued?
      if (token < 0 || token >= nextToken) {
        throw new IllegalArgumentException();
      }
      BrowserManagerProcess process = processByToken.get(token);
      if (process != null) {
        logger.info("Client kill for active browser: " + token);
        process.killBrowser();
      } else if (launchCommandQueue.contains(new LaunchCommand(token))) {
        launchCommandQueue.remove(new LaunchCommand(token));
        logger.info("Client kill for waiting browser: " + token);
      } else {
        logger.info("Client kill for inactive browser: " + token);
      }
    }
  }

  /**
   * @see BrowserManager#launchNewBrowser(java.lang.String, long)
   */
  public int launchNewBrowser(String url, long keepAliveMs) {
    logger.info("Launching browser for url: " + url + " keepAliveMs: "
        + keepAliveMs);
    if (url == null || keepAliveMs <= 0) {
      throw new IllegalArgumentException();
    }

    try {
      synchronized (processByToken) {
        int myToken = nextToken++;
        // Adds self to processByToken.

        if (serializeFlag && !processByToken.isEmpty()) {
          // Queue up a launch request if one is already running.
          launchCommandQueue.add(new LaunchCommand(myToken, url, keepAliveMs));
          logger.info("Queuing up request token: " + myToken + " for url: "
              + url + ".  Another launch command is active.");
        } else {
          execChild(myToken, url, keepAliveMs);
        }
        return myToken;
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error launching browser '" + launchCmd
          + "' for '" + url + "'", e);
      throw new RuntimeException("Error launching browser '" + launchCmd
          + "' for '" + url + "'", e);
    }
  }

  /**
   * This method is mainly in place for writing assertions in the unit test.
   * 
   * @return number of tasks waiting to run if serialized option is enabled
   */
  int numQueued() {
    synchronized (processByToken) {
      return launchCommandQueue.size();
    }
  }

  /**
   * This method is mainly in place for writing assertions in the unit test.
   * 
   * @return number of launch commands running that have not yet exited.
   */
  int numRunning() {
    synchronized (processByToken) {
      return processByToken.size();
    }
  }

  /**
   * Actually create a process and run a browser.
   * 
   * (Assumes that code is already synchronized by processBytoken)
   * 
   * @param token token value of browser that exited.
   * @param url command line arguments to pass to the browser
   * @param keepAliveMs inital keep alive interval in milliseconds
   */
  private void execChild(int token, String url, long keepAliveMs)
      throws IOException {
    // Tokenize the launchCmd by carriage returns (used for unit testing).
    StringTokenizer st = new StringTokenizer(launchCmd, "\n");
    int userTokens = st.countTokens();
    String[] cmdarray = new String[userTokens + 1];
    for (int i = 0; st.hasMoreTokens(); i++) {
      cmdarray[i] = st.nextToken();
    }
    // Append the user-specified URL.
    cmdarray[userTokens] = url;

    // Start the task.
    Process child = Runtime.getRuntime().exec(cmdarray);

    BrowserManagerProcess bmp = new BrowserManagerProcess(childExitCallback,
        timer, token, child, keepAliveMs);
    processByToken.put(token, bmp);
  }

  /**
   * If serialization is enabled on the server, kicks off the next queued
   * command on the delayed command queue.
   * 
   * (Assumes that code is already synchronized by processBytoken)
   */
  private void launchDelayedCommand() {
    if (!serializeFlag || !processByToken.isEmpty()) {
      // No need to launch if serialization is off or
      // something is already running
      return;
    }

    // Loop through the commands until we can launch one
    // successfully.
    while (!launchCommandQueue.isEmpty()) {
      LaunchCommand lc = launchCommandQueue.remove();
      try {
        execChild(lc.token, lc.url, lc.keepAliveMsecs);
        // No exception? Great!
        logger.info("Started delayed browser: " + lc.token);
        return;

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Error launching browser '" + launchCmd
            + "' for '" + lc.url + "'", e);
        throw new RuntimeException("Error launching browser '" + launchCmd
            + "' for '" + lc.url + "'", e);
      }
      // If an exception occurred, keep pulling cmds off the queue.
    }
  }
}
