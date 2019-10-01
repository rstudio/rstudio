/*
 * Xdg.hpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
 */

// Returns the RStudio XDG user config directory.
//
// On Unix-alikes, this is ~/.config/rstudio, or XDG_CONFIG_HOME.
// On Windows, this is 'FOLDERID_RoamingAppData' (typically 'AppData/Roaming').
FilePath userConfigDir();

// Returns the RStudio XDG user data directory.
//
// On Unix-alikes, this is ~/.local/share/rstudio, or XDG_DATA_HOME.
// On Windows, this is 'FOLDERID_LocalAppData' (typically 'AppData/Local').
FilePath userDataDir();

// Returns the RStudio XDG system config directory.
//
// On Unix-alikes, this is /etc/rstudio, XDG_CONFIG_DIRS.
// On Windows, this is 'FOLDERID_ProgramData' (typically 'C:/ProgramData').
FilePath systemConfigDir();

} // namespace xdg
} // namespace system
} // namespace core
} // namespace rstudio

