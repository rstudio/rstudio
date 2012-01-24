#ifndef _H_ServerMethods
#define _H_ServerMethods
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

#include "Value.h"

class HostChannel;
class SessionHandler;

/**
 * Encapsulates the methods defined on the special server object.
 */
class ServerMethods {
public:
  /**
   * Get the value of a property on an object.
   * 
   * @param objectRef ID of object to fetch field on
   * @param dispatchID dispatch ID of field
   * @return the value of the property, undef if none (or on error)
   */
  static gwt::Value getProperty(HostChannel& channel, SessionHandler* handler, int objectRef,
      int dispatchId);
  
  /**
   * Lookup the named method on the specified class.
   * 
   * @return the dispatch ID (non-negative) of the method if present, or -1 if not.
   * If an error is encountered, -2 is returned.
   */
  static int hasMethod(HostChannel& channel, SessionHandler* handler, int classId,
      const std::string& name);
  
  /**
   * Lookup the named property on the specified class.
   * 
   * @return the dispatch ID (non-negative) of the property if present, or -1 if not.
   * If an error is encountered, -2 is returned.
   */
  static int hasProperty(HostChannel& channel, SessionHandler* handler, int classId,
      const std::string& name);

  /**
   * Set the value of a property on an object.
   * 
   * @param objectRef ID of object to fetch field on
   * @param dispatchID dispatch ID of field
   * @param value value to store in the property
   * @return false if an error occurred
   */
  static bool setProperty(HostChannel& channel, SessionHandler* handler, int objectRef,
      int dispatchId, const gwt::Value& value);
  
  /**
   * Tell the server that the client no longer has any references to the specified
   * Java object.
   * 
   * @param objcetRef ID of object to free
   * @return false if an error occurred
   */ 
  static bool freeJava(HostChannel& channel, SessionHandler* handler, int idCount, const int* ids);
};

#endif
