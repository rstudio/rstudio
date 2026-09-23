/*
 * Xdg.hpp
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

#ifndef CORE_SYSTEM_XDG_HPP
#define CORE_SYSTEM_XDG_HPP

#include <boost/optional.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/Types.hpp>

#include <vector>

namespace rstudio {
namespace core {
namespace system {

namespace xdg {

/*
 * These routines return system and user paths for RStudio configuration and data, roughly in
 * accordance with the FreeDesktop XDG Base Directory Specification.
 *
 * https://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
 *
 * All of these can be configured with environment variables as described below.
 *
 * The values of the environment variables can include the following special variables:
 *
 * $USER  The user's name
 * $HOME  The user's home directory
 * ~      The user's home directory
 *
 * These values will be resolved against the current user by default. If you wish to
 * resolve them against a different user, supply their name and home directory using
 * the boost::optional arguments.
 */

// Returns the RStudio XDG user config directory.
//
// On Unix-alikes, this is ~/.config/rstudio, or XDG_CONFIG_HOME.
// On Windows, this is 'FOLDERID_RoamingAppData' (typically 'AppData/Roaming').
FilePath userConfigDir(const boost::optional<std::string>& user = boost::none,
                       const boost::optional<FilePath>& homeDir = boost::none);

// Returns the RStudio XDG user data directory.
//
// On Unix-alikes, this is ~/.local/share/rstudio, or XDG_DATA_HOME.
// On Windows, this is 'FOLDERID_LocalAppData' (typically 'AppData/Local').
FilePath userDataDir(const boost::optional<std::string>& user = boost::none,
                     const boost::optional<FilePath>& homeDir = boost::none);

// Returns the user-specific logging directory underneath the userDataDir
FilePath userLogDir();
                     
// Returns the RStudio XDG user cache directory.
//
// On Unix-alikes, this is ~/.cache, or XDG_CACHE_HOME.
// On Windows, this resolves to %LOCALAPPDATA%/RStudio/Cache by default.
FilePath userCacheDir(const boost::optional<std::string>& user = boost::none,
                      const boost::optional<FilePath>& homeDir = boost::none);

#ifdef _WIN32

// Older versions of RStudio on Windows used FOLDERID_InternetCache for cached data files.
// This function allows callers to query that older location, to allow resources
// to be migrated transparently.
FilePath oldUserCacheDir(
    const boost::optional<std::string>& user = boost::none,
    const boost::optional<FilePath>& homeDir = boost::none);

#endif

// This function verifies that the userConfigDir(), userDataDir(), and the user log directory
// exist and are writable by the running user.
//
// It should be invoked once. Any issues with these directories will be emitted to the session log.
void verifyUserDirs(const boost::optional<std::string>& user = boost::none,
                    const boost::optional<FilePath>& homeDir = boost::none);

// Whether redirectUnwritableUserDataDir() has replaced the user data directory with a
// temporary one in this process.
bool isUserDataDirTemporary();

#ifndef _WIN32

// Returns an error if files can't be created in the given directory, creating the directory
// first if it doesn't exist.
Error checkDirectoryWritable(const FilePath& dir);

// Returns the directory used in place of the user data directory when that can't be written:
// a directory under the system temporary directory, created if necessary, that is owned by
// and private to the current user. Fails if the path is taken by something else, e.g. a
// directory created by another user of a shared temporary directory.
Error temporaryUserDataDir(FilePath* pDir);

// Checks that the user data directory can be written. If it can't, points the user data
// directory at temporaryUserDataDir() instead, by setting RSTUDIO_DATA_HOME so that child
// processes agree, and so that a session can still start.
//
// Returns the error that made the user data directory unusable, or Success() if it is usable.
// When an error is returned, pTemporaryDir is set to the directory now in use, or left empty
// if the temporary directory couldn't be used either (in which case pTemporaryDirError is
// set).
Error redirectUnwritableUserDataDir(FilePath* pTemporaryDir, Error* pTemporaryDirError);

#endif

// Returns the RStudio XDG system config directory.
//
// On Unix-alikes, this is /etc/rstudio, XDG_CONFIG_DIRS.
// On Windows, this is 'FOLDERID_ProgramData' (typically 'C:/ProgramData').
FilePath systemConfigDir();

// Convenience method for finding a configuration file. Checks all the
// directories in XDG_CONFIG_DIRS for the file. If it doesn't find it,
// the path where we expected to find it is returned instead. Doesn't
// do any logging.
FilePath systemConfigFile(const std::string& filename);

// Convenience method for finding a configuration file. Given a context such as
// "load balancer config" or "secure header", it will log, at the INFO level,
// where the config file was found, where it was expected to be found it wasn't,
// and why.
FilePath findSystemConfigFile(const std::string& context, const std::string& filename);

// Sets relevant XDG environment variables
void forwardXdgEnvVars(Options *pEnvironment);

} // namespace xdg
} // namespace system
} // namespace core
} // namespace rstudio

#endif
