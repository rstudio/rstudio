/*
 * Win32FileScanner.cpp
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

#include <core/system/FileScanner.hpp>

#include <boost/system/windows_error.hpp>

#include <core/BoostThread.hpp>
#include <core/FileInfo.hpp>
#include <core/Log.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

FileInfo convertToFileInfo(const FilePath& filePath, bool yield, int *pCount)
{
   // yield every 10 files (defend against pegging the cpu for directories
   // with a huge number of files)
   if (yield)
   {
      *pCount = *pCount + 1;
      if (*pCount % 10 == 0)
         boost::this_thread::yield();

   }

   if (filePath.isDirectory())
   {
      return FileInfo(filePath.getAbsolutePath(), true, filePath.isSymlink());
   }
   else if (filePath.exists())
   {
      return FileInfo(filePath.getAbsolutePath(),
                      false,
                      filePath.getSize(),
                      filePath.getLastWriteTime(),
                      filePath.isSymlink());
   }
   else
   {
      return FileInfo(filePath.getAbsolutePath(), false);
   }
}

} // anonymous namespace


// NOTE: we bail with an error if the top level directory can't be
// enumerated however we merely log errors for children. this reflects
// the notion that a top-level failure will report major problems
// (e.g. permission to access a volume/drive) whereas errors which
// occur in children are more likely to refect some idiosyncratic
// problem with a child dir or file, and we don't want that to
// interfere with the caller getting a listing of everything else
// and proceeding with its work
Error scanFiles(const tree<FileInfo>::iterator_base& fromNode,
                const core::system::FileScannerOptions& options,
                tree<FileInfo>* pTree)
{
   // clear all existing
   pTree->erase_children(fromNode);

   // create FilePath for root
   FilePath rootPath(fromNode->absolutePath());

   // yield if requested (only applies to recursive scans)
   if (options.recursive && options.yield)
      boost::this_thread::yield();

   // call onBeforeScanDir hook
   if (options.onBeforeScanDir)
   {
      Error error = options.onBeforeScanDir(*fromNode);
      if (error)
         return error;
   }

   // read directory entries
   std::vector<FilePath> children;
   Error error = rootPath.getChildren(children);
   if (error)
      return error;

   // convert to FileInfo and sort using alphasort equivilant (for
   // compatability with scandir, which is what is used in our
   // posix-specific implementation
   int count = 0;
   std::vector<FileInfo> childrenFileInfo;
   std::transform(children.begin(),
                  children.end(),
                  std::back_inserter(childrenFileInfo),
                  boost::bind(convertToFileInfo, _1, options.yield, &count));
   std::sort(childrenFileInfo.begin(),
             childrenFileInfo.end(),
             fileInfoPathLessThan);

   // iterate over entries
   for (const FileInfo& childFileInfo : childrenFileInfo)
   {
      // check for interrupts
      if (boost::this_thread::interruption_requested())
         return core::systemError(boost::system::errc::interrupted, ERROR_LOCATION);

      // apply filter if we have one
      if (options.filter && !options.filter(childFileInfo))
         continue;

      // add the correct type of FileEntry
      if (childFileInfo.isDirectory())
      {
         tree<FileInfo>::iterator_base child =
                              pTree->append_child(fromNode, childFileInfo);
         if (options.recursive && !childFileInfo.isSymlink())
         {
            Error error = scanFiles(child, options, pTree);
            if (error && (error != boost::system::windows_error::path_not_found))
               LOG_ERROR(error);
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
} // namespace rstudio

