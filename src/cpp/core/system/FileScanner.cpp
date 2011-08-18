/*
 * PosixFileScanner.cpp
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

#include <core/system/FileScanner.hpp>

#include <core/Error.hpp>

namespace core {
namespace system {

Error scanFiles(const FileInfo& fromRoot,
                bool recursive,
                tree<FileInfo>* pTree)
{
   return scanFiles(pTree->set_head(fromRoot), recursive, pTree);
}

} // namespace system
} // namespace core

