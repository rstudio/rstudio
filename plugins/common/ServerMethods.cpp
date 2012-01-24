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

#include "Debug.h"
#include "FreeValueMessage.h"
#include "HostChannel.h"
#include "InvokeMessage.h"
#include "InvokeSpecialMessage.h"
#include "ReturnMessage.h"
#include "ServerMethods.h"
#include "scoped_ptr/scoped_ptr.h"

using std::string;

gwt::Value ServerMethods::getProperty(HostChannel& channel, SessionHandler* handler, int objectRef,
    int dispatchId) {
  if (!channel.isConnected()) {
    Debug::log(Debug::Debugging) << "Ignoring getProperty after disconnect"
        << Debug::flush;
    return gwt::Value();
  }
  gwt::Value args[2];
  args[0].setInt(objectRef);
  args[1].setInt(dispatchId);
  if (!InvokeSpecialMessage::send(channel, SPECIAL_GET_PROPERTY, 2, args)) {
    Debug::log(Debug::Error) << "  failed to send invoke of GetProperty(disp=" << dispatchId
        << ", obj=" << objectRef << ")" << Debug::flush;
    return gwt::Value();
  }
  scoped_ptr<ReturnMessage> retMsg(channel.reactToMessagesWhileWaitingForReturn(handler));
  if (!retMsg.get()) {
    Debug::log(Debug::Error) << "getProperty: get return value failed for GetProperty(disp="
        << dispatchId << ", obj=" << objectRef << ")" << Debug::flush;
    return gwt::Value();
  }
  return retMsg->getReturnValue();
}

int ServerMethods::hasMethod(HostChannel& channel, SessionHandler* handler, int classId,
    const std::string& name) {
  if (name != "toString" && name.find("::") == string::npos) {
    // only JSNI-style names and toString are valid
    return -1;
  }
  if (!channel.isConnected()) {
    Debug::log(Debug::Debugging) << "Ignoring hasMethod after disconnect"
        << Debug::flush;
    return -2;
  }
  gwt::Value arg;
  arg.setString(name);
  if (!InvokeSpecialMessage::send(channel, SPECIAL_HAS_METHOD, 1, &arg)) {
    Debug::log(Debug::Error) << "hasMethod: invoke(hasMethod) failed" << Debug::flush;
    return -2;
  }
  scoped_ptr<ReturnMessage> retMsg(channel.reactToMessagesWhileWaitingForReturn(handler));
  if (!retMsg.get()) {
    Debug::log(Debug::Error) << "hasMethod: get return value failed" << Debug::flush;
    return -2;
  }
  gwt::Value retval = retMsg->getReturnValue();
  // TODO(jat): better error handling?
  return retval.isInt() ? retval.getInt() : -2;
}

int ServerMethods::hasProperty(HostChannel& channel, SessionHandler* handler, int classId,
    const std::string& name) {
  if (name != "toString" && name.find("::") == string::npos) {
    // only JSNI-style names and toString are valid
    return -1;
  }
  if (!channel.isConnected()) {
    Debug::log(Debug::Debugging) << "Ignoring hasProperty after disconnect"
        << Debug::flush;
    return -2;
  }
  gwt::Value arg;
  arg.setString(name);
  if (!InvokeSpecialMessage::send(channel, SPECIAL_HAS_PROPERTY, 1, &arg)) {
    Debug::log(Debug::Error) << "hasProperty: invoke(hasProperty) failed" << Debug::flush;
    return -2;
  }
  scoped_ptr<ReturnMessage> retMsg(channel.reactToMessagesWhileWaitingForReturn(handler));
  if (!retMsg.get()) {
    Debug::log(Debug::Error) << "hasProperty: get return value failed" << Debug::flush;
    return -2;
  }
  gwt::Value retval = retMsg->getReturnValue();
  // TODO(jat): better error handling?
  return retval.isInt() ? retval.getInt() : -2;
}

bool ServerMethods::setProperty(HostChannel& channel, SessionHandler* handler, int objectRef,
    int dispatchId, const gwt::Value& value) {
  if (!channel.isConnected()) {
    Debug::log(Debug::Debugging) << "Ignoring setProperty after disconnect"
        << Debug::flush;
    return false;
  }
  // TODO(jat): error handling?
  gwt::Value args[3];
  args[0].setInt(objectRef);
  args[1].setInt(dispatchId);
  args[2] = value;
  if (!InvokeSpecialMessage::send(channel, SPECIAL_SET_PROPERTY, 3, args)) {
    Debug::log(Debug::Error) << "  failed to send invoke of SetProperty(disp=" << dispatchId
        << ", obj=" << objectRef << ")" << Debug::flush;
    return false;
  }
  scoped_ptr<ReturnMessage> retMsg(channel.reactToMessagesWhileWaitingForReturn(handler));
  if (!retMsg.get()) {
    Debug::log(Debug::Error) << "setProperty: get return value failed for SetProperty(disp="
        << dispatchId << ", obj=" << objectRef << ")" << Debug::flush;
    return false;
  }
  // TODO: use the returned exception?
  return !retMsg.get()->isException();
}

bool ServerMethods::freeJava(HostChannel& channel, SessionHandler* handler, int idCount,
      const int* ids) {
  // If we are disconnected, assume the server will free all of these anyway.
  // This deals with the problem of getting finalizers called after the channel is dropped.
  if (!channel.isConnected()) {
    Debug::log(Debug::Debugging) << "Ignoring freeJava after disconnect"
        << Debug::flush;
    return true;
  }
  if (!FreeValueMessage::send(channel, idCount, ids)) {
    Debug::log(Debug::Error) << "  failed to send FreeValues message" << Debug::flush;
    return false;
  }
  return true;
}
