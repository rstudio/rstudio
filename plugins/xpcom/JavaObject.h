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
class Value;

class JavaObject {
public:
  static bool isJavaObject(JSContext* ctx, JSObject* obj);
  static JSObject* construct(JSContext* ctx, SessionData* data, int objectRef);
  static int getObjectId(JSContext* ctx, JSObject* obj);
  static JSBool getProperty(JSContext* ctx, JSObject* obj, jsval id, jsval* vp);
  static JSBool setProperty(JSContext* ctx, JSObject* obj, jsval id, jsval* vp);
  static JSBool resolve(JSContext* ctx, JSObject* obj, jsval id);
  static JSBool convert(JSContext* cx, JSObject* obj, JSType type, jsval* vp);
  static JSBool enumerate(JSContext* ctx, JSObject* obj, JSIterateOp op, jsval* statep, jsid* idp);
  static void finalize(JSContext* ctx, JSObject* obj);
  static JSBool toString(JSContext* ctx, JSObject* obj, uintN argc, jsval* argv, jsval* rval);
  static JSBool call(JSContext* ctx, JSObject* obj, uintN argc, jsval* argv, jsval* rval);
private:
  static SessionData* getSessionData(JSContext* ctx, JSObject* obj);
  static JSBool invokeJava(JSContext* ctx, SessionData* data,
      const Value& javaThis, int dispId, int numArgs, const jsval* jsargs,
      jsval* rval);
};

#endif
