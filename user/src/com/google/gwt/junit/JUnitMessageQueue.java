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
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A message queue to pass data between {@link JUnitShell} and {@link
 * com.google.gwt.junit.server.JUnitHostImpl} in a thread-safe manner.
 * 
 * <p>
 * The public methods are called by the servlet to find out what test to execute
 * next, and to report the results of the last test to run.
 * </p>
 * 
 * <p>
 * The protected methods are called by the shell to fetch test results and drive
 * the next test the client should run.
 * </p>
 */
public class JUnitMessageQueue {

  /**
   * Holds the state of an individual client.
   */
  public static class ClientStatus {
    public final String clientId;

    public JUnitResult currentTestResults = null;
    public boolean hasRequestedCurrentTest = false;
    public boolean isNew = true;

    public ClientStatus(String clientId) {
      this.clientId = clientId;
    }
  }

  /**
   * Records results for each client; must lock before accessing.
   */
  private final Map<String, ClientStatus> clientStatuses = new HashMap<String, ClientStatus>();

  /**
   * The lock used to synchronize access to clientStatuses.
   */
  private Object clientStatusesLock = new Object();

  /**
   * The current test to execute.
   */
  private TestInfo currentTest;

  private TreeLogger logger;
  /**
   * The number of TestCase clients executing in parallel.
   */
  private final int numClients;

  /**
   * Only instantiable within this package.
   * 
   * @param numClients The number of parallel clients being served by this
   *          queue.
   * @param loglevel The loglevel to use.  There is a circular dependency in 
   *          startup; we need the message queue to make the GWTShell, but its
   *          logger depends on having mainWnd which requires the GWTShell be
   *          started.  So, initially, we log to a PrintWriterTreeLogger, and
   *          use setLogger after the bootstrap.
   */
  JUnitMessageQueue(int numClients, Type loglevel) {
    PrintWriterTreeLogger consolelog = new PrintWriterTreeLogger();
    consolelog.setMaxDetail(loglevel);
    logger = consolelog;
    synchronized (clientStatusesLock) {
      this.numClients = numClients;
    }
  }

  /**
   * Called by the servlet to query for for the next method to test.
   * 
   * @param timeout how long to wait for an answer
   * @return the next test to run, or <code>null</code> if
   *         <code>timeout</code> is exceeded or the next test does not match
   *         <code>testClassName</code>
   */
  public TestInfo getNextTestInfo(String clientId, long timeout)
      throws TimeoutException {
    synchronized (clientStatusesLock) {
      ClientStatus clientStatus = clientStatuses.get(clientId);
      if (clientStatus == null) {
        clientStatus = new ClientStatus(clientId);
        clientStatuses.put(clientId, clientStatus);
      }

      long startTime = System.currentTimeMillis();
      long stopTime = startTime + timeout;
      while (clientStatus.currentTestResults != null) {
        long timeToWait = stopTime - System.currentTimeMillis();
        if (timeToWait < 1) {
          double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
          throw new TimeoutException("The servlet did not respond to the "
              + "next query to test within " + timeout + "ms.\n"
              + " Client id: " + clientId + "\n" + " Actual time elapsed: "
              + elapsed + " seconds.\n");
        }
        try {
          clientStatusesLock.wait(timeToWait);
        } catch (InterruptedException e) {
          /*
           * Should never happen; but if it does, just send a null back to the
           * client, which will cause it to stop running tests.
           */
          System.err.println("Unexpected thread interruption");
          e.printStackTrace();
          return null;
        }
      }

      // Record that this client has retrieved the current test.
      clientStatus.hasRequestedCurrentTest = true;
      logger.log(TreeLogger.TRACE, 
          "Client " + clientId + " has next test info for " +
          currentTest.getTestClass() + "." + currentTest.getTestMethod());
      return currentTest;
    }
  }

  /**
   * Called by the servlet to report the results of the last test to run.
   * 
   * @param testInfo the testInfo the result is for
   * @param results the result of running the test
   */
  public void reportResults(String clientId, TestInfo testInfo,
      JUnitResult results) {
    synchronized (clientStatusesLock) {
      if (testInfo != null && !testInfo.equals(currentTest)) {
        // A client is reporting results for the wrong test.
        logger.log(TreeLogger.WARN, 
            "Wrong (stale?) test report from " + clientId + ": reporting on " + 
            testInfo.getTestClass() + "." + testInfo.getTestMethod() +
            " (current test is " + testInfo.getTestClass() + "." + 
            testInfo.getTestMethod() + ")");
        return;
      }
      logger.log(TreeLogger.TRACE, "Client " + clientId + "reported results.");
      assert (results != null);
      ClientStatus clientStatus = clientStatuses.get(clientId);
      clientStatus.currentTestResults = results;
      clientStatusesLock.notifyAll();
    }
  }

  public void setLogger(TreeLogger newlog) {
    logger = newlog;
  }
  
  /**
   * Returns any new clients that have contacted the server since the last call.
   * The same client will never be returned from this method twice.
   */
  String[] getNewClients() {
    synchronized (clientStatusesLock) {
      List<String> results = new ArrayList<String>();
      for (ClientStatus clientStatus : clientStatuses.values()) {
        if (clientStatus.isNew) {
          results.add(clientStatus.clientId);
          // Record that this client is no longer new.
          clientStatus.isNew = false;
        }
      }
      return results.toArray(new String[results.size()]);
    }
  }

  /**
   * Returns how many clients have requested the currently-running test.
   */
  int getNumClientsRetrievedCurrentTest() {
    int count = 0;
    synchronized (clientStatusesLock) {
      for (ClientStatus clientStatus : clientStatuses.values()) {
        if (clientStatus.hasRequestedCurrentTest) {
          ++count;
        }
      }
    }
    return count;
  }

  /**
   * Fetches the results of a completed test.
   * 
   * @return A map of results from all clients.
   */
  Map<String, JUnitResult> getResults() {
    synchronized (clientStatusesLock) {
      Map<String, JUnitResult> result = new HashMap<String, JUnitResult>();
      for (ClientStatus clientStatus : clientStatuses.values()) {
        result.put(clientStatus.clientId, clientStatus.currentTestResults);
      }
      return result;
    }
  }

  /**
   * Returns a pretty printed list of clients that have not retrieved the
   * current test. Used for error reporting.
   * 
   * @return a string containing the list of clients that have not retrieved the
   *         current test.
   */
  String getUnretrievedClients() {
    synchronized (clientStatusesLock) {
      StringBuilder buf = new StringBuilder();
      int lineCount = 0;
      for (ClientStatus clientStatus : clientStatuses.values()) {
        if (lineCount > 0) {
          buf.append('\n');
        }

        if (!clientStatus.hasRequestedCurrentTest) {
          buf.append(" - NO RESPONSE: ");
        } else {
          buf.append(" - (ok): ");
        }
        buf.append(clientStatus.clientId);
        ++lineCount;
      }
      int difference = numClients - getNumClientsRetrievedCurrentTest();
      if (difference > 0) {
        if (lineCount > 0) {
          buf.append('\n');
        }
        buf.append(" - "
            + difference
            + " client(s) haven't responded back to JUnitShell since the start of the test.");
      }
      return buf.toString();
    }
  }

  /**
   * Called by the shell to see if the currently-running test has completed.
   * 
   * @return If the test has completed, <code>true</code>, otherwise
   *         <code>false</code>.
   */
  boolean hasResult() {
    synchronized (clientStatusesLock) {
      if (numClients > clientStatuses.size()) {
        return false;
      }
      for (ClientStatus clientStatus : clientStatuses.values()) {
        if (clientStatus.currentTestResults == null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Called by the shell to set the next test to run.
   */
  void setNextTest(TestInfo testInfo) {
    synchronized (clientStatusesLock) {
      this.currentTest = testInfo;
      for (ClientStatus clientStatus : clientStatuses.values()) {
        clientStatus.hasRequestedCurrentTest = false;
        clientStatus.currentTestResults = null;
      }
      clientStatusesLock.notifyAll();
    }
  }

  void waitForResults(int millis) {
    synchronized (clientStatusesLock) {
      try {
        clientStatusesLock.wait(millis);
      } catch (InterruptedException e) {
      }
    }
  }
}
