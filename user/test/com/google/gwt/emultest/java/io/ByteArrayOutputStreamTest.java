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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Unit test for the {@link java.io.ByteArrayOutputStream} emulated class.
 */
public class ByteArrayOutputStreamTest extends OutputStreamBaseTest {

  private static ByteArrayOutputStream outputStream;

  @Override
  protected OutputStream createDefaultOutputStream() {
    outputStream = new ByteArrayOutputStream();
    return outputStream;
  }

  @Override
  protected byte[] getBytesWritten() {
    return outputStream !=  null ? outputStream.toByteArray() : null;
  }

  public void testClose() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // should do nothing (including not throwing an exception).
    outputStream.close();
  }

  public void testFlush() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // should do nothing (including not throwing an exception).
    outputStream.flush();
  }

  public void testReset() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2);

    outputStream.write(TEST_ARRAY);

    byte[] actualBytes = outputStream.toByteArray();
    assertTrue(Arrays.equals(TEST_ARRAY, actualBytes));
 
    outputStream.reset();

    final byte[] expectedBytes = new byte[] { 101, 102 };
    outputStream.write(expectedBytes);

    actualBytes = outputStream.toByteArray();
    assertTrue(Arrays.equals(expectedBytes, actualBytes));
  }

  public void testSize() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1);
    assertEquals(0, outputStream.size());

    outputStream.write(128);
    assertEquals(1, outputStream.size());

    outputStream.write(TEST_ARRAY);
    assertEquals(1 + TEST_ARRAY.length, outputStream.size());

    outputStream.write(TEST_ARRAY, 1, 2);
    assertEquals(3 + TEST_ARRAY.length, outputStream.size());

    outputStream.reset();
    assertEquals(0, outputStream.size());

    outputStream.write(128);
    assertEquals(1, outputStream.size());
  }

  public void testToStringUsingEmptyStream() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1);
    final String actualString = outputStream.toString();
    assertTrue(actualString.isEmpty());
  }

  public void testToStringUsingNonEmptyStream() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1);
    final byte[] values = new byte[] {
        (byte) 0x48, (byte) 0x65, (byte) 0x6c, (byte) 0x6c, (byte) 0x6f
    };
    outputStream.write(values);
    final String actualString = outputStream.toString();
    assertEquals("Hello", actualString);
  }

  public void testToStringWithHighByteAndEmptyStream() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1);
    final String actualString = outputStream.toString(0x01);
    assertTrue(actualString.isEmpty());
  }

  public void testToStringWithHighByteAndNonEmptyStream() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1);
    final byte[] values = new byte[] {
        (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04
    };
    outputStream.write(values);
    final String actualString = outputStream.toString(0x01);
    final String expectedString = new String(new char[] {
        (char) 0x0100, (char) 0x0101, (char) 0x0102, (char) 0x0103, (char) 0x0104
    });
    assertEquals(expectedString, actualString);
  }

  public void testToStringWithCharsetNameAndEmptyStream() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1);
    final String actualString = outputStream.toString("UTF-8");
    assertTrue(actualString.isEmpty());
  }

  public void testToStringWithCharsetNameAndNonEmptyStream() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1);
    final String expectedString = "Hello";

    outputStream.write(expectedString.getBytes("UTF-8"));
    final String actualString = outputStream.toString("UTF-8");
    assertEquals(expectedString, actualString);
  }

  public void testWriteSingleValues() throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2);
    assertEquals(0, outputStream.size());

    for (int i = 0; i < 3; i++) {
      outputStream.write(TEST_ARRAY[i]);
      assertEquals(i + 1, outputStream.size());
    }
    final byte[] expectedBytes = Arrays.copyOf(TEST_ARRAY, 3);
    final byte[] actualBytes = outputStream.toByteArray();
    assertTrue(Arrays.equals(expectedBytes, actualBytes));
  }
}
