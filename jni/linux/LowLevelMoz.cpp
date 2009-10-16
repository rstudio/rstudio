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

// Mozilla-specific hosted-mode methods

// Define to log debug-level output rather than just warnings.
#define DEBUG

#include <cstdio>
#include <cstdarg>
#include <cwchar>

// Mozilla header files
#include "mozilla-headers.h"

#include <jni.h>
#include "gwt-jni.h"
#include "JsRootedValue.h"
#include "ExternalWrapper.h"
#include "Tracer.h"
#include "JsStringWrap.h"

/*
 * Debug definitions -- define FILETRACE to have debug output written to
 * a file named gwt-ll.log, or JAVATRACE to have debug output passed to the
 * Java LowLevelMoz.trace method.
 */
#ifdef ENABLE_TRACING
#define FILETRACE
//#define JAVATRACE
#endif

// include javah-generated header to make sure we match
#include "LowLevelMoz.h"

JNIEnv* savedJNIEnv = 0;
jclass lowLevelMozClass;

// Only include debugging code if we are tracing somewhere.
#ifdef ENABLE_TRACING

/*
 * Template so vsnprintf/vswprintf can be used interchangeably in the
 * append_sprintf template below.
 *   buf - pointer to the start of the output buffer
 *   len - maximum number of characters to write into the buffer
 *         (including the null terminator)
 *   fmt - printf-style format string
 *   args - stdarg-style variable arguments list
 *  Returns the number of characters written (excluding the null terminator)
 *   or -1 if an error occurred.
 * 
 * Note that %lc and %ls are only legal in the wchar_t implementation.
 */
template<class charT>
int safe_vsprintf(charT* buf, size_t len, const charT* fmt, va_list args); 

// specialization for char that maps to vsnprintf
template<>
inline int safe_vsprintf<char>(char* buf, size_t len, const char* fmt,
    va_list args) {
  return ::vsnprintf(buf, len, fmt, args);
}

// specialization for wchar_t that maps to vswprintf
template<>
inline int safe_vsprintf<wchar_t>(wchar_t* buf, size_t len, const wchar_t* fmt,
    va_list args) {
  return ::vswprintf(buf, len, fmt, args);
}

/*
 * Safely append to a string buffer, updating the output pointer and always
 * reserving the last character of the buffer for a null terminator.
 *   bufStart - pointer to the start of the output buffer
 *   bufEnd - pointer just past the end of the output buffer
 *   fmt - format string
 *   additional arguments as passed to *printf
 * Returns the number of characters actually written, not including the null
 * terminator.  Nothing is written, including the null terminator, if the
 * buffer start points beyond the output buffer.
 *
 * Templated to work with any character type that has a safe_vsprintf
 * implementation.
 */
template<class charT>
static int append_sprintf(charT* bufStart, const charT* bufEnd,
    const charT* fmt, ...) {
  va_list args;
  va_start(args, fmt); // initialize variable arguments list
  // compute space left in buffer: -1 for null terminator
  int maxlen = bufEnd - bufStart - 1;
  if (maxlen <= 0) return 0;
  int n = safe_vsprintf(bufStart, maxlen, fmt, args);
  va_end(args);
  if (n > maxlen) {
    n = maxlen;
  }
  bufStart[n] = 0;
  return n;
}

/*
 * Log a given jsval with a prefix.
 *  cx - JSContext for the JS execution context to use
 *  val - jsval to print
 *  prefix - string to print before the value, defaults to empty string
 *
 * TODO(jat): this whole printf-style logging needs to be replaced, but we
 * run into library version issues if we use C++ iostreams so we would need
 * to implement our own equivalent.  Given that this code is all likely to
 * be rewritten for out-of-process hosted mode, it seems unlikely to be worth
 * the effort until that is completed.
 */
void PrintJSValue(JSContext* cx, jsval val, char* prefix="") {
  JSType type = JS_TypeOfValue(cx, val);
  const char* typeString=JS_GetTypeName(cx, type);
  static const int BUF_SIZE = 256;
  char buf[BUF_SIZE];
  const char *bufEnd = buf + BUF_SIZE;
  char* p = buf;
  p += append_sprintf(p, bufEnd, "%s%s", prefix, typeString);
  switch(type) {
    case JSTYPE_VOID:
      break;
    case JSTYPE_BOOLEAN:
      p += append_sprintf(p, bufEnd, ": %s",
          JSVAL_TO_BOOLEAN(val) ? "true" : "false");
      break;
    case JSTYPE_NUMBER:
      if (JSVAL_IS_INT(val)) {
        p += append_sprintf(p, bufEnd, ": %d", JSVAL_TO_INT(val));
      } else {
        p += append_sprintf(p, bufEnd, ": %lf", (double)*JSVAL_TO_DOUBLE(val));
      }
      break;
    case JSTYPE_OBJECT: {
      JSObject* obj = JSVAL_TO_OBJECT(val);
      if (!JSVAL_IS_OBJECT(val)) break;
      JSClass* clazz = obj ? JS_GET_CLASS(cx, obj) : 0;
      p += append_sprintf(p, bufEnd, " @ %08x, class %s",
          (unsigned)obj, clazz ? clazz->name : "<null>");
      break;
    }
    case JSTYPE_FUNCTION:
    case JSTYPE_LIMIT:
      break;
    case JSTYPE_STRING: {
      /*
       * TODO(jat): support JS strings with international characters
       */
      JsStringWrap str(cx, JSVAL_TO_STRING(val));
      p += append_sprintf(p, bufEnd, ": %.*s", str.length(), str.bytes());
      break;
    }
  }
  Tracer::log("%s", buf);
}
#else
// Include a null version just to keep from cluttering up call sites.
static inline void PrintJSValue(JSContext* cx, jsval val, char* prefix="") { }
#endif


static bool InitGlobals(JNIEnv* env, jclass llClass) {
  if (savedJNIEnv)
    return false;

#ifdef FILETRACE
  Tracer::setFile("gwt-ll.log");
#endif // FILETRACE

#ifdef JAVATRACE
  Tracer::setJava(env, llClass);
#endif // JAVATRACE

#ifdef DEBUG
  Tracer::setLevel(Tracer::LEVEL_DEBUG);
#endif

  savedJNIEnv = env;
  lowLevelMozClass = static_cast<jclass>(env->NewGlobalRef(llClass));
  return true;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _executeScriptWithInfo
 * Signature: (ILjava/lang/String;Ljava/lang/String;I)Z
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1executeScriptWithInfo
    (JNIEnv* env, jclass llClass, jint scriptObjectInt, jstring code,
     jstring file, jint line)
{
  Tracer tracer("LowLevelMoz._executeScriptWithInfo");
  JStringWrap jcode(env, code);
  if (!jcode.jstr()) {
    tracer.setFail("null code string");
    return JNI_FALSE;
  }
  JStringWrap jfile(env, file);
  if (!jfile.str()) {
    tracer.setFail("null file name");
    return JNI_FALSE;
  }
  tracer.log("code=%s, file=%s, line=%d", jcode.str(), jfile.str(), line);
  JSContext* cx = JsRootedValue::currentContext();
  nsCOMPtr<nsIScriptContext> scriptContext(GetScriptContextFromJSContext(cx));

  nsIScriptGlobalObject* scriptObject =
      NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjectInt);
  JSObject* scriptWindow =
      reinterpret_cast<JSObject*>(scriptObject->GetGlobalJSObject());
  nsXPIDLString scriptString;
  scriptString = jcode.jstr();

  nsXPIDLString aRetValue;
  PRBool aIsUndefined;
  if (NS_FAILED(scriptContext->EvaluateString(scriptString, scriptWindow, 0,
      jfile.str(), line, 0, aRetValue, &aIsUndefined))) {
    tracer.setFail("EvaluateString failed");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _invoke
 * Signature: (ILjava/lang/String;I[I)I
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1invoke
    (JNIEnv* env, jclass, jint scriptObjInt, jstring methodName, jint jsThisInt,
     jintArray jsArgsInt, jint jsRetValInt)
{
  Tracer tracer("LowLevelMoz._invoke");

  JStringWrap methodStr(env, methodName);
  if (!methodStr.str()) {
    tracer.setFail("null method name");
    return JNI_FALSE;
  }
  JsRootedValue* jsThisRV = reinterpret_cast<JsRootedValue*>(jsThisInt);
  jint jsArgc = env->GetArrayLength(jsArgsInt);
  tracer.log("method=%s, jsthis=%08x, #args=%d", methodStr.str(), jsThisInt,
     jsArgc);
  JSContext* cx = JsRootedValue::currentContext();
  nsIScriptGlobalObject* scriptObject =
      NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
  JSObject* scriptWindow
      = reinterpret_cast<JSObject*>(scriptObject->GetGlobalJSObject());

  jsval fval;
  if (!JS_GetProperty(cx, scriptWindow, methodStr.str(), &fval)) {
    tracer.setFail("JS_GetProperty(method) failed");
    return JNI_FALSE;
  }
  JSFunction* jsFunction = JS_ValueToFunction(cx, fval);
  if (!jsFunction) {
    tracer.setFail("JS_ValueToFunction failed");
    return JNI_FALSE;
  }
  
  // extract arguments in jsval form
  nsAutoArrayPtr<jint> jsargvals(new jint[jsArgc]);
  if (!jsargvals) {
    tracer.setFail("failed to allocate arg array");
    return JNI_FALSE;
  }
  env->GetIntArrayRegion(jsArgsInt, 0, jsArgc, jsargvals);
  if (env->ExceptionCheck()) {
    tracer.setFail("copy from Java array failed");
    return JNI_FALSE;
  }
  nsAutoArrayPtr<jsval> jsargs(new jsval[jsArgc]);
  for (int i = 0; i < jsArgc; ++i) {
    JsRootedValue* arg = reinterpret_cast<JsRootedValue*>(jsargvals[i]);
    jsargs[i] = arg->getValue();
  }

  jsval jsrval;
  JSObject* jsThis;
  if (jsThisRV->isNull()) {
    jsThis = scriptWindow;
  } else {
    jsThis = jsThisRV->getObject();
  }
  
  PrintJSValue(cx, OBJECT_TO_JSVAL(jsThis), "jsThis=");
  for (int i = 0; i < jsArgc; ++i) {
    char buf[256];
    snprintf(buf, sizeof(buf), "arg[%d]=", i);
    PrintJSValue(cx, jsargs[i], buf);
  }
  //tracer.log("fval = %08x, args=%08x", fval, jsargs.get());
  if (!JS_CallFunctionValue(cx, jsThis, fval, jsArgc, jsargs.get(), &jsrval)) {
    tracer.setFail("JS_CallFunctionValue failed");
    return JNI_FALSE;
  }

  PrintJSValue(cx, jsrval, "return value=");
  JsRootedValue* returnVal = reinterpret_cast<JsRootedValue*>(jsRetValInt);
  returnVal->setValue(jsrval);
  return JNI_TRUE;
}


/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _raiseJavaScriptException
 * Signature: ()Z
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1raiseJavaScriptException
    (JNIEnv* env, jclass)
{
  Tracer tracer("LowLevelMoz._raiseJavaScriptException");
  JS_SetPendingException(JsRootedValue::currentContext(), JSVAL_NULL);
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _registerExternalFactoryHandler
 * Signature: ()Z
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1registerExternalFactoryHandler
    (JNIEnv* env, jclass llClass)
{
  if (!InitGlobals(env, llClass))
    return JNI_FALSE;

  // tracing isn't setup until after InitGlobals is called
  Tracer tracer("LowLevelMoz._registerExternalFactoryHandler");

  char buf[256];
  sprintf(buf, " jniEnv=%08x, llClass=%08x", (unsigned)env, (unsigned)llClass);
  tracer.log(buf);
  
  // Register "window.external" as our own class
  if (NS_FAILED(nsComponentManager::RegisterFactory(
      kGwtExternalCID, "externalFactory", GWT_EXTERNAL_CONTRACTID,
      new nsRpExternalFactory(), PR_TRUE))) {
    tracer.setFail("RegisterFactory failed");
    return JNI_FALSE;
  }

  nsCOMPtr<nsICategoryManager> categoryManager =
      do_GetService(NS_CATEGORYMANAGER_CONTRACTID);
  if (!categoryManager) {
    tracer.setFail("unable to get category manager");
    return JNI_FALSE;
  }

  nsXPIDLCString previous;
  if (NS_FAILED(categoryManager->AddCategoryEntry(
      JAVASCRIPT_GLOBAL_PROPERTY_CATEGORY, "external", GWT_EXTERNAL_CONTRACTID,
      PR_TRUE, PR_TRUE, getter_Copies(previous)))) {
    tracer.setFail("AddCategoryEntry failed");
    return JNI_FALSE;
  }

  return JNI_TRUE;
}
