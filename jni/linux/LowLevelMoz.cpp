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

/*
 * Debug definitions -- define FILETRACE to have debug output written to
 * a file named gwt-ll.log, or JAVATRACE to have debug output passed to the
 * Java LowLevelMoz.trace method.
 */
//#define FILETRACE
//#define JAVATRACE

#if defined(FILETRACE) || defined(JAVATRACE)
#define ANYTRACE
#endif

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

// include javah-generated header to make sure we match
#include "LowLevelMoz.h"

// TODO(jat) should be in a header
extern nsCID kGwtExternalCID;

JNIEnv* savedJNIEnv = 0;
jclass lowLevelMozClass;

// Only include debugging code if we are tracing somewhere.
#ifdef ANYTRACE

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
static void PrintJSValue(JSContext* cx, jsval val, char* prefix="") {
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
 * Print the current Java exception.
 */
static void PrintJavaException(JNIEnv* env) {
  jobject exception = env->ExceptionOccurred();
  if (!exception) return;
  fprintf(stderr, "Exception occurred:\n");
  env->ExceptionDescribe();
  env->DeleteLocalRef(exception);
}

/* Called from JavaScript to call a Java method that has previously been
 * wrapped. 
 */ 
JSBool invokeJavaMethod(JSContext *cx, JSObject *obj, uintN argc, jsval *argv,
    jsval *rval)
{
  Tracer tracer("invokeJavaMethod");

  // I kid you not; this is how XPConnect gets their function object so they can
  // multiplex dispatch the call from a common site.  See XPCDispObject.cpp(466)
  //
  // I now have a secondary confirmation that this trick is legit.
  // brandon@mozilla.org writes:
  //
  // argv[-2] is part of the JS API, unabstracted.  Just as argv[0] is the
  // first argument (if argc != 0), argv[-1] is the |this| parameter (equal
  // to OBJECT_TO_JSVAL(obj) in a native method with the standard |obj|
  // second formal parameter name), and argv[-2] is the callee object, tagged
  // as a jsval.
  if (JS_TypeOfValue(cx, argv[-2]) != JSTYPE_FUNCTION) {
    tracer.setFail("not a function type");
    return JS_FALSE;
  }
  JSObject* funObj = JSVAL_TO_OBJECT(argv[-2]);

  // Pull the wrapper object out of the funObj's reserved slot
  jsval jsCleanupObj;
  if (!JS_GetReservedSlot(cx, funObj, 0, &jsCleanupObj)) {
    tracer.setFail("JS_GetReservedSlot failed");
    return JS_FALSE;
  }
  JSObject* cleanupObj = JSVAL_TO_OBJECT(jsCleanupObj);
  if (!cleanupObj) {
    tracer.setFail("cleanupObj is null");
    return JS_FALSE;
  }

  // Get DispatchMethod instance out of the wrapper object
  jobject dispMeth =
      NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, cleanupObj));
  if (!dispMeth) {
    tracer.setFail("dispMeth is null");
    return JS_FALSE;
  }
  jclass dispClass = savedJNIEnv->GetObjectClass(dispMeth);
  if (!dispClass || savedJNIEnv->ExceptionCheck()) {
      tracer.setFail("GetObjectClass returns null");
      return JS_FALSE;
  }

  // lookup the invoke method on the dispatch object
  jmethodID invokeID =
      savedJNIEnv->GetMethodID(dispClass, "invoke", "(II[II)V");
  if (!invokeID || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("GetMethodID failed");
    return JS_FALSE;
  }

  // create an array of integers to hold the JsRootedValue pointers passed
  // to the invoke method
  jintArray args = savedJNIEnv->NewIntArray(argc);
  if (!args || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("NewIntArray failed");
    return JS_FALSE;
  }

  // these arguments are already rooted by the JS interpreter, but we
  // can't easily take advantage of that without complicating the JsRootedValue
  // interface.
  
  // argv[-1] is OBJECT_TO_JSVAL(this)
  JsRootedValue* jsThis = new JsRootedValue(cx, argv[-1]);
  tracer.log("jsthis=%08x, RV=%08x", unsigned(argv[-1]), unsigned(jsThis));

  // create JsRootedValues for arguments  
  JsRootedValue *jsArgs[argc]; 
  for (uintN i = 0; i < argc; ++i) {
    jsArgs[i] = new JsRootedValue(cx, argv[i]);
  }
  savedJNIEnv->SetIntArrayRegion(args, 0, argc,
      reinterpret_cast<jint*>(jsArgs));
  if (savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("SetIntArrayRegion failed");
    return JS_FALSE;
  }
  
  // slot for return value
  JsRootedValue* jsReturnVal = new JsRootedValue(cx);

  // TODO(jat): small window here where invocation may fail before Java
  // takes ownership of the JsRootedValue objects.  One solution would be
  // to reference-count them between Java and C++ (so the reference count
  // would always be 0, 1, or 2).  Also setField has a similar problem.
  // I plan to fix this when switching away from Java holding pointers to
  // C++ objects as part of the fix for 64-bit support (which we could
  // accomplish inefficiently by changing int to long everywhere, but there
  // are other 64-bit issues to resolve and we need to reduce the number of
  // roots the JS interpreter has to search.
  
  // call Java method
  savedJNIEnv->CallVoidMethod(dispMeth, invokeID, reinterpret_cast<int>(cx),
      reinterpret_cast<int>(jsThis), args,
      reinterpret_cast<int>(jsReturnVal));
  
  JSBool returnValue = JS_TRUE;
  
  if (savedJNIEnv->ExceptionCheck()) {
    tracer.log("dispMeth=%08x", unsigned(dispMeth));
    tracer.setFail("java exception is active:");
    PrintJavaException(savedJNIEnv);
    returnValue = JS_FALSE;
  } else if (JS_IsExceptionPending(cx)) {
    tracer.setFail("js exception is active");
    returnValue = JS_FALSE;
  }

  // extract return value
  *rval = jsReturnVal->getValue();

#if 0
  // NOTE: C++ objects are not cleaned up here because Java now owns them.
  // TODO(jat): if reference-counted, they *do* need to be Released here.
  
  // free JsRootedValues
  for (uintN i = 0; i < argc; ++i) {
    delete jsArgs[i];
  }
  delete jsThis;
  delete jsReturnVal;
#endif

  return returnValue;
}

/*
 * Helper function to get reference Java attributes from Javascript.
 * 
 * cx - JSContext pointer
 * obj - JavaScript object which is a wrapped Java object
 * id - property name, as a jsval string
 * dispClass - output parameter of DispatchMethod subclass
 * dispObj - output parameter of Java object
 * jident - output parameter of property name as a Java string
 */
JSBool getJavaPropertyStats(JSContext *cx, JSObject *obj, jsval id,
    jclass& dispClass, jobject& dispObj, jstring& jident)
{
  Tracer tracer("getJavaPropertyStats");
  if (!JSVAL_IS_STRING(id)) {
    tracer.setFail("id is not a string");
    return JS_FALSE;
  }

  jident = savedJNIEnv->NewString(JS_GetStringChars(JSVAL_TO_STRING(id)),
      JS_GetStringLength(JSVAL_TO_STRING(id)));
  if (!jident || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("unable to create Java string");
    return JS_FALSE;
  }

  dispObj = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
  if (!dispObj) {
    tracer.setFail("can't get dispatch object");
    return JS_FALSE;
  }

  dispClass = savedJNIEnv->GetObjectClass(dispObj);
  if (savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("can't get class of dispatch object");
    return JS_FALSE;
  }

  return JS_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _executeScriptWithInfo
 * Signature: (ILjava/lang/String;Ljava/lang/String;I)Z
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1executeScriptWithInfo
    (JNIEnv* env, jclass llClass, jint scriptObject, jstring code,
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

  nsIScriptGlobalObject* globalObject =
      NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObject);
  nsCOMPtr<nsIScriptContext> scriptContext(globalObject->GetContext());
  nsXPIDLString scriptString;
  scriptString = jcode.jstr();

  nsXPIDLString aRetValue;
  PRBool aIsUndefined;
  if (NS_FAILED(scriptContext->EvaluateString(scriptString,
      globalObject->GetGlobalJSObject(), 0, jfile.str(), line, 0,
      aRetValue, &aIsUndefined))) {
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

  nsIScriptGlobalObject* scriptObject =
      NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
  nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
  if (!scriptContext) {
    tracer.setFail("can't get script context");
    return JNI_FALSE;
  }
  JSContext* cx
      = reinterpret_cast<JSContext*>(scriptContext->GetNativeContext());
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
 * Signature: (I)Z
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1raiseJavaScriptException
    (JNIEnv* env, jclass, jint jscontext)
{
  Tracer tracer("LowLevelMoz._raiseJavaScriptException");
  JSContext* cx = reinterpret_cast<JSContext*>(jscontext);
  JS_SetPendingException(cx, JSVAL_NULL);
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
