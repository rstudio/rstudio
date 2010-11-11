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
package com.google.gwt.junit;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.junit.JUnitMessageQueue.ClientInfoExt;
import com.google.gwt.junit.JUnitMessageQueue.ClientStatus;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.client.impl.JUnitHost.TestBlock;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Tests of {@link JUnitMessageQueue}.
 */
public class JUnitMessageQueueTest extends TestCase {

  private final static int ONE_BLOCK = 1;
  private final static int ONE_TEST_PER_BLOCK = 1;
  private final static int TWO_CLIENTS = 2;

  public void testAddTestBlocks() {
    JUnitMessageQueue queue = new JUnitMessageQueue(10);
    assertEquals(0, queue.getTestBlocks().size());
    List<TestInfo[]> expectedBlocks = new ArrayList<TestInfo[]>();

    // Add some blocks.
    {
      List<TestInfo[]> testBlocks = createTestBlocks(5, 3);
      queue.addTestBlocks(testBlocks, false);
      expectedBlocks.addAll(testBlocks);
      assertEquals(expectedBlocks, queue.getTestBlocks());
    }

    // Try to add an empty block.
    {
      List<TestInfo[]> testBlocks = createTestBlocks(5, 0);
      try {
        queue.addTestBlocks(testBlocks, false);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        // expected.
      }
      assertEquals(expectedBlocks, queue.getTestBlocks());
    }

    // Add last block.
    {
      List<TestInfo[]> testBlocks = createTestBlocks(3, 1);
      queue.addTestBlocks(testBlocks, true);
      expectedBlocks.addAll(testBlocks);
      assertEquals(expectedBlocks, queue.getTestBlocks());
    }

    // Try to add more blocks.
    {
      List<TestInfo[]> testBlocks = createTestBlocks(1, 1);
      try {
        queue.addTestBlocks(testBlocks, false);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        // expected.
      }
      assertEquals(expectedBlocks, queue.getTestBlocks());
    }
  }

  public void testGetNumConnectedClients() {
    final long timeout = System.currentTimeMillis() + 15;
    JUnitMessageQueue queue = createQueue(15, 1, 1);
    assertEquals(0, queue.getNumConnectedClients());

    // Add some clients in a few ways.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(1, "gecko1_8"), null);
      queue.reportResults(createClientInfo(2, "safari"), createTestResults(0));
      assertEquals(3, queue.getNumConnectedClients());
    }

    // Add duplicate clients.
    {
      queue.getTestBlock(createClientInfo(3, "ie6"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(3, "ie6"), null);
      queue.reportResults(createClientInfo(4, "safari"), createTestResults(0));
      assertEquals(5, queue.getNumConnectedClients());
    }

    // Add existing clients.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(1, "gecko1_8"), null);
      queue.reportResults(createClientInfo(2, "safari"), createTestResults(0));
      assertEquals(5, queue.getNumConnectedClients());
    }
  }

  public void testGetNumRetrievedClients() {
    final long timeout = System.currentTimeMillis() + 15;
    JUnitMessageQueue queue = createQueue(15, 2, 3);
    TestInfo[] testBlock0 = queue.getTestBlocks().get(0);
    TestInfo test0_0 = testBlock0[0];
    TestInfo test0_1 = testBlock0[1];
    TestInfo test0_2 = testBlock0[2];
    TestInfo[] testBlock1 = queue.getTestBlocks().get(1);
    TestInfo test1_0 = testBlock1[0];
    TestInfo test1_1 = testBlock1[1];
    TestInfo test1_2 = testBlock1[2];
    assertEquals(0, queue.getNumClientsRetrievedTest(test0_0));
    assertEquals(0, queue.getNumClientsRetrievedTest(test0_1));
    assertEquals(0, queue.getNumClientsRetrievedTest(test0_2));
    assertEquals(0, queue.getNumClientsRetrievedTest(test1_0));
    assertEquals(0, queue.getNumClientsRetrievedTest(test1_1));
    assertEquals(0, queue.getNumClientsRetrievedTest(test1_2));

    // First client retrieves the first test block.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 0, timeout);
      assertEquals(1, queue.getNumClientsRetrievedTest(test0_0));
      assertEquals(1, queue.getNumClientsRetrievedTest(test0_1));
      assertEquals(1, queue.getNumClientsRetrievedTest(test0_2));
      assertEquals(0, queue.getNumClientsRetrievedTest(test1_0));
      assertEquals(0, queue.getNumClientsRetrievedTest(test1_1));
      assertEquals(0, queue.getNumClientsRetrievedTest(test1_2));
    }

    // Second client retrieves the first test block.
    {
      queue.getTestBlock(createClientInfo(1, "ie6"), 0, timeout);
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_0));
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_1));
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_2));
      assertEquals(0, queue.getNumClientsRetrievedTest(test1_0));
      assertEquals(0, queue.getNumClientsRetrievedTest(test1_1));
      assertEquals(0, queue.getNumClientsRetrievedTest(test1_2));
    }

    // First client retrieves the second test block.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 1, timeout);
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_0));
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_1));
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_2));
      assertEquals(1, queue.getNumClientsRetrievedTest(test1_0));
      assertEquals(1, queue.getNumClientsRetrievedTest(test1_1));
      assertEquals(1, queue.getNumClientsRetrievedTest(test1_2));
    }

    // First client retrieves the second test block again.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 1, timeout);
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_0));
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_1));
      assertEquals(2, queue.getNumClientsRetrievedTest(test0_2));
      assertEquals(1, queue.getNumClientsRetrievedTest(test1_0));
      assertEquals(1, queue.getNumClientsRetrievedTest(test1_1));
      assertEquals(1, queue.getNumClientsRetrievedTest(test1_2));
    }
  }

  public void testGetResults() {
    JUnitMessageQueue queue = createQueue(3, 2, 3);
    TestInfo[] testBlock0 = queue.getTestBlocks().get(0);
    TestInfo test0_0 = testBlock0[0];

    // The results from the three clients.
    JUnitResult result0 = new JUnitResult();
    result0.setException(new IllegalArgumentException("0"));
    JUnitResult result1 = new JUnitResult();
    result0.setException(new IllegalArgumentException("1"));
    JUnitResult result2 = new JUnitResult();
    result0.setException(new IllegalArgumentException("2"));

    // Client 0 reports results for first test case.
    {
      Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
      results.put(test0_0, result0);
      queue.reportResults(createClientInfo(0, "ie6"), results);
    }

    // Client 1 reports results for first test case.
    {
      Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
      results.put(test0_0, result1);
      queue.reportResults(createClientInfo(1, "ie6"), results);
    }

    // Client 2 reports results for first test case.
    {
      Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
      results.put(test0_0, result2);
      queue.reportResults(createClientInfo(2, "ie6"), results);
    }

    // Get the results
    Map<ClientStatus, JUnitResult> results = queue.getResults(test0_0);
    assertEquals(3, results.size());
    for (Entry<ClientStatus, JUnitResult> entry : results.entrySet()) {
      ClientStatus client = entry.getKey();
      JUnitResult result = entry.getValue();
      switch (client.getId()) {
        case 0:
          assertEquals(result0, result);
          break;
        case 1:
          assertEquals(result1, result);
          break;
        case 2:
          assertEquals(result2, result);
          break;
        default:
          fail("Unexpected client");
          break;
      }
    }
  }

  public void testGetTestBlock() {
    final long timeout = System.currentTimeMillis() + 15;
    JUnitMessageQueue queue = createQueue(15, 2, 3);
    TestInfo[] testBlock0 = queue.getTestBlocks().get(0);
    TestInfo[] testBlock1 = queue.getTestBlocks().get(1);

    // Get the first test block.
    {
      TestBlock block = queue.getTestBlock(createClientInfo(0, "ie6"), 0,
          timeout);
      assertEquals(testBlock0, block.getTests());
      assertEquals(0, block.getIndex());
    }

    // Get the second test block.
    {
      TestBlock block = queue.getTestBlock(createClientInfo(0, "ie6"), 1,
          timeout);
      assertEquals(testBlock1, block.getTests());
      assertEquals(1, block.getIndex());
    }

    // Get the third test block.
    {
      assertNull(queue.getTestBlock(createClientInfo(0, "ie6"), 2, timeout));
    }
  }

  public void testGetUserAgents() {
    final long timeout = System.currentTimeMillis() + 15;
    JUnitMessageQueue queue = createQueue(15, 1, 1);
    assertEquals(0, queue.getUserAgents().length);

    // Add some clients in a few ways.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(1, "gecko1_8"), null);
      queue.reportResults(createClientInfo(2, "safari"), createTestResults(0));
      assertSimilar(new String[] {"ie6", "gecko1_8", "safari"},
          queue.getUserAgents());
    }

    // Add duplicate clients.
    {
      queue.getTestBlock(createClientInfo(3, "ie7"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(3, "ie7"), null);
      queue.reportResults(createClientInfo(4, "gecko1_8"), createTestResults(0));
      queue.getTestBlock(createClientInfo(3, "ie7"), 0, timeout);
      assertSimilar(new String[] {"ie6", "ie7", "gecko1_8", "safari"},
          queue.getUserAgents());
    }

    // Add existing clients.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(1, "gecko1_8"), null);
      queue.reportResults(createClientInfo(2, "safari"), createTestResults(0));
      assertSimilar(new String[] {"ie6", "ie7", "gecko1_8", "safari"},
          queue.getUserAgents());
    }
  }

  public void testHasResults() {
    JUnitMessageQueue queue = createQueue(3, 2, 3);
    TestInfo[] testBlock0 = queue.getTestBlocks().get(0);
    TestInfo test0_0 = testBlock0[0];
    TestInfo test0_1 = testBlock0[1];
    TestInfo test0_2 = testBlock0[2];
    TestInfo[] testBlock1 = queue.getTestBlocks().get(1);
    TestInfo test1_0 = testBlock1[0];
    TestInfo test1_1 = testBlock1[1];
    TestInfo test1_2 = testBlock1[2];
    assertFalse(queue.hasResults(test0_0));
    assertFalse(queue.hasResults(test0_1));
    assertFalse(queue.hasResults(test0_2));
    assertFalse(queue.hasResults(test1_0));
    assertFalse(queue.hasResults(test1_1));
    assertFalse(queue.hasResults(test1_2));

    // First client reports results for the first test.
    {
      Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
      results.put(test0_0, new JUnitResult());
      queue.reportResults(createClientInfo(0, "ie6"), results);
      assertFalse(queue.hasResults(test0_0));
      assertFalse(queue.hasResults(test0_1));
      assertFalse(queue.hasResults(test0_2));
      assertFalse(queue.hasResults(test1_0));
      assertFalse(queue.hasResults(test1_1));
      assertFalse(queue.hasResults(test1_2));
    }

    // Second client reports results for the first test.
    {
      Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
      results.put(test0_0, new JUnitResult());
      queue.reportResults(createClientInfo(1, "ie6"), results);
      assertFalse(queue.hasResults(test0_0));
      assertFalse(queue.hasResults(test0_1));
      assertFalse(queue.hasResults(test0_2));
      assertFalse(queue.hasResults(test1_0));
      assertFalse(queue.hasResults(test1_1));
      assertFalse(queue.hasResults(test1_2));
    }

    // First client reports results for the second test.
    {
      Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
      results.put(test0_1, new JUnitResult());
      queue.reportResults(createClientInfo(0, "ie6"), results);
      assertFalse(queue.hasResults(test0_0));
      assertFalse(queue.hasResults(test0_1));
      assertFalse(queue.hasResults(test0_2));
      assertFalse(queue.hasResults(test1_0));
      assertFalse(queue.hasResults(test1_1));
      assertFalse(queue.hasResults(test1_2));
    }

    // Third client reports results for the first test.
    {
      Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
      results.put(test0_0, new JUnitResult());
      queue.reportResults(createClientInfo(2, "ie6"), results);
      assertTrue(queue.hasResults(test0_0));
      assertFalse(queue.hasResults(test0_1));
      assertFalse(queue.hasResults(test0_2));
      assertFalse(queue.hasResults(test1_0));
      assertFalse(queue.hasResults(test1_1));
      assertFalse(queue.hasResults(test1_2));
    }
  }

  public void testNeedRerunningExceptions() {
    JUnitMessageQueue queue = createQueue(TWO_CLIENTS, ONE_BLOCK,
        ONE_TEST_PER_BLOCK);
    // an exception
    TestInfo testInfo = queue.getTestBlocks().get(0)[0];
    Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
    JUnitResult junitResult = new JUnitResult();
    junitResult.setException(new UnableToCompleteException());
    results.put(testInfo, junitResult);
    queue.reportResults(createClientInfo(0, "ie6"), results);
    results = new HashMap<TestInfo, JUnitResult>();
    junitResult = new JUnitResult();
    junitResult.setException(new JUnitFatalLaunchException());
    results.put(testInfo, junitResult);
    queue.reportResults(createClientInfo(1, "ff3"),
        createTestResults(ONE_TEST_PER_BLOCK));
    assertTrue(queue.needsRerunning(testInfo));

    // an exception but exception in launch module
    queue.removeResults(testInfo);
    results = new HashMap<TestInfo, JUnitResult>();
    junitResult = new JUnitResult();
    junitResult.setException(new JUnitFatalLaunchException());
    results.put(testInfo, junitResult);
    queue.reportResults(createClientInfo(0, "ie6"), results);
    queue.reportResults(createClientInfo(1, "ff3"),
        createTestResults(ONE_TEST_PER_BLOCK));
    assertFalse(queue.needsRerunning(testInfo));
  }

  public void testNeedRerunningIncompleteResults() {
    JUnitMessageQueue queue = createQueue(TWO_CLIENTS, ONE_BLOCK,
        ONE_TEST_PER_BLOCK);
    TestInfo testInfo = queue.getTestBlocks().get(0)[0];

    // incomplete results
    assertTrue(queue.needsRerunning(testInfo));
    queue.reportResults(createClientInfo(0, "ff3"), createTestResults(1));
    assertTrue(queue.needsRerunning(testInfo));

    // complete results
    queue.reportResults(createClientInfo(1, "ie7"), createTestResults(1));
    assertFalse(queue.needsRerunning(testInfo));
  }

  public void testNewClients() {
    final long timeout = System.currentTimeMillis() + 15;
    JUnitMessageQueue queue = createQueue(15, 1, 1);
    assertEquals(0, queue.getNewClients().length);

    // Add some clients in a few ways.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(1, "gecko1_8"), null);
      queue.reportResults(createClientInfo(2, "safari"), createTestResults(0));
      assertSimilar(new String[] {"desc0", "desc1", "desc2"},
          queue.getNewClients());
      assertEquals(0, queue.getNewClients().length);
    }

    // Add duplicate clients.
    {
      queue.getTestBlock(createClientInfo(3, "ie6"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(3, "ie6"), null);
      queue.reportResults(createClientInfo(4, "safari"), createTestResults(0));
      queue.getTestBlock(createClientInfo(3, "ie6"), 0, timeout);
      assertSimilar(new String[] {"desc3", "desc4"}, queue.getNewClients());
      assertEquals(0, queue.getNewClients().length);
    }

    // Add existing clients.
    {
      queue.getTestBlock(createClientInfo(0, "ie6"), 0, timeout);
      queue.reportFatalLaunch(createClientInfo(1, "gecko1_8"), null);
      queue.reportResults(createClientInfo(2, "safari"), createTestResults(0));
      assertEquals(0, queue.getNewClients().length);
    }
  }

  public void testRemove() {
    JUnitMessageQueue queue = createQueue(TWO_CLIENTS, ONE_BLOCK,
        ONE_TEST_PER_BLOCK);
    TestInfo testInfo = queue.getTestBlocks().get(0)[0];
    assertFalse(queue.hasResults(testInfo));

    queue.reportResults(createClientInfo(0, "ie6"),
        createTestResults(ONE_TEST_PER_BLOCK));
    assertFalse(queue.hasResults(testInfo));
    queue.reportResults(createClientInfo(1, "ff3"),
        createTestResults(ONE_TEST_PER_BLOCK));
    assertTrue(queue.hasResults(testInfo));

    queue.removeResults(testInfo);
    assertFalse(queue.hasResults(testInfo));
  }

  public void testRetries() {
    JUnitMessageQueue queue = createQueue(TWO_CLIENTS, ONE_BLOCK,
        ONE_TEST_PER_BLOCK);
    TestInfo testInfo = queue.getTestBlocks().get(0)[0];
    Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
    JUnitResult junitResult = new JUnitResult();
    junitResult.setException(new AssertionError());
    results.put(testInfo, junitResult);
    queue.reportResults(createClientInfo(0, "ff3"), results);
    assertTrue(queue.needsRerunning(testInfo));
    Map<ClientStatus, JUnitResult> queueResults = queue.getResults(testInfo);
    assertEquals(1, queueResults.size());
    for (JUnitResult result : queueResults.values()) {
      assertNotNull(result.getException());
    }

    queue.removeResults(testInfo);

    queue.reportResults(createClientInfo(0, "ff3"),
        createTestResults(ONE_TEST_PER_BLOCK));
    queue.reportResults(createClientInfo(1, "ie6"),
        createTestResults(ONE_TEST_PER_BLOCK));
    assertFalse(queue.needsRerunning(testInfo));
    // check that the updated result appears now.
    queueResults = queue.getResults(testInfo);
    assertEquals(2, queueResults.size());
    for (JUnitResult result : queueResults.values()) {
      assertNull(result.getException());
    }
  }

  /**
   * Assert that two arrays are the same size and contain the same elements.
   * Ordering does not matter.
   * 
   * @param expected the expected array
   * @param actual the actual array
   */
  private void assertSimilar(String[] expected, String[] actual) {
    assertEquals(new HashSet<String>(Arrays.asList(expected)),
        new HashSet<String>(Arrays.asList(actual)));
  }

  private ClientInfoExt createClientInfo(int sessionId, String userAgent) {
    return new ClientInfoExt(sessionId, userAgent, "desc" + sessionId);
  }

  /**
   * Create a {@link JUnitMessageQueue} with the specified number of blocks.
   * 
   * @param numClients the number of remote clients
   * @param numBlocks the number of test blocks to add
   * @param testsPerBlock the number of tests per block
   * @return the message queue
   */
  private JUnitMessageQueue createQueue(int numClients, int numBlocks,
      int testsPerBlock) {
    JUnitMessageQueue queue = new JUnitMessageQueue(numClients);
    queue.addTestBlocks(createTestBlocks(numBlocks, testsPerBlock), true);
    return queue;
  }

  /**
   * Create a list of test blocks.
   * 
   * @param numBlocks the number of test blocks to add
   * @param testsPerBlock the number of tests per block
   * @return the test blocks
   */
  private List<TestInfo[]> createTestBlocks(int numBlocks, int testsPerBlock) {
    List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>();
    for (int i = 0; i < numBlocks; i++) {
      TestInfo[] testBlock = new TestInfo[testsPerBlock];
      for (int test = 0; test < testsPerBlock; test++) {
        testBlock[test] = new TestInfo("testModule" + i, "testClass",
            "testMethod" + test);
      }
      testBlocks.add(testBlock);
    }
    return testBlocks;
  }

  /**
   * Create some fake test results.
   * 
   * @param numTests the number of results to generate
   * @return the test results
   */
  private Map<TestInfo, JUnitResult> createTestResults(int numTests) {
    Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
    for (int i = 0; i < numTests; i++) {
      TestInfo testInfo = new TestInfo("testModule0", "testClass", "testMethod"
          + i);
      results.put(testInfo, new JUnitResult());
    }
    return results;
  }
}
