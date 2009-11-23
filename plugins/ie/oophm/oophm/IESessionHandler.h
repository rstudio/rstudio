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

#pragma once
#include <map>
#include <set>
#include "HostChannel.h"
#include "JavaObject.h"
#include "mshtml.h"
#include "oophm_i.h"
#include "SessionData.h"
#include "SessionHandler.h"

class IESessionHandler :
  public SessionData,
  public SessionHandler
{
public:
  IESessionHandler(HostChannel* channel, IHTMLWindow2* window);
  ~IESessionHandler(void);
  virtual void freeJavaObject(unsigned int objId);
  virtual void makeValue(Value& value, const _variant_t& in);
  virtual void makeValueRef(_variant_t& value, const Value& in);

protected:
  virtual void disconnectDetectedImpl();
  virtual void fatalError(HostChannel& channel, const std::string& messsage);
  virtual void freeValue(HostChannel& channel, int idCount, const int* ids);
  virtual void loadJsni(HostChannel& channel, const std::string& js);
  virtual bool invoke(HostChannel& channel, const Value& thisObj, const std::string& methodName,
      int numArgs, const Value* const args, Value* returnValue);
  virtual bool invokeSpecial(HostChannel& channel, SpecialMethodId method, int numArgs,
      const Value* const args, Value* returnValue);
  virtual void sendFreeValues(HostChannel& channel);


private:
  int jsObjectId;

  /*
  * This must be IUnknown and not IDispatch because the IUnknown
  * interface is the only COM interface guaranteed to be stable for
  * any particular instance of an object.  It appears as though
  * Event objects exhibit the multiple-interface behavior.
  *
  * Furthermore, this map is not a CComPtr map because we don't
  * to artificially add to the retain count of the Java objects.
  */
  std::map<int, IUnknown*> javaObjectsById;
  std::set<int> javaObjectsToFree;

  // Same as above; only one map needs to increment reference count.
  std::map<int, CComPtr<IUnknown>> jsObjectsById;
  std::map<IUnknown*, int> jsIdsByObject;

  /*
   * Create a JavaScript Error object with the given message.
   */
  void makeException(_variant_t& value, const char* message);

  /*
   * Create a exception Value that contains the given message.
   */
  void makeExceptionValue(Value& value, const char* message);
};
