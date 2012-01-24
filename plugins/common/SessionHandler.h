#ifndef _H_SessionHandler
#define _H_SessionHandler
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

#include "BrowserChannel.h"
#include "Value.h"

class HostChannel;

/**
 * Interface for session-handling needs.
 *
 * Note that if this is being "added" onto a class which extends a C structure, the inheritance
 * chain leading to the plain C object must be first in the inheritance list.
 *
 * For example:
 *
 * extern "C" {
 *   typedef struct PlainCstruct {
 *      ... data only members here
 *   } PlainCstruct;
 * };
 *
 * class MyWrapper: public PlainCstruct, SessionHandler {
 *    ... virtual functions ok here
 * };
 */
class SessionHandler {
  friend class HostChannel;
public:
  enum SpecialMethodId {
    HasMethod = SPECIAL_HAS_METHOD,
    HasProperty = SPECIAL_HAS_PROPERTY,
    GetProperty = SPECIAL_GET_PROPERTY,
    SetProperty = SPECIAL_SET_PROPERTY
  };
protected:
  SessionHandler(): alreadyDisconnected(false) {
  }

  /**
   * Called by the server socket when it cannot read, write, or flush.
   */
  void disconnectDetected() {
    if (!alreadyDisconnected) {
      alreadyDisconnected = true;
      disconnectDetectedImpl();
    }
  }

  /**
   * Implementors should invoke __gwt_disconnected() in the hosted.html window
   * to "glass" the screen with a disconnect message.
   */
  virtual void disconnectDetectedImpl() = 0;

  /**
   * Report a fatal error -- the channel will be closed after this method
   * returns.
   */
  virtual void fatalError(HostChannel& channel, const std::string& message) = 0;

  virtual void freeValue(HostChannel& channel, int idCount, const int* ids) = 0;

  virtual void loadJsni(HostChannel& channel, const std::string& js) = 0;

  /**
   * Does not own any of its args -- must copy them if it wants them and should not free the
   * ones passed in.
   *
   * Returns true if an exception occurred.
   */
  virtual bool invoke(HostChannel& channel, const gwt::Value& thisObj,
      const std::string& methodName, int numArgs, const gwt::Value* const args,
      gwt::Value* returnValue) = 0;

  /**
   * Invoke a plugin-provided method with the given args.  As above, this method does not own
   * any of its args.
   *
   * Returns true if an exception occurred.
   */
  virtual bool invokeSpecial(HostChannel& channel, SpecialMethodId method, int numArgs,
      const gwt::Value* const args, gwt::Value* returnValue) = 0;

  /**
   * Send any queued up free values back to the server.
   */
  virtual void sendFreeValues(HostChannel& channel) = 0;

  virtual ~SessionHandler() {}

private:
  bool alreadyDisconnected;
};

#endif
