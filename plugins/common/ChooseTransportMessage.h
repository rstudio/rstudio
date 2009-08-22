#ifndef __CHOOSETRANSPORTMESSAGE_H
#define __CHOOSETRANSPORTMESSAGE_H
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
#include <vector>
#include "Message.h"
#include "BrowserChannel.h"
#include "Value.h"

class HostChannel;

/**
 * Class representing a ChooseTransport message to send to the server.
 *
 * This message type is a request for the server to choose an alternate
 * transport from a provide list, such as a shared memory transport.  The
 * set of transport names is open-ended and thus requires mutual agreement
 * on the names between the client and server.
 */
class ChooseTransportMessage : public Message {
public:
  static const char TYPE = MESSAGE_TYPE_CHOOSE_TRANSPORT;
private:
  std::vector<std::string> transports;

protected:
  ChooseTransportMessage(const std::vector<std::string>& transports)
      : transports(transports) {}

public:
  ~ChooseTransportMessage();
  virtual char getType() const;

  const std::vector<std::string>& getTransports() const { return transports; }

  static ChooseTransportMessage* receive(HostChannel& channel);
  static bool send(HostChannel& channel,
      const std::vector<std::string>& transport);
};
#endif
