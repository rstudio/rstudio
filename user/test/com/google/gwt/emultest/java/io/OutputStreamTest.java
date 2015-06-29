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
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Unit test for the {@link java.io.OutputStream} emulated class.
 */
public class OutputStreamTest extends OutputStreamBaseTest {

  private static LinkedList<Byte> outputBytes;

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    outputBytes = new LinkedList<>();
  }

  @Override
  protected OutputStream createDefaultOutputStream() {
    return new OutputStream() {
      @Override public void write(int b) {
        outputBytes.add((byte) b);
      }
    };
  }

  @Override
  protected byte[] getBytesWritten() {
    byte[] bytesWritten = new byte[outputBytes.size()];
    int i = 0;
    for (Byte nextByte : outputBytes) {
      bytesWritten[i++] = nextByte;
    }
    return bytesWritten;
  }

  public void testDefaultBehaviorOfClose() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    // should do nothing (including not throwing an exception).
    outputStream.close();
  }

  public void testDefaultBehaviorOfFlush() throws IOException {
    final OutputStream outputStream = createDefaultOutputStream();
    // should do nothing (including not throwing an exception).
    outputStream.flush();
  }
}
