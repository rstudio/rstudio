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
package com.google.gwt.validation.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Test for {@link BaseMessageInterpolator}. Full tests are in the TCK.
 */
public class BaseMessageInterpolatorTest extends ValidationClientGwtTestCase {
  BaseMessageInterpolator interpolator;
  Map<String, Object> defaultMap = new HashMap<String, Object>();

  public void testReplace_foo() {
    assertAttributesReplaced("bar", "{foo}", defaultMap);
  }

  public void testReplace_fooFoo() {
    assertAttributesReplaced("bar and bar", "{foo} and {foo}", defaultMap);
  }

  public void testReplace_integer() {
    assertAttributesReplaced("integer=1", "integer={integer}", defaultMap);
  }

  public void testReplace_none() {
    assertAttributesReplaced("none", "none", defaultMap);
  }

  protected void assertAttributesReplaced(String expected, String message,
      Map<String, Object> map) {
    String result = interpolator.replaceParameters(message,
        BaseMessageInterpolator.createAnnotationReplacer(map));
    assertEquals(expected, result);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    interpolator = new GwtMessageInterpolator();
    defaultMap.clear();
    defaultMap.put("foo", "bar");
    defaultMap.put("integer", Integer.valueOf(1));
  }
}
