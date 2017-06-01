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

import com.google.gwt.junit.client.GWTTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Class for reusing tests that are common to {@link java.io.InputStream} and its subclasses.
 */
public abstract class InputStreamBaseTest extends GWTTestCase {

  protected static final byte[] TEST_BYTES = new byte[] { 10, 20, 30, 40, 50 };

  /**
   * Sets module name so that javascript compiler can operate.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * Factory method for creating a stream object.
   *
   * @param expectedBytes expected bytes when reading from the stream.
   * @return input stream object to be tested.
   */
  protected abstract InputStream createInputStream(byte[] expectedBytes);

  public void testReadArrayUsingNullArrayObject() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    try {
      inputStream.read(null, 0, 1);
      fail("should have thrown NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testReadArrayUsingNegativeOffsetValue() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[1];
    try {
      inputStream.read(b, -1, 1);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testReadArrayUsingNegativeLengthValue() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[1];
    try {
      inputStream.read(b, 0, -1);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testReadArrayUsingAnInvalidRange() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[1];
    try {
      inputStream.read(b, 1, 1);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testReadArrayUsingZeroLength() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[1];
    int c = inputStream.read(b, 0, 0);
    assertEquals(0, c);
    c = inputStream.read(b, 1, 0);
    assertEquals(0, c);
  }

  public void testReadArrayFromEmptyStream() throws IOException {
    final InputStream inputStream = createInputStream(new byte[] {});
    final byte[] b = new byte[1];
    final int c = inputStream.read(b, 0, 1);
    assertEquals(-1, c);
  }

  public void testReadArrayPartial() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[2];
    final int result = inputStream.read(b, 1, 1);
    assertEquals(1, result);
    assertEquals(0, b[0]);
    assertEquals(b[1], TEST_BYTES[0]);
  }

  public void testReadArrayReachEndOfStream() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[TEST_BYTES.length + 5];
    final int result = inputStream.read(b, 5, TEST_BYTES.length);
    assertEquals(TEST_BYTES.length, result);

    final byte[] expected = new byte[b.length];
    System.arraycopy(TEST_BYTES, 0, expected, 5, TEST_BYTES.length);
    assertTrue(Arrays.equals(expected, b));
  }

  public void testReadArrayExceedEndOfStream() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[5 + TEST_BYTES.length + 5];
    final int result = inputStream.read(b, 5, TEST_BYTES.length + 5);
    assertEquals(TEST_BYTES.length, result);

    final byte[] expected = new byte[b.length];
    System.arraycopy(TEST_BYTES, 0, expected, 5, TEST_BYTES.length);
    assertTrue(Arrays.equals(expected, b));
  }

  public void testReadArrayUsingNullArrayObjectNoOffset() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    try {
      inputStream.read(null);
      fail("should have thrown NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testReadUsingAnEmptyArrayNoOffset() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[0];
    final int c = inputStream.read(b);
    assertEquals(0, c);
  }

  public void testReadArrayFromEmptyStreamNoOffset() throws IOException {
    final InputStream inputStream = createInputStream(new byte[] {});
    final byte[] b = new byte[1];
    final int c = inputStream.read(b);
    assertEquals(-1, c);
  }

  public void testReadArrayPartialNoOffset() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[TEST_BYTES.length / 2];
    final int result = inputStream.read(b);
    assertEquals(b.length, result);
    assertTrue(Arrays.equals(Arrays.copyOf(TEST_BYTES, b.length), b));
  }

  public void testReadArrayReachEndOfStreamNoOffset() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[TEST_BYTES.length];
    final int result = inputStream.read(b);
    assertEquals(TEST_BYTES.length, result);
    assertTrue(Arrays.equals(TEST_BYTES, b));
  }

  public void testReadArrayExceedEndOfStreamNoOffset() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    final byte[] b = new byte[TEST_BYTES.length * 2];
    final int result = inputStream.read(b);
    assertEquals(TEST_BYTES.length, result);
    assertTrue(Arrays.equals(Arrays.copyOf(TEST_BYTES, b.length), b));
  }

  public void testSkipZeroBytes() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    long result = inputStream.skip(0);
    assertEquals(0, result);
  }

  public void testSkipFewBytes() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    long result = inputStream.skip(3);
    assertEquals(3, result);
  }

  public void testSkipReachEndOfStream() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    long result = inputStream.skip(TEST_BYTES.length);
    assertEquals(TEST_BYTES.length, result);
  }

  public void testSkipExceedEndOfStream() throws IOException {
    final InputStream inputStream = createInputStream(TEST_BYTES);
    long result = inputStream.skip(TEST_BYTES.length + 5);
    assertEquals(TEST_BYTES.length, result);
  }
}
