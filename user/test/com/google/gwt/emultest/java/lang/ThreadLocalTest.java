/*
 * Copyright 2017 Google Inc.
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
package com.google.gwt.emultest.java.lang;

import com.google.gwt.emultest.java.util.EmulTestBase;

/**
 * Tests for {@link java.lang.ThreadLocal}.
 */
public class ThreadLocalTest extends EmulTestBase {

  public void testGetSet() {
    ThreadLocal<String> threadLocal = new ThreadLocal<>();
    threadLocal.set("testString");
    assertEquals("testString", threadLocal.get());
  }

  public void testRemove() {
    ThreadLocal<String> threadLocal = new ThreadLocal<>();
    threadLocal.set("testString");
    assertEquals("testString", threadLocal.get());
    threadLocal.remove();
    assertNull(threadLocal.get());
    threadLocal.remove(); // This shouldn't throw any exception
  }
}
