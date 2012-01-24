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

#include <cstring>

#include "XpcomDebug.h"
#include "JavaObject.h"

#ifdef _WINDOWS
// avoid deprecation warnings for strncpy
#define strncpy(d,s,c) strncpy_s((d),(c),(s),(c))

#include <cstdarg>
inline int snprintf(char* buf, size_t buflen, const char* fmt, ...) {
  va_list args;
  va_start(args, fmt);
  int n = _vsnprintf_s(buf, buflen, buflen, fmt, args);
  va_end(args);
  return n;
}

#endif

std::string dumpJsVal(JSContext* ctx, jsval v) {
  char buf[70];
  if (JSVAL_IS_VOID(v)) {
    strncpy(buf, "undef", sizeof(buf));
  } else if (JSVAL_IS_NULL(v)) {
    strncpy(buf, "null", sizeof(buf));
  } else if (JSVAL_IS_OBJECT(v)) {
    JSObject* obj = JSVAL_TO_OBJECT(v);
    if (JavaObject::isJavaObject(ctx, obj)) {
      int oid = JavaObject::getObjectId(ctx, obj);
      snprintf(buf, sizeof(buf), "JavaObj(%d)", oid);
    } else {
      JSClass* jsClass = JS_GET_CLASS(ctx, obj);
      const char* name = jsClass->name ? jsClass->name : "<null>";
      snprintf(buf, sizeof(buf), "Object(%.20s @ %p)", name, obj);
    }
  } else if (JSVAL_IS_INT(v)) {
    snprintf(buf, sizeof(buf), "int(%d)", JSVAL_TO_INT(v));
  } else if (JSVAL_IS_DOUBLE(v)) {
    double d;
#if GECKO_VERSION < 2000
    d= *JSVAL_TO_DOUBLE(v);
#else
    d = JSVAL_TO_DOUBLE(v);
#endif //GECKO_VERSION
    snprintf(buf, sizeof(buf), "double(%lf)", d);
  } else if (JSVAL_IS_STRING(v)) {
    JSString* str = JSVAL_TO_STRING(v);

    size_t len = JS_GetStringEncodingLength(ctx, str);

    const char* continued = "";
    if (len > 20) {
      len = 20;
      continued = "...";
    }
    // TODO: trashes Unicode
    snprintf(buf, sizeof(buf), "string(%.*s%s)", static_cast<int>(len),
        JS_EncodeString(ctx, str), continued);
  } else if (JSVAL_IS_BOOLEAN(v)) {
    snprintf(buf, sizeof(buf), "bool(%s)", JSVAL_TO_BOOLEAN(v) ? "true"
        : " false");
  } else {
    // TODO(acleung): When we run into this, use the other api to figure out what v is.
    // snprintf(buf, sizeof(buf), "unknown(%08x)", (unsigned) v);
  }
  buf[sizeof(buf) - 1] = 0;
  return std::string(buf);
}
