#ifndef _H_FFSessionHandler
#define _H_FFSessionHandler
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

#include <set>
#include <map>

#include "mozincludes.h"
#include "SessionData.h"

#include "jsapi.h"

class HostChannel;
class Value;

class FFSessionHandler : public SessionData, public SessionHandler {
  friend class JavaObject;
public:
  FFSessionHandler(HostChannel* channel);
  ~FFSessionHandler(void);
  virtual void makeValueFromJsval(Value& retVal, JSContext* ctx,
      const jsval& value);
  virtual void makeJsvalFromValue(jsval& retVal, JSContext* ctx,
      const Value& value);
  virtual void freeJavaObject(int objectId);
  void disconnect();

protected:
  virtual void freeValue(HostChannel& channel, int idCount, const int* ids);
  virtual void loadJsni(HostChannel& channel, const std::string& js);
  virtual bool invoke(HostChannel& channel, const Value& thisObj, const std::string& methodName,
      int numArgs, const Value* const args, Value* returnValue);
  virtual bool invokeSpecial(HostChannel& channel, SpecialMethodId method, int numArgs,
      const Value* const args, Value* returnValue);
  virtual void sendFreeValues(HostChannel& channel);

private:
  void getStringObjectClass(JSContext* ctx);
  void getToStringTearOff(JSContext* ctx);

  int jsObjectId;

  std::map<int, JSObject*> javaObjectsById;
  std::set<int> javaObjectsToFree;

  // Array of JSObjects exposed to the host
  JSObject* jsObjectsById;
  JSClass* stringObjectClass;

  std::map<JSObject*, int> jsIdsByObject;
};

inline Debug::DebugStream& operator<<(Debug::DebugStream& dbg, JSString* str) {
  if (str == NULL) {
    dbg << "null";
  } else {
    dbg << std::string(JS_GetStringBytes(str), JS_GetStringLength(str));
  }
  return dbg;
}

#endif
