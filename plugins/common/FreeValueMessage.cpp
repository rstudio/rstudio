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

#include "FreeValueMessage.h"
#include "HostChannel.h"
#include "scoped_ptr/scoped_ptr.h"

FreeValueMessage::~FreeValueMessage() {
  delete[] ids;
}

FreeValueMessage* FreeValueMessage::receive(HostChannel& channel) {
  int idCount;
  if (!channel.readInt(idCount)) {
    // TODO(jat): error handling
    return 0;
  }
  // TODO: validate idCount
  scoped_array<int> ids(new int[idCount]);

  for (int i = 0; i < idCount; ++i) {
    if (!channel.readInt(ids[i])) return 0;
  }
  return new FreeValueMessage(idCount, ids.release());
}

bool FreeValueMessage::send(HostChannel& channel, int idCount, const int* ids) {
  if (!channel.sendByte(TYPE)) return false;
  if (!channel.sendInt(idCount)) return false;
  for (int i = 0; i < idCount; ++i) {
    if (!channel.sendInt(ids[i])) return false;
  }
  return true;
}
