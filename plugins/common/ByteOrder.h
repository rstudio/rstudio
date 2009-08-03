#ifndef __H_ByteOrder
#define __H_ByteOrder
/*
 * Copyright 2008 Google Inc.
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

#include "Platform.h"
#include <cstring>

class ByteOrder {
private:
  enum FloatByteOrder {
    FLOAT_BIG_ENDIAN,
    FLOAT_LITTLE_ENDIAN,
    // TODO(jat): do we need to consider anything other than straight
    // big-endian or little-endian?  PDP-11 (irrelevant), ARM (probably
    // relevant for mobile devices), MIPS (probably not relevant, but maybe)
    // and others have some intermediate endianess.  Also, FP-endianess is not
    // necessarily the same as that for integers.
  };
#ifdef PLATFORM_FLOAT_ENDIANESS
  static const FloatByteOrder floatByteOrder = PLATFORM_FLOAT_ENDIANESS;
#else
  FloatByteOrder floatByteOrder;
#endif

  typedef union {
    float v;
    char b[sizeof(float)];
  } FloatUnion;

  typedef union {
    double v;
    char b[sizeof(double)];
  } DoubleUnion;

  /**
   * Copy src to dest, reversing the order of the bytes.
   * Assumes src and dest do not overlap.
   */
  void memcpyrev(char* dest, const char* src, size_t n) {
    src += n;
    while (n-- > 0) {
      *dest++ = *--src;
    }
  }
public:
  ByteOrder() {
#ifndef PLATFORM_FLOAT_ENDIANESS
    DoubleUnion u;
    memset(u.b, 0, sizeof(u.b));
    u.b[0] = (char) 0x80;
    u.b[7] = (char) 0x02;
    // TODO(jat): add more tests here if we support other endianess
    floatByteOrder = u.v > 0 ? FLOAT_LITTLE_ENDIAN : FLOAT_BIG_ENDIAN;
    if (Debug::level(Debug::Debugging)) {
      std::string str = "Unknown";
      switch (floatByteOrder) {
         case FLOAT_LITTLE_ENDIAN:
           str = "little-endian";
           break;
         case FLOAT_BIG_ENDIAN:
           str = "big-endian";
           break;
      }
      Debug::log(Debug::Debugging) << "Dynamically detected float byte order: "
          << str << Debug::flush;
    }
#endif
  }

  void bytesFromDouble(double v, char* bytes) {
    DoubleUnion u;
    u.v = v;
    switch (floatByteOrder) {
      case FLOAT_LITTLE_ENDIAN:
        memcpyrev(bytes, u.b, sizeof(u.b));
        break;
      case FLOAT_BIG_ENDIAN:
        memcpy(bytes, u.b, sizeof(u.b));
        break;
    }
  }

  void bytesFromFloat(float v, char* bytes) {
    FloatUnion u;
    u.v = v;
    switch (floatByteOrder) {
      case FLOAT_LITTLE_ENDIAN:
        memcpyrev(bytes, u.b, sizeof(u.b));
        break;
      case FLOAT_BIG_ENDIAN:
        memcpy(bytes, u.b, sizeof(u.b));
        break;
    }
  }

  double doubleFromBytes(const char* bytes) {
    DoubleUnion u;
    switch (floatByteOrder) {
      case FLOAT_LITTLE_ENDIAN:
        memcpyrev(u.b, bytes, sizeof(u.b));
        break;
      case FLOAT_BIG_ENDIAN:
        // TODO(jat): find a way to avoid the extra copy while keeping the
        // compiler happy.
        memcpy(u.b, bytes, sizeof(u.b));
        break;
    }
    return u.v;
  }

  float floatFromBytes(const char* bytes) {
    FloatUnion u;
    switch (floatByteOrder) {
      case FLOAT_LITTLE_ENDIAN:
        memcpyrev(u.b, bytes, sizeof(u.b));
        break;
      case FLOAT_BIG_ENDIAN:
        // TODO(jat): find a way to avoid the extra copy while keeping the
        // compiler happy.
        memcpy(u.b, bytes, sizeof(u.b));
        break;
    }
    return u.v;
  }
};

#endif
