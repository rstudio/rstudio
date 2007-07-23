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
#ifndef JNI_LINUX_GWT_JNI_H_
#define JNI_LINUX_GWT_JNI_H_

#include <jni.h>
#include "JsRootedValue.h"
#include "JStringWrap.h"

extern JNIEnv* savedJNIEnv;
extern jclass lowLevelMozClass;

// JavaScript class objects
extern "C" JSClass gwt_nativewrapper_class;
extern "C" JSClass gwt_functionwrapper_class;

extern jobject NewJsValueMoz(JSContext* context);
extern jobject NewJsValueMoz(JsRootedValue* js_rooted_value);
extern JsRootedValue* GetJsRootedValue(jobject jsvalue);
extern JsRootedValue* GetFieldAsRootedValue(JSContext* context, jclass clazz,
    jobject obj, jstring field_name);
extern bool SetFieldFromRootedValue(JSContext* context, jclass clazz,
    jobject obj, jstring field_name, JsRootedValue* js_rooted_value);
extern JSBool getJavaPropertyStats(JSContext *cx, JSObject *obj, jsval id,
    jclass& dispClass, jobject& dispObj, jstring& jident);
extern JSBool invokeJavaMethod(JSContext *cx, JSObject *obj, uintN argc,
    jsval *argv, jsval *rval);
    
#endif /*JNI_LINUX_GWT_JNI_H_*/
