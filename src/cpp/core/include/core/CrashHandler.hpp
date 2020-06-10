/*
 * CrashHandler.hpp
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

#ifndef CORE_CRASH_HANDLER_HPP
#define CORE_CRASH_HANDLER_HPP

#define kCrashHandlerEnvVar           "RS_CRASH_HANDLER_PATH"
#define kCrashpadHandlerEnvVar        "RS_CRASHPAD_HANDLER_PATH"

namespace rstudio {
namespace core {

class Error;

namespace crash_handler {

enum class ConfigSource
{
   Default,
   Admin,
   User
};

enum class ProgramMode
{
   Server,
   Desktop
};

// initialize crash handler, starting the crash handler process
// to catch this process's uncaught exceptions if configured to do so
Error initialize(ProgramMode programMode = ProgramMode::Server);

// gets the source of the crash handler configuration
// this can come from the admin or user file, or can be Default to indicate
// that the no configuration file is specified and default settings are used
ConfigSource configSource();

// returns the current setting indicating whether or not crash handling
// is currently enabled
bool isHandlerEnabled();

// explicitly enables/disables crash handling for this particular user
// this overwrites the user settings file on disk
// note: this does not actually start/stop the handler for this process
// and does not take effect until the process is restarted
Error setUserHandlerEnabled(bool handlerEnabled);

// returns whether or not this user has been prompted for permission
// to collect crash dumps
// note: this does not say whether permission has been granted, only
// whether or not the user has been prompted
bool hasUserBeenPromptedForPermission();

// marks a special file which indicates that the user has been prompted
// for permission to collect crash dumps - subsequent calls to hasUserBeenPromptedForPermission
// will return true
Error setUserHasBeenPromptedForPermission();

} // namespace crash_handler
} // namespace core
} // namespace rstudio


#endif // CORE_CRASH_HANDLER_HPP

