/*
 * Copyright 2007 Google Inc.
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
#ifndef JNI_LINUX_TRACER_H_
#define JNI_LINUX_TRACER_H_

#include <cstdio>
#include <cstdarg>
#include <cstring>
#include <jni.h>

// comment this out to remove almost all runtime overhead (with usual compiler
// support) from tracing.
//#define ENABLE_TRACING

/*
 * Utility class for tracing.  This class is intended to be used as follows:
 * 
 * {
 *   Tracer tracer("method name");
 *   ... do work
 *   if (fail) {
 *      tracer.setFail("failure explanation");
 *      return;
 *   }
 *   if (fail2) {
 *      tracer.throwHostedModeException("failure explanation");
 *      return;
 *   }
 *   return;
 * }
 * 
 * The class automatically logs an enter message when it is created, as well
 * as leave/fail messages when it is destroyed.  Logging is performed to a
 * file or to a Java static member function on a class (or both) -- these
 * are configured by using static member functions setFile() and setJava().
 * 
 * This class knows about the Java class
 *   com.google.gwt.dev.shell.HostedModeException
 * and throws a new instance of that exception if requested.
 */
class Tracer {
public:
  enum LogLevel {
    LEVEL_ERROR = 0,
    LEVEL_WARNING,
    LEVEL_NOTICE,
    LEVEL_INFO,
    LEVEL_DEBUG,
    LEVEL_DEBUG_V1,
    LEVEL_DEBUG_V2,
  };
protected:
#ifdef ENABLE_TRACING
  // static variables that specify where logging is performed.  This are
  // set by calling setFile() and setJava().
  static FILE*     outfp;
  static JNIEnv*   jniEnv;
  static jclass    traceClass;
  static jmethodID traceMethod;
  static int       indentation;
  static LogLevel  logLevel;

  // method is set when the instance is created.
  const char*		method_;
  // fail_msg is set to indicate a failure has occurred.
  const char*		fail_msg_;
  // level of this trace object
  LogLevel  log_level_;
#endif
  
public:
  /*
   * Set the logging level.
   */
  static void setLevel(LogLevel level) {
#ifdef ENABLE_TRACING
    logLevel = level;
#endif
  }
  
protected:
  /*
   * Log a message (with supplied prefix) to the configured file.
   * Only called if a file was specified and successfully opened for writing.
   */
  static void logFile(const char* msg) {
#ifdef ENABLE_TRACING
    for (int i = 0; i < indentation; ++i) {
      putc(' ', outfp);
    }
    fputs(msg, outfp);
    putc('\n', outfp);
    fflush(outfp);
#else
    (void)msg; // avoid unused warning
#endif
  }

  /*
   * Log a message (with supplied prefix) to the configured Java class.
   * Only called if a file was specified and successfully accessed.
   * 
   * Call static void trace(String msg) on the configured class. 
   */
  static void logJava(const char* msg) {
#ifdef ENABLE_TRACING
    // TODO(jat): fixed buffer size
    char buf[512];
    for (int i = 0; (i < indentation) && (i < int(sizeof(buf))); ++i) {
      buf[i] = ' ';
    }
    strncpy(buf + indentation, msg, sizeof(buf) - indentation);
    buf[sizeof(buf) - 1] = 0; // ensure null termination
    jstring str = jniEnv->NewStringUTF(buf);
    jniEnv->CallStaticVoidMethod(traceClass, traceMethod, str);
#else
    (void)msg; // avoid unused warning
#endif
  }

  /*
   * Log a message to a file and/or class with the default logging level.
   * 
   * If the preprocessor symbol DISABLE_TRACING has been defined, this is
   * completely removed from the code path.
   */
  void logPrefix(const char* prefix) {
#ifdef ENABLE_TRACING
    logPrefix(prefix, log_level_);
#else
    (void)prefix; // avoid unused warning
#endif
  }
  
  /*
   * Log a message to a file and/or class.
   * 
   * If the preprocessor symbol DISABLE_TRACING has been defined, this is
   * completely removed from the code path.
   */
  void logPrefix(const char* prefix, LogLevel level) {
#ifdef ENABLE_TRACING
    if (level>logLevel) return;
    log("%-5.5s %s%s%s", prefix, method_, fail_msg_ ? ": " : "",
        fail_msg_ ? fail_msg_ : "");
#endif
  }
    
public:
  /*
   * Create an instance with the specified method name and no failure
   * message.  Log an ENTER message.
   */
  Tracer(const char* method, LogLevel log_level = LEVEL_ERROR)
#ifdef ENABLE_TRACING
      : method_(method), fail_msg_(0), log_level_(log_level) {
    log("ENTER %s", method);
    indentation++;
#else
  { (void)method; (void)log_level; // avoid unused warnings
#endif
  }

  /*
   * Create an instance with the specified method name and no failure
   * message.  Log an ENTER message and the this pointer.
   */
  Tracer(const char* method, const void* objThis,
      LogLevel log_level = LEVEL_ERROR)
#ifdef ENABLE_TRACING
      : method_(method), fail_msg_(0), log_level_(log_level) {
    log("ENTER %s(this=%08x)", method, unsigned(objThis));
    indentation++;
#else
  { (void)method; (void)objThis; (void)log_level; // avoid unused warnings
#endif
  }

  /*
   * Destroy the instance and log a fail or leave message.
   */
  ~Tracer() {
#ifdef ENABLE_TRACING
    --indentation;
    if(fail_msg_) {
      logPrefix("*FAIL", LEVEL_ERROR);
    } else {
      logPrefix("LEAVE");
    }
#endif
  }
  
  /*
   * Specify a filename to receive logging output.  Close any previously
   * opened file.  If a null filename is passed, disable logging to a
   * file.
   * 
   * filename - the file path to receive logging output.  This file is
   *     truncated if it already exists.
   * 
   * Returns false on failure.
   */
  static bool setFile(const char* filename) {
#ifdef ENABLE_TRACING
    if (outfp) {
      fclose(outfp);
      outfp = 0;
    }
    if (!filename) {
      return true;
    }
    outfp = fopen(filename, "w");
    if (!outfp) {
      return false;
    }
    fprintf(outfp, "== started logging ==\n");
    fflush(outfp);
#else
    (void)filename; // avoid unused warning
#endif
    return true;
  }
  
  /*
   * Specify a Java class to receive logging output.  The supplied class
   * must have a static void trace(String) member function which is called
   * for output.  Logging to a Java class is disabled if the supplied JNI
   * environment is null.
   * 
   * env - JNI environment
   * clazz - the Java class to receive logging output
   * 
   * Returns false on failure.
   */ 
  static bool setJava(JNIEnv* env, jclass clazz)
#ifdef ENABLE_TRACING  
  ;
#else
  // inline a null body if we aren't debugging; avoid unused warnings
  { (void)env; (void)clazz; return true; }
#endif
  
  /*
   * Set a failure message, overwriting any previously specified failure
   * message.  Passing a null string will remove any previous failure
   * notification.
   */
  void setFail(const char* fail_msg) {
#ifdef ENABLE_TRACING
    fail_msg_ = fail_msg;
#else
    (void)fail_msg; // avoid unused warning
#endif
  }
  
  /*
   * Throw a Java HostedModeException as well as set a failure message to
   * be logged.
   * 
   * env - JNI environment to throw exception into
   * fail_msg - failure message 
   */
  void throwHostedModeException(JNIEnv* env, const char* fail_msg);
  
  /*
   * Log an arbitrary message.
   */
  static void log(const char* format, ...) {
#ifdef ENABLE_TRACING
    va_list args;
    va_start(args, format);
    char msg[512]; // TODO(jat): fixed size buffer
    vsnprintf(msg, sizeof(msg), format, args);
    msg[sizeof(msg) - 1] = 0; // ensure null termination
    if(outfp) logFile(msg);
    if(jniEnv) logJava(msg);
    va_end(args);
#else
    (void)format; // avoid unused warning
#endif
  }
};

#endif /* JNI_LINUX_TRACER_H_ */
