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

/*
 * Defines the JavaScript classes gwt_nativewrapper_class and
 * gwt_functionwrapper_class, which interface via JNI to Java objects.
 */

#include <jni.h>
#include "JsRootedValue.h"
#include "gwt-jni.h"
#include "Tracer.h"

extern "C" {
  
static JSBool JS_DLL_CALLBACK gwt_nativewrapper_getProperty(JSContext *cx,
    JSObject *obj, jsval id, jsval *vp)
{
  Tracer tracer("gwt_nativewrapper_getProperty");
  tracer.log("context=%08x", unsigned(cx));

  if (*vp != JSVAL_VOID)
    return JS_TRUE;
  jclass dispClass;
  jobject dispObj;
  jstring ident;
  if (!getJavaPropertyStats(cx, obj, id, dispClass, dispObj, ident)) {
    tracer.setFail("getJavaPropertyStats failed");
    return JS_FALSE;
  }
  JsRootedValue* js_rooted_value = GetFieldAsRootedValue(cx, dispClass,
      dispObj, ident);
  if (!js_rooted_value) {
    tracer.setFail("can't get field");
    return JS_FALSE;
  }
  *vp = js_rooted_value->getValue();
  return JS_TRUE;
}

static void JS_DLL_CALLBACK gwt_nativewrapper_finalize(JSContext *cx,
    JSObject *obj)
{
  jobject dispObj = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
  if (dispObj)
    savedJNIEnv->DeleteGlobalRef(dispObj);
}

static JSBool JS_DLL_CALLBACK gwt_nativewrapper_setProperty(JSContext *cx, JSObject *obj, jsval id, jsval *vp)
{
  Tracer tracer("gwt_nativewrapper_setProperty");
  tracer.log("context=%08x", unsigned(cx));
  jclass dispClass;
  jobject dispObj;
  jstring ident;
  if (!getJavaPropertyStats(cx,obj,id,dispClass,dispObj,ident)) {
    tracer.setFail("getJavaPropertyStats failed");
    return JS_FALSE;
  }
  JsRootedValue* js_rooted_value = new JsRootedValue(cx, *vp); 
  if (!SetFieldFromRootedValue(cx, dispClass, dispObj, ident, js_rooted_value)) {
    tracer.setFail("can't set field");
    return JS_FALSE;
  }
  return JS_TRUE;
}


JSClass gwt_nativewrapper_class = {
  "gwt_nativewrapper_class", JSCLASS_HAS_PRIVATE,
  JS_PropertyStub, JS_PropertyStub, gwt_nativewrapper_getProperty,
  gwt_nativewrapper_setProperty, JS_EnumerateStub, JS_ResolveStub,
  JS_ConvertStub, gwt_nativewrapper_finalize,
  JSCLASS_NO_OPTIONAL_MEMBERS
};

JSClass gwt_functionwrapper_class = {
  "gwt_functionwrapper_class", JSCLASS_HAS_PRIVATE,
  JS_PropertyStub, JS_PropertyStub, JS_PropertyStub, JS_PropertyStub,
  JS_EnumerateStub, JS_ResolveStub, JS_ConvertStub, gwt_nativewrapper_finalize,
  JSCLASS_NO_OPTIONAL_MEMBERS
};

} // extern "C"
