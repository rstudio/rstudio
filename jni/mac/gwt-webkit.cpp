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
#include "gwt-ll.h"
#include "DispWrapper.h"
#include "FuncWrapper.h"

JNIEnv* gEnv;
jclass gClass;
jclass gDispObjCls;
jclass gDispMethCls;
jmethodID gGetFieldMeth;
jmethodID gSetFieldMeth;
jmethodID gInvokeMeth;
jmethodID gToStringMeth;

using namespace KJS;

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    isNull
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isNull
  (JNIEnv *env, jclass, jint jsval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__isNull");

  JSValue* val = (JSValue*)jsval;
  if (!val)
    return JNI_FALSE;

    TRACE("SUCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__isNull");
  return val->isNull();
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    isUndefined
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_isUndefined
  (JNIEnv *env, jclass, jint jsval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__isUndefined");
  JSValue* val = (JSValue*)jsval;
  if (!val)
    return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__isUndefined");
  return val->isUndefined();
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    jsNull
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_jsNull
  (JNIEnv *, jclass) {
  return (jint)jsNull();
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    jsUndefined
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf_jsUndefined
  (JNIEnv *env, jclass) {
    return (jint)jsUndefined();
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _coerceToBoolean
 * Signature: (II[Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToBoolean
  (JNIEnv * env, jclass, jint execState, jint jsval, jbooleanArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToBoolean");

  if (!execState || !jsval)
    return JNI_FALSE;

  jboolean result = ((JSValue*)jsval)->toBoolean((ExecState*)execState);
  env->SetBooleanArrayRegion(rval, 0, 1, &result);
  if (env->ExceptionCheck())
      return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToBoolean");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _coerceToDouble
 * Signature: (II[D)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToDouble
  (JNIEnv *env, jclass, jint execState, jint jsval, jdoubleArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToDouble");

  if (!execState || !jsval)
    return JNI_FALSE;

  jdouble result = ((JSValue*)jsval)->toNumber((ExecState*)execState);
  env->SetDoubleArrayRegion(rval, 0, 1, &result);
  if (env->ExceptionCheck())
      return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToDouble");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _coerceToString
 * Signature: (II[Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToString
  (JNIEnv *env, jclass, jint execState, jint jsval, jobjectArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToString");

  JSValue *val = (JSValue*)jsval;
  if (!execState || !val)
    return JNI_FALSE;

  /* 
   * Convert all objects to their string representation, EXCEPT
   * null and undefined which will be returned as a true NULL.
   */
  jstring result = NULL;
  if (!val->isNull() && !val->isUndefined()) {
    UString str = val->toString((ExecState*)execState);
    result = env->NewString((const jchar*)str.data(), str.size());
    if (env->ExceptionCheck())
      return JNI_FALSE;
  }

  env->SetObjectArrayElement(rval,0,result);
  if (env->ExceptionCheck())
    return JNI_FALSE;

  TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1coerceToString");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _convertBoolean
 * Signature: (IZ[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertBoolean
  (JNIEnv *env, jclass, jboolean jval, jintArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertBoolean");

  JSValue *jsval = (jval == JNI_FALSE) ? jsBoolean(false) : jsBoolean(true);
  if (!jsval)
    return JNI_FALSE;

  env->SetIntArrayRegion(rval,0,1,(const jint*)&jsval);
  if (env->ExceptionCheck())
    return JNI_FALSE;
  
    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertBoolean");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _convertDouble
 * Signature: (ID[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertDouble
  (JNIEnv *env, jclass, jdouble jval, jintArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertDouble");

  JSValue *jsval = jsNumber(jval);
  if (!jsval)
    return JNI_FALSE;

  env->SetIntArrayRegion(rval,0,1,(const jint*)&jsval);
  if (env->ExceptionCheck())
    return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertDouble");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _convertString
 * Signature: (ILjava/lang/String;[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertString
  (JNIEnv *env, jclass, jstring jval, jintArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertString");

  JStringWrap jstr(env, jval);
  if (!jstr.jstr())
    return JNI_FALSE;
  
  JSValue *jsval = jsString(UString((const UChar*)jstr.jstr(), jstr.length()));
  /*
   * TODO / LEAK / HACK: We will be persisting these objects on the java side,
   * so in order to keep the JS GC from collecting our objects when they are
   * away, we need to add them to the protect list. This should be refactored
   * out in favor of better memory mgmt scheme.
   */
  gcProtectNullTolerant(jsval);
  if (!jsval)
    return JNI_FALSE;

  env->SetIntArrayRegion(rval,0,1,(const jint*)&jsval);
  if (env->ExceptionCheck())
    return JNI_FALSE;

  TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1convertString");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _executeScript
 * Signature: (ILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1executeScript
  (JNIEnv* env, jclass, jint execState, jstring code) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1executeScript"); 
  if (!execState || !code)
    return JNI_FALSE;

  JStringWrap jcode(env, code);
  if (!jcode.jstr())
    return JNI_FALSE;

  Interpreter* interp = ((ExecState*)execState)->dynamicInterpreter();
  if (!interp)
    return JNI_FALSE;

  interp->evaluate(UString(), 0, (const UChar*)jcode.jstr(), jcode.length());
    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1executeScript");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _executeScriptWithInfo
 * Signature: (ILjava/lang/String;Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1executeScriptWithInfo
  (JNIEnv* env, jclass, jint execState, jstring code, jstring file, jint line) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1executeScriptWithInfo");
  if (!execState || !code || !file)
    return JNI_FALSE;

  JStringWrap jcode(env, code);
  if (!jcode.jstr())
    return JNI_FALSE;

  JStringWrap jfile(env, file);
  if (!jcode.jstr())
    return JNI_FALSE;

  Interpreter* interp = ((ExecState*)execState)->dynamicInterpreter();
  if (!interp)
    return JNI_FALSE;

  interp->evaluate(UString((const UChar*)jfile.jstr(), jfile.length()), line, (const UChar*)jcode.jstr(), jcode.length());
    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1executeScriptWithInfo");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _gcLock
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1gcLock
  (JNIEnv *, jclass, jint jsval) {
  gcProtectNullTolerant((JSValue*)jsval);
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _gcUnlock
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1gcUnlock
  (JNIEnv *, jclass, jint jsval) {
  gcUnprotectNullTolerant((JSValue*)jsval);
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _getGlobalExecState
 * Signature: (I[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1getGlobalExecState
  (JNIEnv *env, jclass, jint scriptObject, jintArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1getGlobalExecState");

  if (!scriptObject || !((JSValue*)scriptObject)->isObject())
    return JNI_FALSE;

  Interpreter* interp = Interpreter::interpreterWithGlobalObject((JSObject*)scriptObject);
  if (!interp)
    return JNI_FALSE;

  ExecState* execState = interp->globalExec();
    env->SetIntArrayRegion(rval, 0, 1, (jint*)&execState);
    if (env->ExceptionCheck())
        return JNI_FALSE;
    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1getGlobalExecState");
  return JNI_TRUE;

}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _initNative
 * Signature: (Ljava/lang/Class;Ljava/lang/Class;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1initNative
  (JNIEnv* env, jclass llClass, jclass dispObjCls, jclass dispMethCls) {
  Interpreter::setShouldPrintExceptions(true);
  gEnv = env;
  gClass =  static_cast<jclass>(env->NewGlobalRef(llClass));
  gDispObjCls = static_cast<jclass>(env->NewGlobalRef(dispObjCls));
  gDispMethCls = static_cast<jclass>(env->NewGlobalRef(dispMethCls));
  if (!gClass || !gDispObjCls || !gDispMethCls || env->ExceptionCheck()) {
    return false;
  }

  gGetFieldMeth = env->GetMethodID(gDispObjCls, "getField", "(Ljava/lang/String;)I");
  gSetFieldMeth = env->GetMethodID(gDispObjCls, "setField", "(Ljava/lang/String;I)V");
  gInvokeMeth = env->GetMethodID(gDispMethCls, "invoke", "(II[I)I");
  gToStringMeth = env->GetMethodID(gDispObjCls, "toString", "()Ljava/lang/String;");
  if (!gGetFieldMeth || !gSetFieldMeth || !gInvokeMeth || !gToStringMeth || env->ExceptionCheck()) {
    return false;
  }

#ifdef FILETRACE
    gout = fopen("gwt-ll.log", "w");
    filetrace("LOG STARTED");
#endif // FILETRACE

#ifdef JAVATRACE
    gTraceMethod = env->GetStaticMethodID(gClass, "trace", "(Ljava/lang/String;)V");
    if (!gTraceMethod || env->ExceptionCheck())
        return false;
#endif // JAVATRACE

  return true;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _invoke
 * Signature: (IILjava/lang/String;II[I[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1invoke
  (JNIEnv* env, jclass, jint jsexecState, jint jsScriptObject, jstring method, jint jsthis, jint argc, jintArray argv, jintArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1invoke");

  if (!jsexecState || !jsScriptObject || !method || !rval)
    return JNI_FALSE;
  
  ExecState* execState = (ExecState*)jsexecState;

  JSObject* scriptObj = (JSObject*)jsScriptObject;
  if (!scriptObj->isObject())
    return JNI_FALSE;

  JStringWrap jmethod(env, method);
  if (!jmethod.jstr())
    return JNI_FALSE;

  JSObject* thisObj = (JSObject*)jsthis;
  if (!thisObj || thisObj->isNull() || thisObj->isUndefined()) {
    thisObj = scriptObj;
  }
  if (!thisObj->isObject())
    return JNI_FALSE;

  JSValue* maybeFunc = scriptObj->get(execState, Identifier((const UChar*)jmethod.jstr(), jmethod.length()));
  if (!maybeFunc || !maybeFunc->isObject())
    return JNI_FALSE;
  
  JSObject* func = (JSObject*)maybeFunc;
  if (!func->implementsCall())
    return JNI_FALSE;

  List args;
  for (int i = 0; i < argc; ++i) {
    jint argi;
    env->GetIntArrayRegion(argv, i, 1, &argi);
    if (env->ExceptionCheck())
      return JNI_FALSE;

    if (argi) {
      args.append((JSValue*)argi);
    } else {
      args.append(jsNull());
    }
  }

  JSValue* result = func->call(execState, thisObj, args);
    env->SetIntArrayRegion(rval, 0, 1, (jint*)&result);
    if (env->ExceptionCheck())
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1invoke");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _isObject
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1isObject
  (JNIEnv *, jclass, jint jsval) {
  if (!jsval)
    return JNI_FALSE;
  return ((JSValue*)jsval)->isObject() ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _isString
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1isString
  (JNIEnv *, jclass, jint jsval) {
  if (!jsval)
    return JNI_FALSE;
  return ((JSValue*)jsval)->isString() ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _isWrappedDispatch
 * Signature: (I[Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1isWrappedDispatch
  (JNIEnv* env, jclass, jint jsval, jbooleanArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1isWrappedDispatch");
  if (!jsval)
    return JNI_FALSE;

  JSValue* val = (JSValue*)jsval;
  jboolean result = val->isObject(&DispWrapper::info) ? JNI_TRUE : JNI_FALSE;

    env->SetBooleanArrayRegion(rval, 0, 1, &result);
    if (env->ExceptionCheck())
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1isWrappedDispatch");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _jsLock
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1jsLock
  (JNIEnv *, jclass) {
  JSLock::lock();
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _jsUnlock
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1jsUnlock
  (JNIEnv *, jclass) {
  JSLock::unlock();
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _raiseJavaScriptException
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1raiseJavaScriptException
  (JNIEnv *env, jclass, jint execState, jint jsval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1raiseJavaScriptException");

  if (!execState || !jsval)
    return JNI_FALSE;

  ((ExecState*)execState)->setException((JSValue*)jsval);
    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1raiseJavaScriptException");
  return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _unwrapDispatch
 * Signature: (I[Lcom/google/gwt/dev/shell/mac/LowLevelSaf/DispatchObject;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1unwrapDispatch
  (JNIEnv* env, jclass, jint jsval, jobjectArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1unwrapDispatch");
  if (!jsval)
    return JNI_FALSE;

  JSValue* val = (JSValue*)jsval;
  if (!val->isObject(&DispWrapper::info))
    return JNI_FALSE;

  DispWrapper* wrapper = static_cast<DispWrapper*>(val);
    env->SetObjectArrayElement(rval, 0, wrapper->getDispObj());
    if (env->ExceptionCheck())
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1unwrapDispatch");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _wrapDispatch
 * Signature: (Lcom/google/gwt/dev/shell/mac/LowLevelSaf/DispatchObject;[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1wrapDispatch
  (JNIEnv* env, jclass, jobject dispObj, jintArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1wrapDispatch");

    jobject dispObjRef = env->NewGlobalRef(dispObj);
    if (!dispObjRef || env->ExceptionCheck())
        return JNI_FALSE;
  
  DispWrapper* wrapper = new DispWrapper(dispObjRef);
  /*
   * TODO / LEAK / HACK: We will be persisting these objects on the java side,
   * so in order to keep the JS GC from collecting our objects when they are
   * away, we need to add them to the protect list. This should be refactored
   * out in favor of better memory mgmt scheme.
   */
  gcProtectNullTolerant(wrapper);
    env->SetIntArrayRegion(rval, 0, 1, (jint*)&wrapper);
    if (env->ExceptionCheck())
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1wrapDispatch");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _wrapFunction
 * Signature: (Ljava/lang/String;Lcom/google/gwt/dev/shell/mac/LowLevelSaf/DispatchMethod;[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1wrapFunction
  (JNIEnv* env, jclass, jstring name, jobject dispMeth, jintArray rval) {
    TRACE("ENTER Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1wrapFunction");

    jobject dispMethRef = env->NewGlobalRef(dispMeth);
    if (!dispMethRef || env->ExceptionCheck())
        return JNI_FALSE;
  
  JStringWrap jname(env, name);
  if (!jname.jstr())
    return JNI_FALSE;
  
  FuncWrapper* wrapper = new FuncWrapper(UString((const UChar*)jname.jstr(), jname.length()), dispMethRef);
  /*
   * TODO / LEAK / HACK: We will be persisting these objects on the java side,
   * so in order to keep the JS GC from collecting our objects when they are
   * away, we need to add them to the protect list. This should be refactored
   * out in favor of better memory mgmt scheme.
   */
  gcProtectNullTolerant(wrapper);
    env->SetIntArrayRegion(rval, 0, 1, (jint*)&wrapper);
    if (env->ExceptionCheck())
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1wrapFunction");
    return JNI_TRUE;
}

#ifdef FILETRACE
FILE* gout = 0;
void filetrace(const char* s)
{
    fprintf(gout, s);
    fprintf(gout, "\n");
    fflush(gout);
}
#endif // FILETRACE

#ifdef JAVATRACE
jmethodID gTraceMethod = 0;
void javatrace(const char* s)
{
    if (!gEnv->ExceptionCheck())
    {
        jstring out = gEnv->NewStringUTF(s);
        if (!gEnv->ExceptionCheck())
            gEnv->CallStaticVoidMethod(gClass, gTraceMethod, out);
        else
            gEnv->ExceptionClear();
    }
}
#endif // JAVATRACE

