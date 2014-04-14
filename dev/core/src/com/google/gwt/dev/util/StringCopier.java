/*
 * Copyright 2006 Google Inc.
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

import java.io.CharArrayWriter;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Utility class for directly modifying a character array.
 */
public class StringCopier {
  private final CharArrayWriter out = new CharArrayWriter();
  private final char[] in;
  private int inPos = 0;

  public StringCopier(char[] in) {
    this.in = in;
  }

  public void commit(char[] replaceWith, int startReplace, int endReplace) {
    if (startReplace < inPos) {
      throw new BufferUnderflowException();
    }
    if (endReplace > in.length) {
      throw new BufferOverflowException();
    }

    // commit any characters up to the beginning of the replacement
    out.write(in, inPos, startReplace - inPos);

    // commit the replacement
    out.write(replaceWith, 0, replaceWith.length);

    // skip over the replaced characters
    inPos = endReplace;
  }

  public char[] finish() {
    // commit any uncommitted characters
    out.write(in, inPos, in.length - inPos);
    inPos = in.length;
    return out.toCharArray();
  }
}
