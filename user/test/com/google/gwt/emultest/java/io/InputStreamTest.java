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
package com.google.gwt.emultest.java.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Unit test for the {@link java.io.InputStream} emulated class.
 */
public class InputStreamTest extends InputStreamBaseTest {

  @Override
  protected InputStream createInputStream(final byte[] expectedBytes) {
    return new InputStream() {
      // note that GWT fails here when trying to use clone().
      private final byte[] b = Arrays.copyOf(expectedBytes, expectedBytes.length);
      private int index = 0;
      @Override public int read() {
        int c = -1;
        if (index < b.length) {
          c = b[index];
          index++;
        }
        return c;
      }
    };
  }

  public void testDefaultBehaviorOfAvailable() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final int available = inputStream.available();
    assertEquals(0, available);
  }

  public void testDefaultBehaviorOfClose() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    // should do nothing (including not throwing an exception)
    inputStream.close();
  }

  public void testDefaultBehaviorOfMark() {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    // should do nothing (including not throwing an exception)
    inputStream.mark(1000);
  }

  public void testDefaultBehaviorOfMarkSupported() {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final boolean markSupported = inputStream.markSupported();
    assertFalse(markSupported);
  }

  public void testDefaultBehaviorOfReset() {
    final InputStream inputStream = createInputStream(new byte[] {});
    try {
      inputStream.reset();
      fail("should have thrown IOException");
    } catch (IOException expected) {
    }
  }
}
