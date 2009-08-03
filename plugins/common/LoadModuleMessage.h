#ifndef __LOADMODULEMESSAGE_H
#define __LOADMODULEMESSAGE_H
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

#include <string>

#include "Message.h"
#include "BrowserChannel.h"
#include "HostChannel.h"
#include "SessionHandler.h"

class HostChannel;

class LoadModuleMessage : public Message {
  static const char TYPE = MESSAGE_TYPE_LOAD_MODULE;

public:  
  LoadModuleMessage(int version, std::string& moduleName, std::string& userAgent) :
    version(version), moduleName(moduleName), userAgent(userAgent) { }

  int getVersion() const { return version; }
  const std::string& getModuleName() const { return moduleName; }
  const std::string& getUserAgent() const { return userAgent; }
  virtual char getType() const;
  
  static bool send(HostChannel& channel, uint32_t version, const char* moduleName,
      uint32_t moduleNameLen, const char* userAgent, SessionHandler* handler);
private:
  int version;
  std::string moduleName;
  std::string userAgent;
};
#endif
