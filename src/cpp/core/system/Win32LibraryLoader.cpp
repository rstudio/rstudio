/*
 * Win32LibraryLoader.cpp
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

#include <core/system/LibraryLoader.hpp>

#include <core/Error.hpp>

namespace core {
namespace system {

Error loadLibrary(const std::string& libPath, void** ppLib)
{
   return Success();
}

Error loadSymbol(void* pLib, const std::string& name, void** ppSymbol)
{


   return Success();
}

Error closeLibrary(void* pLib)
{


   return Success();
}

} // namespace system
} // namespace core
