/*
 * Win32Environment.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <core/system/Environment.hpp>

#include <windows.h>

#include <vector>

#include <boost/algorithm/string/predicate.hpp>

#include <core/StringUtils.hpp>

namespace core {
namespace system {

namespace impl {

bool optionIsNamed(const Option& option, const std::string& name)
{
   return boost::algorithm::iequals(option.first, name);
}

} // namespace impl


void environment(Options* pEnvironment)
{
   // get all environment strings (as unicode)
   LPWSTR lpEnv = ::GetEnvironmentStringsW();
   if (lpEnv == NULL)
   {
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
      return;
   }

   // iterate over them
   LPWSTR lpszEnvVar = NULL;
   for (lpszEnvVar = lpEnv; *lpszEnvVar; lpszEnvVar++)
   {
      // get the variable
      std::wstring envVarWide;
      while (*lpszEnvVar)
      {
         wchar_t ch = *lpszEnvVar;
         envVarWide.append(1, ch);
         lpszEnvVar++;
      }

      // convert to utf8 and parse
      Option envVar;
      if (parseEnvVar(string_utils::wideToUtf8(envVarWide), &envVar))
         pEnvironment->push_back(envVar);
   }


   // free environment strings
   if (!::FreeEnvironmentStringsW(lpEnv))
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
}

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

