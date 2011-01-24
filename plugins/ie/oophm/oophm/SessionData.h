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

#pragma once
#include "stdafx.h"
#include "comutil.h"
#include "HostChannel.h"
#include "Mshtml.h"
#include "SessionHandler.h"
#include "scoped_ptr/scoped_ptr.h"

/*
* Encapsules per-OOPHM-session data.
*/
class SessionData {
public:
  SessionData(HostChannel* channel,
    IHTMLWindow2* window,
    SessionHandler* sessionHandler) : channel(channel),
    window(window),
    sessionHandler(sessionHandler) {
  }

  virtual void freeJavaObject(unsigned int objId)=0;

  HostChannel* getHostChannel() const {
    return channel.get();
  }

  SessionHandler* getSessionHandler() const {
    return sessionHandler;
  }

  IHTMLWindow2* getWindow() const {
    return window;
  }

  /*
  * Convert a value from the JavaScript into something that can be sent back
  * to the OOPHM host.
  */
  virtual void makeValue(Value& value, const _variant_t& in)=0;

  /*
  * Convert a value from the OOPHM host into something that can be passed into
  * the JavaScript execution environment.
  */
  virtual void makeValueRef(_variant_t& value, const Value& in)=0;

protected:
  /*
  * The communication channel used for the OOPHM session.
  */
  scoped_ptr<HostChannel> const channel;

  /*
  * A reference to the SessionHandler being used in the OOPHM session.
  */
  SessionHandler* const sessionHandler;

  CComPtr<IHTMLWindow2> const window;
};
typedef SessionData* SessionDataRef;

// TODO move these to a utility header

__inline static std::string BSTRToUTF8(BSTR bstr) {
  // Need an explict length due to the possibility of embedded nulls
  int length = SysStringLen(bstr);
  int numChars = WideCharToMultiByte(CP_UTF8, 0, bstr, length, NULL, 0, NULL, NULL);
  char* buffer = new char[numChars];
  int res = WideCharToMultiByte(CP_UTF8, 0, bstr, length, buffer, numChars, NULL, NULL);
  // TODO assert res == numChars?
  std::string toReturn = std::string(buffer, res);
  delete[] buffer;
  return toReturn;
}

/*
 * Convert a utf8-encoded string into a BSTR.  The length is explicitly
 * specified because the incoming string may have embedded null charachers.
 */
__inline static _bstr_t UTF8ToBSTR(int length, const char* utf8) {
  // We explicitly use MultiByteToWideChar to handle embedded nulls
  int numChars = MultiByteToWideChar(CP_UTF8, 0, utf8, length, NULL, 0);
  OLECHAR* buffer = new OLECHAR[numChars];
  int res = MultiByteToWideChar(CP_UTF8, 0, utf8, length, buffer, numChars);
  // Manually allocate the BSTR to set the length; _bstr_t assumes C-strings
  _bstr_t toReturn = _bstr_t(SysAllocStringLen(buffer, res), false);
  delete[] buffer;
  return toReturn;
}
