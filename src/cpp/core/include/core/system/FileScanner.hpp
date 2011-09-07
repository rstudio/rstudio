/*
 * FileScanner.hpp
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


#ifndef CORE_SYSTEM_FILE_SCANNER_HPP
#define CORE_SYSTEM_FILE_SCANNER_HPP

#include <boost/function.hpp>

#include <core/Error.hpp>
#include <core/FileInfo.hpp>

#include <core/collection/Tree.hpp>


namespace core {

class Error;

// recursively enumerate files from the specified root. these functions
// are symlink aware -- this has two implications:
//
//   (1) The FileInfo::isSymlink member returns accurate symlink status
//   (2) Symlink to directories are not traversed recursively

namespace system {

Error scanFiles(const tree<FileInfo>::iterator_base& fromNode,
                bool recursive,
                const boost::function<bool(const FileInfo&)>& filter,
                const boost::function<void(const FileInfo&)>& onBeforeScanDir,
                tree<FileInfo>* pTree);

Error scanFiles(const FileInfo& fromRoot,
                bool recursive,
                const boost::function<bool(const FileInfo&)>& filter,
                const boost::function<void(const FileInfo&)>& onBeforeScanDir,
                tree<FileInfo>* pTree)
{
   return scanFiles(pTree->set_head(fromRoot),
                    recursive,
                    filter,
                    onBeforeScanDir,
                    pTree);
}


Error scanFiles(const FileInfo& fromRoot,
                bool recursive,
                const boost::function<bool(const FileInfo&)>& filter,
                tree<FileInfo>* pTree)
{
   return scanFiles(fromRoot,
                    recursive,
                    filter,
                    boost::function<void(const FileInfo&)>(),
                    pTree);
}

Error scanFiles(const tree<FileInfo>::iterator_base& fromNode,
                bool recursive,
                const boost::function<bool(const FileInfo&)>& filter,
                tree<FileInfo>* pTree)
{
   return scanFiles(fromNode,
                    recursive,
                    filter,
                    boost::function<void(const FileInfo&)>(),
                    pTree);
}

} // namespace system
} // namespace core

#endif // CORE_SYSTEM_FILE_SCANNER_HPP
