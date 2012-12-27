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
package com.google.gwt.emultest.java.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.EmptyStackException;
import java.util.Stack;

/**
 * TODO: document me.
 */
public class StackTest extends GWTTestCase {

  private static final int TEST_SEARCH_SIZE = 10;
  private static final int TEST_SIZE = 10;

  /** Sets module name so that javascript compiler can operate. */
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /** Tests clone on Stacks. */
  public void testClone() {
    Stack<Integer> large = createLargeStack();
    @SuppressWarnings("unchecked")
    Stack<Integer> cloned = (Stack<Integer>) large.clone();
    checkLargeStack(cloned, 0);
    assertEquals(large.size(), TEST_SIZE);
  }

  /**
   * Tests pushing many elements into a stack, and seeing if they come out in
   * order. Also verifies that we get the correct exception when we run out of
   * elements, and tests peek
   */
  public void testCountAndOrderWithPeek() {
    Stack<Integer> large = new Stack<Integer>();
    for (int i = 0; i < TEST_SIZE; i++) {
      large.push(i);
      assertTrue(large.peek() == i);
      assertTrue(large.pop() == i);
      int theFinalInt = i + TEST_SIZE;
      large.push(theFinalInt);
      assertTrue(large.peek() == theFinalInt);
    }
    checkLargeStack(large, TEST_SIZE);
  }

  /** tests empty and tries to get emptyStackException as desired. */
  public void testEmptyAndEmptyStackException() {
    Stack<String> s = new Stack<String>();
    String item = "empty1";
    s.push(item);
    assertEquals(1, s.size());
    assertFalse(s.empty());
    assertEquals(s.pop(), item);
    checkEmptiness(s);
  }

  /** Tests pop and peek. */
  public void testPopAndPeek() {
    Stack<String> testStack = createStack();
    int x = testStack.size();
    Object item = testStack.peek();
    assertTrue(testStack.pop() == item);
    assertEquals(x - 1, testStack.size());
  }

  /** Tests push and peek. */
  public void testPushAndPeek() {
    Stack<String> testStack = createStack();
    int x = testStack.size();
    String item = "4";
    testStack.push(item);
    assertEquals(x + 1, testStack.size());
    assertTrue(testStack.peek() == item);
  }

  /**
   * Tests all combinations of search for a stack that attains a max size of
   * TEST_SEARCH_SIZE.
   */
  public void testSearch() {
    Stack<Integer> searchStack = new Stack<Integer>();
    checkEmptiness(searchStack);
    for (int stackSizeIncreasing = 0; stackSizeIncreasing < TEST_SEARCH_SIZE;
        stackSizeIncreasing++) {
      for (int theInt = 0; theInt < stackSizeIncreasing; theInt++) {
        assertEquals(
            "Increasing search found", searchStack.search(theInt), searchStack.size() - theInt);
      }
      for (int theInt = stackSizeIncreasing; theInt < TEST_SEARCH_SIZE; theInt++) {
        assertEquals("Increasing not found", searchStack.search(theInt), -1);
      }
      searchStack.push(stackSizeIncreasing);
    }
    for (int stackSizeDecreasing = TEST_SEARCH_SIZE - 1; stackSizeDecreasing >= 0;
        stackSizeDecreasing--) {
      for (int theInt = TEST_SEARCH_SIZE - 1; theInt > stackSizeDecreasing; theInt--) {
        assertEquals("Search decreasing not found", searchStack.search(theInt), -1);
      }
      for (int theInt = stackSizeDecreasing; theInt >= 0; theInt--) {
        assertEquals(
            "Search decreasing found", searchStack.search(theInt), searchStack.size() - theInt);
      }
      searchStack.pop();
    }
    checkEmptiness(searchStack);
  }

  private void checkLargeStack(Stack<Integer> stack, int offset) {
    for (int i = TEST_SIZE - 1; i >= 0; i--) {
      int theObtainedInt = stack.pop();
      assertEquals(i + offset, theObtainedInt);
    }
    checkEmptiness(stack);
  }

  private Stack<Integer> createLargeStack() {
    Stack<Integer> large = new Stack<Integer>();
    for (int i = 0; i < TEST_SIZE; i++) {
      large.push(i);
    }
    return large;
  }
  
  private Stack<String> createStack() {
    Stack<String> stack = new Stack<String>();
    stack.push("1");
    stack.push("2");
    stack.push("3");
    return stack;
  }

  /** Checks for emptiness of stack by peeking and popping. */
  private void checkEmptiness(Stack<?> s) {
    assertTrue(s.empty());
    try {
      s.pop();
      fail("Did not throw exception on pop of checkEmptiness");
    } catch (EmptyStackException es) {
      try {
        s.peek();
        fail("Did not throw exception on peek of checkEmptiness");
      } catch (EmptyStackException es2) {
        // we wanted to get here
      }
    }
  }
}
