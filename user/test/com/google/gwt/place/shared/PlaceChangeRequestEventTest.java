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
package com.google.gwt.place.shared;

import junit.framework.TestCase;

/**
 * Eponymous test class.
 */
public class PlaceChangeRequestEventTest extends TestCase {
  private static final String W1 = "foo";

  public void testNoClobberWarning() {
    PlaceChangeRequestEvent e = new PlaceChangeRequestEvent(new Place() {
    });

    assertNull(e.getWarning());
    e.setWarning(W1);
    assertEquals(W1, e.getWarning());
    e.setWarning("bar");
    assertEquals(W1, e.getWarning());
    e.setWarning(null);
    assertEquals(W1, e.getWarning());
  }
}
