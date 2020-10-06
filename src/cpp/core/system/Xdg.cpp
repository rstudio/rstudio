/*
 * Xdg.cpp
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

#ifdef _WIN32
#include <Windows.h>
#include <ShlObj_core.h>
#include <KnownFolders.h>
#include <winsock2.h>
#endif

#include <core/Algorithm.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <core/Thread.hpp>

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

/**
 * Resolves an XDG directory based on the user and environment.
 *
 * @param rstudioEnvVer The RStudio-specific environment variable specifying
 *   the directory (given precedence)
 * @param xdgEnvVar The XDG standard environment variable
 * @param defaultDir Fallback default directory if neither environment variable
 *   is present
 * @param windowsFolderId The ID of the Windows folder to resolve against
 * @param user Optionally, the user to return a directory for; if omitted the
 *   current user is used
 * @param homeDir Optionally, the home directory to resolve against; if omitted
 *   the current user's home directory is used
 */
FilePath resolveXdgDir(
      const std::string& rstudioEnvVar,
      const std::string& xdgEnvVar,
#ifdef _WIN32
      const GUID windowsFolderId,
#endif
      const std::string& defaultDir,
      const boost::optional<std::string>& user,
      const boost::optional<FilePath>& homeDir)
{
   FilePath xdgHome;
   bool finalPath = true;

   // Look for the RStudio-specific environment variable
   std::string env = getenv(rstudioEnvVar);
   if (env.empty())
   {
      // The RStudio environment variable specifices the final path; if it isn't
      // set we will need to append "rstudio" to the path later.
      finalPath = false;
      env = getenv(xdgEnvVar);
   }

   if (env.empty())
   {
      // No root specified for xdg home; we will need to generate one.
#ifdef _WIN32
      // On Windows, the default path is in Application Data/Roaming.
      wchar_t *path = nullptr;
      HRESULT hr = ::SHGetKnownFolderPath(
            windowsFolderId,
            0,
            nullptr, // current user
            &path);

      if (hr == S_OK)
      {
         xdgHome = FilePath(std::wstring(path));
      }
      else
      {
         LOG_ERROR_MESSAGE("Unable to retrieve app settings path. HRESULT:  " +
                           safe_convert::numberToString(hr));
      }

      // Free memory if allocated
      if (path != nullptr)
      {
         ::CoTaskMemFree(path);
      }

#endif
      if (xdgHome.isEmpty())
      {
         // Use the default subdir for POSIX. We also use this folder as a fallback on Windows
         //if we couldn't read the app settings path.
         xdgHome = FilePath(defaultDir);
      }
   }
   else
   {
      // We have a manually specified xdg directory from an environment variable.
      xdgHome = FilePath(env);
   }

   // expand HOME, USER, and HOSTNAME if given
   core::system::Options environment;
   core::system::setenv(&environment, "HOME",
                        homeDir ? homeDir->getAbsolutePath() :
                                  userHomePath().getAbsolutePath());
   core::system::setenv(&environment, "USER",
                        user ? *user : username());

   // check for manually specified hostname in environment variable
   std::string hostname = core::system::getenv("HOSTNAME");

   // when omitted, look up the hostname using a system call
   if (hostname.empty())
   {
      hostname = getHostname();
   }
   core::system::setenv(&environment, "HOSTNAME", hostname);

   std::string expanded = core::system::expandEnvVars(environment, xdgHome.getAbsolutePath());

   // resolve aliases in the path
   xdgHome = FilePath::resolveAliasedPath(expanded, homeDir ? *homeDir : userHomePath());

   // If this is the final path, we can return it as-is
   if (finalPath)
   {
      return xdgHome;
   }

   // Otherwise, it's a root folder in which we need to create our own subfolder
   return xdgHome.completePath(
#ifdef _WIN32
      "RStudio"
#else
      "rstudio"
#endif
   );
}

} // anonymous namespace

FilePath userConfigDir(
        const boost::optional<std::string>& user,
        const boost::optional<FilePath>& homeDir)
{
   return resolveXdgDir("RSTUDIO_CONFIG_HOME",
        "XDG_CONFIG_HOME",
#ifdef _WIN32
         FOLDERID_RoamingAppData,
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
   return resolveXdgDir("RSTUDIO_DATA_HOME",
         "XDG_DATA_HOME",
#ifdef _WIN32
         FOLDERID_LocalAppData,
#endif
         "~/.local/share",
         user,
         homeDir
   );
}

FilePath systemConfigDir()
{
#ifndef _WIN32
   if (getenv("RSTUDIO_CONFIG_DIR").empty())
   {
      // On POSIX operating systems, it's possible to specify multiple config
      // directories. We have to select one, so read the list and take the first
      // one that contains an "rstudio" folder.
      std::string env = getenv("XDG_CONFIG_DIRS");
      if (env.find_first_of(":") != std::string::npos)
      {
         std::vector<std::string> dirs = algorithm::split(env, ":");
         for (const std::string& dir: dirs)
         {
            FilePath resolved = FilePath(dir).completePath("rstudio");
            if (resolved.exists())
            {
               return resolved;
            }
         }
      }
   }
#endif
   return resolveXdgDir("RSTUDIO_CONFIG_DIR",
         "XDG_CONFIG_DIRS",
#ifdef _WIN32
         FOLDERID_ProgramData,
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
    return systemConfigDir().completeChildPath(filename);
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
            FilePath resolved = FilePath(dir).completePath("rstudio")
                  .completeChildPath(filename);
            if (resolved.exists())
            {
               return resolved;
            }
         }
      }
   }

   // We didn't find the file on the search path, so return the location where
   // we expected to find it.
   return systemConfigDir().completeChildPath(filename);
#endif
}

void forwardXdgEnvVars(Options *pEnvironment)
{
   // forward relevant XDG environment variables (i.e. all those we respect above)
   for (auto&& xdgVar: {"RSTUDIO_CONFIG_HOME", "RSTUDIO_CONFIG_DIR",
                        "RSTUDIO_DATA_HOME",   "RSTUDIO_DATA_DIR",
                        "XDG_CONFIG_HOME",     "XDG_CONFIG_DIRS",
                        "XDG_DATA_HOME",       "XDG_DATA_DIRS"})
   {
      // only forward value if non-empty; avoid overwriting a previously set
      // value with an empty one
      std::string val = core::system::getenv(xdgVar);
      if (!val.empty())
      {
         // warn if we're changing values; we typically are forwarding values in
         // order to ensure a consistent view of configuration and state across
         // RStudio processes, which merits overwriting, but it's also hard to
         // imagine that these vars would be set unintentionally in the existing
         // environment.
         std::string oldVal = core::system::getenv(*pEnvironment, xdgVar);
         if (!oldVal.empty() && oldVal != val)
         {
             LOG_WARNING_MESSAGE("Overriding " + std::string(xdgVar) +
                                 ": '" + oldVal + "' => '" + val + "'");
         }
         core::system::setenv(pEnvironment, xdgVar, val);
      }
   }
}


} // namespace xdg
} // namespace system
} // namespace core
} // namespace rstudio

