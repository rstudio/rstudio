/*
 * ServerSecureKeyFile.cpp
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <core/system/PosixSystem.hpp>
#include <core/system/FileMode.hpp>

#include <server/ServerSecureKeyFile.hpp>

namespace rstudio {
namespace server {
namespace key_file {

core::Error readSecureKeyFile(const std::string& file, std::string* pContents)
{
   // determine path to use for secure cookie key file
   core::FilePath secureKeyPath;
   if (core::system::effectiveUserIsRoot())
   {
      secureKeyPath = core::FilePath("/etc/rstudio").complete(file);
      if (!secureKeyPath.exists())
         secureKeyPath = core::FilePath("/var/lib/rstudio-server") 
                                       .complete(file);
   }
   else
      secureKeyPath = core::FilePath("/tmp/rstudio-server").complete(file);

   // read file if it already exists
   if (secureKeyPath.exists())
   {
      // read the key
      std::string secureKey;
      core::Error error = core::readStringFromFile(secureKeyPath, &secureKey);
      if (error)
         return error;

      // check for non-empty key
      if (secureKey.empty())
      {
         error = systemError(boost::system::errc::no_such_file_or_directory,
                             ERROR_LOCATION);
         error.addProperty("path", secureKeyPath.absolutePath());
         return error;
      }

      // save the key and return success
      *pContents = secureKey;
      return core::Success();
   }

   // otherwise generate a new key and write it to the file
   else
   {
      // generate a new key
      std::string secureKey = core::system::generateUuid(false);

      // ensure the parent directory
      core::Error error = secureKeyPath.parent().ensureDirectory();
      if (error)
         return error;

      // attempt to write it
      error = writeStringToFile(secureKeyPath, secureKey);
      if (error)
         return error;

      // change mode it so it is only readable and writeable by this user
      if (changeFileMode(secureKeyPath,
                         core::system::UserReadWriteMode) < 0)
      {
         return systemError(errno, ERROR_LOCATION);
      }

      // successfully generated the cookie key, set it
      *pContents = secureKey;
   }

   // return success
   return core::Success();
}

} // namespace key_file
} // namespace server
} // namespace rstudio


