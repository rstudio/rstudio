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

import junit.framework.TestCase;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

/**
 * Exercise the BrowserManagerServer with the serialized option turned on and
 * off.
 */
public class BrowserManagerServerTest extends TestCase {
  /**
   * Seconds for simulated browser to hang.
   */
  static final int TIMEOUT_MS = 2000;
  /**
   * Time to wait between keepAlive calls to all browsers.
   */
  static final int PING_INTERVAL_MS = 100;
  static final boolean LOG = false;

  private static final String REGISTRATION_KEY = "sleep-" + (TIMEOUT_MS / 1000)
      + "-seconds";
  private static Registry rmiRegistry = null;

  private int portArg = Registry.REGISTRY_PORT;
  private BrowserManagerServer server = null;

  /**
   * Run a test with the 'serialized' flag on.
   */
  public void testSerializedServer() throws Exception {
    BrowserManager browserManager = startBrowserManagerServer(true);

    // Launch some browsers all at once.
    final int NUM_BROWSERS = 4;
    int tokens[] = new int[NUM_BROWSERS];

    for (int i = 0; i < NUM_BROWSERS; i++) {
      tokens[i] = launchBrowser(browserManager, i);
    }

    // Give them a chance to startup.
    Thread.sleep(TIMEOUT_MS / 4);

    int numQueued = server.numQueued();
    int numRunning = server.numRunning();
    assertEquals("Did not find the number of expected browsers queued up",
        NUM_BROWSERS - 1, numQueued);
    assertEquals("Expected only one running at a time for serialized.", 1,
        numRunning);

    outer : for (int runningBrowser = 0; runningBrowser < NUM_BROWSERS; ++runningBrowser) {
      // The current browser should be dead within twice the expected timeout.
      long shouldBeDeadBy = System.currentTimeMillis() + (TIMEOUT_MS * 2);
      while (System.currentTimeMillis() < shouldBeDeadBy) {
        assertTrue(server.numRunning() <= 1);

        // Ping every alive browser.
        for (int i = runningBrowser; i < NUM_BROWSERS; ++i) {
          // Keep the browser alive with a margin of safety until the next ping.
          try {
            browserManager.keepAlive(tokens[runningBrowser], TIMEOUT_MS);
          } catch (IllegalStateException ise) {
            // Expected.
            assertEquals("The wrong browser is dead", runningBrowser, i);
            if (LOG) {
              System.out.println("Browser token: " + tokens[i]
                  + " exited sucessfully");
            }
            // Ensure it's legal to kill the dead browser.
            browserManager.killBrowser(tokens[i]);
            // Continue with the next active browser.
            continue outer;
          }
        }
        Thread.sleep(PING_INTERVAL_MS);
      }
      // Error case
      fail("Browser " + runningBrowser + " failed to exit in a timely manner");
    }
  }

  /**
   * Run a test with the 'serialized' flag on.
   */
  public void testUnserializedServer() throws Exception {
    BrowserManager browserManager = startBrowserManagerServer(false);

    // Launch some browsers all at once.
    final int NUM_BROWSERS = 6;
    int tokens[] = new int[NUM_BROWSERS];

    for (int i = 0; i < NUM_BROWSERS; i++) {
      tokens[i] = launchBrowser(browserManager, i);
    }

    // Give them a chance to startup.
    Thread.sleep(TIMEOUT_MS / 4);

    int numQueued = server.numQueued();
    int numRunning = server.numRunning();
    assertEquals("No queuing should occur", 0, numQueued);
    assertEquals("All browers should be running", NUM_BROWSERS, numRunning);

    // The current browser should be dead within twice the expected timeout.
    long shouldBeDeadBy = System.currentTimeMillis() + (TIMEOUT_MS * 2);
    int liveBrowsers = NUM_BROWSERS;
    while (System.currentTimeMillis() < shouldBeDeadBy) {
      // Ping every alive browser.
      for (int i = 0; i < NUM_BROWSERS; ++i) {
        if (tokens[i] == 0) {
          // This one's already dead.
          continue;
        }
        // Keep the browser alive with a margin of safety until the next ping.
        try {
          browserManager.keepAlive(tokens[i], TIMEOUT_MS);
        } catch (IllegalStateException ise) {
          // Expected.
          if (LOG) {
            System.out.println("Browser token: " + tokens[i]
                + " exited sucessfully");
          }
          // Ensure it's legal to kill the dead browser.
          browserManager.killBrowser(tokens[i]);

          tokens[i] = 0;
          --liveBrowsers;

          if (liveBrowsers == 0) {
            // All done;
            return;
          }
        }
      }
      Thread.sleep(PING_INTERVAL_MS);
    }
    // Error case
    fail(liveBrowsers + " browsers failed to exit in a timely manner");
  }

  /**
   * Start up the RMI registry and create a shell script that just sleeps for
   * 'timeout' seconds.
   */
  @Override
  protected void setUp() throws Exception {
    if (rmiRegistry == null) {
      rmiRegistry = LocateRegistry.createRegistry(portArg);
    }
  }

  /**
   * Clean up temporary files.
   */
  @Override
  protected void tearDown() throws Exception {
    // De-register the server.
    if (rmiRegistry != null) {
      rmiRegistry.unbind(REGISTRATION_KEY);
    }
  }

  /**
   * Start a browser task on the server.
   * 
   * @param browserManager handle to the browser manager instance
   * @param token browser ident number
   */
  private int launchBrowser(BrowserManager browserManager, int token)
      throws RemoteException {
    return browserManager.launchNewBrowser("# client" + token, TIMEOUT_MS);
  }

  /**
   * Starts up an instance of BrowserManagerServer.
   * 
   * @param isSerialized true to enable the serialized mode (run one browser
   *          instance at a time, queue any others.)
   * @return the newly created instance of BrowserManagerServer on success
   */
  private BrowserManager startBrowserManagerServer(boolean isSerialized)
      throws RemoteException, MalformedURLException, NotBoundException {
    // Construct a launch command for relaunching the JVM out of process,
    // running DummyProcess.
    StringBuilder sb = new StringBuilder();
    Properties properties = System.getProperties();
    sb.append(properties.getProperty("java.home"));
    sb.append(File.separatorChar);
    sb.append("bin");
    sb.append(File.separatorChar);
    sb.append("java");
    sb.append('\n');

    sb.append("-classpath");
    sb.append('\n');

    sb.append(properties.getProperty("java.class.path"));
    sb.append('\n');

    sb.append(DummyProcess.class.getName());

    server = new BrowserManagerServer(sb.toString(), isSerialized);
    rmiRegistry.rebind(REGISTRATION_KEY, server);

    // Server started. Now, create a client and send some commands to it
    String url = "rmi://localhost/" + REGISTRATION_KEY;
    BrowserManager browserManager = (BrowserManager) Naming.lookup(url);
    return browserManager;
  }
}
