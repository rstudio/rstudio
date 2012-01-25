/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.typedarrays.server;

import com.google.gwt.typedarrays.shared.ArrayBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Pure Java implementation of {@link ArrayBuffer}, plus package-protected methods
 * for use by related implementation classes.
 */
public class ArrayBufferImpl implements ArrayBuffer {

  private final Object lock = new Object();

  private final ByteBuffer buf;

  /**
   * @param length
   */
  public ArrayBufferImpl(int length) {
    buf = ByteBuffer.allocate(length);
    // JS lets native byte order show through, which is typically little-endian
    // so if there is code that incorrectly assumes anything, it will be
    // little-endian.
    buf.order(ByteOrder.LITTLE_ENDIAN);
  }

  @Override
  public int byteLength() {
    return buf.capacity();
  }

  float getFloat32(int byteOffset, boolean littleEndian) {
    synchronized (lock) {
      try {
        if (!littleEndian) {
          buf.order(ByteOrder.BIG_ENDIAN);
        }
        return buf.getFloat(byteOffset);
      } finally {
        if (!littleEndian) {
          buf.order(ByteOrder.LITTLE_ENDIAN);
        }
      }
    }
  }

  double getFloat64(int byteOffset, boolean littleEndian) {
    synchronized (lock) {
      try {
        if (!littleEndian) {
          buf.order(ByteOrder.BIG_ENDIAN);
        }
        return buf.getDouble(byteOffset);
      } finally {
        if (!littleEndian) {
          buf.order(ByteOrder.LITTLE_ENDIAN);
        }
      }
    }
  }

  short getInt16(int byteOffset, boolean littleEndian) {
    synchronized (lock) {
      try {
        if (!littleEndian) {
          buf.order(ByteOrder.BIG_ENDIAN);
        }
        return buf.getShort(byteOffset);
      } finally {
        if (!littleEndian) {
          buf.order(ByteOrder.LITTLE_ENDIAN);
        }
      }
    }
  }

  int getInt32(int byteOffset, boolean littleEndian) {
    synchronized (lock) {
      try {
        if (!littleEndian) {
          buf.order(ByteOrder.BIG_ENDIAN);
        }
        return buf.getInt(byteOffset);
      } finally {
        if (!littleEndian) {
          buf.order(ByteOrder.LITTLE_ENDIAN);
        }
      }
    }
  }

  byte getInt8(int byteOffset) {
    return buf.get(byteOffset);
  }

  void setFloat32(int byteOffset, float value, boolean littleEndian) {
    synchronized (lock) {
      try {
        if (!littleEndian) {
          buf.order(ByteOrder.BIG_ENDIAN);
        }
        buf.putFloat(byteOffset, value);
      } finally {
        if (!littleEndian) {
          buf.order(ByteOrder.LITTLE_ENDIAN);
        }
      }
    }
  }

  void setFloat64(int byteOffset, double value, boolean littleEndian) {
    synchronized (lock) {
      try {
        if (!littleEndian) {
          buf.order(ByteOrder.BIG_ENDIAN);
        }
        buf.putDouble(byteOffset, value);
      } finally {
        if (!littleEndian) {
          buf.order(ByteOrder.LITTLE_ENDIAN);
        }
      }
    }
  }

  void setInt16(int byteOffset, short value, boolean littleEndian) {
    synchronized (lock) {
      try {
        if (!littleEndian) {
          buf.order(ByteOrder.BIG_ENDIAN);
        }
        buf.putShort(byteOffset, value);
      } finally {
        if (!littleEndian) {
          buf.order(ByteOrder.LITTLE_ENDIAN);
        }
      }
    }
  }

  void setInt32(int byteOffset, int value, boolean littleEndian) {
    synchronized (lock) {
      try {
        if (!littleEndian) {
          buf.order(ByteOrder.BIG_ENDIAN);
        }
        buf.putInt(byteOffset, value);
      } finally {
        if (!littleEndian) {
          buf.order(ByteOrder.LITTLE_ENDIAN);
        }
      }
    }
  }

  void setInt8(int byteOffset, byte value) {
    buf.put(byteOffset, value);
  }
}
