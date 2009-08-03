#ifndef _H_Debug
#define _H_Debug
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
#ifndef _WINDOWS
// TODO(jat): remove, for snprintf prototypes which should go away
#include <stdio.h>
#endif

#include <ostream>
#include <string>

#include "Platform.h"

// Get a default debug config if none supplied.
#ifndef GWT_DEBUGLEVEL
#include "DebugLevel.h"
#endif

/**
 * Debugging class to get debugging output to a platform-specific location, with multiple
 * levels supported.
 * 
 * To use:
 *   #define GWT_DEBUGLEVEL level  // where level is in LogLevel
 *   #include "Debug.h"
 *   Debug::log(Debug::Warning) << ...
 * 
 * Zero overhead if GWT_DEBUGDISABLE is defined, other than the effort spent on side
 * effects for stream object construction.  If that is expensive, use something like
 * Debug::level(Debug::Warning) to conditionalize expensive logging computations.
 */
class Debug {
public:
  enum LogLevel {
    None,
    Error,
    Warning,
    Info,
    Debugging,
    Spam
  };

private:
  static const LogLevel minLogLevel = GWT_DEBUGLEVEL;

public:
  /**
   * Return true if the requested level would be logged.  Use to protect
   * expensive computations for logging.
   */
  static bool level(LogLevel testLevel) {
#ifdef GWT_DEBUGDISABLE
    return false;
#else
    return testLevel <= minLogLevel;
#endif
  }

private:
  // complete the current log message
  static void logFinish();
  
  // begin a new log message
  static void logStart(LogLevel level);
  
  // add a string to the current log message
  static void logString(const char* str);
  static void logString(const std::string& str) {
    logString(str.c_str());
  }
  
public:
  // Note that flush is special-cased in the manipulator output operator,
  // and its implementation is inlined there.  If its implementation is
  // modified, the implementation of the manipulator output operator will
  // need to be modified as well.
  class DebugStream;
  static DebugStream& flush(DebugStream& dbg);

  class DebugStream {

    const bool shouldLog;

    // TODO(jat): better implementations of output operators
  public:
    DebugStream(LogLevel level) : shouldLog(Debug::level(level)) {
      if (shouldLog) {
        Debug::logStart(level);
      }
    }

    bool isActive() const {
      return shouldLog;
    }

    DebugStream& operator<<(long v) {
      if (shouldLog) {
        char buf[20];
        snprintf(buf, sizeof(buf), "%ld", v);
        Debug::logString(buf);
      }
      return *this;
    }

    DebugStream& operator<<(unsigned long v) {
      if (shouldLog) {
        char buf[20];
        snprintf(buf, sizeof(buf), "%lu", v);
        Debug::logString(buf);
      }
      return *this;
    }

    DebugStream& operator<<(long long v) {
      if (shouldLog) {
        char buf[40];
        snprintf(buf, sizeof(buf), "%lld", v);
        Debug::logString(buf);
      }
      return *this;
    }

    DebugStream& operator<<(unsigned long long v) {
      if (shouldLog) {
        char buf[40];
        snprintf(buf, sizeof(buf), "%llu", v);
        Debug::logString(buf);
      }
      return *this;
    }

    DebugStream& operator<<(int v) {
      if (shouldLog) {
        char buf[20];
        snprintf(buf, sizeof(buf), "%d", v);
        Debug::logString(buf);
      }
      return *this;
    }

    DebugStream& operator<<(unsigned int v) {
      if (shouldLog) {
        char buf[20];
        snprintf(buf, sizeof(buf), "%u", v);
        Debug::logString(buf);
      }
      return *this;
    }

    DebugStream& operator<<(double v) {
      if (shouldLog) {
        char buf[20];
        snprintf(buf, sizeof(buf), "%g", v);
        Debug::logString(buf);
      }
      return *this;
    }

    DebugStream& operator<<(const std::string& str) {
      if (shouldLog) {
        Debug::logString(str);
      }
      return *this;
    }

    DebugStream& operator<<(const char* str) {
      if (shouldLog) {
        Debug::logString(str);
      }
      return *this;
    }

    DebugStream& operator<<(const void* v) {
      if (shouldLog) {
        char buf[20];
        snprintf(buf, sizeof(buf), "%p", v);
        Debug::logString(buf);
      }
      return *this;
    }

    DebugStream& operator<<(DebugStream& (*manip)(DebugStream&)) {
      if (shouldLog) {
        // Special-case flush for efficiency.
        if (manip == Debug::flush) {
          Debug::logFinish();
        } else {
          return manip(*this);
        }
      }
      return *this;
    }
  };

  static DebugStream log(LogLevel testLevel) {
    return DebugStream(testLevel);
  }
};

#endif
