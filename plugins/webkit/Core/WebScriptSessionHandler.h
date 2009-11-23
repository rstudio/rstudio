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

#include <map>
#include <set>
#import "Debug.h"
#import "HostChannel.h"
#import "JavaScriptCore/JavaScriptCore.h"
#import "pthread.h"
#import "TrackingData.h"

class CrashHandler {
public:
  virtual void crash(const char* functionName, const char* message) = 0;
  virtual bool hasCrashed() = 0;
};
typedef CrashHandler* CrashHandlerRef;

/*
 * This comprises the guts of the JavaScriptCore-specific code and is
 * responsible for message dispatch and value conversion.  This class should
 * be portable to any runtime that uses JavaScriptCore.
 */
class WebScriptSessionHandler : public SessionHandler, public SessionData  {
public:
  WebScriptSessionHandler(HostChannel* channel, JSGlobalContextRef contextRef,
                          CrashHandler* crashHandler);

  /*
   * Invoking the destructor will perform a clean shutdown of the OOPHM session.
   */
  ~WebScriptSessionHandler();

  /*
   * This is a panic method for shutting down the OOPHM debugging session due
   * to unrecoverable errors.
   */
  void initiateAutodestructSequence(const char* functionName, const char* message);

  /*
   * Invoke a Java method.
   */
  JSValueRef javaFunctionCallbackImpl(int dispatchId,
                                      JSObjectRef thisObject,
                                      size_t argumentCount,
                                      const JSValueRef arguments[],
                                      JSValueRef* exception);

  /*
   * Finalize a Javo object proxy.
   */
  void javaObjectFinalizeImpl(int objId);

  /*
   * Determine if a Java class has a named property.
   */
  bool javaObjectHasPropertyImpl(TrackingDataRef tracker, JSObjectRef object,
                                JSStringRef propertyName);

  /*
   * Obtain the value of named property of an object.
   */
  JSValueRef javaObjectGetPropertyImpl(TrackingDataRef tracker, JSObjectRef object,
                                       JSStringRef propertyName, JSValueRef* exception);

  /*
   * Set the value of a named property on a Java object.
   */
  bool javaObjectSetPropertyImpl(TrackingDataRef tracker, JSObjectRef object,
                                 JSStringRef propertyName, JSValueRef value,
                                 JSValueRef* exception);

  /*
   * Create a JavaScript Error object with the given message.
   */
  JSValueRef makeException(const char* message);

protected:
  virtual void disconnectDetectedImpl();
  virtual void fatalError(HostChannel& channel, const std::string& message);
  virtual void freeValue(HostChannel& channel, int idCount, const int* ids);
  virtual void loadJsni(HostChannel& channel, const std::string& js);
  virtual bool invoke(HostChannel& channel, const Value& thisObj, const std::string& methodName,
                      int numArgs, const Value* const args, Value* returnValue);
  virtual bool invokeSpecial(HostChannel& channel, SpecialMethodId method, int numArgs,
                             const Value* const args, Value* returnValue);
  virtual void sendFreeValues(HostChannel& channel);

private:
  CrashHandlerRef const crashHandler;
  int jsObjectId;
  std::map<int, JSObjectRef> javaObjectsById;
  std::set<int> javaObjectsToFree;
  pthread_mutex_t javaObjectsLock;
  std::map<int, JSObjectRef> jsObjectsById;
  std::map<JSObjectRef, int> jsIdsByObject;
  JSClassRef javaObjectWrapperClass;

  /* A reference to __gwt_makeResult, which we use constantly */
  JSObjectRef makeResultFunction;

  /* The String() function */
  JSObjectRef stringConstructor;

  /*
   * Free server-side references.
   */
  void freeJavaObjects();

  /*
   * Create a exception Value that contains the given message.
   */
  void makeExceptionValue(Value& value, const char* message);

  /*
   * Create a Java object proxy to be passed into the JavaScript execution
   * environment.
   */
  JSObjectRef makeJavaWrapper(int objId);

  /*
   * Convert a value from the JavaScript into something that can be sent back
   * to the OOPHM host.
   *
   * Returns true if an exception was encountered.
   */
  bool makeValue(Value& ret, JSValueRef value);

  /*
   * Convert a string-like object to something that can be sent back to the OOPHM
   * host.
   *
   * Returns true if an exception was encountered.
   */
  bool makeValueFromString(Value& ret, JSValueRef value);

  /*
   * Convert a value from the OOPHM host into something that can be passed into
   * the JavaScript execution environment.
   */
  JSValueRef makeValueRef(const Value& value);
};
typedef WebScriptSessionHandler* WebScriptSessionHandlerRef;

