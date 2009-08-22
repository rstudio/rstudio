#ifndef __PROTOCOLVERSIONMESSAGE_H
#define __PROTOCOLVERSIONMESSAGE_H
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
 * Class representing a ProtocolVersion message received from the server, and a
 * way to send one.
 *
 * This message indicates which version of the protocol the server has chosen
 * for the remainder of the communication.
 */
class ProtocolVersionMessage : public Message {
public:
  static const char TYPE = MESSAGE_TYPE_PROTOCOL_VERSION;
private:
  int version;

protected:
  /**
   * @param version selected protocol version
   */
  ProtocolVersionMessage(int version) : version(version) {}

public:
  ~ProtocolVersionMessage();
  virtual char getType() const;

  int getVersion() const { return version; }

  static ProtocolVersionMessage* receive(HostChannel& channel);
  static bool send(HostChannel& channel, int version);
};
#endif
