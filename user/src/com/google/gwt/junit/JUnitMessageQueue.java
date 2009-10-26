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

import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.client.impl.JUnitHost.TestBlock;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public boolean isNew = true;
    public int blockIndex = 0;

    public ClientStatus(String clientId) {
      this.clientId = clientId;
    }
  }

  /**
   * Records results for each client; must lock before accessing.
   */
  private final Map<String, ClientStatus> clientStatuses = new HashMap<String, ClientStatus>();

  /**
   * A set of the GWT user agents (eg. ie6, gecko) that have connected. 
   */
  private final Set<String> userAgents = new HashSet<String>();

  /**
   * The lock used to synchronize access to clientStatuses.
   */
  private final Object clientStatusesLock = new Object();

  /**
   * The number of TestCase clients executing in parallel.
   */
  private final int numClients;

  /**
   * Maps the TestInfo to the results from each clientId. If JUnitResult is
   * null, it means that the client requested the test but did not report the
   * results yet.
   */
  private final Map<TestInfo, Map<String, JUnitResult>> testResults = new HashMap<TestInfo, Map<String, JUnitResult>>();

  /**
   * The list of test blocks to run.
   */
  private final List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>();

  /**
   * Set to true when the last test block has been added. This is used to tell
   * clients that all tests are complete.
   */
  private boolean isLastTestBlockAvailable;

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
   * @param clientId the ID of the client
   * @param userAgent the user agent property of the client
   * @param blockIndex the index of the test block to get
   * @param timeout how long to wait for an answer
   * @return the next test to run, or <code>null</code> if <code>timeout</code>
   *         is exceeded or the next test does not match
   *         <code>testClassName</code>
   */
  public TestBlock getTestBlock(String clientId, String userAgent,
      int blockIndex, long timeout) throws TimeoutException {
    synchronized (clientStatusesLock) {
      userAgents.add(userAgent);
      ClientStatus clientStatus = ensureClientStatus(clientId);
      clientStatus.blockIndex = blockIndex;

      // The client has finished all of the tests.
      if (isLastTestBlockAvailable && blockIndex >= testBlocks.size()) {
        return null;
      }

      long startTime = System.currentTimeMillis();
      long stopTime = startTime + timeout;
      while (blockIndex >= testBlocks.size()) {
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

      // Record that this client has retrieved the current tests.
      TestInfo[] tests = testBlocks.get(blockIndex);
      for (TestInfo testInfo : tests) {
        ensureResults(testInfo).put(clientId, null);
      }
      return new TestBlock(tests, blockIndex);
    }
  }

  public void reportFatalLaunch(String clientId, String userAgent,
      JUnitResult result) {
    // Fatal launch error, cause this client to fail the whole block.
    ClientStatus clientStatus = ensureClientStatus(clientId);
    Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
    for (TestInfo testInfo : testBlocks.get(clientStatus.blockIndex)) {
      results.put(testInfo, result);
    }
    reportResults(clientId, userAgent, results);
  }

  /**
   * Called by the servlet to report the results of the last test to run.
   * 
   * @param clientId the ID of the client
   * @param userAgent the user agent property of the client
   * @param results the result of running the test block
   */
  public void reportResults(String clientId, String userAgent,
      Map<TestInfo, JUnitResult> results) {
    synchronized (clientStatusesLock) {
      if (results == null) {
        throw new IllegalArgumentException("results cannot be null");
      }
      userAgents.add(userAgent);
      ensureClientStatus(clientId);

      // Cache the test results.
      for (Map.Entry<TestInfo, JUnitResult> entry : results.entrySet()) {
        TestInfo testInfo = entry.getKey();
        ensureResults(testInfo).put(clientId, entry.getValue());
      }

      clientStatusesLock.notifyAll();
    }
  }

  /**
   * Called by the shell to add test blocks to test.
   * 
   * @param isLastBlock true if this is the last test block that will be added
   */
  void addTestBlocks(List<TestInfo[]> newTestBlocks, boolean isLastBlock) {
    synchronized (clientStatusesLock) {
      if (isLastTestBlockAvailable) {
        throw new IllegalArgumentException(
            "Cannot add test blocks after the last block is added");
      }
      for (TestInfo[] testBlock : newTestBlocks) {
        if (testBlock.length == 0) {
          throw new IllegalArgumentException("TestBlocks cannot be empty");
        }
      }
      testBlocks.addAll(newTestBlocks);
      if (isLastBlock) {
        isLastTestBlockAvailable = true;
      }
      clientStatusesLock.notifyAll();
    }
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
      clientStatusesLock.notifyAll();
      return results.toArray(new String[results.size()]);
    }
  }

  /**
   * Returns how many clients have requested the currently-running test.
   * 
   * @param testInfo the {@link TestInfo} that the clients retrieved
   */
  int getNumClientsRetrievedTest(TestInfo testInfo) {
    synchronized (clientStatusesLock) {
      int count = 0;
      Map<String, JUnitResult> results = testResults.get(testInfo);
      if (results != null) {
        count = results.size();
      }
      return count;
    }
  }

  /**
   * Returns how many clients have connected.
   */
  int getNumConnectedClients() {
    synchronized (clientStatusesLock) {
      return clientStatuses.size();
    }
  }

  /**
   * Fetches the results of a completed test.
   * 
   * @param testInfo the {@link TestInfo} to check for results
   * @return A map of results from all clients.
   */
  Map<String, JUnitResult> getResults(TestInfo testInfo) {
    synchronized (clientStatusesLock) {
      return testResults.get(testInfo);
    }
  }

  /**
   * Visible for testing.
   * 
   * @return the test blocks
   */
  List<TestInfo[]> getTestBlocks() {
    return testBlocks;
  }

  /**
   * Returns a pretty printed list of clients that have not retrieved the
   * current test. Used for error reporting.
   * 
   * @param testInfo the {@link TestInfo} we are waiting for
   * @return a string containing the list of clients that have not retrieved the
   *         current test.
   */
  String getUnretrievedClients(TestInfo testInfo) {
    synchronized (clientStatusesLock) {
      Map<String, JUnitResult> results = testResults.get(testInfo);
      StringBuilder buf = new StringBuilder();
      int lineCount = 0;
      for (ClientStatus clientStatus : clientStatuses.values()) {
        if (lineCount > 0) {
          buf.append('\n');
        }

        if (results == null || !results.containsKey(clientStatus.clientId)) {
          buf.append(" - NO RESPONSE: ");
        } else {
          buf.append(" - (ok): ");
        }
        buf.append(clientStatus.clientId);
        ++lineCount;
      }
      int difference = numClients - getNumClientsRetrievedTest(testInfo);
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
   * Returns a list of all user agents that have connected.
   */
  String[] getUserAgents() {
    synchronized (clientStatusesLock) {
      return userAgents.toArray(new String[userAgents.size()]);
    }
  }

  /**
   * Returns a human-formatted message identifying what clients have connected
   * but have not yet reported results for this test. It is used in a timeout
   * condition, to identify what we're still waiting on.
   * 
   * @param testInfo the {@link TestInfo} that the clients are working on
   * @return human readable message
   */
  String getWorkingClients(TestInfo testInfo) {
    synchronized (clientStatusesLock) {
      // Print a list of clients that have connected but not returned results.
      int itemCount = 0;
      StringBuilder buf = new StringBuilder();
      Map<String, JUnitResult> results = testResults.get(testInfo);
      if (results != null) {
        for (Map.Entry<String, JUnitResult> entry : results.entrySet()) {
          if (entry.getValue() == null) {
            buf.append(entry.getKey());
            buf.append("\n");
            itemCount++;
          }
        }
      }

      // Print the number of other clients.
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
   * @param testInfo the {@link TestInfo} to check for results
   * @return If the test has completed, <code>true</code>, otherwise
   *         <code>false</code>.
   */
  boolean hasResults(TestInfo testInfo) {
    synchronized (clientStatusesLock) {
      Map<String, JUnitResult> results = testResults.get(testInfo);
      if (results == null || results.size() < numClients) {
        return false;
      }
      for (JUnitResult result : results.values()) {
        if (result == null) {
          return false;
        }
      }
      return true;
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

  /**
   * Ensure that a {@link ClientStatus} for the clientId exists. 
   * 
   * @param clientId the id of the client
   * @return the {@link ClientStatus} for the client
   */
  private ClientStatus ensureClientStatus(String clientId) {
    ClientStatus clientStatus = clientStatuses.get(clientId);
    if (clientStatus == null) {
      clientStatus = new ClientStatus(clientId);
      clientStatuses.put(clientId, clientStatus);
    }
    return clientStatus;
  }
  
  /**
   * Get the map of test results from all clients for a given {@link TestInfo},
   * creating it if necessary.
   * 
   * @param testInfo the {@link TestInfo}
   * @return the map of all results
   */
  private Map<String, JUnitResult> ensureResults(TestInfo testInfo) {
    Map<String, JUnitResult> results = testResults.get(testInfo);
    if (results == null) {
      results = new HashMap<String, JUnitResult>();
      testResults.put(testInfo, results);
    }
    return results;
  }
}
