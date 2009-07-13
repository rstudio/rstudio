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

import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A message queue to pass data between {@link JUnitShell} and
 * {@link com.google.gwt.junit.server.JUnitHostImpl} in a thread-safe manner.
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
    /**
     * Stores the testResults for the current block of tests.
     */
    public Map<TestInfo, JUnitResult> currentTestBlockResults = null;
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
  private final Object clientStatusesLock = new Object();

  /**
   * The current test to execute.
   */
  private TestInfo[] currentBlock;

  /**
   * The number of TestCase clients executing in parallel.
   */
  private final int numClients;

  /**
   * Only instantiable within this package.
   * 
   * @param numClients The number of parallel clients being served by this
   *          queue.
   */
  JUnitMessageQueue(int numClients) {
    synchronized (clientStatusesLock) {
      this.numClients = numClients;
    }
  }

  /**
   * Called by the servlet to query for for the next block to test.
   * 
   * @param timeout how long to wait for an answer
   * @return the next test to run, or <code>null</code> if <code>timeout</code>
   *         is exceeded or the next test does not match
   *         <code>testClassName</code>
   */
  public TestInfo[] getNextTestBlock(String clientId, long timeout)
      throws TimeoutException {
    synchronized (clientStatusesLock) {
      ClientStatus clientStatus = clientStatuses.get(clientId);
      if (clientStatus == null) {
        clientStatus = new ClientStatus(clientId);
        clientStatuses.put(clientId, clientStatus);
      }

      long startTime = System.currentTimeMillis();
      long stopTime = startTime + timeout;
      while (clientStatus.currentTestBlockResults != null) {
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
      return currentBlock;
    }
  }

  public void reportFatalLaunch(String clientId, JUnitResult result) {
    // Fatal launch error, cause this client to fail the whole block.
    Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
    for (TestInfo testInfo : currentBlock) {
      results.put(testInfo, result);
    }
    reportResults(clientId, results);
  }

  /**
   * Called by the servlet to report the results of the last test to run.
   * 
   * @param results the result of running the test block
   */
  public void reportResults(String clientId, Map<TestInfo, JUnitResult> results) {
    synchronized (clientStatusesLock) {
      if (results != null && !resultsMatchCurrentBlock(results)) {
        // A client is reporting results for the wrong test.
        return;
      }
      assert (results != null);
      ClientStatus clientStatus = clientStatuses.get(clientId);
      /*
       * Unknown client, but valid testInfo; this can happen if the client's
       * module fails to load.
       */
      if (clientStatus == null) {
        clientStatus = new ClientStatus(clientId);
        clientStatuses.put(clientId, clientStatus);
      }
      clientStatus.currentTestBlockResults = results;
      clientStatusesLock.notifyAll();
    }
  }

  /**
   * Gets a human-readable string.
   * 
   * @return Fetches a human-readable representation of the current test object
   */
  String getCurrentTestName() {
    if (currentBlock == null) {
      return "(no test)";
    }
    return currentBlock[0].toString();
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
  Map<TestInfo, Map<String, JUnitResult>> getResults() {
    synchronized (clientStatusesLock) {
      /*
       * All this overly complicated piece of code does is transform mappings
       * keyed by clientId into mappings keyed by TestInfo.
       */
      Map<TestInfo, Map<String, JUnitResult>> result = new HashMap<TestInfo, Map<String, JUnitResult>>();
      for (ClientStatus clientStatus : clientStatuses.values()) {
        for (Entry<TestInfo, JUnitResult> entry : clientStatus.currentTestBlockResults.entrySet()) {
          TestInfo testInfo = entry.getKey();
          JUnitResult clientResultForThisTest = entry.getValue();
          Map<String, JUnitResult> targetMap = result.get(testInfo);
          if (targetMap == null) {
            targetMap = new HashMap<String, JUnitResult>();
            result.put(testInfo, targetMap);
          }
          targetMap.put(clientStatus.clientId, clientResultForThisTest);
        }
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
   * Returns a human-formatted message identifying what clients have connected
   * but have not yet reported results for this test. It is used in a timeout
   * condition, to identify what we're still waiting on.
   * 
   * @return human readable message
   */
  String getWorkingClients() {
    synchronized (clientStatusesLock) {
      StringBuilder buf = new StringBuilder();
      int itemCount = 0;
      for (ClientStatus clientStatus : clientStatuses.values()) {
        if (clientStatus.hasRequestedCurrentTest
            && clientStatus.currentTestBlockResults == null) {
          if (itemCount > 0) {
            buf.append(", ");
          }
          buf.append(clientStatus.clientId);
          ++itemCount;
        }
      }
      int difference = numClients - itemCount;
      if (difference > 0) {
        if (itemCount > 0) {
          buf.append('\n');
        }
        buf.append(difference
            + " other client(s) haven't responded back to JUnitShell since the start of the test.");
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
        if (clientStatus.currentTestBlockResults == null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Called by the shell to set the next test to run.
   */
  void setNextTestBlock(TestInfo[] testBlock) {
    synchronized (clientStatusesLock) {
      this.currentBlock = testBlock;
      for (ClientStatus clientStatus : clientStatuses.values()) {
        clientStatus.hasRequestedCurrentTest = false;
        clientStatus.currentTestBlockResults = null;
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

  private boolean resultsMatchCurrentBlock(Map<TestInfo, JUnitResult> results) {
    assert results.size() == currentBlock.length;
    for (TestInfo testInfo : currentBlock) {
      if (!results.containsKey(testInfo)) {
        return false;
      }
    }
    return true;
  }
}
