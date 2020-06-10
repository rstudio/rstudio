/*
 * Win32Environment.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
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

#include <core/Log.hpp>
#include <core/StringUtils.hpp>

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
   return detail::getenv(name);
}

void setenv(const std::string& name, const std::string& value)
{
   ::SetEnvironmentVariableW(string_utils::utf8ToWide(name).c_str(),
                             string_utils::utf8ToWide(value).c_str());
}

void unsetenv(const std::string& name)
{
   ::SetEnvironmentVariable(name.c_str(), nullptr);
}


} // namespace system
} // namespace core
} // namespace rstudio

