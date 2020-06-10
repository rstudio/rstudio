/*
 * FileScanner.hpp
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


#ifndef CORE_SYSTEM_FILE_SCANNER_HPP
#define CORE_SYSTEM_FILE_SCANNER_HPP

#include <boost/function.hpp>

#include <shared_core/Error.hpp>
#include <core/FileInfo.hpp>

#include <core/collection/Tree.hpp>


namespace rstudio {
namespace core {

// recursively enumerate files from the specified root. these functions
// are symlink aware -- this has two implications:
//
//   (1) The FileInfo::isSymlink member returns accurate symlink status
//   (2) Symlink to directories are not traversed recursively

namespace system {  

struct FileScannerOptions
{
   FileScannerOptions()
      : recursive(false), yield(false)
   {
   }

   bool recursive;
   bool yield;
   boost::function<bool(const FileInfo&)> filter;
   boost::function<Error(const FileInfo&)> onBeforeScanDir;
};

Error scanFiles(const tree<FileInfo>::iterator_base& fromNode,
                const FileScannerOptions& options,
                tree<FileInfo>* pTree);

inline Error scanFiles(const FileInfo& fromRoot,
                       const FileScannerOptions& options,
                       tree<FileInfo>* pTree)
{
   return scanFiles(pTree->set_head(fromRoot), options, pTree);
}


} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_FILE_SCANNER_HPP
