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

import java.io.OutputStream;

/**
 * An {@link OutputFileSet} that discards all data sent to it.
 */
public class NullOutputFileSet extends OutputFileSet {
  static class NullOutputStream extends OutputStream {
    @Override
    public void write(byte[] b) {
    }

    @Override
    public void write(byte[] b, int i, int j) {
    }

    @Override
    public void write(int b) {
    }
  }

  public NullOutputFileSet() {
    super("NULL");
  }

  @Override
  public void close() {
  }

  @Override
  protected OutputStream createNewOutputStream(String path,
      long lastModifiedTime) {
    return new NullOutputStream();
  }
}
