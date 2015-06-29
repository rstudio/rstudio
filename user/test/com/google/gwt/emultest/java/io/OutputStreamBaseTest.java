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
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Class for reusing tests that are commong to {@link java.io.OutputStream} and its subclasses.
 */
public abstract class OutputStreamBaseTest extends GWTTestCase {

  protected static final byte[] TEST_ARRAY = new byte[] { 10, 20, 30, 40, 50 };

  /**
   * Factory method for creating a stream object.
   *
   * @return output stream object to be tested.
   */
  protected abstract OutputStream createDefaultOutputStream();

  /**
   * Retrieves the array of bytes written by the seam.
   *
   * @return bytes written by the stream.
   */
  protected abstract byte[] getBytesWritten();

  /**
   * Sets module name so that javascript compiler can operate.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testWriteArrayUsingNullArrayObject() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    try {
      outputStream.write(null, 0, 1);
      fail("should have thrown NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testWriteArrayUsingNegativeOffsetValue() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    try {
      outputStream.write(TEST_ARRAY, -1, 1);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testWriteArrayUsingNegativeLengthValue() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    try {
      outputStream.write(TEST_ARRAY, 0, -1);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testWriteArrayUsingAnInvalidRange() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    try {
      outputStream.write(TEST_ARRAY, 1, TEST_ARRAY.length);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testWriteArrayZeroLength() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    outputStream.write(TEST_ARRAY, 0, 0);
    assertEquals(0, getBytesWritten().length);
  }

  public void testWriteArrayZeroOffset() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    outputStream.write(TEST_ARRAY, 0, TEST_ARRAY.length);
    assertTrue(Arrays.equals(TEST_ARRAY, getBytesWritten()));
  }

  public void testWriteArrayFirstBytesOnly() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    outputStream.write(TEST_ARRAY, 0, TEST_ARRAY.length - 2);

    final byte[] expected = Arrays.copyOf(TEST_ARRAY, TEST_ARRAY.length - 2);
    assertTrue(Arrays.equals(expected, getBytesWritten()));
  }

  public void testWriteArrayLastBytesOnly() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    outputStream.write(TEST_ARRAY, 2, TEST_ARRAY.length - 2);

    final byte[] expected = Arrays.copyOfRange(TEST_ARRAY, 2, TEST_ARRAY.length);
    assertTrue(Arrays.equals(expected, getBytesWritten()));
  }

  public void testWriteArrayMiddleBytesOnly() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    outputStream.write(TEST_ARRAY, 2, TEST_ARRAY.length - 4);

    final byte[] expected = Arrays.copyOfRange(TEST_ARRAY, 2, TEST_ARRAY.length - 2);
    assertTrue(Arrays.equals(expected, getBytesWritten()));
  }

  public void testWriteArrayUsingNullArrayObjectAndNoOffset() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    try {
      outputStream.write(null);
      fail("should have thrown NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testWriteArrayZeroBytesNoOffset() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    outputStream.write(new byte[0]);
    assertEquals(0, getBytesWritten().length);
  }

  public void testWriteArrayNoOffset() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    outputStream.write(TEST_ARRAY);
    assertTrue(Arrays.equals(TEST_ARRAY, getBytesWritten()));
  }
}
