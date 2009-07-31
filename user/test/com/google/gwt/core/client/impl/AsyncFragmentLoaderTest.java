/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadErrorHandler;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadingStrategy;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.Logger;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Tests the fragment loader. */
public class AsyncFragmentLoaderTest extends TestCase {
  private static class MockErrorHandler implements LoadErrorHandler {
    private boolean wasCalled = false;

    public boolean getWasCalled() {
      return wasCalled;
    }

    public void loadFailed(Throwable reason) {
      wasCalled = true;
    }
  }

  private static class MockLoadStrategy implements LoadingStrategy {
    public final Map<Integer, LoadErrorHandler> errorHandlers = new HashMap<Integer, LoadErrorHandler>();
    private List<Integer> loadRequests = new LinkedList<Integer>();

    public void assertFragmentsRequested(int... expectedAry) {
      List<Integer> actual = new ArrayList<Integer>(loadRequests);
      loadRequests.clear();
      List<Integer> expected = toList(expectedAry);

      if (!sameContents(actual, expected)) {
        fail("Expected= " + commaSeparated(expected) + "; actual="
            + commaSeparated(actual));
      }
    }

    public void startLoadingFragment(int fragment,
        LoadErrorHandler loadErrorHandler) {
      errorHandlers.put(fragment, loadErrorHandler);
      loadRequests.add(fragment);
    }

    private String commaSeparated(List<Integer> ary) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (Integer x : ary) {
        if (first) {
          first = false;
        } else {
          sb.append(",");
        }
        sb.append(x);
      }
      return sb.toString();
    }

    private boolean sameContents(List<Integer> actual, List<Integer> expected) {
      if (actual.size() != expected.size()) {
        return false;
      }
      for (int i = 0; i < actual.size(); i++) {
        if (!actual.get(i).equals(expected.get(i))) {
          return false;
        }
      }
      return true;
    }

    private List<Integer> toList(int[] ary) {
      List<Integer> list = new ArrayList<Integer>();
      for (int i = 0; i < ary.length; i++) {
        list.add(ary[i]);
      }
      return list;
    }
  }

  private static final LoadErrorHandler NULL_ERROR_HANDLER = new LoadErrorHandler() {
    public void loadFailed(Throwable reason) {
    }
  };

  private static final Logger NULL_LOGGER = new Logger() {
    public void logEventProgress(String eventGroup, String type,
        Integer fragment, Integer size) {
    }
  };

  private static Throwable makeLoadFailedException() {
    return new RuntimeException("Load Failed");
  }

  public void testBasics() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    int numEntries = 5;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries,
        new int[] {}, reqs, NULL_LOGGER);

    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(numEntries);
    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(1);
    loader.fragmentHasLoaded(1);

    loader.inject(2, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(2);
    loader.fragmentHasLoaded(2);
  }

  /**
   * Check the behavior when there are download failures.
   */
  public void testDownloadFailures() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, NULL_LOGGER);

    // request fragment 1
    MockErrorHandler error1try1 = new MockErrorHandler();
    loader.inject(1, error1try1);
    reqs.assertFragmentsRequested(1);

    // fragment 1 fails
    loadFailed(reqs, 1);
    assertTrue(error1try1.getWasCalled());

    // try again on fragment 1
    MockErrorHandler error1try2 = new MockErrorHandler();
    loader.inject(1, error1try2);
    reqs.assertFragmentsRequested(1);

    // this time fragment 1 succeeds
    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested();
    assertFalse(error1try2.getWasCalled());

    // request a later initial fragment (3), and see what happens if an
    // intermediate download fails
    MockErrorHandler error3try1 = new MockErrorHandler();
    loader.inject(3, error3try1);
    reqs.assertFragmentsRequested(2);

    loadFailed(reqs, 2);
    assertTrue(error3try1.wasCalled);

    // request both 3 and 5, an see what happens if
    // the leftovers download fails
    MockErrorHandler error3try2 = new MockErrorHandler();
    MockErrorHandler error5try1 = new MockErrorHandler();
    loader.inject(3, error3try2);
    loader.inject(5, error5try1);
    reqs.assertFragmentsRequested(2);

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(3);

    loader.fragmentHasLoaded(3);
    reqs.assertFragmentsRequested(numEntries);

    loadFailed(reqs, numEntries); // leftovers fails!
    assertFalse(error3try2.getWasCalled()); // 3 should have succeeded
    assertTrue(error5try1.getWasCalled()); // 5 failed
    reqs.errorHandlers.get(numEntries);

    // now try 5 again, and have everything succeed
    MockErrorHandler error5try2 = new MockErrorHandler();
    loader.inject(5, error5try2);
    reqs.assertFragmentsRequested(numEntries);

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(5);

    loader.fragmentHasLoaded(5);
    reqs.assertFragmentsRequested();
    assertFalse(error5try2.getWasCalled());
  }

  /**
   * If only the first part of the initial load sequence is requested, then
   * don't request more.
   */
  public void testLoadingPartOfInitialSequence() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, NULL_LOGGER);

    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(1);

    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested(); // should stop

    loader.inject(2, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(2);

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(); // again, should stop

    loader.inject(3, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(3);

    loader.fragmentHasLoaded(3);
    reqs.assertFragmentsRequested(numEntries); // last initial, so it should
    // request the leftovers

    loader.fragmentHasLoaded(numEntries);
    reqs.assertFragmentsRequested();

    // check that exclusives now load
    loader.inject(5, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(5);

    loader.fragmentHasLoaded(5);
    reqs.assertFragmentsRequested();
  }

  /**
   * This test catches a case in an earlier version of AsyncFragmentLoader where
   * AsyncFragmentLoader.waitingForInitialFragments could exhaust its available
   * space.
   */
  public void testOverflowInWaitingForInitialFragments() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, NULL_LOGGER);

    /*
     * Repeatedly queue up extra downloads waiting on an initial and then fail.
     */
    for (int i = 0; i < 10; i++) {
      MockErrorHandler error = new MockErrorHandler();
      loader.inject(4, error);
      reqs.assertFragmentsRequested(1);

      loadFailed(reqs, 1);
      assertTrue(error.getWasCalled());
    }
  }

  /**
   * A thorough exercise of loading with an initial load sequence specified.
   */
  public void testWithInitialLoadSequence() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, NULL_LOGGER);

    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(1);

    loader.inject(3, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(); // still waiting on fragment 1

    loader.inject(5, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(); // still waiting on fragment 1

    // say that 1 loads, which should trigger a chain of backlogged downloads
    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested(2); // next initial

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(3); // next initial

    loader.fragmentHasLoaded(3);
    reqs.assertFragmentsRequested(numEntries); // leftovers

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(5);

    loader.fragmentHasLoaded(5);
    reqs.assertFragmentsRequested(); // quiescent

    // check that new exclusive fragment requests work
    loader.inject(4, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(4);
    loader.fragmentHasLoaded(4);
    reqs.assertFragmentsRequested();
  }

  private void loadFailed(MockLoadStrategy reqs, int fragment) {
    reqs.errorHandlers.get(fragment).loadFailed(makeLoadFailedException());
  }
}
