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

#include <windows.h>
#include <winnt.h>
#include <winreg.h>
#include "Debug.h"
#include "Preferences.h"
#include "AllowedConnections.h"

#define REG_ACCESS_LIST "SOFTWARE\\Google\\Google Web Toolkit\\gwt-dev-plugin.accessList"

/**
 * Return a null-terminated string containing the access list.
 *
 * @param HKEY registry key for the access list value
 * @return null-terminated string containing the access list (an empty string
 *     if the value does not exist) -- caller is responsible for freeing with
 *     delete[]
 */
static char* getAccessList(HKEY keyHandle) {
  char *buf;
  DWORD len = 512;
  while(true) {
    buf = new char[len];
    int cc = RegQueryValueExA(keyHandle, NULL, 0, NULL, (LPBYTE) buf, &len);
    if (cc == ERROR_SUCCESS) {
      break;
    } else if (cc == ERROR_FILE_NOT_FOUND) {
      // special handling if the value doesn't exist
      len = 0;
      break;
    } else if (cc != ERROR_MORE_DATA) {
      // log unexpected errors
      Debug::log(Debug::Error) << "Unable to load access list from registry: "
          << cc << Debug::flush;
      len = 0;
      break;
    }
    // Buffer wasn't big enough, so make it bigger and try again
    delete [] buf;
    len *= 2;
  }
  buf[len] = 0;
  return buf;
}

void Preferences::addNewRule(const std::string& pattern, bool exclude) {
  HKEY keyHandle;
  if (RegCreateKeyExA(HKEY_CURRENT_USER, REG_ACCESS_LIST, 0, 0,
      REG_OPTION_NON_VOLATILE, KEY_ALL_ACCESS, NULL, &keyHandle, NULL)
      != ERROR_SUCCESS) {
    return;
  }
  char *buf = getAccessList(keyHandle);
  std::string pref(buf);
  delete [] buf;
  if (pref.length() > 0) {
    pref += ',';
  }
  if (exclude) {
    pref += '!';
  }
  pref += pattern;
  int cc = RegSetValueExA(keyHandle, NULL, 0, REG_SZ, (LPBYTE) pref.c_str(),
      pref.length() + 1);
  if (cc != ERROR_SUCCESS) {
      Debug::log(Debug::Error) << "Unable to store access list in registry: "
          << cc << Debug::flush;
  }
  RegCloseKey(keyHandle);
}

void Preferences::loadAccessList() {
  // TODO(jat): can Reg* routines throw exceptions?  If so, we need to make
  // this exception safe about closing the key hendle and freeing the buffer.
  HKEY keyHandle;
  if (RegCreateKeyExA(HKEY_CURRENT_USER, REG_ACCESS_LIST, 0, 0,
      REG_OPTION_NON_VOLATILE, KEY_ALL_ACCESS, NULL, &keyHandle, NULL)
      != ERROR_SUCCESS) {
    return;
  }
  char *buf = getAccessList(keyHandle);
  AllowedConnections::initFromAccessList(buf);
  delete [] buf;
  RegCloseKey(keyHandle);
}
