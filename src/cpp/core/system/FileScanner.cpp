/*
 * FileScanner.cpp
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

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/FileInfo.hpp>
#include <core/FilePath.hpp>

namespace core {
namespace system {

namespace {

inline FileInfo toFileInfo(const FilePath& filePath)
{
   return FileInfo(filePath);
}

} // anonymous namespace

Error scanFiles(const FileInfo& fromRoot,
                bool recursive,
                const boost::function<bool(const FileInfo&)>& filter,
                tree<FileInfo>* pTree)
{
   return scanFiles(pTree->set_head(fromRoot), recursive, filter, pTree);
}

// NOTE: we bail with an error if the top level directory can't be
// enumerated however we merely log errors for children. this reflects
// the notion that a top-level failure will report major problems
// (e.g. permission to access a volume/drive) whereas errors which
// occur in children are more likely to refect some idiosyncratic
// problem with a child dir or file, and we don't want that to
// interfere with the caller getting a listing of everything else
// and proceeding with its work
Error scanFiles(const tree<FileInfo>::iterator_base& fromNode,
                bool recursive,
                const boost::function<bool(const FileInfo&)>& filter,
                tree<FileInfo>* pTree)
{
   // clear all existing
   pTree->erase_children(fromNode);

   // create FilePath for root
   FilePath rootPath(fromNode->absolutePath());

   // read directory entries
   std::vector<FilePath> children;
   Error error = rootPath.children(&children);
   if (error)
      return error;

   // convert to FileInfo and sort using alphasort equivilant (for
   // compatability with scandir, which is what is used in our
   // posix-specific implementation
   std::vector<FileInfo> childrenFileInfo;
   std::transform(children.begin(),
                  children.end(),
                  std::back_inserter(childrenFileInfo),
                  toFileInfo);
   std::sort(childrenFileInfo.begin(),
             childrenFileInfo.end(),
             fileInfoPathLessThan);

   // iterate over entries
   BOOST_FOREACH(const FileInfo& childFileInfo, childrenFileInfo)
   {
      // apply filter if we have one
      if (filter && !filter(childFileInfo))
         continue;

      // add the correct type of FileEntry
      if (childFileInfo.isDirectory())
      {
         tree<FileInfo>::iterator_base child =
                              pTree->append_child(fromNode, childFileInfo);
         if (recursive)
         {
            Error error = scanFiles(child, true, filter, pTree);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }
         }
      }
      else
      {
         pTree->append_child(fromNode, childFileInfo);
      }
   }

   // return success
   return Success();
}


} // namespace system
} // namespace core

