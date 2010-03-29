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

import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadTerminatedHandler;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadingStrategy;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.Logger;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Tests that the fragment loader requests the right fragments and logs the
 * correct lightweight metrics under a variety of request patterns.
 */
public class AsyncFragmentLoaderTest extends TestCase {
  private static class MockErrorHandler implements LoadTerminatedHandler {
    private boolean wasCalled = false;

    public boolean getWasCalled() {
      return wasCalled;
    }

    public void loadTerminated(Throwable reason) {
      wasCalled = true;
    }
  }

  private static class MockLoadStrategy implements LoadingStrategy {
    public final Map<Integer, LoadTerminatedHandler> errorHandlers = new HashMap<Integer, LoadTerminatedHandler>();
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
        LoadTerminatedHandler loadErrorHandler) {
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

  private static class MockProgressEvent {
    public final String eventGroup;
    public final int fragment;
    public final String type;

    public MockProgressEvent(String eventGroup, String type, int fragment) {
      this.eventGroup = eventGroup;
      this.type = type;
      this.fragment = fragment;
    }
  }

  private static class MockProgressLogger implements Logger {
    private Queue<MockProgressEvent> events = new LinkedList<MockProgressEvent>();

    public void assertEvent(String eventGroup, String type, int fragment) {
      MockProgressEvent event = events.remove();
      assertEquals(eventGroup, event.eventGroup);
      assertEquals(type, event.type);
      assertEquals(fragment, event.fragment);
    }

    public void assertNoEvents() {
      assertTrue("Expected no more progress events, but there are "
          + events.size(), events.size() == 0);
    }

    public void logEventProgress(String eventGroup, String type,
        int fragment, int size) {
      events.add(new MockProgressEvent(eventGroup, type, fragment));
    }
  }

  private static final String BEGIN = "begin";
  private static final String END = "end";
  private static final String LEFTOVERS_DOWNLOAD = "leftoversDownload";

  private static final LoadTerminatedHandler NULL_ERROR_HANDLER = new LoadTerminatedHandler() {
    public void loadTerminated(Throwable reason) {
    }
  };

  private static Throwable makeLoadFailedException() {
    return new RuntimeException("Load Failed");
  }

  public void testBasics() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 5;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries,
        new int[] {}, reqs, progress);

    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent(LEFTOVERS_DOWNLOAD, BEGIN, numEntries);

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(1);
    progress.assertEvent(LEFTOVERS_DOWNLOAD, END, numEntries);
    progress.assertEvent("download1", BEGIN, 1);

    loader.fragmentHasLoaded(1);
    progress.assertEvent("download1", END, 1);

    loader.inject(2, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download2", BEGIN, 2);

    loader.fragmentHasLoaded(2);
    progress.assertEvent("download2", END, 2);

    progress.assertNoEvents();
  }

  /**
   * Check the behavior when there are download failures.
   */
  public void testDownloadFailures() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 10;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, progress);

    // request fragment 1
    MockErrorHandler error1try1 = new MockErrorHandler();
    loader.inject(1, error1try1);
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("download1", BEGIN, 1);

    // fragment 1 fails
    loadFailed(reqs, 1);
    assertTrue(error1try1.getWasCalled());

    // try again on fragment 1
    MockErrorHandler error1try2 = new MockErrorHandler();
    loader.inject(1, error1try2);
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("download1", BEGIN, 1);

    // this time fragment 1 succeeds
    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested();
    assertFalse(error1try2.getWasCalled());
    progress.assertEvent("download1", END, 1);

    // request a later initial fragment (3), and see what happens if an
    // intermediate download fails
    MockErrorHandler error3try1 = new MockErrorHandler();
    loader.inject(3, error3try1);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download2", BEGIN, 2);

    loadFailed(reqs, 2);
    assertTrue(error3try1.wasCalled);

    // request both 3 and 5, and see what happens if
    // the leftovers download fails
    MockErrorHandler error3try2 = new MockErrorHandler();
    MockErrorHandler error5try1 = new MockErrorHandler();
    loader.inject(3, error3try2);
    loader.inject(5, error5try1);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download2", BEGIN, 2);

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(3);
    progress.assertEvent("download2", END, 2);
    progress.assertEvent("download3", BEGIN, 3);

    loader.fragmentHasLoaded(3);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent("download3", END, 3);
    progress.assertEvent(LEFTOVERS_DOWNLOAD, BEGIN, numEntries);

    loadFailed(reqs, numEntries); // leftovers fails!
    assertFalse(error3try2.getWasCalled()); // 3 should have succeeded
    assertTrue(error5try1.getWasCalled()); // 5 failed

    // now try 5 again, and have everything succeed
    MockErrorHandler error5try2 = new MockErrorHandler();
    loader.inject(5, error5try2);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent(LEFTOVERS_DOWNLOAD, BEGIN, numEntries);

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(5);
    progress.assertEvent(LEFTOVERS_DOWNLOAD, END, numEntries);
    progress.assertEvent("download5", BEGIN, 5);

    loader.fragmentHasLoaded(5);
    reqs.assertFragmentsRequested();
    assertFalse(error5try2.getWasCalled());
    progress.assertEvent("download5", END, 5);

    // try 6 but have it fail
    MockErrorHandler error6try1 = new MockErrorHandler();
    loader.inject(6, error6try1);
    reqs.assertFragmentsRequested(6);
    progress.assertEvent("download6", BEGIN, 6);

    loadFailed(reqs, 6);
    assertTrue(error6try1.getWasCalled());

    // try 7 and have it succeed
    loader.inject(7, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(7);
    progress.assertEvent("download7", BEGIN, 7);

    loader.fragmentHasLoaded(7);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download7", END, 7);

    // try 6 again and have it succeed this time
    MockErrorHandler error6try2 = new MockErrorHandler();
    loader.inject(6, error6try2);
    reqs.assertFragmentsRequested(6);
    progress.assertEvent("download6", BEGIN, 6);

    loader.fragmentHasLoaded(6);
    reqs.assertFragmentsRequested();
    assertFalse(error6try2.getWasCalled());
    progress.assertEvent("download6", END, 6);
    
    // a finish event should do nothing if the fragment has already succeeded
    progress.assertNoEvents();
    loadFailed(reqs, 6);
    assertFalse(error6try2.getWasCalled());
    progress.assertNoEvents();
  }

  public void testExclusivesLoadSequentially1() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries,
        new int[] {}, reqs, progress);

    // Load fragment 1
    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(numEntries); // leftovers
    progress.assertEvent("leftoversDownload", BEGIN, numEntries);
    loader.fragmentHasLoaded(numEntries);
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("leftoversDownload", END, numEntries);
    progress.assertEvent("download1", BEGIN, 1);
    loader.fragmentHasLoaded(1);
    progress.assertEvent("download1", END, 1);
    progress.assertNoEvents();

    // Request 2 and 3 immediately
    loader.inject(2, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download2", BEGIN, 2);
    loader.inject(3, NULL_ERROR_HANDLER);
    progress.assertNoEvents(); // waiting on 2 to finish

    // 2 loads, 3 should be requested
    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(3);
    progress.assertEvent("download2", END, 2);
    progress.assertEvent("download3", BEGIN, 3);
    progress.assertNoEvents();

    // 3 loads
    loader.fragmentHasLoaded(3);
    progress.assertEvent("download3", END, 3);
    progress.assertNoEvents();
  }

  public void testExclusivesLoadSequentially2() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries,
        new int[] {}, reqs, progress);

    // Request 1
    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent("leftoversDownload", BEGIN, numEntries);

    // Request 2, resulting in two fragments being queued behind the leftovers
    // download
    loader.inject(2, NULL_ERROR_HANDLER);
    progress.assertNoEvents();

    // Leftovers arrives, but only fragment 1 should initially be requested
    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("leftoversDownload", END, numEntries);
    progress.assertEvent("download1", BEGIN, 1);
    progress.assertNoEvents();

    // fragment 1 arrives, 2 requested
    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download1", END, 1);
    progress.assertEvent("download2", BEGIN, 2);

    // fragment 2 arrives, all done
    loader.fragmentHasLoaded(2);
    progress.assertEvent("download2", END, 2);
    progress.assertNoEvents();
  }

  /**
   * If only the first part of the initial load sequence is requested, then
   * don't request more.
   */
  public void testLoadingPartOfInitialSequence() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, progress);

    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("download1", BEGIN, 1);

    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested(); // should stop
    progress.assertEvent("download1", END, 1);

    loader.inject(2, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download2", BEGIN, 2);

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(); // again, should stop
    progress.assertEvent("download2", END, 2);

    loader.inject(3, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(3);
    progress.assertEvent("download3", BEGIN, 3);

    loader.fragmentHasLoaded(3);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download3", END, 3);
    progress.assertNoEvents();

    // check that exclusives now load
    loader.inject(5, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent(LEFTOVERS_DOWNLOAD, BEGIN, numEntries);

    loader.fragmentHasLoaded(numEntries);
    reqs.assertFragmentsRequested(5);
    progress.assertEvent(LEFTOVERS_DOWNLOAD, END, numEntries);
    progress.assertEvent("download5", BEGIN, 5);

    loader.fragmentHasLoaded(5);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download5", END, 5);

    progress.assertNoEvents();
  }

  /**
   * This test catches a case in an earlier version of AsyncFragmentLoader where
   * AsyncFragmentLoader.waitingForInitialFragments could exhaust its available
   * space.
   */
  public void testOverflowInWaitingForInitialFragments() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, progress);

    /*
     * Repeatedly queue up extra downloads waiting on an initial and then fail.
     */
    for (int i = 0; i < 10; i++) {
      MockErrorHandler error = new MockErrorHandler();
      loader.inject(4, error);
      reqs.assertFragmentsRequested(1);
      progress.assertEvent("download1", BEGIN, 1);

      loadFailed(reqs, 1);
      assertTrue(error.getWasCalled());
      progress.assertNoEvents();
    }
  }

  public void testPrefetch() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 20;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, progress);
    loader.startPrefetching();
    // request a prefetch of something in the initial load sequence
    loader.setPrefetchQueue(2);
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("download1", BEGIN, 1);

    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download1", END, 1);
    progress.assertEvent("download2", BEGIN, 2);

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download2", END, 2);
    progress.assertNoEvents();
    // request a prefetch of an exclusive
    loader.setPrefetchQueue(4);
    reqs.assertFragmentsRequested(3);
    progress.assertEvent("download3", BEGIN, 3);

    loader.fragmentHasLoaded(3);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent("download3", END, 3);
    progress.assertEvent("leftoversDownload", BEGIN, numEntries);

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(4);
    progress.assertEvent("leftoversDownload", END, numEntries);
    progress.assertEvent("download4", BEGIN, 4);

    loader.fragmentHasLoaded(4);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download4", END, 4);
    progress.assertNoEvents();
    // request a prefetch, but check that an inject call takes priority
    loader.setPrefetchQueue(5, 6);
    reqs.assertFragmentsRequested(5);
    progress.assertEvent("download5", BEGIN, 5);

    loader.inject(7, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested();
    progress.assertNoEvents();

    loader.fragmentHasLoaded(5);
    reqs.assertFragmentsRequested(7);
    progress.assertEvent("download5", END, 5);
    progress.assertEvent("download7", BEGIN, 7);

    loader.fragmentHasLoaded(7);
    reqs.assertFragmentsRequested(6);
    progress.assertEvent("download7", END, 7);
    progress.assertEvent("download6", BEGIN, 6);

    loader.fragmentHasLoaded(6);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download6", END, 6);
    progress.assertNoEvents();
    // request prefetches, then request different prefetches
    loader.setPrefetchQueue(8, 9);
    reqs.assertFragmentsRequested(8);
    progress.assertEvent("download8", BEGIN, 8);
    loader.setPrefetchQueue(10);
    reqs.assertFragmentsRequested();
    progress.assertNoEvents();

    loader.fragmentHasLoaded(8);
    reqs.assertFragmentsRequested(10);
    progress.assertEvent("download8", END, 8);
    progress.assertEvent("download10", BEGIN, 10);

    loader.fragmentHasLoaded(10);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download10", END, 10);
    progress.assertNoEvents();
    // request prefetches that have already been loaded
    loader.setPrefetchQueue(1, 3, 7, 10);
    reqs.assertFragmentsRequested();
    progress.assertNoEvents();
  }

  /**
   * Prefetch initial split points out of order.
   */
  public void testPrefetchInitialsOutOfOrder() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 20;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, progress);
    loader.startPrefetching();
    // request a prefetch of something in the initial load sequence
    loader.setPrefetchQueue(3, 2, 1);
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("download1", BEGIN, 1);

    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download1", END, 1);
    progress.assertEvent("download2", BEGIN, 2);

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(3);
    progress.assertEvent("download2", END, 2);
    progress.assertEvent("download3", BEGIN, 3);

    loader.fragmentHasLoaded(3);
    progress.assertEvent("download3", END, 3);
    reqs.assertFragmentsRequested();
    progress.assertNoEvents();

    // check that the loader is in a sane state by downloading an exclusive
    loader.inject(5, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent("leftoversDownload", BEGIN, numEntries);

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(5);
    progress.assertEvent("leftoversDownload", END, numEntries);
    progress.assertEvent("download5", BEGIN, 5);

    loader.fragmentHasLoaded(5);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download5", END, 5);
    progress.assertNoEvents();
  }

  /**
   * Test that no prefetching happens if prefetching is turned off.
   */
  public void testPrefetchingDisabled() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 20;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries,
        new int[] {}, reqs, progress);
    loader.stopPrefetching();
    // Prefetch 1, but leave prefetching off
    loader.setPrefetchQueue(1);
    reqs.assertFragmentsRequested();
    progress.assertNoEvents();

    // Inject 2, which should lead to leftovers and 2 loading
    loader.inject(2, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent("leftoversDownload", BEGIN, numEntries);

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("leftoversDownload", END, numEntries);
    progress.assertEvent("download2", BEGIN, 2);

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download2", END, 2);
    progress.assertNoEvents();

    // Enable prefetching; now 1 should load
    loader.startPrefetching();
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("download1", BEGIN, 1);

    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download1", END, 1);
    progress.assertNoEvents();
  }

  /**
   * Test prefetching an item and then injecting it while the prefetch is in
   * progress.
   */
  public void testPrefetchThenInjectOfSame() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 20;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries,
        new int[] {}, reqs, progress);
    loader.startPrefetching();

    // Load the leftovers and one fragment
    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(numEntries);
    progress.assertEvent("leftoversDownload", BEGIN, numEntries);

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("leftoversDownload", END, numEntries);
    progress.assertEvent("download1", BEGIN, 1);

    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download1", END, 1);
    progress.assertNoEvents();
    // Start prefetching a fragment
    loader.setPrefetchQueue(2);
    reqs.assertFragmentsRequested(2);
    progress.assertEvent("download2", BEGIN, 2);

    // Inject the same fragment and another one
    loader.inject(2, NULL_ERROR_HANDLER);
    loader.inject(3, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested();
    progress.assertNoEvents();

    // Finish the fragment loads
    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(3);
    progress.assertEvent("download2", END, 2);
    progress.assertEvent("download3", BEGIN, 3);

    loader.fragmentHasLoaded(3);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download3", END, 3);
    progress.assertNoEvents();
  }

  /**
   * A thorough exercise of loading with an initial load sequence specified.
   */
  public void testWithInitialLoadSequence() {
    MockLoadStrategy reqs = new MockLoadStrategy();
    MockProgressLogger progress = new MockProgressLogger();
    int numEntries = 6;
    AsyncFragmentLoader loader = new AsyncFragmentLoader(numEntries, new int[] {
        1, 2, 3}, reqs, progress);

    loader.inject(1, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(1);
    progress.assertEvent("download1", BEGIN, 1);

    loader.inject(3, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(); // still waiting on fragment 1
    progress.assertNoEvents();

    loader.inject(5, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(); // still waiting on fragment 1
    progress.assertNoEvents();

    // say that 1 loads, which should trigger a chain of backlogged downloads
    loader.fragmentHasLoaded(1);
    reqs.assertFragmentsRequested(2); // next initial
    progress.assertEvent("download1", END, 1);
    progress.assertEvent("download2", BEGIN, 2);

    loader.fragmentHasLoaded(2);
    reqs.assertFragmentsRequested(3); // next initial
    progress.assertEvent("download2", END, 2);
    progress.assertEvent("download3", BEGIN, 3);

    loader.fragmentHasLoaded(3);
    reqs.assertFragmentsRequested(numEntries); // leftovers
    progress.assertEvent("download3", END, 3);
    progress.assertEvent("leftoversDownload", BEGIN, numEntries);

    loader.leftoversFragmentHasLoaded();
    reqs.assertFragmentsRequested(5);
    progress.assertEvent("leftoversDownload", END, numEntries);
    progress.assertEvent("download5", BEGIN, 5);

    loader.fragmentHasLoaded(5);
    reqs.assertFragmentsRequested(); // quiescent
    progress.assertEvent("download5", END, 5);
    progress.assertNoEvents();

    // check that new exclusive fragment requests work
    loader.inject(4, NULL_ERROR_HANDLER);
    reqs.assertFragmentsRequested(4);
    progress.assertEvent("download4", BEGIN, 4);

    loader.fragmentHasLoaded(4);
    reqs.assertFragmentsRequested();
    progress.assertEvent("download4", END, 4);
    progress.assertNoEvents();
  }

  private void loadFailed(MockLoadStrategy reqs, int fragment) {
    reqs.errorHandlers.get(fragment).loadTerminated(makeLoadFailedException());
  }
}
