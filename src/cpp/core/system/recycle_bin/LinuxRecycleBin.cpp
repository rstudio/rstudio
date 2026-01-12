/*
 * LinuxRecycleBin.cpp
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

#include <ctime>
#include <iomanip>
#include <sstream>

#include <boost/date_time/posix_time/posix_time.hpp>

#include <shared_core/DateTime.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace recycle_bin {

namespace {

// Get the XDG trash directory (typically ~/.local/share/Trash)
FilePath trashDir()
{
   // Check XDG_DATA_HOME environment variable, default to ~/.local/share
   std::string xdgDataHome = core::system::getenv("XDG_DATA_HOME");
   FilePath dataDir;
   if (!xdgDataHome.empty())
   {
      dataDir = FilePath(xdgDataHome);
   }
   else
   {
      dataDir = core::system::userHomePath().completePath(".local/share");
   }
   return dataDir.completePath("Trash");
}

// Generate a unique filename in the trash directory.
// If the base name exists, append .2, .3, etc. (per FreeDesktop spec)
std::string uniqueTrashName(const FilePath& trashFilesDir,
                            const FilePath& trashInfoDir,
                            const std::string& baseName)
{
   FilePath filesPath = trashFilesDir.completePath(baseName);
   FilePath infoPath = trashInfoDir.completePath(baseName + ".trashinfo");

   // If neither exists, we can use the base name
   if (!filesPath.exists() && !infoPath.exists())
      return baseName;

   // Find a unique name by appending a number (per spec, start at 2)
   for (int i = 2; i < 100000; ++i)
   {
      std::string uniqueName = baseName + "." + std::to_string(i);
      filesPath = trashFilesDir.completePath(uniqueName);
      infoPath = trashInfoDir.completePath(uniqueName + ".trashinfo");
      if (!filesPath.exists() && !infoPath.exists())
         return uniqueName;
   }

   // Fallback using timestamp (should never happen in practice)
   return baseName + "." + std::to_string(std::time(nullptr));
}

// Percent-encode a path for the .trashinfo file per FreeDesktop XDG Trash spec.
// Uses RFC 3986 encoding but preserves '/' path separators as required by the spec.
std::string encodePathForTrashInfo(const std::string& path)
{
   std::ostringstream encoded;
   encoded << std::hex << std::uppercase << std::setfill('0');

   for (char ch : path)
   {
      // Unreserved characters per RFC 3986: ALPHA / DIGIT / "-" / "." / "_" / "~"
      // Plus "/" which must be preserved for paths per XDG Trash spec
      if ((ch >= 'A' && ch <= 'Z') ||
          (ch >= 'a' && ch <= 'z') ||
          (ch >= '0' && ch <= '9') ||
          ch == '-' || ch == '.' || ch == '_' || ch == '~' || ch == '/')
      {
         encoded << ch;
      }
      else
      {
         encoded << '%' << std::setw(2) << static_cast<int>(static_cast<unsigned char>(ch));
      }
   }

   return encoded.str();
}

// Get current local time in ISO 8601 format (YYYY-MM-DDTHH:MM:SS)
std::string currentTimeIso8601()
{
   boost::posix_time::ptime now = boost::posix_time::second_clock::local_time();
   return date_time::format(now, "%Y-%m-%dT%H:%M:%S");
}

} // anonymous namespace

Error sendTo(const FilePath& filePath)
{
   // Validate the file path
   std::string baseName = filePath.getFilename();
   if (baseName.empty())
   {
      Error error = systemError(boost::system::errc::invalid_argument,
                                "Cannot move file with empty name to trash",
                                ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }

   // Get trash directories
   FilePath trashDirectory = trashDir();
   FilePath trashFilesDir = trashDirectory.completePath("files");
   FilePath trashInfoDir = trashDirectory.completePath("info");

   // Ensure trash directories exist
   Error error = trashFilesDir.ensureDirectory();
   if (error)
   {
      error.addProperty("path", filePath);
      error.addProperty("trashDir", trashFilesDir);
      return error;
   }

   error = trashInfoDir.ensureDirectory();
   if (error)
   {
      error.addProperty("path", filePath);
      error.addProperty("trashInfoDir", trashInfoDir);
      return error;
   }

   // Generate a unique filename in the trash
   std::string trashName = uniqueTrashName(trashFilesDir, trashInfoDir, baseName);

   // Create the .trashinfo file content per FreeDesktop.org spec
   std::string trashInfoContent =
      "[Trash Info]\n"
      "Path=" + encodePathForTrashInfo(filePath.getAbsolutePath()) + "\n"
      "DeletionDate=" + currentTimeIso8601() + "\n";

   // Write the .trashinfo file first (per spec, should exist before move)
   FilePath trashInfoPath = trashInfoDir.completePath(trashName + ".trashinfo");
   error = writeStringToFile(trashInfoPath, trashInfoContent);
   if (error)
   {
      error.addProperty("path", filePath);
      error.addProperty("trashInfoPath", trashInfoPath);
      return error;
   }

   // Move the file to trash
   FilePath targetPath = trashFilesDir.completePath(trashName);
   error = filePath.move(targetPath);
   if (error)
   {
      // Clean up the .trashinfo file on failure
      Error cleanupError = trashInfoPath.removeIfExists();
      if (cleanupError)
      {
         LOG_WARNING_MESSAGE("Failed to clean up .trashinfo file after move failure: " +
                             trashInfoPath.getAbsolutePath());
      }

      error.addProperty("path", filePath);
      error.addProperty("targetPath", targetPath);
      return error;
   }

   return Success();
}

} // namespace recycle_bin
} // namespace system
} // namespace core
} // namespace rstudio

