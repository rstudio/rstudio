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
#include "Preferences.h"

#include "jsapi.h"

class HostChannel;
namespace gwt {
  class Value;
}

class FFSessionHandler : public SessionData, public SessionHandler {
  friend class JavaObject;
public:
  FFSessionHandler(HostChannel* channel);
  ~FFSessionHandler(void);
  virtual void makeValueFromJsval(gwt::Value& retVal, JSContext* ctx,
      const jsval& value);
  virtual void makeJsvalFromValue(jsval& retVal, JSContext* ctx,
      const gwt::Value& value);
  virtual void freeJavaObject(int objectId);
  void disconnect();

protected:
  virtual void disconnectDetectedImpl();
  virtual void freeValue(HostChannel& channel, int idCount, const int* ids);
  virtual void loadJsni(HostChannel& channel, const std::string& js);
  virtual bool invoke(HostChannel& channel, const gwt::Value& thisObj, const std::string& methodName,
      int numArgs, const gwt::Value* const args, gwt::Value* returnValue);
  virtual bool invokeSpecial(HostChannel& channel, SpecialMethodId method, int numArgs,
      const gwt::Value* const args, gwt::Value* returnValue);
  virtual void sendFreeValues(HostChannel& channel);
  virtual void fatalError(HostChannel& channel, const std::string& message);

private:
  void getStringObjectClass(JSContext* ctx);
  void getToStringTearOff(JSContext* ctx);
  void* identityFromObject(JSObject* obj);

  int jsObjectId;

  std::map<int, JSObject*> javaObjectsById;
  std::set<int> javaObjectsToFree;

  // Array of JSObjects exposed to the host
  JSObject* jsObjectsById;
  JSClass* stringObjectClass;

  std::map<void*, int> jsIdsByObject;
};

#endif
