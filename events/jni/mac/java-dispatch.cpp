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

#include <string>
#include <sstream>
#include "jni.h"
#include "java-dispatch.h"
#include "trace.h"
#include "JStringWrap.h"

namespace gwt {

  /*
   * Declarations for private functions.
   */
  JSClassRef DispatchObjectClassCreate();

  JSClassRef DispatchMethodClassCreate();

  JSValueRef DispatchObjectGetProperty(JSContextRef, JSObjectRef, JSStringRef,
                                       JSValueRef*);

  JSValueRef DispatchObjectToString(JSContextRef, JSObjectRef, JSObjectRef,
                                    size_t, const JSValueRef*, JSValueRef*);

  bool DispatchObjectSetProperty(JSContextRef, JSObjectRef, JSStringRef,
                                 JSValueRef, JSValueRef*);

  void DispatchObjectFinalize(JSObjectRef);

  JSValueRef DispatchMethodCallAsFunction(JSContextRef, JSObjectRef,
                                          JSObjectRef, size_t,
                                          const JSValueRef*, JSValueRef*);

  JSValueRef DispatchMethodGetToString(JSContextRef, JSObjectRef, JSStringRef,
                                       JSValueRef*);

  JSValueRef DispatchMethodToString(JSContextRef, JSObjectRef, JSObjectRef,
                                    size_t, const JSValueRef*, JSValueRef*);

  void DispatchMethodFinalize(JSObjectRef);

  /*
   * Call this when an underlying Java Object should be freed.
   */
  void ReleaseJavaObject(jobject jObject);


  /*
   * The class definition stuct for DispatchObjects.
   */
  static JSClassDefinition _dispatchObjectClassDef = { 0,
      kJSClassAttributeNone, "DispatchObject", 0, 0, 0, 0,
      DispatchObjectFinalize, 0, DispatchObjectGetProperty,
      DispatchObjectSetProperty, 0, 0, 0, 0, 0, 0 };

  /*
   * The class definition structs for DispatchMethods.
   */
  static JSStaticValue _dispatchMethodStaticValues[] = {
    { "toString", DispatchMethodGetToString, 0, kJSPropertyAttributeNone },
    { 0, 0, 0, 0 }
  };
  static JSClassDefinition _dispatchMethodClassDef = { 0,
      kJSClassAttributeNoAutomaticPrototype, "DispatchMethod", 0,
      _dispatchMethodStaticValues, 0, 0, DispatchMethodFinalize, 0, 0, 0, 0,
      0, DispatchMethodCallAsFunction, 0, 0, 0 };

  /*
   * The classes used to create DispatchObjects and DispatchMethods.
   */
  static JSClassRef _dispatchObjectClass = DispatchObjectClassCreate();
  static JSClassRef _dispatchMethodClass = DispatchMethodClassCreate();

  /*
   * Java class and method references needed to do delegation.
   */
  
  /*
   * The main JVM, used by foreign threads to attach.
   */
  static JavaVM* _javaVM;

  /*
   * Only valid for the main thread!  WebKit can finalized on a foreign thread.
   */
  static JNIEnv* _javaEnv;

  static jclass _javaDispatchObjectClass;
  static jclass _javaDispatchMethodClass;
  static jclass _lowLevelSafClass;
  static jmethodID _javaDispatchObjectSetFieldMethod;
  static jmethodID _javaDispatchObjectGetFieldMethod;
  static jmethodID _javaDispatchMethodInvokeMethod;
  static jmethodID _javaDispatchObjectToStringMethod;
  static jmethodID _lowLevelSafReleaseObject;

  /*
   * Structure to hold DispatchMethod private data.
   *
   * NOTE: utf8Name is defensively copied.
   */
  class DispatchMethodData {
   public:
    DispatchMethodData(jobject jObject, std::string& utf8Name)
        : _jObject(jObject), _utf8Name(utf8Name) { }
    ~DispatchMethodData() {
      ReleaseJavaObject(_jObject);
    }
    jobject _jObject;
    std::string _utf8Name;
  };

/*
 * The following takes the prototype from the Function constructor, this allows
 * us to easily support call and apply on our objects that support CallAsFunction.
 *
 * NOTE: The return value is not protected.
 */
JSValueRef GetFunctionPrototype(JSContextRef jsContext, JSValueRef* exception) {
  TR_ENTER();
  JSObjectRef globalObject = JSContextGetGlobalObject(jsContext);
  JSStringRef fnPropName= JSStringCreateWithUTF8CString("Function");
  JSValueRef fnCtorValue = JSObjectGetProperty(jsContext, globalObject,
      fnPropName, exception);
  JSStringRelease(fnPropName);
  if (!fnCtorValue) {
    return JSValueMakeUndefined(jsContext);
  }

  JSObjectRef fnCtorObject = JSValueToObject(jsContext, fnCtorValue, exception);
  if (!fnCtorObject) {
    return JSValueMakeUndefined(jsContext);
  }

  JSStringRef protoPropName = JSStringCreateWithUTF8CString("prototype");
  JSValueRef fnPrototype = JSObjectGetProperty(jsContext, fnCtorObject,
      protoPropName, exception);
  JSStringRelease(protoPropName);
  if (!fnPrototype) {
    return JSValueMakeUndefined(jsContext);
  }

  TR_LEAVE();
  return fnPrototype;
}

/*
 *
 */
JSClassRef GetDispatchObjectClass() {
  TR_ENTER();
  TR_LEAVE();
  return _dispatchObjectClass;
}

/*
 *
 */
JSClassRef GetDispatchMethodClass() {
  TR_ENTER();
  TR_LEAVE();
  return _dispatchMethodClass;
}

/*
 *
 */
JSClassRef DispatchObjectClassCreate() {
  TR_ENTER();
  JSClassRef dispClass = JSClassCreate(&_dispatchObjectClassDef);
  JSClassRetain(dispClass);
  TR_LEAVE();
  return dispClass;
}

/*
 *
 */
JSClassRef DispatchMethodClassCreate() {
  TR_ENTER();
  JSClassRef dispClass = JSClassCreate(&_dispatchMethodClassDef);
  JSClassRetain(dispClass);
  TR_LEAVE();
  return dispClass;
}

/*
 * NOTE: The object returned from this function is not protected.
 */
JSObjectRef DispatchObjectCreate(JSContextRef jsContext, jobject jObject) {
  TR_ENTER();
  JSObjectRef dispInst = JSObjectMake(jsContext, _dispatchObjectClass,
      _javaEnv->NewGlobalRef(jObject));
  TR_LEAVE();
  return dispInst;
}

/*
 * NOTE: The object returned from this function is not protected.
 */
JSObjectRef DispatchMethodCreate(JSContextRef jsContext, std::string& name,
    jobject jObject) {
  TR_ENTER();
 
  JSObjectRef dispInst = JSObjectMake(jsContext, _dispatchMethodClass,
      new DispatchMethodData(_javaEnv->NewGlobalRef(jObject), name));

  // This could only be cached relative to jsContext.
  JSValueRef fnProtoValue = GetFunctionPrototype(jsContext, NULL);
  JSObjectSetPrototype(jsContext, dispInst, fnProtoValue);
  TR_LEAVE();
  return dispInst;
}

/*
 * NOTE: The value returned from this function is not protected, but all
 * JSValues that are passed into Java are protected before the invocation.
 */
JSValueRef DispatchObjectGetProperty(JSContextRef jsContext,
    JSObjectRef jsObject, JSStringRef jsPropertyName,
    JSValueRef* jsException) {
  TR_ENTER();

  // If you call toString on a DispatchObject, you should get the results
  // of the java object's toString invcation.
  if (JSStringIsEqualToUTF8CString(jsPropertyName, "toString")) {
    JSObjectRef jsFunction = JSObjectMakeFunctionWithCallback(jsContext,
        jsPropertyName, DispatchObjectToString);
    return jsFunction;
  }

  // The class check is omitted because it should not be possible to tear off
  // a getter.
  jobject jObject = reinterpret_cast<jobject>(JSObjectGetPrivate(jsObject));

  jstring jPropertyName = _javaEnv->NewString(
      static_cast<const jchar*>(JSStringGetCharactersPtr(jsPropertyName)),
      static_cast<jsize>(JSStringGetLength(jsPropertyName)));
  if (!jObject || !jPropertyName || _javaEnv->ExceptionCheck()) {
    TR_FAIL();
    _javaEnv->ExceptionClear();
    return JSValueMakeUndefined(jsContext);
  }

  JSValueRef jsResult = reinterpret_cast<JSValueRef>(
      _javaEnv->CallIntMethod(jObject, _javaDispatchObjectGetFieldMethod,
      reinterpret_cast<jint>(jsContext),
      jPropertyName));
  if (!jsResult || _javaEnv->ExceptionCheck()) {
    TR_FAIL();
    _javaEnv->ExceptionClear();
    return JSValueMakeUndefined(jsContext);
  }

  // Java left us an extra reference to eat.
  JSValueUnprotectChecked(jsContext, jsResult);
  TR_LEAVE();
  return jsResult;
}

/*
 *
 */
bool DispatchObjectSetProperty(JSContextRef jsContext, JSObjectRef jsObject,
    JSStringRef jsPropertyName, JSValueRef jsValue, JSValueRef* jsException) {
  TR_ENTER();

  // The class check is omitted because it should not be possible to tear off
  // a getter.
  jobject jObject = reinterpret_cast<jobject>(JSObjectGetPrivate(jsObject));

  jstring jPropertyName = _javaEnv->NewString(
      static_cast<const jchar*>(JSStringGetCharactersPtr(jsPropertyName)),
      static_cast<jsize>(JSStringGetLength(jsPropertyName)));
  if (!jObject || !jPropertyName || _javaEnv->ExceptionCheck()) {
    _javaEnv->ExceptionClear();
    return false;
  }

  JSValueProtectChecked(jsContext, jsValue);

  _javaEnv->CallIntMethod(jObject, _javaDispatchObjectSetFieldMethod,
      reinterpret_cast<jint>(jsContext), jPropertyName,
      reinterpret_cast<jint>(jsValue));
  if (_javaEnv->ExceptionCheck()) {
    _javaEnv->ExceptionClear();
    return false;
  }

  TR_LEAVE();
  return true;
}

/*
 *
 */
void DispatchObjectFinalize(JSObjectRef jsObject) {
  TR_ENTER();
  jobject jObject = reinterpret_cast<jobject>(JSObjectGetPrivate(jsObject));
  ReleaseJavaObject(jObject);
  TR_LEAVE();
}

/*
 *
 */
void DispatchMethodFinalize(JSObjectRef jsObject) {
  TR_ENTER();
  DispatchMethodData* data = reinterpret_cast<DispatchMethodData*>(
      JSObjectGetPrivate(jsObject));
  delete data;
  TR_LEAVE();
}

/*
 * NOTE: The value returned from this function is not protected.
 */
JSValueRef DispatchObjectToString(JSContextRef jsContext, JSObjectRef,
    JSObjectRef jsThis, size_t, const JSValueRef*, JSValueRef*) {
  TR_ENTER();

  // This function cannot be torn off and applied to any JSValue. If this does
  // not reference a DispatchObject, return undefined.
  if (!JSValueIsObjectOfClass(jsContext, jsThis, GetDispatchObjectClass())) {
    return JSValueMakeUndefined(jsContext);
  }

  jobject jObject = reinterpret_cast<jobject>(JSObjectGetPrivate(jsThis));
  jstring jResult = reinterpret_cast<jstring>(
      _javaEnv->CallObjectMethod(jObject, _javaDispatchObjectToStringMethod));
  if (_javaEnv->ExceptionCheck()) {
    return JSValueMakeUndefined(jsContext);
  } else if (!jResult) {
    return JSValueMakeNull(jsContext);
  } else {
    JStringWrap result(_javaEnv, jResult);
    JSStringRef resultString = JSStringCreateWithCharacters(
        static_cast<const JSChar*>(result.jstr()),
        static_cast<size_t>(result.length()));
    JSValueRef jsResultString = JSValueMakeString(jsContext, resultString);
    JSStringRelease(resultString);
    return jsResultString;
  }
  TR_LEAVE();
}

/*
 *
 */
JSValueRef DispatchMethodCallAsFunction(JSContextRef jsContext,
    JSObjectRef jsFunction, JSObjectRef jsThis, size_t argumentCount,
    const JSValueRef arguments[], JSValueRef* exception) {
  TR_ENTER();

  // We don't need to check the class here because we take the private
  // data from jsFunction and not jsThis.

  DispatchMethodData* data = reinterpret_cast<DispatchMethodData*>(
      JSObjectGetPrivate(jsFunction));
  jobject jObject = data->_jObject;

  jintArray jArguments = _javaEnv->NewIntArray(argumentCount);
  if (!jArguments || _javaEnv->ExceptionCheck()) {
    return JSValueMakeUndefined(jsContext);
  }

  // This single element int array will be passed into the java call to allow the
  // called java method to raise an exception. We will check for a non-null value
  // after the call is dispatched.
  jintArray jException = _javaEnv->NewIntArray(1);
  if (!jException || _javaEnv->ExceptionCheck()) {
    return JNI_FALSE;
  }

  for (size_t i = 0; i < argumentCount; ++i) {
    JSValueRef arg = arguments[i];
    // Java will take ownership of the arguments.
    JSValueProtectChecked(jsContext, arg);
    _javaEnv->SetIntArrayRegion(jArguments, i, 1, reinterpret_cast<jint*>(&arg));
    if (_javaEnv->ExceptionCheck()) {
      return JSValueMakeUndefined(jsContext);
    }
  }

  // Java will take ownership of this.
  JSValueProtectChecked(jsContext, jsThis);

  JSValueRef jsResult = reinterpret_cast<JSValueRef>(_javaEnv->CallIntMethod(jObject,
      _javaDispatchMethodInvokeMethod, reinterpret_cast<jint>(jsContext),
      reinterpret_cast<jint>(jsThis), jArguments, jException));
  if (_javaEnv->ExceptionCheck()) {
    return JSValueMakeUndefined(jsContext);
  }

  JSValueRef jsException = NULL;
  _javaEnv->GetIntArrayRegion(jException, 0, 1, reinterpret_cast<jint*>(&jsException));
  if (!_javaEnv->ExceptionCheck() && jsException) {
    // If the java dispatch set an exception, then we pass it back to our caller.
	if (exception) {
      *exception = jsException;
	}
    // Java left us an extra reference to eat.
    JSValueUnprotectChecked(jsContext, jsException);
  }

  // Java left us an extra reference to eat.
  JSValueUnprotectChecked(jsContext, jsResult);
  TR_LEAVE();
  return jsResult;
}

/*
 * NOTE: The object returned from this function is not protected.
 */
JSValueRef DispatchMethodToString(JSContextRef jsContext, JSObjectRef,
    JSObjectRef thisObject, size_t, const JSValueRef*, JSValueRef*) {
  TR_ENTER();
  
  // This function cannot be torn off and applied to any JSValue. If this does
  // not reference a DispatchMethod, return undefined.
  if (!JSValueIsObjectOfClass(jsContext, thisObject, GetDispatchMethodClass())) {
    return JSValueMakeUndefined(jsContext);
  }

  std::ostringstream ss;
  DispatchMethodData* data = reinterpret_cast<DispatchMethodData*>(
      JSObjectGetPrivate(thisObject));
  ss << "function " << data->_utf8Name << "() {\n    [native code]\n}\n";
  JSStringRef stringRep = JSStringCreateWithUTF8CString(ss.str().c_str());
  JSValueRef jsStringRep = JSValueMakeString(jsContext, stringRep);
  JSStringRelease(stringRep);
  TR_LEAVE();
  return jsStringRep;
}

/*
 * NOTE: The object returned from this function is not protected.
 */
JSValueRef DispatchMethodGetToString(JSContextRef jsContext,
    JSObjectRef jsObject, JSStringRef jsPropertyName, JSValueRef* jsException) {
  TR_ENTER();
  JSObjectRef toStringFn = JSObjectMakeFunctionWithCallback(jsContext,
      jsPropertyName, DispatchMethodToString);
  TR_LEAVE();
  return toStringFn;
}

/*
 *
 */
bool Initialize(JNIEnv* javaEnv, jclass javaDispatchObjectClass,
    jclass javaDispatchMethodClass, jclass lowLevelSafClass) {
  TR_ENTER();
  if (!javaEnv || !javaDispatchObjectClass || !javaDispatchMethodClass
      || !lowLevelSafClass) {
    return false;
  }

  _javaVM = 0;
  javaEnv->GetJavaVM(&_javaVM);

  _javaEnv = javaEnv;
  _javaDispatchObjectClass = static_cast<jclass>(
      javaEnv->NewGlobalRef(javaDispatchObjectClass));
  _javaDispatchMethodClass = static_cast<jclass>(
      javaEnv->NewGlobalRef(javaDispatchMethodClass));
  _lowLevelSafClass = static_cast<jclass>(
      javaEnv->NewGlobalRef(lowLevelSafClass));
  _javaDispatchObjectSetFieldMethod = javaEnv->GetMethodID(
      javaDispatchObjectClass, "setField", "(ILjava/lang/String;I)V");
  _javaDispatchObjectGetFieldMethod = javaEnv->GetMethodID(
      javaDispatchObjectClass, "getField", "(ILjava/lang/String;)I");
  _javaDispatchMethodInvokeMethod = javaEnv->GetMethodID(
      javaDispatchMethodClass, "invoke", "(II[I[I)I");
  _javaDispatchObjectToStringMethod = javaEnv->GetMethodID(
      javaDispatchObjectClass, "toString", "()Ljava/lang/String;");
  _lowLevelSafReleaseObject = javaEnv->GetStaticMethodID(
      lowLevelSafClass, "releaseObject", "(Ljava/lang/Object;)V");

  if (!_javaVM
      || !_javaDispatchObjectSetFieldMethod || !_javaDispatchObjectGetFieldMethod
      || !_javaDispatchMethodInvokeMethod || !_javaDispatchObjectToStringMethod
      || !_lowLevelSafReleaseObject || javaEnv->ExceptionCheck()) {
    return false;
  }

  TR_LEAVE();
  return true;
}

void ReleaseJavaObject(jobject jObject) {
  // Tricky: this call may be on a foreign thread.
  JNIEnv* javaEnv = 0;
  if ((_javaVM->AttachCurrentThreadAsDaemon(reinterpret_cast<void**>(&javaEnv),
      NULL) < 0) || !javaEnv) {
    TR_FAIL();
    return;
  }

  // Tell the Java code we're done with this object.
  javaEnv->CallStaticVoidMethod(_lowLevelSafClass, _lowLevelSafReleaseObject,
      jObject);
  if (javaEnv->ExceptionCheck()) {
    TR_FAIL();
    return;
  }
  javaEnv->DeleteGlobalRef(jObject);
}

} // namespace gwt
