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
package com.google.gwt.junit;

import java.util.HashMap;
import java.util.Map;

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
   * Maps the name of a test class to the method that should be run. Access must
   * be synchronized.
   */
  private final Map nameMap = new HashMap();

  /**
   * Maps the name of a test class to the last results to be reported. Access
   * must be synchronized.
   */
  private final Map resultsMap = new HashMap();

  /**
   * Only instantiatable within this package.
   */
  JUnitMessageQueue() {
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
  public String getNextTestName(String testClassName, long timeout) {
    synchronized (nameMap) {
      long stopTime = System.currentTimeMillis() + timeout;
      while (!nameMap.containsKey(testClassName)) {
        long timeToWait = stopTime - System.currentTimeMillis();
        if (timeToWait < 1) {
          return null;
        }
        try {
          nameMap.wait(timeToWait);
        } catch (InterruptedException e) {
          // just abort
          return null;
        }
      }

      return (String) nameMap.remove(testClassName);
    }
  }

  /**
   * Called by the servlet to report the results of the last test to run.
   * 
   * @param testClassName The name of the test class.
   * @param t The exception thrown by the last test, or <code>null</code> if
   *          the test completed without error.
   */
  public void reportResults(String testClassName, Throwable t) {
    synchronized (resultsMap) {
      resultsMap.put(testClassName, t);
    }
  }

  /**
   * Called by the shell to fetch the results of a completed test.
   * 
   * @param testClassName The name of the test class.
   * @return An exception thrown from a failed test, or <code>null</code> if
   *         the test completed without error.
   */
  Throwable getResult(String testClassName) {
    synchronized (resultsMap) {
      return (Throwable) resultsMap.remove(testClassName);
    }
  }

  /**
   * Called by the shell to see if the servlet has begun running the current
   * test.
   * 
   * @param testClassName
   * @return <code>true</code> if the servlet has not yet fetched the next
   *         test name, otherwise <code>false</code>.
   */
  boolean hasNextTestName(String testClassName) {
    synchronized (nameMap) {
      return nameMap.containsKey(testClassName);
    }
  }

  /**
   * Called by the shell to see if the currently-running test has completed.
   * 
   * @param testClassName The name of the test class.
   * @return If the test has completed, <code>true</code>, otherwise
   *         <code>false</code>.
   */
  boolean hasResult(String testClassName) {
    synchronized (resultsMap) {
      return resultsMap.containsKey(testClassName);
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
    synchronized (nameMap) {
      nameMap.put(testClassName, testName);
      nameMap.notifyAll();
    }
  }
}