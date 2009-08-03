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

#import "HostChannel.h"
#import "JavaScriptCore/JavaScriptCore.h"
#import "SessionHandler.h"

/*
 * Encapsules per-OOPHM-session data.
 */
class SessionData {
public:
  SessionData(HostChannel* channel, JSGlobalContextRef contextRef, 
              SessionHandler* sessionHandler) : channel(channel),
                                                contextRef(contextRef),
                                                sessionHandler(sessionHandler) {
  }
  
  JSGlobalContextRef getContext() const {
    return contextRef;
  }
  
  HostChannel* getHostChannel() const {
    return channel;
  }
  
  SessionHandler* getSessionHandler() const {
    return sessionHandler;
  }

protected:
  /*
   * The communication channel used for the OOPHM session.
   */
  HostChannel* const channel;
  
  /*
   * The JavaScriptCore interpreter instance.
   */
  JSGlobalContextRef const contextRef;
  
  /*
   * A reference to the SessionHandler being used in the OOPHM session.
   */
  SessionHandler* const sessionHandler;
};
typedef SessionData* SessionDataRef;