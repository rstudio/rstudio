#ifndef _H_AllowDialog
#define _H_AllowDialog
/*
 * Copyright 2009 Google Inc.
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

#ifdef _WINDOWS
#include <windows.h>
#include <winnt.h>
#include <windef.h>

class AllowDialog {
public:
  static void setHInstance(HINSTANCE hInstance);

  /**
   * Ask the user if a connection should be allowed.
   *
   * @param remember *remember is set to true if the user asked us to remember this decision,
   *     false otherwise
   * @return return true if this connection should be allowed
   */
  static bool askUserToAllow(bool* remember);

private:
  static HINSTANCE hInstance;
};
#endif

#endif
