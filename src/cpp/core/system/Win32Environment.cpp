/*
 * Win32Environment.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// http://msdn.microsoft.com/en-us/library/windows/desktop/ms683187(v=vs.85).aspx
// http://msdn.microsoft.com/en-us/library/windows/desktop/ms682009(v=vs.85).aspx

#include <core/system/Environment.hpp>

#include <windows.h>

#include <vector>

#include <core/StringUtils.hpp>

namespace core {
namespace system {

// Value returned is UTF-8 encoded
std::string getenv(const std::string& name)
{
   std::wstring nameWide(name.begin(), name.end());

   // get the variable
   DWORD nSize = 256;
   std::vector<wchar_t> buffer(nSize);
   DWORD result = ::GetEnvironmentVariableW(nameWide.c_str(), &(buffer[0]), nSize);
   if (result == 0) // not found
   {
      return std::string();
   }
   if (result > nSize) // not enough space in buffer
   {
      nSize = result;
      buffer.resize(nSize);
      result = ::GetEnvironmentVariableW(nameWide.c_str(), &(buffer[0]), nSize);
      if (result == 0 || result > nSize)
         return std::string(); // VERY unexpected failure case
   }

   // return it
   return string_utils::wideToUtf8(&(buffer[0]));
}

void setenv(const std::string& name, const std::string& value)
{
   ::SetEnvironmentVariableW(string_utils::utf8ToWide(name).c_str(),
                             string_utils::utf8ToWide(value).c_str());
}

void unsetenv(const std::string& name)
{
   ::SetEnvironmentVariable(name.c_str(), NULL);
}


} // namespace system
} // namespace core

