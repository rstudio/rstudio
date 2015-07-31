/*
 * Copyright 2015 Google Inc.
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

package com.google.gwt.emultest.java.nio.charset;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Unit test for the {@link java.nio.charset.StandardCharsets} emulated class.
 */
public class StandardCharsetsTest extends EmulTestBase {

  public void testIso88591() {
    assertEquals(Charset.forName("ISO-8859-1"), StandardCharsets.ISO_8859_1);
  }

  public void testUtf8() {
    assertEquals(Charset.forName("UTF-8"), StandardCharsets.UTF_8);
  }
}
