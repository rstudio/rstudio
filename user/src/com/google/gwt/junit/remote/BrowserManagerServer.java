/*
 * Copyright 2006 Google Inc.
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

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages instances of a web browser as child processes. This class is
 * experimental and unsupported. An instance of this class can create browser
 * windows using one specific shell-level command. It performs process
 * managagement (babysitting) on behalf of a remote client. This can be useful
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
 * This system has been tested on Internet Explorer 6. Firefox does not work in
 * the general case; if an existing Firefox process is already running, new
 * processes simply delegate to the existing process and terminate, which breaks
 * the model. Safari on MacOS requires very special treatment given Safari's
 * poor command line support, but that is beyond the scope of this
 * documentation.
 * </p>
 * 
 * <p>
 * TODO(scottb): We technically need a watchdog thread to slurp up stdout and
 * stderr from the child processes, or they might block. However, most browsers
 * never write to stdout and stderr, so this is low priority.
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
   * Manages one web browser child process. This class contains a TimerTask
   * which tries to kill the managed process.
   * 
   * Invariants:
   * <ul>
   * <li> If process is alive, this manager is in <code>processByToken</code>.
   * </li>
   * <li> If process is dead, this manager <i>might</i> be in
   * <code>processByToken</code>. It will be observed to be dead next time
   * {@link #keepAlive(long)} or {@link #doKill()} are called. </li>
   * <li> Calling {@link #keepAlive(long)} and {@link #doKill()} require the
   * lock on <code>processByToken</code> to be held, so they cannot be called
   * at the same time. </li>
   * </ul>
   */
  private final class ProcessManager {

    /**
     * Kills the child process when fired, unless it is no longer the active
     * {@link ProcessManager#killTask} in its outer ProcessManager.
     */
    private final class KillTask extends TimerTask {
      /*
       * @see java.lang.Runnable#run()
       */
      public void run() {
        synchronized (processByToken) {
          /*
           * CORNER CASE: Verify we're still the active KillTask, because it's
           * possible we were bumped out by a keepAlive call after our execution
           * started but before we could grab the lock on processByToken.
           */
          if (killTask == this) {
            doKill();
          }
        }
      }
    }

    /**
     * The key associated with <code>process</code> in
     * <code>processByToken</code>.
     */
    private Object key;

    /**
     * If non-null, the active TimerTask which will kill <code>process</code>
     * when it fires.
     */
    private KillTask killTask;

    /**
     * The managed child process.
     */
    private final Process process;

    /**
     * Constructs a new ProcessManager for the specified process, and adds
     * itself to <code>processByToken</code> using the supplied key. You must
     * hold the lock on <code>processByToken</code> to call this method.
     * 
     * @param key the key to be used when adding the new object to
     *          <code>processByToken</code>
     * @param process the process being managed
     * @param initKeepAliveMs the initial time to wait before killing
     *          <code>process</code>
     */
    ProcessManager(Object key, Process process, long initKeepAliveMs) {
      this.process = process;
      this.key = key;
      schedule(initKeepAliveMs);
      processByToken.put(key, this);
    }

    /**
     * Kills the managed process. You must hold the lock on
     * <code>processByToken</code> to call this method.
     */
    public void doKill() {
      Object removed = processByToken.remove(key);
      assert (removed == this);
      process.destroy();
      schedule(0);
    }

    /**
     * Keeps the underlying process alive for <code>keepAliveMs</code>
     * starting now. If the managed process is already dead, cleanup is
     * performed and the method return false. You must hold the lock on
     * <code>processByToken</code> to call this method.
     * 
     * @param keepAliveMs the time to wait before killing the underlying process
     * @return <code>true</code> if the process was successfully kept alive,
     *         <code>false</code> if the process is already dead.
     */
    public boolean keepAlive(long keepAliveMs) {
      try {
        /*
         * See if the managed process is still alive. WEIRD: The only way to
         * check the process's liveness appears to be asking for its exit status
         * and seeing whether it throws an IllegalThreadStateException.
         */
        process.exitValue();
      } catch (IllegalThreadStateException e) {
        // The process is still alive.
        schedule(keepAliveMs);
        return true;
      }

      // The process is dead already; perform cleanup.
      doKill();
      return false;
    }

    /**
     * Cancels any existing kill task and optionally schedules a new one to run
     * <code>keepAliveMs</code> from now. You must hold the lock on
     * <code>processByToken</code> to call this method.
     * 
     * @param keepAliveMs if > 0, schedules a new kill task to run in
     *          keepAliveMs milliseconds; if <= 0, a new kill task is not
     *          scheduled.
     */
    private void schedule(long keepAliveMs) {
      if (killTask != null) {
        killTask.cancel();
        killTask = null;
      }
      if (keepAliveMs > 0) {
        killTask = new KillTask();
        timer.schedule(killTask, keepAliveMs);
      }
    }
  }

  /**
   * Starts up and registers one or more browser servers. Command-line entry
   * point.
   */
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println(""
          + "Manages local browser windows for a remote client using RMI.\n"
          + "\n"
          + "Pass in an even number of args, at least 2. The first argument\n"
          + "is a short registration name, and the second argument is the\n"
          + "executable to run when that name is used; for example,\n" + "\n"
          + "\tie6 \"C:\\Program Files\\Internet Explorer\\IEXPLORE.EXE\"\n"
          + "\n"
          + "would register Internet Explorer to \"rmi://localhost/ie6\".\n"
          + "The third and fourth arguments make another pair, and so on.\n");
      System.exit(1);
    }

    if (args.length < 2) {
      throw new IllegalArgumentException("Need at least 2 arguments");
    }

    if (args.length % 2 != 0) {
      throw new IllegalArgumentException("Need an even number of arguments");
    }

    // Create an RMI registry so we don't need an external process.
    // Uses the default RMI port.
    // TODO(scottb): allow user to override the port via command line option.
    LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    System.out.println("RMI registry ready.");

    for (int i = 0; i < args.length; i += 2) {
      BrowserManagerServer bms = new BrowserManagerServer(args[i + 1]);
      Naming.rebind(args[i], bms);
      System.out.println(args[i] + " started and awaiting connections");
    }
  }

  /**
   * The shell command to launch when a new browser is requested.
   */
  private final String launchCmd;

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
  private final Map processByToken = new HashMap();

  /**
   * A single shared Timer used by all instances of
   * {@link ProcessManager.KillTask}.
   */
  private final Timer timer = new Timer();

  /**
   * Constructs a manager for a particular shell command.
   * 
   * @param launchCmd the path to a browser's executable, suitable for passing
   *          to {@link Runtime#exec(java.lang.String)}. The invoked process
   *          must accept a URL as a command line argument.
   */
  public BrowserManagerServer(String launchCmd) throws RemoteException {
    this.launchCmd = launchCmd;
  }

  /*
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
      Integer intTok = new Integer(token);
      ProcessManager process = (ProcessManager) processByToken.get(intTok);
      if (process != null) {
        if (process.keepAlive(keepAliveMs)) {
          // The process was successfully kept alive.
          return;
        } else {
          // The process is already dead. Fall through to failure.
        }
      }
    }

    throw new IllegalStateException("Process " + token + " already dead");
  }

  /*
   * @see BrowserManager#killBrowser(int)
   */
  public void killBrowser(int token) {
    synchronized (processByToken) {
      // Is the token one we've issued?
      if (token < 0 || token >= nextToken) {
        throw new IllegalArgumentException();
      }
      Integer intTok = new Integer(token);
      ProcessManager process = (ProcessManager) processByToken.get(intTok);
      if (process != null) {
        process.doKill();
      }
    }
  }

  /*
   * @see BrowserManager#launchNewBrowser(java.lang.String, long)
   */
  public int launchNewBrowser(String url, long keepAliveMs) {

    if (url == null || keepAliveMs <= 0) {
      throw new IllegalArgumentException();
    }

    try {
      Process child = Runtime.getRuntime().exec(new String[] {launchCmd, url});
      synchronized (processByToken) {
        int myToken = nextToken++;
        Integer intTok = new Integer(myToken);
        // Adds self to processByToken.
        new ProcessManager(intTok, child, keepAliveMs);
        return myToken;
      }
    } catch (IOException e) {
      throw new RuntimeException("Error launching browser '" + launchCmd
          + "' for '" + url + "'", e);
    }
  }
}
