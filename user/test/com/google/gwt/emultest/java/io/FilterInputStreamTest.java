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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Unit test for the {@link java.io.FilterInputStream} emulated class.
 */
public class FilterInputStreamTest extends GWTTestCase {

  /**
   * Mock for {@link InputStream}.
   */
  private static class MockInputStream extends InputStream {
    public static final int RETURNED_VALUE_FOR_AVAILABLE = 100;
    public static final int RETURNED_VALUE_FOR_READ_BYTE = 150;
    public static final int RETURNED_VALUE_FOR_READ_BYTES = 200;
    public static final boolean RETURNED_VALUE_FOR_MARK_SUPPORTED = true;
    public static final long RETURNED_VALUE_FOR_SKIP = 250;

    // Flags for knowing whether or not the underlying stream methods have been called.
    private boolean availableCalled;
    private boolean closeCalled;
    private boolean markCalled;
    private boolean markSupportedCalled;
    private boolean readByteCalled;
    private boolean readBytesCalled;
    private boolean resetCalled;
    private boolean skipCalled;

    // Input parameters to ensure the filter passes the right arguments to the stream.
    private int requestedReadLimit;
    private long requestedSkipBytes;
    private byte[] requestedReadBuffer;
    private int requestedReadOffset;
    private int requestedReadLength;

    public MockInputStream() {
      availableCalled = false;
      closeCalled = false;
      markCalled = false;
      markSupportedCalled = false;
      readByteCalled = false;
      readBytesCalled = false;
      resetCalled = false;
      skipCalled = false;

      requestedReadLimit = 0;
      requestedSkipBytes = 0L;
      requestedReadBuffer = null;
      requestedReadOffset = 0;
      requestedReadLength = 0;
    }

    @Override
    public int available() {
      availableCalled = true;
      return RETURNED_VALUE_FOR_AVAILABLE;
    }

    @Override
    public void close() {
      closeCalled = true;
    }

    @Override
    public void mark(int readLimit) {
      markCalled = true;
      requestedReadLimit = readLimit;
    }

    @Override
    public boolean markSupported() {
      markSupportedCalled = true;
      return RETURNED_VALUE_FOR_MARK_SUPPORTED;
    }

    @Override
    public int read() {
      readByteCalled = true;
      return RETURNED_VALUE_FOR_READ_BYTE;
    }

    @Override
    public int read(byte[] b, int off, int len) {
      readBytesCalled = true;
      requestedReadBuffer = b;
      requestedReadOffset = off;
      requestedReadLength = len;
      return RETURNED_VALUE_FOR_READ_BYTES;
    }

    @Override
    public void reset() {
      resetCalled = true;
    }

    @Override
    public long skip(long n) {
      skipCalled = true;
      requestedSkipBytes = n;
      return RETURNED_VALUE_FOR_SKIP;
    }

    public boolean getAvailableCalled() {
      return availableCalled;
    }

    public boolean getCloseCalled() {
      return closeCalled;
    }

    public boolean getMarkCalled() {
      return markCalled;
    }

    public int getMarkReadLimit() {
      return requestedReadLimit;
    }

    public boolean getMarkSupportedCalled() {
      return markSupportedCalled;
    }

    public boolean getReadByteCalled() {
      return readByteCalled;
    }

    public boolean getReadBytesCalled() {
      return readBytesCalled;
    }

    public boolean getResetCalled() {
      return resetCalled;
    }

    public boolean getSkipCalled() {
      return skipCalled;
    }

    public int getRequestedReadLimit() {
      return requestedReadLimit;
    }

    public long getRequestedSkipBytes() {
      return requestedSkipBytes;
    }

    public byte[] getRequestedReadBuffer() {
      return requestedReadBuffer;
    }

    public int getRequestedReadOffset() {
      return requestedReadOffset;
    }

    public int getRequestedReadLength() {
      return requestedReadLength;
    }
  }

  /**
   * The constructor of {@link java.io.FilterInputStream} is protected. This class provides a public
   * constructor so that it can be instantiated.
   */
  private static class InstantiableFilterInputStream extends FilterInputStream {
    public InstantiableFilterInputStream(InputStream inputStream) {
      super(inputStream);
    }
  }

  private FilterInputStream filter;

  protected MockInputStream mockInputStream;

  /**
   * Sets module name so that javascript compiler can operate.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    mockInputStream = new MockInputStream();
    filter = new InstantiableFilterInputStream(mockInputStream);
  }

  public void testAvailable() throws IOException {
    int available = filter.available();
    assertTrue(mockInputStream.getAvailableCalled());
    assertEquals(MockInputStream.RETURNED_VALUE_FOR_AVAILABLE, available);
  }

  public void testClose() throws IOException {
    filter.close();
    assertTrue(mockInputStream.getCloseCalled());
  }

  public void testMark() {
    int readLimit = 12;
    filter.mark(readLimit);
    assertTrue(mockInputStream.getMarkCalled());
    assertEquals(readLimit, mockInputStream.getMarkReadLimit());
  }

  public void testMarkSupported() {
    boolean markSupported = filter.markSupported();
    assertTrue(mockInputStream.getMarkSupportedCalled());
    assertEquals(MockInputStream.RETURNED_VALUE_FOR_MARK_SUPPORTED, markSupported);
  }

  public void testReadValue() throws IOException {
    int value = filter.read();
    assertTrue(mockInputStream.getReadByteCalled());
    assertEquals(MockInputStream.RETURNED_VALUE_FOR_READ_BYTE, value);
  }

  public void testReadArray() throws IOException {
    byte[] b = new byte[500];
    int bytesRead = filter.read(b);
    assertTrue(mockInputStream.getReadBytesCalled());
    assertEquals(b, mockInputStream.getRequestedReadBuffer());
    assertEquals(0, mockInputStream.getRequestedReadOffset());
    assertEquals(b.length, mockInputStream.getRequestedReadLength());
    assertEquals(MockInputStream.RETURNED_VALUE_FOR_READ_BYTES, bytesRead);
  }

  public void testReadArrayRange() throws IOException {
    byte[] b = new byte[500];
    int offset = 100;
    int length = 300;
    int bytesRead = filter.read(b, offset, length);
    assertTrue(mockInputStream.getReadBytesCalled());
    assertEquals(b, mockInputStream.getRequestedReadBuffer());
    assertEquals(offset, mockInputStream.getRequestedReadOffset());
    assertEquals(length, mockInputStream.getRequestedReadLength());
    assertEquals(MockInputStream.RETURNED_VALUE_FOR_READ_BYTES, bytesRead);
  }

  public void testReset() throws IOException {
    filter.reset();
    assertTrue(mockInputStream.getResetCalled());
  }

  public void testSkip() throws IOException {
    long bytesToSkip = MockInputStream.RETURNED_VALUE_FOR_SKIP * 3;
    long skippedBytes = filter.skip(bytesToSkip);
    assertTrue(mockInputStream.getSkipCalled());
    assertEquals(bytesToSkip, mockInputStream.getRequestedSkipBytes());
    assertEquals(skippedBytes, mockInputStream.RETURNED_VALUE_FOR_SKIP);
  }
}
