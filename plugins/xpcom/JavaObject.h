#ifndef _H_JavaObject
#define _H_JavaObject
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

#include "mozincludes.h"
#include "jsapi.h"

class SessionData;
namespace gwt {
  class Value;
}

#if GECKO_VERSION < 2000
#define jsid jsval
#define JSID_IS_STRING JSVAL_IS_STRING
#define JSID_TO_STRING JSVAL_TO_STRING
#define JSID_IS_INT JSVAL_IS_INT
#define JSID_TO_INT JSVAL_TO_INT
#define INT_TO_JSID INT_TO_JSVAL
#define JS_GetStringEncodingLength(ctx, str) JS_GetStringLength(str)
#define JS_EncodeString(ctx, str) JS_GetStringBytes(str)
#endif

class JavaObject {
public:
  static bool isJavaObject(JSContext* ctx, JSObject* obj);
  static JSObject* construct(JSContext* ctx, SessionData* data, int objectRef);
  static int getObjectId(JSContext* ctx, JSObject* obj);
  static JSBool getProperty(JSContext* ctx, JSObject* obj, jsid id, jsval* vp);

#if GECKO_VERSION < 2000
  static JSBool setProperty(JSContext* ctx, JSObject* obj, jsid id, jsval* vp);
#else
  static JSBool setProperty(JSContext* ctx, JSObject* obj, jsid id, JSBool strict, jsval* vp);
#endif //GECKO_VERSION

  static JSBool resolve(JSContext* ctx, JSObject* obj, jsval id);
  static JSBool convert(JSContext* cx, JSObject* obj, JSType type, jsval* vp);
  static JSBool enumerate(JSContext* ctx, JSObject* obj, JSIterateOp op, jsval* statep, jsid* idp);
#if GECKO_VERSION >= 14000
  static void finalize(JSFreeOp* fop, JSObject* obj);
#else
  static void finalize(JSContext* ctx, JSObject* obj);
#endif //GECKO_VERSION
  static JSBool toString(JSContext* ctx, JSObject* obj, uintN argc, jsval* argv, jsval* rval);
  static JSBool call(JSContext* ctx, JSObject* obj, uintN argc, jsval* argv, jsval* rval);

#if GECKO_VERSION >= 2000
  static JSBool toString20(JSContext* ctx, uintN argc, jsval* vp);
  static JSBool call20(JSContext* ctx, uintN argc, jsval* vp);
#endif //GECKO_VERSION

private:
  static SessionData* getSessionData(JSContext* ctx, JSObject* obj);
  static JSBool invokeJava(JSContext* ctx, SessionData* data,
      const gwt::Value& javaThis, int dispId, int numArgs, const jsval* jsargs,
      jsval* rval);
};

#endif
