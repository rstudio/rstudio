#ifndef __H_LoadJsniMessage
#define __H_LoadJsniMessage
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

class LoadJsniMessage : public Message {
public:  
  static const char TYPE = MESSAGE_TYPE_LOADJSNI;
private:
  const std::string js;
  
  LoadJsniMessage(const std::string& js) : js(js) {}
  
public:  
  virtual char getType() const {
    return TYPE;
  }
  
  std::string getJs() const {
    return js;
  }

  static LoadJsniMessage* receive(HostChannel& channel);
};
#endif
