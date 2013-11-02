/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.js;

import junit.framework.TestCase;

/**
 * Verifies that we don't use symbols defined by browsers.
 */
public class JsProtectedNamesTest extends TestCase {

  public void testChrome30Symbol() throws Exception {
    checkNotLegal("webkitAudioContext");
  }

  public void testFirefox25Symbol() throws Exception {
    checkNotLegal("uneval");
  }

  public void testIE9Symbol() throws Exception {
    checkNotLegal("ActiveXObject");
  }

  private void checkNotLegal(String global) {
    assertFalse(global + " shouldn't be legal", JsProtectedNames.isLegalName(global));
  }
}
