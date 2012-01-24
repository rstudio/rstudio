#ifndef __INVOKESPECIALMESSAGE_H
#define __INVOKESPECIALMESSAGE_H
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
#include "SessionHandler.h"
#include "Value.h"

class HostChannel;

/**
 * Class representing an InvokeSpecial message received from the server, and a
 * way to send an invoke message to the server.
 */
class InvokeSpecialMessage : public Message {
public:
  static const char TYPE = MESSAGE_TYPE_INVOKESPECIAL;
private:
  SessionHandler::SpecialMethodId dispatchId;
  int numArgs;
  const gwt::Value* args;

protected:
  /**
   * @param args array of arguments -- InvokeMessage takes ownership and will
   *     destroy when it is destroyed.
   */
  InvokeSpecialMessage(SessionHandler::SpecialMethodId dispatchId, int numArgs,
      const gwt::Value* args) : dispatchId(dispatchId), numArgs(numArgs), args(args) {}

public:
  ~InvokeSpecialMessage();
  virtual char getType() const;

  SessionHandler::SpecialMethodId getDispatchId() const { return dispatchId; }
  int getNumArgs() const { return numArgs; }
  const gwt::Value* const getArgs() const { return args; }

  static InvokeSpecialMessage* receive(HostChannel& channel);
  static bool send(HostChannel& channel, int dispatchId, int numArgs,
      const gwt::Value* args);
};
#endif
