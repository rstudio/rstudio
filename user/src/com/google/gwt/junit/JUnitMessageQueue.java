/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.junit.client.TestResults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
   * Tracks which test each client is requesting.
   * 
   * Key = client-id (e.g. agent+host) Value = the index of the current
   * requested test
   */
  private Map/* <String,Integer> */clientTestRequests = new HashMap/* <String,Integer> */();

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
   * The lock used to synchronize access around testResults.
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
   * The results for the current test method.
   */
  private List/* <TestResults> */testResults = new ArrayList/* <TestResults> */();

  /**
   * Creates a message queue with one client.
   * 
   * @see JUnitMessageQueue#JUnitMessageQueue(int)
   */
  JUnitMessageQueue() {
  }

  /**
   * Only instantiatable within this package.
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
   * @param testClassName The name of the test class.
   * @param timeout How long to wait for an answer.
   * @return The next test to run, or <code>null</code> if
   *         <code>timeout</code> is exceeded or the next test does not match
   *         <code>testClassName</code>.
   */
  public String getNextTestName(String clientId, String testClassName,
      long timeout) {
    synchronized (readTestLock) {
      long stopTime = System.currentTimeMillis() + timeout;
      while (!testIsAvailableFor(clientId, testClassName)) {
        long timeToWait = stopTime - System.currentTimeMillis();
        if (timeToWait < 1) {
          return null;
        }
        try {
          readTestLock.wait(timeToWait);
        } catch (InterruptedException e) {
          // just abort
          return null;
        }
      }

      if (!testClassName.equals(testClass)) {
        // it's an old client that is now done
        return null;
      }

      bumpClientTestRequest(clientId);
      return testMethod;
    }
  }

  /**
   * Called by the servlet to report the results of the last test to run.
   * 
   * @param testClassName The name of the test class.
   * @param results The result of running the test.
   */
  public void reportResults(String testClassName, TestResults results) {
    synchronized (resultsLock) {
      if (!testClassName.equals(testClass)) {
        // an old client is trying to report results, do nothing
        return;
      }
      testResults.add(results);
    }
  }

  /**
   * Fetches the results of a completed test.
   * 
   * @param testClassName The name of the test class.
   * @return An getException thrown from a failed test, or <code>null</code>
   *         if the test completed without error.
   */
  List/* <TestResults> */getResults(String testClassName) {
    assert (testClassName.equals(testClass));
    return testResults;
  }

  /**
   * Called by the shell to see if the currently-running test has completed.
   * 
   * @param testClassName The name of the test class.
   * @return If the test has completed, <code>true</code>, otherwise
   *         <code>false</code>.
   */
  boolean hasResult(String testClassName) {
    synchronized (resultsLock) {
      assert (testClassName.equals(testClass));
      return testResults.size() == numClients;
    }
  }

  /**
   * Returns <code>true</code> if all clients have requested the
   * currently-running test.
   */
  boolean haveAllClientsRetrievedCurrentTest() {
    synchronized (readTestLock) {
      // If a client hasn't yet contacted, it will have no entry
      Collection clientIndices = clientTestRequests.values();
      if (clientIndices.size() < numClients) {
        return false;
      }
      // Every client must have been bumped PAST the current test index
      for (Iterator it = clientIndices.iterator(); it.hasNext();) {
        Integer value = (Integer) it.next();
        if (value.intValue() <= currentTestIndex) {
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
   * @param testClassName The name of the test class.
   * @param testName The name of the method to run.
   */
  void setNextTestName(String testClassName, String testName) {
    synchronized (readTestLock) {
      testClass = testClassName;
      testMethod = testName;
      ++currentTestIndex;
      testResults = new ArrayList/* <TestResults> */(numClients);
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
    Integer index = (Integer) clientTestRequests.get(clientId);
    clientTestRequests.put(clientId, new Integer(index.intValue() + 1));
  }

  // This method requires that readTestLock is being held for the duration.
  private boolean testIsAvailableFor(String clientId, String testClassName) {
    if (!testClassName.equals(testClass)) {
      // the "null" test is always available for an old client
      return true;
    }
    Integer index = (Integer) clientTestRequests.get(clientId);
    if (index == null) {
      index = new Integer(0);
      clientTestRequests.put(clientId, index);
    }
    return index.intValue() == currentTestIndex;
  }
}
