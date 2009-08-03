#ifndef _H_SessionData
#define _H_SessionData
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

#include "SessionHandler.h"

#include "jsapi.h"

class HostChannel;

class SessionData {
public:
  SessionData(HostChannel* channel, SessionHandler* sessionHandler,
      JSContext* ctx) : channel(channel), sessionHandler(sessionHandler),
      global(JS_GetGlobalObject(ctx)), runtime(JS_GetRuntime(ctx)),
      toStringTearOff(JSVAL_VOID)
  {
  }

  HostChannel* getHostChannel() const {
    return channel;
  }

  SessionHandler* getSessionHandler() const {
    return sessionHandler;
  }

  JSObject* getGlobalObject() const {
    return global;
  }

  jsval getToStringTearOff() const {
    return toStringTearOff;
  }

  /*
  * Convert a value from the JavaScript into something that can be sent back
  * to the OOPHM host.
  */
  virtual void makeValueFromJsval(Value& retVal, JSContext* ctx, const jsval& value)=0;

  /*
  * Convert a value from the OOPHM host into something that can be passed into
  * the JavaScript execution environment.
  */
  virtual void makeJsvalFromValue(jsval& retVal, JSContext* ctx, const Value& value)=0;
  
  /*
  * Removes the JavaObject wrapper with the given id and notifies the host.
  */
  virtual void freeJavaObject(int objectId)=0;

protected:
  /*
  * The communication channel used for the OOPHM session.
  */
  HostChannel* const channel;

  /*
  * A reference to the SessionHandler being used in the OOPHM session.
  */
  SessionHandler* const sessionHandler;

  JSRuntime* runtime;
  
  JSObject* const global; 
  
  /**
   * A function object representing the toString tear-off method.
   */
  jsval toStringTearOff;
};

#endif
