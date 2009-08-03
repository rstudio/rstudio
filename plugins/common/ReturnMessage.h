#ifndef __RETURNMESSAGE_H
#define __RETURNMESSAGE_H
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

class ReturnMessage : public Message {
  static const char TYPE = MESSAGE_TYPE_RETURN;
private:
  bool bisException;
  Value retval;
  
public:  
  ReturnMessage(bool isException, const Value& retValue) : bisException(isException),
      retval(retValue) {}

  bool isException() const { return bisException; }
  const Value& getReturnValue() const { return retval; }
  virtual char getType() const;
  
  static ReturnMessage* receive(HostChannel& channel);
  static bool send(HostChannel& channel, bool isException, const Value& retValue);
};
#endif
