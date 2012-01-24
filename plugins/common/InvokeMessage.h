#ifndef __INVOKEMESSAGE_H
#define __INVOKEMESSAGE_H
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
#include "Value.h"

class HostChannel;

/**
 * Class representing an InvokeMessage received from the server, and a way
 * to send an invoke message to the server.
 *
 * Note that the wire protocol is different in the two directions, as the
 * server sends a string for the method name and the client send an integer
 * dispatchID.
 */
class InvokeMessage : public Message {
public:
  static const char TYPE = MESSAGE_TYPE_INVOKE;
  static const int TOSTRING_DISP_ID = 0;
private:
  gwt::Value thisRef;
  std::string methodName;
  int methodDispatchId;
  int numArgs;
  const gwt::Value* args;

protected:
  /**
   * @param args array of arguments -- InvokeMessage takes ownership and will
   *     destroy when it is destroyed.
   */
  InvokeMessage(const gwt::Value& thisRef, const std::string& methodName,
      int numArgs, const gwt::Value* args) : thisRef(thisRef), methodName(methodName),
      numArgs(numArgs), args(args) {}

public:
  ~InvokeMessage();
  virtual char getType() const;

  gwt::Value getThis() const { return thisRef; }
  const std::string& getMethodName() const { return methodName; }
  int getNumArgs() const { return numArgs; }
  const gwt::Value* const getArgs() const { return args; }

  static InvokeMessage* receive(HostChannel& channel);
  static bool send(HostChannel& channel, const gwt::Value& thisRef, int methodDispatchId,
      int numArgs, const gwt::Value* args);
};
#endif
