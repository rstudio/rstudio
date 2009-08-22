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

/**
 * Class representing a LoadModule message sent to the server.
 *
 * This message requests that the server load the requested module.  A return
 * message is not sent until the module is unloaded or fails to load.
 */

class LoadModuleMessage : public Message {
  static const char TYPE = MESSAGE_TYPE_LOAD_MODULE;

public:
  LoadModuleMessage(const std::string& url, const std::string& tabKey,
      const std::string& sessionKey, const std::string& moduleName,
      const std::string& userAgent) : url(url), tabKey(tabKey),
      sessionKey(sessionKey), moduleName(moduleName), userAgent(userAgent) { }

  const std::string& getUrl() const { return url; }
  const std::string& getTabKey() const { return tabKey; }
  const std::string& getSessionKey() const { return sessionKey; }
  const std::string& getModuleName() const { return moduleName; }
  const std::string& getUserAgent() const { return userAgent; }
  virtual char getType() const;

  static bool send(HostChannel& channel, const std::string& url,
      const std::string& tabKey, const std::string& sessionKey,
      const std::string& moduleName, const std::string& userAgent,
      SessionHandler* handler);
private:
  std::string url;
  std::string tabKey;
  std::string sessionKey;
  std::string moduleName;
  std::string userAgent;
};
#endif
