/*
 * SecureKeyFile.cpp
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

#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <core/system/PosixSystem.hpp>
#include <core/system/Xdg.hpp>

#include <server_core/SecureKeyFile.hpp>

namespace rstudio {
namespace core {
namespace key_file {

core::Error readSecureKeyFile(const FilePath& secureKeyPath,
                              std::string* pContents)
{
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
         error.addProperty("path", secureKeyPath.getAbsolutePath());
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
      core::Error error = secureKeyPath.getParent().ensureDirectory();
      if (error)
         return error;

      // attempt to write it
      error = writeStringToFile(secureKeyPath, secureKey);
      if (error)
         return error;

      // change mode it so it is only readable and writeable by this user
      error = secureKeyPath.changeFileMode(core::FileMode::USER_READ_WRITE);
      if (error)
         return error;

      // successfully generated the cookie key, set it
      *pContents = secureKey;
   }

   // return success
   return core::Success();
}

core::Error readSecureKeyFile(const std::string& filename,
                              std::string* pContents)
{
   // determine path to use for secure cookie key file
   core::FilePath secureKeyPath;
   if (core::system::effectiveUserIsRoot())
   {
      // check in our default configuration folder
      secureKeyPath = core::system::xdg::systemConfigFile(filename);
      if (!secureKeyPath.exists())
         secureKeyPath = core::FilePath("/var/lib/rstudio-server")
            .completePath(filename);
   }
   else
      secureKeyPath = core::FilePath("/tmp/rstudio-server").completePath(filename);

   return readSecureKeyFile(secureKeyPath, pContents);
}

} // namespace key_file
} // namespace server
} // namespace rstudio


