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

#include <cstdio>

#ifdef _WINDOWS
#include <windows.h>
#endif

#include "Debug.h"

#ifdef GWT_DEBUGDISABLE
// Dummy implementations for when debugging has been disabled
Debug::DebugStream& Debug::flush(Debug::DebugStream& dbg) {
  return dbg;
}

void Debug::logFinish() {}

void Debug::logString(const char* str) {}

#else
// Warning: this function is inlined in the manipulator output operator in DebugStream.
// It only remains here because some compiler/linker combinations such as MSVC will
// give unresolved symbol errors if it isn't -- GCC, for example, will completely remove
// all traces of this method.
Debug::DebugStream& Debug::flush(Debug::DebugStream& dbg) {
  Debug::logFinish();
  return dbg;
}

// These methods are implemented in an Objective-C++ file on OSX
#if !defined(__APPLE_CC__) || defined(__mac)

#ifdef _WINDOWS
#define DEBUG_BUF_SIZE 2048

static char buf[DEBUG_BUF_SIZE + 3]; // room for CR NL Null
static char *bufPtr = buf;
#endif

void Debug::logFinish() {
#ifdef _WINDOWS
  logString("\r\n");
  ::OutputDebugStringA(buf);
  bufPtr = buf;
#else
  putchar('\n');
#endif
}

// logStart may be called multiple times per logFinish
void Debug::logStart(LogLevel level) {
}

void Debug::logString(const char* str) {
#ifdef _WINDOWS
  size_t len = strlen(str);
  size_t buflen = DEBUG_BUF_SIZE - (bufPtr - buf);
  if (len >= buflen) {
    len = buflen - 1;
  }
  strncpy_s(bufPtr, buflen, str, len);
  bufPtr += len;
#else
  fputs(str, stdout);
#endif
}
#endif
#endif

