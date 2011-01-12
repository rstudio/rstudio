#ifndef _H_RootedObject
#define _H_RootedObject
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

#include "Debug.h"

#include "jsapi.h"

#if GECKO_VERSION < 2000
#define JS_AddNamedObjectRoot JS_AddNamedRoot
#define JS_AddNamedValueRoot JS_AddNamedRoot
#define JS_RemoveObjectRoot JS_RemoveRoot
#define JS_RemoveValueRoot JS_RemoveRoot
#endif

class RootedObject {
public:
  RootedObject(JSContext* ctx, const char* name = 0) : ctx(ctx), obj(0) {
    if (!JS_AddNamedObjectRoot(ctx, &obj, name)) {
      Debug::log(Debug::Error) << "RootedObject(" << (name ? name : "")
          << "): JS_AddNamedRoot failed" << Debug::flush;
    }
  }
  
  ~RootedObject() {
    // Always returns success, so no need to check.
    JS_RemoveObjectRoot(ctx, &obj);
  }
  
  JSObject& operator*() const {
    assert(obj != 0);
    return *obj;
  }

  JSObject* operator->() const  {
    assert(obj != 0);
    return obj;
  }

  bool operator==(JSObject* p) const {
    return obj == p;
  }

  bool operator!=(JSObject* p) const {
    return obj != p;
  }

  JSObject* get() const  {
    return obj;
  }
  
  RootedObject& operator=(JSObject* val) {
    obj = val;
    return *this;
  }

private:
  JSContext* ctx;
  JSObject* obj; 
};

inline bool operator==(JSObject* p, const RootedObject& ro) {
  return p == ro.get();
}

inline bool operator!=(JSObject* p, const RootedObject& ro) {
  return p != ro.get();
}

#endif
