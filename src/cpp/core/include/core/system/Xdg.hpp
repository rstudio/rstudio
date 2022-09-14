/*
 * Xdg.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
                     
// This function verifies that the userConfigDir() and userDataDir() exist and are owned by the running user.
// 
// It should be invoked once. Any issues with these directories will be emitted to the session log.
void verifyUserDirs(const boost::optional<std::string>& user = boost::none,
                    const boost::optional<FilePath>& homeDir = boost::none);

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
