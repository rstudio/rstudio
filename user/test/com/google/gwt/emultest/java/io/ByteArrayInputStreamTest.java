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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Unit test for the {@link java.io.ByteArrayInputStream} emulated class.
 */
public class ByteArrayInputStreamTest extends InputStreamBaseTest {

  @Override
  protected InputStream createInputStream(final byte[] expectedBytes) {
    // note that GWT fails here when trying to use clone().
    return new ByteArrayInputStream(Arrays.copyOf(expectedBytes, expectedBytes.length));
  }

  public void testAvailableForEmptyArray() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
    final int available = inputStream.available();
    assertEquals(0, available);
  }

  public void testAvailableForNonEmptyArray() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES);
    final int available = inputStream.available();
    assertEquals(TEST_BYTES.length, available);
  }

  public void testAvailableForEmptyArrayRange() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES, 1, 0);
    final int available = inputStream.available();
    assertEquals(0, available);
  }

  public void testAvailableForNonEmptyArrayRange() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES, 1, 3);
    final int available = inputStream.available();
    assertEquals(3, available);
  }

  public void testClose() throws IOException {
    // should do nothing (including not throwing an exception)
    try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES)) {
    }
  }

  public void testMarkSupported() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES);
    final boolean markSupported = inputStream.markSupported();
    assertTrue(markSupported);
  }

  public void testReadSingleValueFromEmptyArray() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
    final int c = inputStream.read();
    assertEquals(-1, c);
  }

  public void testReadSingleValuesFromNonEmptyArray() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES);
    for (int i = 0; i < TEST_BYTES.length; i++) {
      final int c = inputStream.read();
      assertEquals(TEST_BYTES[i], c);

      final int available = inputStream.available();
      assertEquals(TEST_BYTES.length - i - 1, available);
    }
    // at this point we should have reached the end of the stream.
    final int c = inputStream.read();
    assertEquals(-1, c);

    final int available = inputStream.available();
    assertEquals(0, available);
  }

  public void testReadSingleValueFromEmptyArrayRange() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES, 1, 0);
    final int c = inputStream.read();
    assertEquals(-1, c);
  }

  public void testReadSingleValuesFromNonEmptyArrayRange() {
    final int startAt = 1;
    final int count = 3;
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES, startAt, count);
    for (int i = 0; i < count; i++) {
      final int c = inputStream.read();
      assertEquals(TEST_BYTES[i + startAt], c);

      final int available = inputStream.available();
      assertEquals(count - i - 1, available);
    }
    // at this point we should have reached the end of the stream.
    final int c = inputStream.read();
    assertEquals(-1, c);

    final int available = inputStream.available();
    assertEquals(0, available);
  }

  public void testResetWithoutInvokingMark() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES);

    inputStream.reset();

    int c = inputStream.read();
    assertEquals(TEST_BYTES[0], c);
    c = inputStream.read();
    assertEquals(TEST_BYTES[1], c);
  }

  public void testResetAfterInvokingMark() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_BYTES);

    int c = inputStream.read();
    assertEquals(TEST_BYTES[0], c);

    inputStream.mark(0);

    c = inputStream.read();
    assertEquals(TEST_BYTES[1], c);
    c = inputStream.read();
    assertEquals(TEST_BYTES[2], c);

    inputStream.reset();

    c = inputStream.read();
    assertEquals(TEST_BYTES[1], c);
    c = inputStream.read();
    assertEquals(TEST_BYTES[2], c);
  }
}
