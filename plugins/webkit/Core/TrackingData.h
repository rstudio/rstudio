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

#import "SessionData.h"

/*
 * This class encapsulates per-object information to determine how to dispatch
 * a given JSObjectRef that is a Java object proxy. Instances of
 * this class are intended to be stored in the private data area provided
 * by JSObjectRef.
 */
class TrackingData {
public:
  /*
   * Constructor for Java object proxies.
   */
  TrackingData(const SessionDataRef sessionData, const unsigned objId) :
  objId(objId), sessionData(sessionData) {
  }
  
  const unsigned getObjectId() const {
    return objId;
  }
  
  SessionDataRef getSessionData() const {
    return sessionData;
  }
  
private:
  /*
   * The tracking number assigned to the object.
   */
  const unsigned objId;
  
  /*
   * A reference to the per-OOPHM-session data.
   */
  SessionDataRef const sessionData;  
};
typedef const TrackingData* TrackingDataRef;
