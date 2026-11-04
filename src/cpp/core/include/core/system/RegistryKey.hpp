/*
 * RegistryKey.hpp
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

#ifndef REGISTRYKEY_HPP
#define REGISTRYKEY_HPP

#ifndef _WIN32
#error RegistryKey.hpp is Windows-specific
#endif

#include <string>

#include <Windows.h>

#include <boost/noncopyable.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace system {

class RegistryKey : boost::noncopyable
{
public:
    RegistryKey();
    virtual ~RegistryKey();

    core::Error open(HKEY hKey, std::string subKey, REGSAM samDesired);

    // open subKey under hKey, creating it first when it does not exist
    core::Error create(HKEY hKey, std::string subKey, REGSAM samDesired);

    bool isOpen();

    HKEY handle();

    core::Error getStringValue(std::string name, std::string* pValue);
    std::string getStringValue(std::string name, std::string defaultValue);

    // The string value 'name' as stored (REG_EXPAND_SZ data is not expanded,
    // unlike getStringValue()), with its kind in *pType. A value of any other
    // kind fails with ERROR_INVALID_DATATYPE. Names and data are UTF-8.
    core::Error getValue(const std::string& name, std::string* pData, DWORD* pType);

    // set a REG_SZ or REG_EXPAND_SZ value
    core::Error setValue(const std::string& name, const std::string& data, DWORD type);

    core::Error deleteValue(const std::string& name);

    std::vector<std::string> keyNames();

private:
    HKEY hKey_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // REGISTRYKEY_HPP
