#ifndef __H_FreeValueMessage
#define __H_FreeValueMessage
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

/**
 * Class representing an FreeValue message sent or received from the server.
 *
 * This message gives a list of object IDs which are no longer needed by the
 * sending side.  Note that there is no reply to this message, and it can
 * only be sent when the other side is expecting to receive a return value or
 * another invoke.  Special care must be taken to ensure that any freed values
 * referenced by the succeeding invoke/return are resurrected instead of freed.
 */
class FreeValueMessage : public Message {
public:
  static const char TYPE = MESSAGE_TYPE_FREEVALUE;
private:
  int idCount;
  const int* ids;

  FreeValueMessage(int idCount, const int* ids) : idCount(idCount), ids(ids) {}

public:
  ~FreeValueMessage();

  virtual char getType() const {
    return TYPE;
  }

  int getIdCount() const {
    return idCount;
  }

  const int* const getIds() const {
    return ids;
  }

  static FreeValueMessage* receive(HostChannel& channel);
  static bool send(HostChannel& channel, int idCount, const int* ids);
};
#endif
