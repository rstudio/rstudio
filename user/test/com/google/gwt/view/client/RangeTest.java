/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.view.client;

import junit.framework.TestCase;

/**
 * Tests for {@link Range}.
 */
public class RangeTest extends TestCase {

  public void testAccessors() {
    Range range = new Range(10, 20);
    assertEquals(10, range.getStart());
    assertEquals(20, range.getLength());
  }

  public void testEquals() {
    Range range0 = new Range(10, 20);
    Range range1 = new Range(10, 20);
    assertEquals(range0, range1);
    assertTrue(range0.equals(range1));
    assertEquals(range0.hashCode(), range1.hashCode());
  }

  public void testEqualsNull() {
    Range range0 = new Range(10, 20);
    assertFalse(range0.equals(null));
  }

  public void testEqualsObject() {
    Range range0 = new Range(10, 20);
    assertFalse(range0.equals("not a range"));
  }

  public void testNotEqualsLength() {
    Range range0 = new Range(10, 20);
    Range range1 = new Range(10, 19);
    assertNotSame(range0, range1);
    assertFalse(range0.equals(range1));
    assertNotSame(range0.hashCode(), range1.hashCode());
  }

  public void testNotEqualsStart() {
    Range range0 = new Range(10, 20);
    Range range1 = new Range(9, 20);
    assertNotSame(range0, range1);
    assertFalse(range0.equals(range1));
    assertNotSame(range0.hashCode(), range1.hashCode());
  }
}
