/*
 * PosixFileScanner.cpp
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

#include <dirent.h>
#include <sys/stat.h>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <shared_core/FilePath.hpp>
#include <core/BoostThread.hpp>

#include "config.h"

namespace rstudio {
namespace core {
namespace system {

namespace {

#if defined(__APPLE__) && !defined(HAVE_SCANDIR_POSIX)
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

// note: because R may change LC_COLLATE, we cannot
// use strcoll (otherwise we run into race issues where
// the file monitor attempts to access LC_COLLATE just as
// R is replacing it). to avoid this, we use strcmp and
// don't sort according to locale.
#if !defined(HAVE_SCANDIR_POSIX)
int alphasort(const void* voidlhs, const void* voidrhs)
{
   const dirent** lhs = (const dirent**)voidlhs;
   const dirent** rhs = (const dirent**)voidrhs;
#else
int alphasort(const dirent** lhs, const dirent** rhs)
{
#endif
   return strcmp((*lhs)->d_name, (*rhs)->d_name);
}

// wrapper for scandir api
Error scanDir(const std::string& dirPath, std::vector<std::string>* pNames)
{
   // read directory contents into namelist
   struct dirent **namelist;
   int entries = ::scandir(dirPath.c_str(),
                           &namelist,
                           entryFilter,
                           alphasort);
   if (entries == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", dirPath);
      return error;
   }

   // extract the namelist then free it
   for(int i=0; i<entries; i++)
   {
      // get the name (then free it)
      std::string name(namelist[i]->d_name);
      ::free(namelist[i]);

      // add to the vector
      pNames->push_back(name);
   }
   ::free(namelist);

   return Success();
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
   std::vector<std::string> names;
   Error error = scanDir(fromNode->absolutePath(), &names);
   if (error)
      return error;

   // iterate over the names
   for (const std::string& name : names)
   {
      // check for interrupt (mark as expected to suppress logging)
      if (boost::this_thread::interruption_requested())
      {
         Error error = core::systemError(boost::system::errc::interrupted, ERROR_LOCATION);
         error.setExpected();
         return error;
      }

      // compute the path
      std::string path = rootPath.completeChildPath(name).getAbsolutePath();

      // get the attributes
      struct stat st;
      int res = ::lstat(path.c_str(), &st);
      if (res == -1)
      {
         if (errno != ENOENT && errno != EACCES)
         {
            Error error = systemError(errno, ERROR_LOCATION);
            error.addProperty("path", path);
            LOG_ERROR(error);
         }
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
               // try to scan the files in the subdirectory -- if we fail
               // we continue because we don't want one "bad" directory
               // to cause us to abort the entire scan. yes the tree
               // will be incomplete however it will be even more incompete
               // if we fail entirely
               Error error = scanFiles(child, options, pTree);
               if (error)
                  LOG_ERROR(error);
            }
         }
         else
         {
            pTree->append_child(fromNode, fileInfo);
         }
      }
   }

   // return success
   return Success();
}

} // namespace system
} // namespace core
} // namespace rstudio

