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

#include <dirent.h>
#include <sys/stat.h>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/BoostThread.hpp>

namespace core {
namespace system {

namespace {
#ifdef __APPLE__
int entryFilter(struct dirent *entry)
#else
int entryFilter(const struct dirent *entry)
#endif
{
   if (::strcmp(entry->d_name, ".") == 0 || ::strcmp(entry->d_name, "..") == 0)
      return 0;
   else
      return 1;
}

} // anonymous namespace

Error scanFiles(const tree<FileInfo>::iterator_base& fromNode,
                const FileScannerOptions& options,
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

   // read directory contents
   struct dirent **namelist;
   int entries = ::scandir(fromNode->absolutePath().c_str(),
                           &namelist,
                           entryFilter,
                           ::alphasort);
   if (entries == -1)
   {
      Error error = systemError(boost::system::errc::no_such_file_or_directory,
                                ERROR_LOCATION);
      error.addProperty("path", fromNode->absolutePath());
      return error;
   }

   // iterate over entries
   for(int i=0; i<entries; i++)
   {
      // get the entry (then free it) and compute the path
      dirent entry = *namelist[i];
      ::free(namelist[i]);
      std::string name(entry.d_name,
#ifdef __APPLE__
                       entry.d_namlen);
#else
                       entry.d_reclen);
#endif
      std::string path = rootPath.childPath(name).absolutePath();

      // get the attributes
      struct stat st;
      int res = ::lstat(path.c_str(), &st);
      if (res == -1)
      {
         LOG_ERROR(systemError(errno, ERROR_LOCATION));
         continue;
      }

      // create the FileInfo
      FileInfo fileInfo;
      bool isSymlink = S_ISLNK(st.st_mode);
      if (S_ISDIR(st.st_mode))
      {
         fileInfo = FileInfo(path, true, isSymlink);
      }
      else
      {
         fileInfo = FileInfo(path,
                             false,
                             st.st_size,
#ifdef __APPLE__
                             st.st_mtimespec.tv_sec,
#else
                             st.st_mtime,
#endif
                             isSymlink);
      }

      // apply the filter (if any)
      if (!options.filter || options.filter(fileInfo))
      {
         // add the correct type of FileEntry
         if (fileInfo.isDirectory())
         {
            tree<FileInfo>::iterator_base child = pTree->append_child(fromNode,
                                                                      fileInfo);
            // recurse if requested and this isn't a link
            if (options.recursive && !fileInfo.isSymlink())
            {
               Error error = scanFiles(child, options, pTree);
               if (error)
                  return error;
            }
         }
         else
         {
            pTree->append_child(fromNode, fileInfo);
         }
      }
   }

   // free the namelist
   ::free(namelist);

   // return success
   return Success();
}

} // namespace system
} // namespace core

