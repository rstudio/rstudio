/*
 * Xdg.cpp
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

#ifdef _WIN32
# include <Windows.h>
# include <comdef.h>
# include <ShlObj_core.h>
# include <KnownFolders.h>
# include <winsock2.h>
#endif

#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/User.hpp>

#include <core/Algorithm.hpp>
#include <core/Backtrace.hpp>
#include <core/StringUtils.hpp>
#include <core/Thread.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

#ifdef _WIN32
# define kRStudioDataFolderName "RStudio"
# define kRStudioCacheSuffix    "Cache"
#else
# define kRStudioDataFolderName "rstudio"
# define kRStudioCacheSuffix    ""
#endif

namespace rstudio {
namespace core {
namespace system {
namespace xdg {
namespace {

/**
 * Returns the hostname from the operating system
 */
std::string getHostname()
{
   // Use a static string to store the hostname so we don't have to look it up
   // multiple times
   static std::string hostname;
   static boost::mutex mutex;
   std::string result;

   // Lock to ensure that we don't try to read/write the hostname from two
   // threads
   LOCK_MUTEX(mutex)
   {
      if (hostname.empty())
      {
         char buffer[256];
         int status = ::gethostname(buffer, 255);
         if (status == 0)
         {
            // If successful, store the hostname for later; swallow errors here
            // since they are not actionable
            hostname = std::string(buffer);
         }
      }
      result = hostname;
   }
   END_LOCK_MUTEX

   return result;
}

namespace {

FilePath resolveXdgDirImpl(FilePath rstudioXdgPath,
                           const boost::optional<std::string>& user,
                           const boost::optional<FilePath>& homeDir,
                           const std::string& suffix = "")
{
   // expand HOME, USER, and HOSTNAME if given
   std::string hostname = getenv("HOSTNAME");
   std::string resolvedHostname = hostname.empty() ? getHostname() : hostname;
   std::string resolvedUser = user ? *user : username();
   FilePath resolvedHome = homeDir ? *homeDir : userHomePath();
   
   core::system::Options environment;
   core::system::setenv(&environment, "HOME", resolvedHome.getAbsolutePath());
   core::system::setenv(&environment, "USER", resolvedUser);
   core::system::setenv(&environment, "HOSTNAME", resolvedHostname);

   // resolve aliases in the path
   std::string expanded = core::system::expandEnvVars(environment, rstudioXdgPath.getAbsolutePath());
   rstudioXdgPath = FilePath::resolveAliasedPath(expanded, homeDir ? *homeDir : userHomePath());
   
   // if a suffix was provided, use it
   if (!suffix.empty())
      rstudioXdgPath = rstudioXdgPath.completePath(suffix);

   return rstudioXdgPath;
}

FilePath xdgDefaultDir(
#ifdef _WIN32
      const GUID& windowsFolderId,
      const std::string& windowsFolderIdName,
#endif
      const boost::optional<std::string>& user,
      const boost::optional<FilePath>& homeDir,
      const std::string& defaultDir)
{
   
#ifdef _WIN32
   std::string xdgHomeDir;
   
   // On Windows, the default path is in Application Data/Roaming.
   wchar_t* path = nullptr;
   HRESULT hr = ::SHGetKnownFolderPath(windowsFolderId, 0, nullptr, &path);
   if (hr == S_OK)
   {
      xdgHomeDir = core::string_utils::wideToUtf8(std::wstring(path));
   }
   else
   {
      _com_error error(hr);
      WLOGF(
          "Error {} computing SHGetKnownFolderPath({}): {}",
          safe_convert::numberToHexString(hr),
          windowsFolderIdName,
          error.ErrorMessage());
   }

   ::CoTaskMemFree(path);
   
   if (!xdgHomeDir.empty())
      return xdgHomeDir;
#endif

   // The default directory might need to be resolved relative
   // to the user's home directory, so do that now.
   return resolveXdgDirImpl(FilePath(defaultDir), user, homeDir);
}

} // end anonymous namespace

/**
 * Resolves an XDG directory based on the user and environment.
 *
 * @param rstudioEnvVer The RStudio-specific environment variable specifying
 *   the directory (given precedence)
 * @param xdgEnvVar The XDG standard environment variable
 * @param defaultDir Fallback default directory if neither environment variable
 *   is present
 * @param windowsFolderId The ID of the Windows folder to resolve against
 * @param windowsFolderIdName The symbolic name of the Windows folder to resolve against
 * @param user Optionally, the user to return a directory for; if omitted the
 *   current user is used
 * @param homeDir Optionally, the home directory to resolve against; if omitted
 *   the current user's home directory is used
 * @param suffix An optional path component to append to the computed path.
 */
FilePath resolveXdgDir(
      const std::string& rstudioEnvVar,
      const std::string& xdgEnvVar,
#ifdef _WIN32
      const GUID& windowsFolderId,
      const std::string& windowsFolderIdName,
#endif
      const std::string& defaultDir,
      const boost::optional<std::string>& user,
      const boost::optional<FilePath>& homeDir,
      const std::string& suffix = std::string())
{
   // If the RStudio-specific environment variable is provided, use it.
   std::string rstudioEnvValue = getenv(rstudioEnvVar);
   if (!rstudioEnvValue.empty())
   {
      // TODO: What if this variable is provided, but we cannot create or use
      // the provided directory? Should we fall back to an XDG directory?
      FilePath rstudioXdgPath(rstudioEnvValue);
      Error error = rstudioXdgPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);
      
      DLOGF("Using XDG directory: {} => {}", rstudioEnvVar, rstudioXdgPath.getAbsolutePath());
      return resolveXdgDirImpl(rstudioXdgPath, user, homeDir, suffix);
   }
   
   // Build list of directories to search.
   std::vector<FilePath> xdgPaths;
   
   std::string xdgEnvValue = getenv(xdgEnvVar);
   if (!xdgEnvValue.empty())
   {
      for (auto&& xdgPath : core::algorithm::split(xdgEnvValue, ":"))
      {
         FilePath resolvedXdgPath = resolveXdgDirImpl(FilePath(xdgPath), user, homeDir);
         xdgPaths.push_back(resolvedXdgPath);
      }
   }
   
   // Include the default directory in the search list.
   FilePath xdgDefaultHome = xdgDefaultDir(
#ifdef _WIN32
            windowsFolderId,
            windowsFolderIdName,
#endif
            user,
            homeDir,
            defaultDir);
   
   xdgPaths.push_back(xdgDefaultHome);
   
   // First, look for an already-existing RStudio directory.
   for (const FilePath& xdgPath : xdgPaths)
   {
      FilePath rstudioXdgHome = xdgPath.completePath(kRStudioDataFolderName);
      if (rstudioXdgHome.exists())
      {
         DLOGF("Using pre-existing XDG directory: {} => {}", xdgEnvVar, rstudioXdgHome.getAbsolutePath());
         return resolveXdgDirImpl(rstudioXdgHome, user, homeDir, suffix);
      }
   }
   
   // If we couldn't find an RStudio directory, use the first XDG path that exists.
   for (const FilePath& xdgPath : xdgPaths)
   {
      if (xdgPath.exists())
      {
         FilePath rstudioXdgHome = FilePath(xdgPath).completePath(kRStudioDataFolderName);
         DLOGF("Using new XDG directory: {} => {}", xdgEnvVar, rstudioXdgHome.getAbsolutePath());
         return resolveXdgDirImpl(rstudioXdgHome, user, homeDir, suffix);
      }
   }
   
   // If none of the provided directories exist (very unexpected!) use the default.
   FilePath rstudioXdgHome = xdgDefaultHome.completePath(kRStudioDataFolderName);
   DLOGF("Using default XDG directory: {}", xdgDefaultHome.getAbsolutePath());
   return resolveXdgDirImpl(rstudioXdgHome, user, homeDir, suffix);
}

} // anonymous namespace

FilePath userConfigDir(
   const boost::optional<std::string>& user,
   const boost::optional<FilePath>& homeDir)
{
   return resolveXdgDir(
         "RSTUDIO_CONFIG_HOME",
         "XDG_CONFIG_HOME",
#ifdef _WIN32
         FOLDERID_RoamingAppData,
         "FOLDERID_RoamingAppData",
#endif
         "~/.config",
         user,
         homeDir
   );
}

FilePath userDataDir(
        const boost::optional<std::string>& user,
        const boost::optional<FilePath>& homeDir)
{
   return resolveXdgDir(
         "RSTUDIO_DATA_HOME",
         "XDG_DATA_HOME",
#ifdef _WIN32
         FOLDERID_LocalAppData,
         "FOLDERID_LocalAppData",
#endif
         "~/.local/share",
         user,
         homeDir
   );
}

FilePath userCacheDir(
        const boost::optional<std::string>& user,
        const boost::optional<FilePath>& homeDir)
{
   return resolveXdgDir(
         "RSTUDIO_CACHE_HOME",
         "XDG_CACHE_HOME",
#ifdef _WIN32
         FOLDERID_LocalAppData,
         "FOLDERID_LocalAppData",
#endif
         "~/.cache",
         user,
         homeDir,
         kRStudioCacheSuffix
   );
}

#ifdef _WIN32

FilePath oldUserCacheDir(
        const boost::optional<std::string>& user,
        const boost::optional<FilePath>& homeDir)
{
   return resolveXdgDir("RSTUDIO_CACHE_HOME",
         "XDG_CACHE_HOME",
#ifdef _WIN32
         FOLDERID_InternetCache,
         "FOLDERID_InternetCache",
#endif
         "~/.cache",
         user,
         homeDir
   );
}

#endif

FilePath userLogDir()
{
   return userDataDir().completePath("log");
}

void verifyUserDirs(
   const boost::optional<std::string>& user,
   const boost::optional<FilePath>& homeDir)
{
#ifndef _WIN32
   auto testDir = [](const FilePath& dir, const ErrorLocation& errorLoc)
   {
      Error error, permError;
      if (dir.exists())
      {
         bool writeable = false;
         // Test the directory for write access; this just checks the directory itself for a write
         // access bit. 
         error = dir.isWriteable(writeable);
         if (error)
         {
            // We couldn't even read the directory's access bits
            rstudio::core::log::logWarningMessage("Could not access " + dir.getAbsolutePath() + 
                  " to check write " "permissions. Some features may not work correctly.",
                  errorLoc);
            rstudio::core::log::logError(error, errorLoc);
         }
         else if (!writeable)
         {
            // We determined that the directory was not writable. There's nothing we can do to
            // correct this, so just log a warning to help diagnose downstream failures (which are
            // virtually guaranteed).
            rstudio::core::log::logWarningMessage("Missing write permissions to " + 
                  dir.getAbsolutePath() + ". Some features may not work correctly.",
                  errorLoc);
         }
      }
   };

   testDir(userConfigDir(user, homeDir), ERROR_LOCATION);
   testDir(userDataDir(user, homeDir), ERROR_LOCATION);
#endif
}

FilePath systemConfigDir()
{
   return resolveXdgDir(
         "RSTUDIO_CONFIG_DIR",
         "XDG_CONFIG_DIRS",
#ifdef _WIN32
         FOLDERID_ProgramData,
         "FOLDERID_ProgramData",
#endif
         "/etc",
         boost::none,  // no specific user
         boost::none   // no home folder resolution
   );
}

FilePath systemConfigFile(const std::string& filename)
{
#ifdef _WIN32
    // Passthrough on Windows
    return systemConfigDir().completePath(filename);
#else
   if (getenv("RSTUDIO_CONFIG_DIR").empty())
   {
      // On POSIX, check for a search path.
      std::string env = getenv("XDG_CONFIG_DIRS");
      if (env.find_first_of(":") != std::string::npos)
      {
         // This is a search path; check each element for the file.
         std::vector<std::string> dirs = algorithm::split(env, ":");
         for (const std::string& dir: dirs)
         {
            FilePath resolved = FilePath(dir)
                  .completePath("rstudio")
                  .completePath(filename);
            if (resolved.exists())
            {
               return resolved;
            }
         }
      }
   }

   // We didn't find the file on the search path, so return the location where
   // we expected to find it.
   return systemConfigDir().completePath(filename);
#endif
}

FilePath findSystemConfigFile(const std::string& context, const std::string& filename)
{
   FilePath configFile = systemConfigFile(filename);
   if (configFile.exists())
   {
      // We found the file, so just say where we found it
      rstudio::core::log::logInfoMessage("Reading " + context + " from '" +
            configFile.getAbsolutePath() + "'");
   }
   else
   {
      if (getenv("RSTUDIO_CONFIG_DIR").empty())
      {
         if (getenv("XDG_CONFIG_DIRS").empty())
         {
            // No env vars so just say we didn't find the file at its default location
            rstudio::core::log::logInfoMessage("No " + context + " found at " +
                  configFile.getAbsolutePath());
         }
         else
         {
            // XDG_CONFIG_DIRS was set, so emit the search path we used
            rstudio::core::log::logInfoMessage("No " + context + " '" + filename + "' "
                  "found in XDG_CONFIG_DIRS, expected in an 'rstudio' folder in one of "
                  "'" + getenv("XDG_CONFIG_DIRS") + "'");
         }
      }
      else
      {
         // RSTUDIO_CONFIG_DIR was set, so emit where we expected the file to be
         rstudio::core::log::logInfoMessage("No " + context + " found in RSTUDIO_CONFIG_DIR, "
               "expected at '" + configFile.getAbsolutePath() + "'");
      }
   }
   return configFile;
}

void forwardXdgEnvVars(Options *pEnvironment)
{
   // forward relevant XDG environment variables (i.e. all those we respect above)
   core::system::forwardEnvVars({"RSTUDIO_CONFIG_HOME", "RSTUDIO_CONFIG_DIR",
                                 "RSTUDIO_DATA_HOME",   "RSTUDIO_DATA_DIR",
                                 "XDG_CONFIG_HOME",     "XDG_CONFIG_DIRS",
                                 "XDG_DATA_HOME",       "XDG_DATA_DIRS"},
                                pEnvironment);
}


} // namespace xdg
} // namespace system
} // namespace core
} // namespace rstudio
