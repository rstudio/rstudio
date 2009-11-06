#ifndef _H_ExternalWrapper
#define _H_ExternalWrapper
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

#include "mozincludes.h"

#include "IOOPHM.h"

#include "Preferences.h"
#include "FFSessionHandler.h"
#include "Debug.h"
#include "scoped_ptr/scoped_ptr.h"

#include "nsCOMPtr.h"
#include "nsISecurityCheckedComponent.h"
#include "nsStringAPI.h"
#include "nsIWindowWatcher.h"
#include "nsIDOMWindow.h"

class nsIDOMWindow;

// {028DD88B-6D65-401D-AAFD-17E497D15D09}
#define OOPHM_CID \
  { 0x028DD88B, 0x6D65, 0x401D, \
  { 0xAA, 0xFD, 0x17, 0xE4, 0x97, 0xD1, 0x5D, 0x09 } }

#define OOPHM_CONTRACTID "@gwt.google.com/oophm/ExternalWrapper;1"
#define OOPHM_CLASSNAME "GWT DevMode component"

class ExternalWrapper : public IOOPHM,
                        public nsISecurityCheckedComponent {
  NS_DECL_ISUPPORTS
  NS_DECL_IOOPHM
  NS_DECL_NSISECURITYCHECKEDCOMPONENT

  ExternalWrapper();
  virtual ~ExternalWrapper();

private:
  nsCOMPtr<nsIDOMWindow> domWindow;
  nsCOMPtr<nsIDOMWindowInternal> topWindow;
  nsString url;
  nsCOMPtr<Preferences> preferences;
  scoped_ptr<FFSessionHandler> sessionHandler;
  nsCOMPtr<nsIWindowWatcher> windowWatcher;

  /**
   * Prompt the user whether a connection should be allowed, and optionally
   * update the preferences.
   */
  bool askUserToAllow(const std::string& url);

  /**
   * Compute a stable tab identity value for the DOM window.
   *
   * @return a unique tab identifier which is stable across reloads, or an
   *     empty string if it cannot be computed
   */
  std::string computeTabIdentity();

};

inline Debug::DebugStream& operator<<(Debug::DebugStream& dbg,
    const nsACString& str) {
  nsCString copy(str);
  dbg << copy.get();
  return dbg;
}

inline Debug::DebugStream& operator<<(Debug::DebugStream& dbg,
    const nsAString& str) {
  NS_ConvertUTF16toUTF8 copy(str);
  dbg << copy.get();
  return dbg;
}

#endif
