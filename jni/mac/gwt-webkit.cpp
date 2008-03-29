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

#include <iostream>
#include <JavaScriptCore/JavaScriptCore.h>
#include "gwt-webkit.h"
#include "JStringWrap.h"
#include "java-dispatch.h"
#include "trace.h"


// http://unixjunkie.blogspot.com/2006/07/access-argc-and-argv-from-anywhere.html
extern "C" int *_NSGetArgc(void);
extern "C" char ***_NSGetArgv(void);

/*
 *
 */
JSContextRef ToJSContextRef(jint context) {
  return reinterpret_cast<JSContextRef>(context);
}

/*
 *
 */
JSValueRef ToJSValueRef(jint value) {
  return reinterpret_cast<JSValueRef>(value);
}

/*
 *
 */
JSObjectRef ToJSObjectRef(JSContextRef jsContext, jint object,
    JSValueRef* jsException) {
  JSValueRef jsValue = reinterpret_cast<JSValueRef>(object);
  if (!jsValue || !JSValueIsObject(jsContext, jsValue)) {
    return NULL;
  }
  return JSValueToObject(jsContext, jsValue, jsException);
}

/*
 *
 */
JSObjectRef ToJSObjectRef(JSContextRef jsContext, JSValueRef jsValue,
    JSValueRef* jsException) {
  if (!jsValue || !JSValueIsObject(jsContext, jsValue)) {
    return NULL;
  }
  return JSValueToObject(jsContext, jsValue, jsException);
}

/*
 *
 */
JSObjectRef GetStringConstructor(JSContextRef jsContext,
    JSValueRef* jsException) {
  // This could only be cached relative to jsContext.
  JSStringRef script = JSStringCreateWithUTF8CString("(String)");
  JSValueRef ctorVal = JSEvaluateScript(jsContext, script, NULL, NULL, 0, jsException);
  JSStringRelease(script);
  return ToJSObjectRef(jsContext, ctorVal, jsException);
}

/*
 *
 */
bool IsObjectOfStringConstructor(JSContextRef jsContext, JSValueRef jsValue,
    JSValueRef* jsException) {
  JSObjectRef jsObject = ToJSObjectRef(jsContext, jsValue, jsException);
  if (!jsObject) {
    return false;
  }
  JSObjectRef stringCtor = GetStringConstructor(jsContext, jsException);
  if (!stringCtor) {
    return false;
  }
  return JSValueIsInstanceOfConstructor(jsContext, jsObject, stringCtor,
      jsException);
}

#if 0 // For debugging purposes only.
void PrintJSString(JSStringRef jsString)
{
  size_t length = JSStringGetMaximumUTF8CStringSize(jsString);
  char* buffer = new char[length];
  JSStringGetUTF8CString(jsString, buffer, length);
  std::cerr << "JSString: " << buffer << std::endl;
  delete[] buffer;
}

void PrintJSValue(JSContextRef jsContext, JSValueRef jsValue)
{
  JSValueRef jsException = NULL;
  JSStringRef jsResult = JSValueToStringCopy(jsContext, jsValue,
      &jsException);
  if (!jsException && jsValue) {
    PrintJSString(jsResult);
  } else {
    std::cerr << "Could not convert the value to string." << std::endl;
  }
}
#endif

/*
 *
 */
JSStringRef ToJSStringRef(JNIEnv* env, jstring jstr) {
  if (!jstr) {
    return NULL;
  }

  JStringWrap jstrw(env, jstr);
  if (!jstrw.jstr()) {
    return NULL;
  }

  return JSStringCreateWithCharacters(static_cast<const JSChar*>(jstrw.jstr()),
      static_cast<size_t>(jstrw.length()));
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isJsNull
    (JNIEnv *env, jclass klass, jint context, jint value) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }
  TR_LEAVE();
  return static_cast<jboolean>(JSValueIsNull(jsContext, jsValue));
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isJsUndefined
    (JNIEnv *env, jclass klass, jint context, jint value) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }
  TR_LEAVE();
  return static_cast<jboolean>(JSValueIsUndefined(jsContext, jsValue));
}

/*
 *
 */
JNIEXPORT jint JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_getJsUndefined
    (JNIEnv *env, jclass klass, jint context) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    return static_cast<jint>(NULL);
  }

  JSValueRef jsUndefined = JSValueMakeUndefined(jsContext);
  JSValueProtectChecked(jsContext, jsUndefined);
  TR_LEAVE();
  return reinterpret_cast<jint>(jsUndefined);
}

/*
 *
 */
JNIEXPORT jint JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_getJsNull
    (JNIEnv *env, jclass klass, jint context) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    return static_cast<jint>(NULL);
  }
  JSValueRef jsNull = JSValueMakeNull(jsContext);
  JSValueProtectChecked(jsContext, jsNull);
  TR_LEAVE();
  return reinterpret_cast<jint>(jsNull);
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isJsBoolean
    (JNIEnv *env, jclass klass, jint context, jint value) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }
  TR_LEAVE();
  return static_cast<jboolean>(JSValueIsBoolean(jsContext, jsValue));
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isJsNumber
    (JNIEnv *env, jclass klass, jint context, jint value) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }
  TR_LEAVE();
  return static_cast<jboolean>(JSValueIsNumber(jsContext, jsValue));
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_toJsBooleanImpl
    (JNIEnv *env, jclass klass, jint context, jboolean jValue, jintArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueRef jsValue = JSValueMakeBoolean(jsContext, static_cast<bool>(jValue));

  env->SetIntArrayRegion(rval, 0, 1, reinterpret_cast<const jint*>(&jsValue));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }
  JSValueProtectChecked(jsContext, jsValue);

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_gcUnprotect
    (JNIEnv *env, jclass klass, jint context, jint value) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    return;
  }
  JSValueUnprotectChecked(jsContext, jsValue);
  TR_LEAVE();
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_toJsNumberImpl
    (JNIEnv *env, jclass klass, jint context, jdouble jValue, jintArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueRef jsValue = JSValueMakeNumber(jsContext, static_cast<jdouble>(jValue));

  env->SetIntArrayRegion(rval, 0, 1, reinterpret_cast<const jint*>(&jsValue));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueProtectChecked(jsContext, jsValue);

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_executeScriptWithInfoImpl
    (JNIEnv *env, jclass klass, jint context, jstring jScript, jstring jUrl,
    jint jLine, jintArray rval) {
  TR_ENTER();
  JSValueRef jsException = NULL;

  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSStringRef jsScript = ToJSStringRef(env, jScript);
  if (!jsScript) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSStringRef jsUrl = ToJSStringRef(env, jUrl);

  // Evaluate will set this to global object.
  JSValueRef jsResult = JSEvaluateScript(jsContext, jsScript, NULL, jsUrl,
      static_cast<int>(jLine), &jsException);
  if (jsException) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSStringRelease(jsScript);
  if (jsUrl) {
    JSStringRelease(jsUrl);
  }

  env->SetIntArrayRegion(rval, 0, 1, reinterpret_cast<const jint*>(&jsResult));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueProtectChecked(jsContext, jsResult);

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_toJsStringImpl
    (JNIEnv *env, jclass klass, jint context, jstring jValue, jintArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSStringRef jsString = ToJSStringRef(env, jValue);
  if (!jsString) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueRef jsValue = JSValueMakeString(jsContext, jsString);
  JSStringRelease(jsString);

  env->SetIntArrayRegion(rval, 0, 1, reinterpret_cast<const jint*>(&jsValue));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueProtectChecked(jsContext, jsValue);

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isJsStringImpl
    (JNIEnv *env, jclass klass, jint context, jint value, jbooleanArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }

  bool isString = JSValueIsString(jsContext, jsValue);
  if (!isString) {
    JSValueRef jsException = NULL;
    isString = IsObjectOfStringConstructor(jsContext, jsValue, &jsException);
    if (jsException) {
      TR_FAIL();
      return JNI_FALSE;
    }
  }

  jboolean jIsString = static_cast<jboolean>(isString);
  env->SetBooleanArrayRegion(rval, 0, 1, &jIsString);
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_invokeImpl
    (JNIEnv *env, jclass klass, jint context, jint scriptValue,
    jstring jMethodName, jint thisVal, jintArray jArgs, jint jArgsLength,
    jintArray rval) {
  TR_ENTER();

  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSObjectRef jsScriptObj = ToJSObjectRef(jsContext, scriptValue, NULL);
  if (!jsScriptObj) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueRef jsThisVal = ToJSValueRef(thisVal);
  JSObjectRef jsThisObj = NULL;
  // If thisVal is null, jsNull, or jsUndefined use the script object
  // as this.
  if (!jsThisVal || JSValueIsNull(jsContext, jsThisVal)
      || JSValueIsUndefined(jsContext, jsThisVal)) {
    jsThisObj = jsScriptObj;
  } else {
    // If we are given a value, ensure that it is an object.
    jsThisObj = ToJSObjectRef(jsContext, jsThisVal, NULL);
    if (!jsThisObj) {
      TR_FAIL();
      return JNI_FALSE;
    }
  }

  JSStringRef jsMethodName = ToJSStringRef(env, jMethodName);
  if (!jsMethodName) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSObjectRef jsMethod = ToJSObjectRef(jsContext, JSObjectGetProperty(jsContext,
      jsScriptObj, jsMethodName, NULL), NULL);
  if (!jsMethod || !JSObjectIsFunction(jsContext, jsMethod)) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSStringRelease(jsMethodName);

  // NOTE (knorton): Fix for 64-bit.
  JSValueRef* jsArgs = new JSValueRef[static_cast<size_t>(jArgsLength)];
  env->GetIntArrayRegion(jArgs, 0, jArgsLength,
      reinterpret_cast<jint*>(jsArgs));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    delete[] jsArgs;
    return JNI_FALSE;
  }

  JSValueRef jsException = NULL;
  JSValueRef jsResult = JSObjectCallAsFunction(jsContext, jsMethod, jsThisObj,
      static_cast<size_t>(jArgsLength), jsArgs, &jsException);
  if (jsException) {
    TR_FAIL();
    delete[] jsArgs;
    return JNI_FALSE;
  }
  delete[] jsArgs;

  env->SetIntArrayRegion(rval, 0, 1, reinterpret_cast<const jint*>(&jsResult));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueProtectChecked(jsContext, jsResult);

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isJsObject
    (JNIEnv *env, jclass klass, jint context, jint value) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }

  TR_LEAVE();
  return static_cast<jboolean>(JSValueIsObject(jsContext, jsValue));
}

JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_toBooleanImpl
    (JNIEnv *env, jclass klass, jint context, jint value, jbooleanArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }

  jboolean jResult = static_cast<jboolean>(JSValueToBoolean(jsContext, jsValue));
  env->SetBooleanArrayRegion(rval, 0, 1, &jResult);
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_toDoubleImpl
    (JNIEnv *env, jclass klass, jint context, jint value, jdoubleArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueRef jsException = NULL;
  double result = JSValueToNumber(jsContext, jsValue, &jsException);
  if (jsException) {
    TR_FAIL();
    return JNI_FALSE;
  }

  env->SetDoubleArrayRegion(rval, 0, 1, static_cast<jdouble*>(&result));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_toStringImpl
    (JNIEnv *env, jclass klass, jint context, jint value, jobjectArray rval) {
  TR_ENTER();
  JSValueRef jsException = NULL;
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }

  jstring jResult = NULL;
   // Convert all objects to their string representation, EXCEPT
   // null and undefined which will be returned as a true NULL.
  if (!JSValueIsNull(jsContext, jsValue) &&
      !JSValueIsUndefined(jsContext, jsValue)) {
    JSStringRef jsResult = JSValueToStringCopy(jsContext, jsValue, &jsException);
    if (jsException) {
      TR_FAIL();
      return JNI_FALSE;
    }

    jResult = env->NewString(
        static_cast<const jchar*>(JSStringGetCharactersPtr(jsResult)),
        static_cast<jsize>(JSStringGetLength(jsResult)));
    if (env->ExceptionCheck()) {
      TR_FAIL();
      return JNI_FALSE;
    }

    JSStringRelease(jsResult);
  }

  env->SetObjectArrayElement(rval, 0, jResult);
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_wrapDispatchObjectImpl
    (JNIEnv *env, jclass klass, jint context, jobject dispatch, jintArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSObjectRef jsDispatch = gwt::DispatchObjectCreate(jsContext, dispatch);
  if (!jsDispatch || env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  env->SetIntArrayRegion(rval, 0, 1, reinterpret_cast<jint*>(&jsDispatch));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueProtectChecked(jsContext, jsDispatch);

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_unwrapDispatchObjectImpl
    (JNIEnv *env, jclass klass, jint context, jint value, jobjectArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }

  if (!JSValueIsObjectOfClass(jsContext, jsValue, gwt::GetDispatchObjectClass())) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSObjectRef jsObject = ToJSObjectRef(jsContext, jsValue, NULL);
  if (!jsObject) {
    TR_FAIL();
    return JNI_FALSE;
  }

  env->SetObjectArrayElement(rval, 0, reinterpret_cast<jobject>(JSObjectGetPrivate(jsObject)));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_initImpl
    (JNIEnv *env, jclass klass, jclass dispatchObjectClass,
    jclass dispatchMethodClass, jclass lowLevelSafClass) {
  TR_ENTER();
  TR_LEAVE();
  return static_cast<jboolean>(gwt::Initialize(env, dispatchObjectClass, dispatchMethodClass, lowLevelSafClass));
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_wrapDispatchMethodImpl
    (JNIEnv *env, jclass klass, jint context, jstring name, jobject jDispatch,
    jintArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JStringWrap nameWrap(env, name);
  std::string nameStr(nameWrap.str());
  JSObjectRef jsDispatch = gwt::DispatchMethodCreate(jsContext, nameStr,
      jDispatch);
  if (!jsDispatch || env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  env->SetIntArrayRegion(rval, 0, 1, reinterpret_cast<jint*>(&jsDispatch));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueProtectChecked(jsContext, jsDispatch);

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jstring JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_getTypeString
    (JNIEnv *env, jclass klass, jint context, jint value) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    return NULL;
  }

  switch (JSValueGetType(jsContext, jsValue)) {
    case kJSTypeUndefined:
      return env->NewStringUTF("undefined");
    case kJSTypeNull:
      return env->NewStringUTF("null");
    case kJSTypeBoolean:
      return env->NewStringUTF("boolean");
    case kJSTypeNumber:
      return env->NewStringUTF("number");
    case kJSTypeString:
      return env->NewStringUTF("string");
    case kJSTypeObject:
      return (JSValueIsObjectOfClass(jsContext, jsValue, gwt::GetDispatchObjectClass()))
        ? env->NewStringUTF("Java object") : env->NewStringUTF("JavaScript object");
    default:
      return env->NewStringUTF("unknown");
  }
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isDispatchObjectImpl
    (JNIEnv *env, jclass klass, jint context, jint value, jbooleanArray rval) {
  TR_ENTER();
  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    TR_FAIL();
    return JNI_FALSE;
  }

  jboolean jIsDispatchObject = static_cast<jboolean>(JSValueIsObjectOfClass(
      jsContext, jsValue, gwt::GetDispatchObjectClass()));
  env->SetBooleanArrayRegion(rval, 0, 1, &jIsDispatchObject);
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT jint JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_getArgc
    (JNIEnv *env, jclass klass) {
  return *_NSGetArgc();
}

/*
 *
 */
JNIEXPORT jstring JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_getArgv
    (JNIEnv *env, jclass klass, jint index) {
  int argc = *_NSGetArgc();
  if (index < 0 || index >= argc) {
    return 0;
  }
  char **argv = *_NSGetArgv();
  return env->NewStringUTF(argv[index]);
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_getGlobalJsObjectImpl
    (JNIEnv *env, jclass klass, jint context, jintArray rval) {
  TR_ENTER();

  JSContextRef jsContext = ToJSContextRef(context);
  if (!jsContext) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSObjectRef jsGlobalObject = JSContextGetGlobalObject(jsContext);
  env->SetIntArrayRegion(rval, 0, 1, reinterpret_cast<jint*>(&jsGlobalObject));
  if (env->ExceptionCheck()) {
    TR_FAIL();
    return JNI_FALSE;
  }

  JSValueProtectChecked(jsContext, jsGlobalObject);

  TR_LEAVE();
  return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_gcProtect
    (JNIEnv *env, jclass klass, jint context, jint value) {
  TR_ENTER();

  JSContextRef jsContext = ToJSContextRef(context);
  JSValueRef jsValue = ToJSValueRef(value);
  if (!jsContext || !jsValue) {
    return;
  }

  JSValueProtectChecked(jsContext, jsValue);
  TR_LEAVE();
}

/*
 *
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_retainJsGlobalContext
    (JNIEnv *env, jclass klass, jint context) {
  TR_ENTER();
  JSGlobalContextRef jsContext = reinterpret_cast<JSGlobalContextRef>(context);
  if (!jsContext) {
    TR_FAIL();
    return;
  }
  JSGlobalContextRetain(jsContext);
  TR_LEAVE();
}

/*
 *
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_releaseJsGlobalContext
    (JNIEnv *env, jclass klass, jint context) {
  TR_ENTER();
  JSGlobalContextRef jsContext = reinterpret_cast<JSGlobalContextRef>(context);
  if (!jsContext) {
    TR_FAIL();
    return;
  }
  JSGlobalContextRelease(jsContext);
  TR_LEAVE();
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isGcProtected
    (JNIEnv *env, jclass klass, jint value) {
  JSValueRef jsValue = ToJSValueRef(value);
  TR_ENTER();
  TR_LEAVE();
  return static_cast<jboolean>(JSValueIsProtected(jsValue));
}

JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isJsValueProtectionCheckingEnabledImpl
    (JNIEnv *env, jclass klass) {
  TR_ENTER();
  TR_LEAVE();
  return static_cast<jboolean>(JSValueProtectCheckingIsEnabled());
}
