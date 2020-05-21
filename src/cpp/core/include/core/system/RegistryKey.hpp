/*
 * RegistryKey.hpp
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
    bool isOpen();

    HKEY handle();

    core::Error getStringValue(std::string name, std::string* pValue);
    std::string getStringValue(std::string name, std::string defaultValue);

    std::vector<std::string> keyNames();

private:
    HKEY hKey_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // REGISTRYKEY_HPP
