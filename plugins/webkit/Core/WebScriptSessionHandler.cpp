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

#import "WebScriptSessionHandler.h"
#import "InvokeMessage.h"
#import "ObjectFunctions.h"
#import "ServerMethods.h"
#import "TrackingData.h"
#import "scoped_ptr.h"


WebScriptSessionHandler::WebScriptSessionHandler(HostChannel* channel,
                                                 JSGlobalContextRef contextRef,
                                                 CrashHandlerRef crashHandler) :
SessionData(channel, contextRef, this), jsObjectId(1), crashHandler(crashHandler) {

  JSClassDefinition def = kJSClassDefinitionEmpty;
  def.className = "JavaObject";
  def.hasProperty = JavaObjectHasProperty;
  def.callAsFunction = JavaObjectCallAsFunction;
  def.finalize = JavaObjectFinalize;
  def.getProperty = JavaObjectGetProperty;
  def.setProperty = JavaObjectSetProperty;
  javaObjectWrapperClass = JSClassCreate(&def);
  JSClassRetain(javaObjectWrapperClass);

  // Get the String constructor function to tell Strings from other Objects
  JSStringRef stringString = JSStringCreateWithUTF8CString("String");
  JSValueRef stringConstructorValue = JSObjectGetProperty(contextRef,
                                                          JSContextGetGlobalObject(contextRef),
                                                          stringString, NULL);
  stringConstructor = JSValueToObject(contextRef, stringConstructorValue, NULL);
  JSValueProtect(contextRef, stringConstructor);
  JSStringRelease(stringString);

  // Call out to the utility __gwt_makeResult function to create the return array
  JSStringRef makeResultString = JSStringCreateWithUTF8CString("__gwt_makeResult");
  JSValueRef makeResultValue = JSObjectGetProperty(contextRef, JSContextGetGlobalObject(contextRef), makeResultString, NULL);
  JSStringRelease(makeResultString);

  if (!JSValueIsObject(contextRef, makeResultValue)) {
    crashHandler->crash(__PRETTY_FUNCTION__, "Could not find __gwt_makeResult");
  } else {
    makeResultFunction = JSValueToObject(contextRef, makeResultValue, NULL);
    JSValueProtect(contextRef, makeResultFunction);
  }

  pthread_mutexattr_t mutexAttrs;
  pthread_mutexattr_init(&mutexAttrs);
  // This behaves basically like the Java synchronized keyword
  pthread_mutexattr_settype(&mutexAttrs, PTHREAD_MUTEX_RECURSIVE);
  pthread_mutex_init(&javaObjectsLock, &mutexAttrs);
  pthread_mutexattr_destroy(&mutexAttrs);
}

WebScriptSessionHandler::~WebScriptSessionHandler() {
  std::map<int, JSObjectRef>::iterator i;

  pthread_mutex_lock(&javaObjectsLock);
  while ((i = javaObjectsById.begin()) != javaObjectsById.end()) {
    JavaObjectFinalize(i->second);
  }
  pthread_mutex_unlock(&javaObjectsLock);

  for (i = jsObjectsById.begin(); i != jsObjectsById.end(); i++) {
    JSObjectRef ref = i->second;
    delete static_cast<TrackingDataRef>(JSObjectGetPrivate(ref));
    JSObjectSetPrivate(ref, NULL);
    JSValueUnprotect(contextRef, i->second);
  }

  JSClassRelease(javaObjectWrapperClass);

  JSValueUnprotect(contextRef, stringConstructor);
  JSValueUnprotect(contextRef, makeResultFunction);

  JSGarbageCollect(contextRef);
  pthread_mutex_destroy(&javaObjectsLock);
}

void WebScriptSessionHandler::disconnectDetectedImpl() {
  crashHandler->crash(__PRETTY_FUNCTION__, "Server disconnect detected");
}

void WebScriptSessionHandler::fatalError(HostChannel& channel,
    const std::string& message) {
  // TODO: better way of reporting error?
  Debug::log(Debug::Error) << "Fatal error: " << message << Debug::flush;
}

void WebScriptSessionHandler::sendFreeValues(HostChannel& channel) {
  pthread_mutex_lock(&javaObjectsLock);
  int idCount = javaObjectsToFree.size();
  if (idCount == 0) {
    pthread_mutex_unlock(&javaObjectsLock);
    return;
  }

  int ids[idCount];
  std::set<int>::iterator it = javaObjectsToFree.begin();
  for (int i = 0; i < idCount; it++) {
    ids[i++] = *it;
  }
  if (!ServerMethods::freeJava(channel, this, idCount, ids)) {
    Debug::log(Debug::Error) << "Unable to free Java ids" << Debug::flush;
  } else {
    Debug::log(Debug::Debugging) << "Freed " << idCount << " Java ids" << Debug::flush;
  }
  javaObjectsToFree.clear();
  pthread_mutex_unlock(&javaObjectsLock);
}

void WebScriptSessionHandler::freeValue(HostChannel& channel, int idCount, const int* ids) {
  Debug::log(Debug::Spam) << "freeValue freeing " << idCount << " js objects" << Debug::flush;
  if (idCount == 0) {
    return;
  }

  for (int i = 0; i < idCount; i++) {
    int objId = ids[i];

    std::map<int, JSObjectRef>::iterator x = jsObjectsById.find(objId);
    if (x == jsObjectsById.end()) {
      Debug::log(Debug::Error) << "Unknown object id " << objId << Debug::flush;
      continue;
    }

    JSObjectRef ref = x->second;
    jsObjectsById.erase(objId);
    jsIdsByObject.erase(ref);
    JSValueUnprotect(contextRef, ref);
  }
  Debug::log(Debug::Debugging) << "Freed " << idCount << " JS objects" << Debug::flush;
}

void WebScriptSessionHandler::initiateAutodestructSequence(const char* functionName, const char* message) {
  crashHandler->crash(functionName, message);
}

bool WebScriptSessionHandler::invoke(HostChannel& channel, const Value& thisObj,
                                    const std::string& methodName,
                                    int numArgs, const Value* args, Value* returnValue) {
  Debug::log(Debug::Spam) << "invoke " << methodName << Debug::flush;

  JSValueRef argsJS[numArgs];
  JSValueRef localException = NULL;
  JSStringRef methodNameJS = JSStringCreateWithUTF8CString(methodName.c_str());
  JSObjectRef thisObjJs;

  if (thisObj.isNull()) {
    thisObjJs = JSContextGetGlobalObject(contextRef);
  } else {
    thisObjJs = (JSObjectRef) makeValueRef(thisObj);
  }

  JSValueRef functionValueJS = JSObjectGetProperty(contextRef, JSContextGetGlobalObject(contextRef),
                                                   methodNameJS, &localException);
  JSStringRelease(methodNameJS);

  if (!JSValueIsObject(contextRef, functionValueJS)) {
    char message[512];
    snprintf(message, sizeof(message), "Could not find method for property name %s on %s", methodName.c_str(), thisObj.toString().c_str());
    makeExceptionValue(*returnValue, message);
    return true;
  }

  JSObjectRef functionJS = JSValueToObject(contextRef, functionValueJS,
                                           &localException);
  if (localException) {
    makeValue(*returnValue, localException);
    return true;
  }

  // Convert the arguments
  for (int i = 0; i < numArgs; i++) {
    argsJS[i] = makeValueRef(args[i]);
  }

  JSValueRef retVal = JSObjectCallAsFunction(contextRef, functionJS,
                                             thisObjJs, numArgs, argsJS,
                                             &localException);

  if (localException) {
    Debug::log(Debug::Spam) << "Returning exception to server" << Debug::flush;
    makeValue(*returnValue, localException);
    return true;
  } else {
    makeValue(*returnValue, retVal);
    return false;
  }
}

bool WebScriptSessionHandler::invokeSpecial(HostChannel& channel, SpecialMethodId method, int numArgs,
                                            const Value* const args, Value* returnValue) {
  Debug::log(Debug::Spam) << "invokeSpecial " << method << Debug::flush;
  switch (method) {
    case GetProperty:
    {
      int objId = args[0].getInt();
      std::map<int, JSObjectRef>::iterator i = jsObjectsById.find(objId);
      if (i == jsObjectsById.end()) {
        char message[50];
        snprintf(message, sizeof(message), "Unknown object id %i", objId);
        makeExceptionValue(*returnValue, message);
        return true;
      }

      JSObjectRef jsObj = i->second;
      if (args[1].isString()) {
        JSStringRef asString = JSValueToStringCopy(contextRef, jsObj, NULL);
        int maxLength = JSStringGetMaximumUTF8CStringSize(asString);
        scoped_array<char> asChars(new char[maxLength]);
        JSStringGetUTF8CString(asString, asChars.get(), maxLength);

        JSValueRef localException = NULL;
        JSStringRef str = JSStringCreateWithUTF8CString(args[1].getString().c_str());
        JSValueRef value = JSObjectGetProperty(contextRef, jsObj, str, &localException);
        JSStringRelease(str);
        if (localException) {
          makeValue(*returnValue, localException);
          return true;
        } else {
          makeValue(*returnValue, value);
          return false;
        }
      } else if (args[1].isInt()) {
        JSValueRef localException = NULL;
        JSValueRef value = JSObjectGetPropertyAtIndex(contextRef, jsObj, args[1].getInt(), &localException);

        if (localException) {
          makeValue(*returnValue, localException);
          return true;
        } else {
          makeValue(*returnValue, value);
          return false;
        }
      } else {
        char message[50];
        snprintf(message, sizeof(message), "Unhandled argument type %s for getProperty", args[1].toString().c_str());
        makeExceptionValue(*returnValue, message);
        return true;
      }
    }
    default:
      Debug::log(Debug::Error) << "Unhandled invokeSpecial " << method << Debug::flush;
      makeExceptionValue(*returnValue, "Unhandled invokeSpecial");
      return true;
  }
}

JSValueRef WebScriptSessionHandler::javaFunctionCallbackImpl (int dispatchId,
                                                              JSObjectRef thisObject,
                                                              size_t argumentCount,
                                                              const JSValueRef arguments[],
                                                              JSValueRef* exception){
  /*
   * NB: Because throwing exceptions in JavaScriptCore is trivial, we don't rely
   * on any special return values to indicate that an exception is thrown, we'll simply
   * throw the exception.
   */
  Debug::log(Debug::Debugging) << "Java method " << dispatchId << " invoked" << Debug::flush;

  JSValueRef toReturn;
  if (crashHandler->hasCrashed()) {
    Debug::log(Debug::Debugging) << "Not executing method since we have crashed" << Debug::flush;
    toReturn =  JSValueMakeUndefined(contextRef);
  } else {
    /*
     * If a JS function is evaluated without an meaningful this object or the global
     * object is implicitly used as the this object, we'll assume that the
     * Java-derived method is static, and send a null this object to the server
     */
    Value thisValue;
    if (JSValueIsEqual(contextRef, thisObject, JSContextGetGlobalObject(contextRef), NULL)) {
      thisValue = Value();
      thisValue.setNull();
    } else {
      makeValue(thisValue, thisObject);
    }

    // Argument conversion is straightforward
    Value *args = new Value[argumentCount];
    for (int i = 0; i < argumentCount; i++) {
      makeValue(args[i], arguments[i]);
    }
      
    bool status = InvokeMessage::send(*channel, thisValue, dispatchId,
                                      argumentCount, args);
    delete[] args;
    if (!status) {
      initiateAutodestructSequence(__PRETTY_FUNCTION__, "Unable to send invocation message");
      *exception = makeException("Unable to send invocation message");
      return JSValueMakeUndefined(contextRef);
    }

    scoped_ptr<ReturnMessage> ret(channel->reactToMessagesWhileWaitingForReturn(sessionHandler));

    if (!ret.get()) {
      initiateAutodestructSequence(__PRETTY_FUNCTION__, "Unable to receive return message");
      *exception = makeException("Unable to receive return message");
      return JSValueMakeUndefined(contextRef);
    }

    Value v = ret->getReturnValue();

    if (ret->isException()) {
      *exception = makeValueRef(v);
      toReturn = JSValueMakeUndefined(contextRef);
    } else {
      toReturn = makeValueRef(v);
    }
  }

  JSValueRef makeResultArguments[] = {JSValueMakeBoolean(contextRef, false), toReturn};
  return JSObjectCallAsFunction(contextRef, makeResultFunction, NULL, 2, makeResultArguments, exception);
}

void WebScriptSessionHandler::javaObjectFinalizeImpl(int objId) {
  if (pthread_mutex_lock(&javaObjectsLock)) {
    Debug::log(Debug::Error) << "Unable to acquire javaObjectsLock in thread " << pthread_self() << " " << __PRETTY_FUNCTION__ << Debug::flush;
    initiateAutodestructSequence(__PRETTY_FUNCTION__, "Unable to acquire javaObjectsLock");
    return;
  }
  javaObjectsById.erase(objId);
  javaObjectsToFree.insert(objId);
  if (pthread_mutex_unlock(&javaObjectsLock)) {
    Debug::log(Debug::Error) << "Unable to release javaObjectsLock in thread " << pthread_self() << " " << __PRETTY_FUNCTION__ << Debug::flush;
    initiateAutodestructSequence(__PRETTY_FUNCTION__, "Unable to release javaObjectsLock");
  }
}

JSValueRef WebScriptSessionHandler::javaObjectGetPropertyImpl (TrackingDataRef tracker, JSObjectRef object,
                                                               JSStringRef propertyName, JSValueRef* exception) {
  *exception = NULL;
  
  if (crashHandler->hasCrashed()) {
    Debug::log(Debug::Debugging) << "Not executing since we have crashed" << Debug::flush;
    return JSValueMakeUndefined(contextRef);
  }

  // Convert the name
  int maxLength = JSStringGetMaximumUTF8CStringSize(propertyName);
  scoped_array<char> propertyNameChars(new char[maxLength]);
  JSStringGetUTF8CString(propertyName, propertyNameChars.get(), maxLength);
  JSValueRef toReturn;

  if (!strcmp(propertyNameChars.get(), "toString")) {
    // We'll call out to the JSNI tear-off support function
    JSStringRef tearOffName =JSStringCreateWithUTF8CString("__gwt_makeTearOff");
    JSValueRef makeTearOffValue = JSObjectGetProperty(contextRef, JSContextGetGlobalObject(contextRef), tearOffName, exception);
    JSStringRelease(tearOffName);
    if (*exception) {
      return JSValueMakeUndefined(contextRef);
    }

    JSObjectRef makeTearOff = JSValueToObject(contextRef, makeTearOffValue, exception);
    if (*exception) {
      return JSValueMakeUndefined(contextRef);
    }

    JSValueRef arguments[3];
    arguments[0] = object;
    arguments[1] = JSValueMakeNumber(contextRef, 0);
    arguments[2] = JSValueMakeNumber(contextRef, 0);
    toReturn = JSObjectCallAsFunction(contextRef, makeTearOff, JSContextGetGlobalObject(contextRef), 3, arguments, exception);
  } else {
    char* endptr;
    int dispatchId = strtol(propertyNameChars.get(), &endptr, 10);

    if (*endptr != '\0') {
      Debug::log(Debug::Error) << "Unable to parse dispatch id " << propertyNameChars.get() << Debug::flush;
      *exception = makeException("Unable to parse dispatch id");
    } else if (dispatchId < 0) {
      Debug::log(Debug::Error) << "Dispatch ids may not be negative" << Debug::flush;
      *exception = makeException("Dispatch ids may not be negative");
    } else {
      Value v = ServerMethods::getProperty(*channel, this, tracker->getObjectId(), dispatchId);
      toReturn = makeValueRef(v);
    }
  }

  return toReturn;
}

bool WebScriptSessionHandler::javaObjectHasPropertyImpl (TrackingDataRef tracker, JSObjectRef object, JSStringRef propertyName) {
  // The property name must either be "toString" or a number
  int maxLength = JSStringGetMaximumUTF8CStringSize(propertyName);
  scoped_array<char> propertyNameChars(new char[maxLength]);
  JSStringGetUTF8CString(propertyName, propertyNameChars.get(), maxLength);
  if (!strcmp(propertyNameChars.get(), "toString")) {
    return true;
  }

  char* endptr;
  int dispatchId = strtol(propertyNameChars.get(), &endptr, 10);

  if (*endptr != '\0') {
    return false;
  } else if (dispatchId < 0) {
    return false;
  } else {
    return true;
  }
}

bool WebScriptSessionHandler::javaObjectSetPropertyImpl (TrackingDataRef tracker, JSObjectRef object,
                                                         JSStringRef propertyName, JSValueRef jsValue,
                                                         JSValueRef* exception) {

  if (crashHandler->hasCrashed()) {
    Debug::log(Debug::Debugging) << "Not executing since we have crashed" << Debug::flush;
    return true;
  }

  int maxLength = JSStringGetMaximumUTF8CStringSize(propertyName);
  scoped_array<char> propertyNameChars(new char[maxLength]);
  JSStringGetUTF8CString(propertyName, propertyNameChars.get(), maxLength);
  Value value;

  char* endptr;
  int dispatchId = strtol(propertyNameChars.get(), &endptr, 10);

  if (*endptr != '\0') {
    // TODO Figure out the right policy here; when we throw a Java object, JSCore wants to
    // add expandos to record stack information.  It would be possible to map the limited
    // number of properties into a synthetic causal exception in the exception being thrown.
  } else if (dispatchId < 0) {
    // Do nothing.
    Debug::log(Debug::Error) << "Dispatch ids may not be negative" << Debug::flush;
    *exception = makeException("Dispatch ids may not be negative");
  } else {

    makeValue(value, jsValue);

    if (!ServerMethods::setProperty(*channel, this, tracker->getObjectId(), dispatchId, value)) {
      char message[50];
      snprintf(message, sizeof(message), "Unable to set value object %i dispatchId %i", tracker->getObjectId(), dispatchId);
      *exception = makeException(message);
    }
  }

  // true means to not try to follow the prototype chain; not an indication of success
  return true;
}

void WebScriptSessionHandler::loadJsni(HostChannel& channel, const std::string& js) {
  Debug::log(Debug::Spam) << "loadJsni " << js << Debug::flush;
  JSValueRef localException = NULL;

  JSStringRef script = JSStringCreateWithUTF8CString(js.c_str());
  JSEvaluateScript(contextRef, script, NULL, NULL, NULL, &localException);
  JSStringRelease(script);

  if (localException) {
    // TODO Exception handling
    Debug::log(Debug::Error) << "Exception thrown during loadJsni" << Debug::flush;
  } else {
    Debug::log(Debug::Spam) << "Success" << Debug::flush;
  }
}

JSValueRef WebScriptSessionHandler::makeException(const char* message) {
  JSValueRef localException = NULL;
  JSObjectRef global = JSContextGetGlobalObject(contextRef);

  JSStringRef errorName = JSStringCreateWithUTF8CString("Error");
  JSValueRef errorValue = JSObjectGetProperty(contextRef, global, errorName, &localException);
  JSStringRelease(errorName);

  if (!JSValueIsObject(contextRef, errorValue)) {
    initiateAutodestructSequence(__PRETTY_FUNCTION__, "Could not get reference to Error");
    return JSValueMakeUndefined(contextRef);
  }

  JSObjectRef errorObject = (JSObjectRef) errorValue;

  if (!JSObjectIsFunction(contextRef, errorObject)) {
    initiateAutodestructSequence(__PRETTY_FUNCTION__, "Error was not a function");
    return JSValueMakeUndefined(contextRef);
  }

  JSValueRef args[1];
  JSStringRef messageJs = JSStringCreateWithUTF8CString(message);
  args[0] = JSValueMakeString(contextRef, messageJs);
  JSStringRelease(messageJs);

  return JSObjectCallAsConstructor(contextRef, errorObject, 1, args, &localException);
}

void WebScriptSessionHandler::makeExceptionValue(Value& value, const char* message) {
  makeValue(value, makeException(message));
}

JSObjectRef WebScriptSessionHandler::makeJavaWrapper(int objId) {
  Debug::log(Debug::Spam) << "Creating wrapper for Java object " << objId << Debug::flush;

  TrackingDataRef data = new TrackingData(this, objId);
  return JSObjectMake(contextRef, javaObjectWrapperClass,
                      const_cast<TrackingData*>(data));
}

JSValueRef WebScriptSessionHandler::makeValueRef(const Value& v) {
  std::map<int, JSObjectRef>::iterator i;
  switch (v.getType()) {
    case Value::NULL_TYPE:
      return JSValueMakeNull(contextRef);

    case Value::BOOLEAN:
      return JSValueMakeBoolean(contextRef, v.getBoolean());

    case Value::BYTE:
      return JSValueMakeNumber(contextRef, v.getByte());

    case Value::CHAR:
      return JSValueMakeNumber(contextRef, v.getChar());

    case Value::SHORT:
      return JSValueMakeNumber(contextRef, v.getShort());

    case Value::INT:
      return JSValueMakeNumber(contextRef, v.getInt());

    case Value::LONG:
      return JSValueMakeNumber(contextRef, v.getLong());

    case Value::FLOAT:
      return JSValueMakeNumber(contextRef, v.getFloat());

    case Value::DOUBLE:
      return JSValueMakeNumber(contextRef, v.getDouble());

    case Value::STRING:
    {
      std::string stringValue = v.getString();

      // We need to handle the conversion ourselves to be able to get both
      // UTF8 encoding as well as explicit control over the length of the string
      // due to the possibility of null characters being part of the data
      CFStringRef cfString = CFStringCreateWithBytesNoCopy(NULL, (UInt8*)stringValue.data(),
                                                           stringValue.length(), kCFStringEncodingUTF8,
                                                           false, kCFAllocatorNull);
      JSStringRef stringRef = JSStringCreateWithCFString(cfString);
      JSValueRef toReturn = JSValueMakeString(contextRef, stringRef);
      JSStringRelease(stringRef);
      CFRelease(cfString);
      return toReturn;
    }

    case Value::JAVA_OBJECT:
    {
      unsigned javaId = v.getJavaObjectId();
      JSObjectRef ref;

      pthread_mutex_lock(&javaObjectsLock);
      i = javaObjectsById.find(javaId);

      /*
       * It's possible that we've already finalized the JsObjectRef that
       * represented the object with the given id.  If so, we must remove it
       * from the list of finalized object ids to avoid prematurely freeing
       * the object on the server.
       */
      javaObjectsToFree.erase(javaId);

      if (i == javaObjectsById.end()) {
        /*
         * We don't call JSValueProtect so that the JavaObject peer can be
         * garbage-collected during the lifetime of the program.  Object
         * identity is maintained as long as the object hasn't been finalized.
         * If it had been finalized, then there wouldn't be an object to use
         * as a basis for identity comparison.
         *
         * NB: The act of creating the wrapper may trigger a GC.
         */
        ref = makeJavaWrapper(javaId);

        javaObjectsById[javaId] = ref;

      } else {
        ref = i->second;
      }
      pthread_mutex_unlock(&javaObjectsLock);


      return ref;
    }

    case Value::JS_OBJECT:
    {
      int jsId = v.getJsObjectId();

      i = jsObjectsById.find(jsId);
      if (i == jsObjectsById.end()) {
        char errMsg[50];
        snprintf(errMsg, sizeof(errMsg), "Missing JsObject with id %i", jsId);
        return makeException(errMsg);

      } else {
        return i->second;
      }
    }

    case Value::UNDEFINED:
      return JSValueMakeUndefined(contextRef);

    default:
    {
      char message[50];
      snprintf(message, sizeof(message), "Could not convert %s", v.toString().c_str());
      initiateAutodestructSequence(__PRETTY_FUNCTION__, message);
      return makeException(message);
    }
  }
}

bool WebScriptSessionHandler::makeValue(Value& ret, JSValueRef v) {
  JSValueRef localException = NULL;

  if (JSValueIsNull(contextRef, v)) {
    ret.setNull();

  } else if (JSValueIsUndefined(contextRef, v)) {
    ret.setUndefined();

  } else if (JSValueIsBoolean(contextRef, v)) {
    ret.setBoolean(JSValueToBoolean(contextRef, v));

  } else if (JSValueIsNumber(contextRef, v)) {
    double d = JSValueToNumber(contextRef, v, &localException);
    int i = round(d);
    if (i == d) {
      ret.setInt(i);
    } else {
      ret.setDouble(d);
    }

  } else if (JSValueIsString(contextRef, v) ||
             JSValueIsInstanceOfConstructor(contextRef, v, stringConstructor, &localException)) {
    return makeValueFromString(ret, v);

  } else if (JSValueIsObjectOfClass(contextRef, v, javaObjectWrapperClass)) {
    // It's one of our Java object proxies
    JSObjectRef objectRef = JSValueToObject(contextRef, v, &localException);

    if (!localException) {
      TrackingDataRef tracker = (TrackingDataRef) JSObjectGetPrivate(objectRef);
      int objId = tracker->getObjectId();
      ret.setJavaObject(objId);
      Debug::log(Debug::Spam) << "Made a Java object Value " << objId << Debug::flush;
    }

  } else if (JSValueIsObject(contextRef, v)) {
    JSObjectRef objectRef = JSValueToObject(contextRef, v, &localException);
    if (!localException) {
      /*
       * Then this is just a plain-old JavaScript Object.  Because JSCore
       * doesn't retain private data for objects derived from the built-in
       * Object type, we'll simply revert to using a pair of maps to provide
       * a 1:1 mapping of JSObjectRefs and ints.
       */
      std::map<JSObjectRef, int>::iterator i = jsIdsByObject.find(objectRef);
      if (i != jsIdsByObject.end()) {
        // We've seen the object before
        ret.setJsObjectId(i->second);
      } else {
        // Allocate a new id
        int objId = ++jsObjectId;
        JSValueProtect(contextRef, objectRef);

        jsObjectsById[objId] = objectRef;
        jsIdsByObject[objectRef] = objId;

        ret.setJsObjectId(objId);
        Debug::log(Debug::Spam) << "Made JS Value " << objId << Debug::flush;
      }
    }
  } else {
    Debug::log(Debug::Error) << "Unhandled JSValueRef -> Value conversion in plugin" << Debug::flush;
    ret.setString("Unhandled JSValueRef -> Value conversion in plugin");
  }

  if (localException) {
    makeValue(ret, localException);
    return true;
  } else {
    return false;
  }
}

bool WebScriptSessionHandler::makeValueFromString(Value& ret, JSValueRef value) {
  JSValueRef localException = NULL;

  JSStringRef jsString = JSValueToStringCopy(contextRef, value, &localException);
  if (localException) {
    makeValue(ret, localException);
    return true;
  }

  CFStringRef cfString = JSStringCopyCFString(NULL, jsString);
  JSStringRelease(jsString);

  CFIndex cfLength = CFStringGetLength(cfString);
  CFIndex maxLength = CFStringGetMaximumSizeForEncoding(cfLength, kCFStringEncodingUTF8);
  scoped_array<char> utf8(new char[maxLength]);

  CFIndex numBytes;
  CFStringGetBytes(cfString, CFRangeMake(0, cfLength), kCFStringEncodingUTF8,
                   0, false, (UInt8*) utf8.get(), maxLength, &numBytes);
  CFRelease(cfString);

  ret.setString(utf8.get(), numBytes);
  Debug::log(Debug::Spam) << "Made a string Value " << ret.getString() << Debug::flush;
  return false;
}
