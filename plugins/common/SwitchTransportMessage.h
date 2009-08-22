#ifndef __SWITCHTRANSPORTMESSAGE_H
#define __SWITCHTRANSPORTMESSAGE_H
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
 * Class representing an SwitchTransport message received from the server, and a
 * way to send one.
 *
 * The SwitchTransport message is sent by the server in response to a
 * ChooseTransport message, and will select one of the available transports
 * advertised by the client.  The empty string represents the in-band channel,
 * and is always an acceptable response.
 */
class SwitchTransportMessage : public Message {
public:
  static const char TYPE = MESSAGE_TYPE_SWITCH_TRANSPORT;
private:
  std::string transport;

protected:
  SwitchTransportMessage(const std::string& transport) : transport(transport) {}

public:
  ~SwitchTransportMessage();
  virtual char getType() const;

  const std::string& getTransport() const { return transport; }

  static SwitchTransportMessage* receive(HostChannel& channel);
  static bool send(HostChannel& channel, const std::string& transport);
};
#endif
