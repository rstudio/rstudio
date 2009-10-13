/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * A utility for tests that allow writing to a temporary buffer and reading
 * from the same buffer to verify that serialization/deserialization works.
 */
public class TemporaryBufferStream {

  private ArrayList<Byte> buf = new ArrayList<Byte>();

  private InputStream inputStream = new InputStream() {
    @Override
    public int read() throws IOException {
      try {
        return buf.remove(0) & 255;
      } catch (IndexOutOfBoundsException e) {
        return -1;
      }
    }
  };
  
  private OutputStream outputStream = new OutputStream() {
    @Override
    public void write(int b) throws IOException {
      buf.add(Byte.valueOf((byte) b));
    }
  };
  
  public InputStream getInputStream() {
    return inputStream;
  }
  
  public OutputStream getOutputStream() {
    return outputStream;
  }
}
