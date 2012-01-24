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

#include "Debug.h"

#include <cstring>
#include <string>

#include "LoadModuleMessage.h"
#include "scoped_ptr/scoped_ptr.h"

char LoadModuleMessage::getType() const {
  return LoadModuleMessage::TYPE;
}

bool LoadModuleMessage::send(HostChannel& channel, const std::string& url,
    const std::string& tabKey, const std::string& sessionKey,
    const std::string& moduleName, const std::string& userAgent,
    SessionHandler* handler) {
  Debug::log(Debug::Debugging) << "LoadModule(url=\"" << url << "\", tabKey=\""
      << "\", sessionKey=\"" << sessionKey << "\", module=\"" << moduleName
      << "\")" << Debug::flush;
  if (!channel.sendByte(TYPE) || !channel.sendString(url)
      || !channel.sendString(tabKey)
      || !channel.sendString(sessionKey)
      || !channel.sendString(moduleName)
      || !channel.sendString(userAgent)) {
    return false;
  }
  scoped_ptr<ReturnMessage> ret(channel.reactToMessagesWhileWaitingForReturn(
      handler));
  if (!ret.get()) {
    return false;
  }
  return !ret.get()->isException();
}
