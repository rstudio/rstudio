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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Unit test class for the {@link java.io.FilterOutputStream} emulated class.
 */
public class FilterOutputStreamTest extends GWTTestCase {

  private static final byte[] BYTES_TO_WRITE = {
      (byte) 0x10, (byte) 0x20, (byte) 0x30, (byte) 0x40, (byte) 0x50, (byte) 0x60
  };

  /**
   * Mock for {@link OutputStream}.
   */
  private static class MockOutputStream extends OutputStream {

    // Flags for knowing whether or not the underlying stream methods have been called.
    private boolean closeCalled;
    private boolean flushCalled;
    private boolean writeByteCalled;

    /**
     * Stores all the values written with {@code write(int b)}.
     */
    private LinkedList<Byte> writtenBytes;

    private MockOutputStream() {
      closeCalled = false;
      flushCalled = false;
      writeByteCalled = false;

      writtenBytes = new LinkedList<>();
    }

    @Override
    public void close() {
      closeCalled = true;
    }

    @Override
    public void flush() {
      flushCalled = true;
    }

    @Override
    public void write(int b) {
      writeByteCalled = true;
      writtenBytes.add((byte) b);
    }

    public boolean getCloseCalled() {
      return closeCalled;
    }

    public boolean getFlushCalled() {
      return flushCalled;
    }

    public boolean getWriteByteCalled() {
      return writeByteCalled;
    }

    public int getLastRequestedByteToWrite() {
      return (int) writtenBytes.get(writtenBytes.size() - 1);
    }

    public byte[] getWrittenValues() {
      int i = 0;
      byte[] result = new byte[writtenBytes.size()];
      for (Byte b : writtenBytes) {
        result[i++] = b.byteValue();
      }
      return result;
    }
  }

  private FilterOutputStream filter;

  private MockOutputStream mockOutputStream;

  /**
   * Sets module name so that javascript compiler can operate.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    mockOutputStream = new MockOutputStream();
    filter = new FilterOutputStream(mockOutputStream);
  }

  public void testClose() throws IOException {
    filter.close();
    assertTrue(mockOutputStream.getCloseCalled());
  }

  public void testFlush() throws IOException {
    filter.flush();
    assertTrue(mockOutputStream.getFlushCalled());
  }

  public void testWriteValue() throws IOException {
    int value = 12;
    filter.write(value);
    assertTrue(mockOutputStream.getWriteByteCalled());
    assertEquals(value, mockOutputStream.getLastRequestedByteToWrite());
  }

  public void testWriteArray() throws IOException {
    filter.write(BYTES_TO_WRITE);
    assertTrue(Arrays.equals(BYTES_TO_WRITE, mockOutputStream.getWrittenValues()));
  }

  public void testWriteArrayRange() throws IOException {
    int offset = 1;
    int length = 2;
    byte[] expectedWrittenValues = new byte[length];
    System.arraycopy(BYTES_TO_WRITE, offset, expectedWrittenValues, 0, length);
    filter.write(BYTES_TO_WRITE, offset, length);
    assertTrue(Arrays.equals(expectedWrittenValues, mockOutputStream.getWrittenValues()));
  }
}
