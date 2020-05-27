/*
 * RegistryKey.cpp
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
#include <core/system/RegistryKey.hpp>

#include <core/system/System.hpp>
#include <shared_core/Error.hpp>
#include <core/Log.hpp>

namespace rstudio {
namespace core {
namespace system {

RegistryKey::RegistryKey()
   : hKey_(nullptr)
{
}

RegistryKey::~RegistryKey()
{
   try
   {
      if (hKey_)
         ::RegCloseKey(hKey_);
      hKey_ = nullptr;
   }
   catch (...)
   {
   }
}

Error RegistryKey::open(HKEY hKey, std::string subKey, REGSAM samDesired)
{
   if (hKey_)
      ::RegCloseKey(hKey_);
   hKey_ = nullptr;

   LONG error = ::RegOpenKeyEx(hKey, subKey.c_str(), 0, samDesired, &hKey_);
   if (error != ERROR_SUCCESS)
   {
      hKey_ = nullptr;
      return systemError(error, ERROR_LOCATION);
   }

   return Success();
}

bool RegistryKey::isOpen()
{
   return hKey_;
}

HKEY RegistryKey::handle()
{
   return hKey_;
}

std::string RegistryKey::getStringValue(std::string name,
                                        std::string defaultValue)
{
   std::string value;
   Error error = getStringValue(name, &value);
   if (!error)
      return value;
   return defaultValue;
}

Error RegistryKey::getStringValue(std::string name, std::string *pValue)
{
   if (!hKey_)
      return systemError(ERROR_INVALID_HANDLE, ERROR_LOCATION);

   std::vector<char> buffer(256);
   while (true)
   {
      DWORD type;
      DWORD size = static_cast<DWORD>(buffer.capacity());

      LONG result = ::RegQueryValueEx(hKey_,
                                      name.c_str(),
                                      nullptr,
                                      &type,
                                      (LPBYTE)(&buffer[0]),
                                      &size);
      switch (result)
      {
      case ERROR_SUCCESS:
         {
            if (type != REG_SZ && type != REG_EXPAND_SZ)
               return systemError(ERROR_INVALID_DATATYPE, ERROR_LOCATION);

            *pValue = std::string(&buffer[0], buffer.capacity());

            // REG_SZ and friends may or may not be null-terminated.
            // So trim the string at the first null, if any.
            size_t idxNull = pValue->find('\0');
            if (idxNull != std::string::npos)
               pValue->resize(idxNull);

            if (type == REG_EXPAND_SZ)
            {
               Error error = expandEnvironmentVariables(*pValue, pValue);
               if (error)
                  return error;
            }

            return Success();
         }

      case ERROR_MORE_DATA:
         buffer.reserve(size);
         continue;

      default:
         return systemError(result, ERROR_LOCATION);
      }
   }
}

std::vector<std::string> RegistryKey::keyNames()
{
   if (!hKey_)
      return std::vector<std::string>();

   LONG result;

   DWORD subKeys, maxLen;
   result = ::RegQueryInfoKey(hKey_, nullptr, nullptr, nullptr, &subKeys, &maxLen,
                              nullptr, nullptr, nullptr, nullptr, nullptr, nullptr);


   std::vector<char> nameBuffer(maxLen+2);
   std::vector<std::string> results;
   results.reserve(subKeys);

   for (DWORD i = 0; ; i++)
   {
      DWORD size = static_cast<DWORD>(nameBuffer.capacity());
      LONG result = ::RegEnumKeyEx(hKey_,
                                   i,
                                   &nameBuffer[0],
                                   &size,
                                   nullptr, nullptr, nullptr, nullptr);
      switch (result)
      {
      case ERROR_SUCCESS:
         results.push_back(std::string(&nameBuffer[0], size));
         break;
      case ERROR_NO_MORE_ITEMS:
         return results;
      default:
         Error error = systemError(result, ERROR_LOCATION);
         LOG_ERROR(error);
         break;
      }

   }

   return std::vector<std::string>();
}

} // namespace system
} // namespace core
} // namespace rstudio
