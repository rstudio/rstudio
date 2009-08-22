#ifndef __CHECKVERSIONSMESSAGE_H
#define __CHECKVERSIONSMESSAGE_H
/*
 * Copyright 2009 Google Inc.
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
#include "Value.h"

class HostChannel;

/**
 * Class representing a CheckVersions message sent to the server.
 *
 * This message tells the server the range or protocol versions this plugin
 * understands as well as the hosted.html version (so stale copies of it can
 * be detected).
 */
class CheckVersionsMessage : public Message {
public:
  static const char TYPE = MESSAGE_TYPE_CHECK_VERSIONS;
private:
  int minVersion;
  int maxVersion;
  const std::string& hostedHtmlVersion;

protected:
  CheckVersionsMessage(int minVersion, int maxVersion,
      const std::string& hostedHtmlVersion) : minVersion(minVersion),
      maxVersion(maxVersion), hostedHtmlVersion(hostedHtmlVersion) {}

public:
  ~CheckVersionsMessage();
  virtual char getType() const;

  const std::string& getHostedHtmlVersion() const { return hostedHtmlVersion; }

  static CheckVersionsMessage* receive(HostChannel& channel);
  static bool send(HostChannel& channel, int minVersion, int maxVersion,
      const std::string& hostedHtmlVersion);
};
#endif
