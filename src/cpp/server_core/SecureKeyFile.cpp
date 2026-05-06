/*
 * SecureKeyFile.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <server_core/SecureKeyFile.hpp>

#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <core/system/PosixSystem.hpp>
#include <core/system/Xdg.hpp>

#include <shared_core/Hash.hpp>


namespace rstudio {
namespace core {

namespace key_file {

core::Error writeSecureKeyFile(const std::string& secureKey, const FilePath& secureKeyPath)
{
   // ensure the parent directory
   core::Error error = secureKeyPath.getParent().ensureDirectory();
   if (error)
      return error;

   std::string keyHash = hash::crc32HexHash(secureKey);
   LOG_DEBUG_MESSAGE("Creating secure key file: \"" + secureKeyPath.getAbsolutePath() + 
                     "\" key hash: (" + keyHash + ")");

   // attempt to write it
   error = writeStringToFile(secureKeyPath, secureKey);
   if (error)
      return error;

   // change mode it so it is only readable and writeable by this user
   error = secureKeyPath.changeFileMode(core::FileMode::USER_READ_WRITE);
   if (error)
      return error;

   return Success();
}

core::Error readSecureKeyFile(const FilePath& secureKeyPath,
                              std::string* pContents,
                              std::string* pContentsHash,
                              std::string* pKeyPathUsed)
{
   // read file if it already exists
   if (secureKeyPath.exists())
   {
      *pKeyPathUsed = secureKeyPath.getAbsolutePath();
      LOG_DEBUG_MESSAGE("Using secure key file: " + *pKeyPathUsed);

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

      *pContentsHash = hash::crc32HexHash(secureKey);
      LOG_DEBUG_MESSAGE("Secure key hash: (" + *pContentsHash + ")");

      // save the key and return success
      *pContents = secureKey;
      return core::Success();
   }

   // otherwise generate a new key and write it to the file
   else
   {
      // generate a new key
      std::string secureKey = core::system::generateUuid(false);

      core::Error error = writeSecureKeyFile(secureKey, secureKeyPath);
      if (error)
         return error;

      *pKeyPathUsed = secureKeyPath.getAbsolutePath();
      *pContentsHash = hash::crc32HexHash(secureKey);

      // successfully generated the cookie key, set it
      *pContents = secureKey;
   }

   // return success
   return core::Success();
}

core::Error readSecureKeyFile(const std::string& filename,
                              std::string* pContents,
                              std::string* pContentsHash,
                              std::string* pKeyPathUsed)
{
   // Try system paths first - the service account owns these and can read
   // them, so non-root rserver deployments share /var/lib/rstudio-server
   // with privileged setup steps.
   core::FilePath secureKeyPath = core::system::xdg::findSystemConfigFile(
         "secure key", filename);
   if (!secureKeyPath.exists())
      secureKeyPath = core::FilePath("/var/lib/rstudio-server")
         .completePath(filename);

   // If no system key exists and we're not privileged, fall back to the
   // user cache (read if present, generate otherwise).
   if (!secureKeyPath.exists() && !core::system::effectiveUserIsRoot())
   {
      secureKeyPath = core::system::xdg::userCacheDir().completePath(filename);
      if (secureKeyPath.exists())
      {
         LOG_INFO_MESSAGE("Running without privilege; using secure key at " +
                          secureKeyPath.getAbsolutePath());
      }
      else
      {
         LOG_INFO_MESSAGE("Running without privilege; generating secure key at " +
                          secureKeyPath.getAbsolutePath());
      }
   }

   return readSecureKeyFile(secureKeyPath, pContents, pContentsHash, pKeyPathUsed);
}

core::FilePath systemKeyFilePath(const std::string& filename)
{
   // use the silent variant; callers use this as a probe and the
   // INFO-logging findSystemConfigFile would emit a line on every miss
   core::FilePath path = core::system::xdg::systemConfigFile(filename);
   if (!path.exists())
      path = core::FilePath("/var/lib/rstudio-server").completePath(filename);
   return path;
}

core::Error readSecureKeyFile(const FilePath& secureKeyPath,
                              std::string* pContents)
{
   std::string keyFileUsed;
   std::string contentsHash;
   return readSecureKeyFile(secureKeyPath, pContents, &contentsHash, &keyFileUsed);
}

core::Error readSecureKeyFile(const std::string& filename,
                              std::string* pContents)
{
   std::string keyFileUsed;
   std::string contentsHash;
   return readSecureKeyFile(filename, pContents, &contentsHash, &keyFileUsed);
}

} // namespace key_file
} // namespace server
} // namespace rstudio


