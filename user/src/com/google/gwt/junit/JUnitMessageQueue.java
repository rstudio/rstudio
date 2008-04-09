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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
   * Tracks which test each client is requesting.
   * 
   * Key = client-id (e.g. agent+host) Value = the index of the current
   * requested test
   */
  private Map<String, Integer> clientTestRequests = new HashMap<String, Integer>();

  /**
   * The index of the current test being executed.
   */
  private int currentTestIndex = -1;

  /**
   * The number of TestCase clients executing in parallel.
   */
  private int numClients = 1;

  /**
   * The lock used to synchronize access around testMethod, clientTestRequests,
   * and currentTestIndex.
   */
  private Object readTestLock = new Object();

  /**
   * The lock used to synchronize access around testResult.
   */
  private Object resultsLock = new Object();

  /**
   * The name of the test class to execute.
   */
  private String testClass;

  /**
   * The name of the test method to execute.
   */
  private String testMethod;

  /**
   * The name of the module to execute.
   */
  private String testModule;

  /**
   * The results for the current test method.
   */
  private List<JUnitResult> testResult = new ArrayList<JUnitResult>();

  /**
   * Creates a message queue with one client.
   * 
   * @see JUnitMessageQueue#JUnitMessageQueue(int)
   */
  JUnitMessageQueue() {
  }

  /**
   * Only instantiable within this package.
   * 
   * @param numClients The number of parallel clients being served by this
   *          queue.
   */
  JUnitMessageQueue(int numClients) {
    this.numClients = numClients;
  }

  /**
   * Called by the servlet to query for for the next method to test.
   * 
   * @param moduleName the name of the executing module
   * @param timeout how long to wait for an answer
   * @return the next test to run, or <code>null</code> if
   *         <code>timeout</code> is exceeded or the next test does not match
   *         <code>testClassName</code>
   */
  public TestInfo getNextTestInfo(String clientId, String moduleName,
      long timeout) {
    synchronized (readTestLock) {
      long startTime = System.currentTimeMillis();
      long stopTime = System.currentTimeMillis() + timeout;
      while (!testIsAvailableFor(clientId, moduleName)) {
        long timeToWait = stopTime - System.currentTimeMillis();
        if (timeToWait < 1) {
          return null;
        }
        try {
          readTestLock.wait(timeToWait);
        } catch (InterruptedException e) {
          double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
          throw new TimeoutException("The servlet did not respond to the "
              + "next query to test within "
              + timeout + "ms.\n" + " Module Name: " + moduleName + "\n"
              + " Client id: " + clientId + "\n"
              + " Actual time elapsed: " + elapsed + " seconds.\n");
        }
      }

      if (!moduleName.equals(testModule)) {
        // it's an old client that is now done
        return null;
      }

      bumpClientTestRequest(clientId);
      return new TestInfo(testClass, testMethod);
    }
  }

  /**
   * Called by the servlet to report the results of the last test to run.
   * 
   * @param moduleName the name of the test module
   * @param results the result of running the test
   */
  public void reportResults(String moduleName, JUnitResult results) {
    synchronized (resultsLock) {
      if (!moduleName.equals(testModule)) {
        // an old client is trying to report results, do nothing
        return;
      }
      testResult.add(results);
    }
  }

  /**
   * Fetches the results of a completed test.
   * 
   * @param moduleName the name of the test module
   * @return An getException thrown from a failed test, or <code>null</code>
   *         if the test completed without error.
   */
  List<JUnitResult> getResults(String moduleName) {
    assert (moduleName.equals(testModule));
    return testResult;
  }

  /**
   * Returns a pretty printed list of clients that have not retrieved the
   * current test. Used for error reporting.
   * 
   * @return a string containing the list of clients that have not retrieved the
   *         current test.
   */
  String getUnretrievedClients() {
    int lineCount = 0;
    StringBuilder buf = new StringBuilder();
    synchronized (readTestLock) {
      Set<String> keys = clientTestRequests.keySet();

      for (String key : keys) {
        if (lineCount > 0) {
          buf.append('\n');
        }
        
        if (clientTestRequests.get(key) <= currentTestIndex) {
          buf.append(" - NO RESPONSE: ");
          buf.append(key);
        } else {
          buf.append(" - (ok): ");
          buf.append(key);
        }
        lineCount++;        
      }
      int difference = numClients - keys.size();
      if (difference > 0) {
        if (lineCount > 0) {
          buf.append('\n');
        }
        buf.append(" - " + difference + " client(s) haven't responded back to the servlet at all since the start of the test.");
      }
    }
    return buf.toString();
  }

  /**
   * Called by the shell to see if the currently-running test has completed.
   * 
   * @param moduleName the name of the test module
   * @return If the test has completed, <code>true</code>, otherwise
   *         <code>false</code>.
   */
  boolean hasResult(String moduleName) {
    synchronized (resultsLock) {
      assert (moduleName.equals(testModule));
      return testResult.size() == numClients;
    }
  }

  /**
   * Returns <code>true</code> if all clients have requested the
   * currently-running test.
   */
  boolean haveAllClientsRetrievedCurrentTest() {
    synchronized (readTestLock) {
      // If a client hasn't yet been contacted, it will have no entry
      Collection<Integer> clientIndices = clientTestRequests.values();
      if (clientIndices.size() < numClients) {
        return false;
      }
      // Every client must have been bumped PAST the current test index
      for (Integer value : clientIndices) {
        if (value <= currentTestIndex) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Called by the shell to set the name of the next method to run for this test
   * class.
   * 
   * @param testModule the name of the module to be run.
   * @param testClass The name of the test class.
   * @param testMethod The name of the method to run.
   */
  void setNextTestName(String testModule, String testClass, String testMethod) {
    synchronized (readTestLock) {
      this.testModule = testModule;
      this.testClass = testClass;
      this.testMethod = testMethod;
      ++currentTestIndex;
      testResult = new ArrayList<JUnitResult>(numClients);
      readTestLock.notifyAll();
    }
  }

  /**
   * Sets the number of clients that will be executing the JUnit tests in
   * parallel.
   * 
   * @param numClients must be > 0
   */
  void setNumClients(int numClients) {
    this.numClients = numClients;
  }

  // This method requires that readTestLock is being held for the duration.
  private void bumpClientTestRequest(String clientId) {
    Integer index = clientTestRequests.get(clientId);
    clientTestRequests.put(clientId, index + 1);
  }

  // This method requires that readTestLock is being held for the duration.
  private boolean testIsAvailableFor(String clientId, String moduleName) {
    if (!moduleName.equals(testModule)) {
      // the "null" test is always available for an old client
      return true;
    }
    Integer index = clientTestRequests.get(clientId);
    if (index == null) {
      index = 0;
      clientTestRequests.put(clientId, index);
    }
    return index == currentTestIndex;
  }
}
