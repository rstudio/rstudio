/*
 * Win32Environment.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <core/system/Environment.hpp>

#include <windows.h>

#include <stdlib.h>

#include <system_error>
#include <vector>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/StringUtils.hpp>

#include <shared_core/system/EnvironmentLock.hpp>
#include <shared_core/system/User.hpp> // For detail::getenv

namespace rstudio {
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
   EnvironmentLock lock;

   // get all environment strings (as unicode)
   LPWSTR lpEnv = ::GetEnvironmentStringsW();
   if (lpEnv == nullptr)
   {
      LOG_ERROR(LAST_SYSTEM_ERROR());
      return;
   }

   // iterate over them
   LPWSTR lpszEnvVar = nullptr;
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
   {
      LOG_ERROR(LAST_SYSTEM_ERROR());
   }
}

// Value returned is UTF-8 encoded
std::string getenv(const std::string& name)
{
   // no EnvironmentLock here: detail::getenv takes it, and the lock is
   // not recursive
   return detail::getenv(name);
}

void setenv(const std::string& name, const std::string& value)
{
   EnvironmentLock lock;

   std::wstring nameWide = string_utils::utf8ToWide(name);
   std::wstring valueWide = string_utils::utf8ToWide(value);

   // write to the Win32 process environment block: the source of truth for
   // getenv() above, and what child processes inherit
   if (!::SetEnvironmentVariableW(nameWide.c_str(), valueWide.c_str()))
   {
      Error error = LAST_SYSTEM_ERROR();
      error.addProperty("name", name);
      LOG_ERROR(error);
   }

   // also write through the C runtime: the CRT keeps its own copy of the
   // environment, snapshotted lazily from the process block and never
   // refreshed by SetEnvironmentVariable, so without this raw ::getenv
   // calls (including those made by in-process libraries and by R itself,
   // which reads the environment via its CRT) would not see the update.
   // a failure in one store but not the other leaves the two out of sync,
   // so log it
   errno_t status = ::_wputenv_s(nameWide.c_str(), valueWide.c_str());
   if (status != 0)
   {
      Error error = systemError(std::error_code(status, std::generic_category()), ERROR_LOCATION);
      error.addProperty("name", name);
      LOG_ERROR(error);
   }
}

void unsetenv(const std::string& name)
{
   EnvironmentLock lock;

   std::wstring nameWide = string_utils::utf8ToWide(name);

   // remove from the Win32 process environment block; removing a variable
   // that is not set fails with ERROR_ENVVAR_NOT_FOUND, which is fine
   if (!::SetEnvironmentVariableW(nameWide.c_str(), nullptr))
   {
      DWORD errorCode = ::GetLastError();
      if (errorCode != ERROR_ENVVAR_NOT_FOUND)
      {
         Error error = systemError(errorCode, ERROR_LOCATION);
         error.addProperty("name", name);
         LOG_ERROR(error);
      }
   }

   // remove from the CRT environment as well (an empty value deletes)
   errno_t status = ::_wputenv_s(nameWide.c_str(), L"");
   if (status != 0)
   {
      Error error = systemError(std::error_code(status, std::generic_category()), ERROR_LOCATION);
      error.addProperty("name", name);
      LOG_ERROR(error);
   }
}


} // namespace system
} // namespace core
} // namespace rstudio

