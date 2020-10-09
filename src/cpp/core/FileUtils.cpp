/*
 * FileUtils.cpp
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

#include <fstream>
#include <iostream>

#include <boost/bind.hpp>

#include <core/FileUtils.hpp>
#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>
#include <core/StringUtils.hpp>

#include <core/system/System.hpp>

#ifndef _WIN32
#include <core/system/PosixUser.hpp>
#endif

namespace rstudio {
namespace core {
namespace file_utils {

namespace {

bool copySourceFile(const FilePath& sourceDir,
                    const FilePath& destDir,
                    const FilePath& sourceFilePath)
{
   // compute the target path
   std::string relativePath = sourceFilePath.getRelativePath(sourceDir);
   FilePath targetPath = destDir.completePath(relativePath);

   // if the copy item is a directory just create it
   if (sourceFilePath.isDirectory())
   {
      Error error = targetPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }
   // otherwise copy it
   else
   {
      Error error = sourceFilePath.copy(targetPath);
      if (error)
         LOG_ERROR(error);
   }

   return true;
}

} // anonymous namespace

FilePath uniqueFilePath(const FilePath& parent, const std::string& prefix, const std::string& extension)
{
   // try up to 100 times then fallback to a uuid
   for (int i=0; i<100; i++)
   {
      // get a shortened uuid
      std::string shortentedUuid = core::system::generateShortenedUuid();

      // form full path
      FilePath uniqueDir = parent.completeChildPath(prefix + shortentedUuid + extension);

      // return if it doesn't exist
      if (!uniqueDir.exists())
         return uniqueDir;
   }

   // if we didn't succeed then return prefix + uuid
   return parent.completeChildPath(prefix + core::system::generateUuid(false) + extension);
}

std::string readFile(const FilePath& filePath)
{
   std::ifstream stream(
      filePath.getAbsolutePath().c_str(),
            std::ios::in | std::ios::binary);
   
   std::string content;
   if (stream)
   {
      stream.seekg(0, std::ios::end);
      std::streamsize size = stream.tellg();
      content.resize(static_cast<size_t>(size));
      stream.seekg(0, std::ios::beg);
      stream.read(&content[0], size);
      stream.close();
   }
   
   return content;
}

#ifdef _WIN32
// test a filename to see if it corresponds to a reserved device name on
// Windows
bool isWindowsReservedName(const std::string& name)
{
   const char* reserved[] = { "con", "prn", "aux", "nul", "com1", "com2",
                              "com3", "com4", "com5", "com6", "com7",
                              "com8", "com9", "lpt1", "lpt2", "lpt3",
                              "lpt4", "lpt5", "lpt6", "lpt7", "lpt8",
                              "lpt9" };
   std::string lowerName = string_utils::toLower(name);
   for (int i = 0; i < sizeof(reserved)/sizeof(char*); i++)
   {
       if (lowerName == reserved[i])
       {
           return true;
       }
   }
   return false;
}
#endif

Error copyDirectory(const FilePath& sourceDirectory,
                    const FilePath& targetDirectory)
{
   // create the target directory
   Error error = targetDirectory.ensureDirectory();
   if (error)
      return error;

   // iterate over the source
   return sourceDirectory.getChildrenRecursive(
     boost::bind(copySourceFile, sourceDirectory, targetDirectory, _2));
}

bool isDirectoryWriteable(const FilePath& directory)
{
   std::string prefix(
#ifndef _WIN32
   "."
#endif
   "write-test-");

   FilePath testFile = directory.completePath(prefix + core::system::generateUuid());
   Error error = core::writeStringToFile(testFile, "test");
   if (error)
   {
      return false;
   }
   else
   {
      error = testFile.removeIfExists();
      if (error)
         LOG_ERROR(error);

      return true;
   }
}

} // namespace file_utils
} // namespace core
} // namespace rstudio
