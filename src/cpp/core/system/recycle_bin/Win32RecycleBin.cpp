/*
 * Win32RecycleBin.cpp
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

#include <core/Error.hpp>
#include <core/FilePath.hpp>

namespace core {
namespace system {
namespace recycle_bin {
      
Error sendTo(const FilePath& filePath)
{
   return filePath.remove();
}

} // namespace recycle_bin
} // namespace system
} // namespace core

