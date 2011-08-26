/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.event.shared;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit test for {@link #UmbrellaException}.
 */
public class UmbrellaExceptionTest extends TestCase {
  public void testNone() {
    // Why?
    try {
      throw new UmbrellaException(Collections.<Throwable> emptySet());
    } catch (UmbrellaException e) {
      assertNull(e.getCause());
      assertNull(e.getMessage());
    }
  }

  public void testOne() {
    Set<Throwable> causes = new HashSet<Throwable>();
    String message = "Just me";
    RuntimeException theOne = new RuntimeException(message);
    causes.add(theOne);

    try {
      throw new UmbrellaException(causes);
    } catch (UmbrellaException e) {
      assertSame(theOne, e.getCause());
      assertEquals(UmbrellaException.ONE + message, e.getMessage());
    }
  }

  public void testSome() {
    Set<Throwable> causes = new HashSet<Throwable>();
    String oneMessage = "one";
    RuntimeException oneException = new RuntimeException(oneMessage);
    causes.add(oneException);

    String twoMessage = "two";
    RuntimeException twoException = new RuntimeException(twoMessage);
    causes.add(twoException);

    try {
      throw new UmbrellaException(causes);
    } catch (UmbrellaException e) {
      // A bit non-deterministic for a unit test, but I've checked both paths --
      // rjrjr
      if (e.getCause() == oneException) {
        assertCauseMatchesFirstMessage(e, oneMessage, twoMessage);
      } else if (e.getCause() == twoException) {
        assertCauseMatchesFirstMessage(e, twoMessage, oneMessage);
      } else {
        fail("Expected one of the causes and its message");
      }
    }
  }

  private void assertCauseMatchesFirstMessage(UmbrellaException e, String firstMessage,
      String otherMessage) {
    assertTrue("Cause should be first message", e.getMessage().startsWith(
        2 + UmbrellaException.MULTIPLE + firstMessage));
    assertTrue("Should also see the other message", e.getMessage().contains(otherMessage));
  }
}
