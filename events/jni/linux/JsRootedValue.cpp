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

#include "JsRootedValue.h"

// Initialize static value used to hold the JavaScript String class.
JSClass* JsRootedValue::stringClass = 0;

// Initialize static reference to the sole JSRuntime value in Gecko.
JSRuntime* JsRootedValue::runtime = 0;

// Static stack of JavaScript execution contexts.
std::stack<JSContext*> JsRootedValue::contextStack;

/*
 * Actually get the stringClass pointer from JavaScript.
 */
void JsRootedValue::fetchStringClass() const {
  Tracer tracer("JsRootedValue::fetchStringClass");
  JSContext* cx = currentContext();
  jsval val = JS_GetEmptyStringValue(cx);
  JSObject* obj;
  // on error, leave stringClass null
  if (!JS_ValueToObject(cx, val, &obj)) return;
  if (!obj) {
    tracer.log("ensureStringClass: null object");
    return;
  }
  stringClass = JS_GET_CLASS(cx, obj);
  tracer.log("stringClass=%08x", unsigned(stringClass));
}
