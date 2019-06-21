/*
 * UserPrefsComputedLayer.cpp
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

#include "UserPrefsComputedLayer.hpp"

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserPrefValues.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/RVersionSettings.hpp>

#include <core/json/Json.hpp>
#include <core/CrashHandler.hpp>

#include "../modules/SessionVCS.hpp"
#include "../modules/SessionSVN.hpp"
#include "../modules/SessionSpelling.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserPrefsComputedLayer::UserPrefsComputedLayer():
   PrefLayer(kUserPrefsComputedLayer)
{
}

Error UserPrefsComputedLayer::readPrefs()
{
   json::Object layer;

   // VCS executable paths ---------------------------------------------------
   layer[kGitExePath] = modules::git::detectedGitExePath().absolutePath();
   layer[kSvnExePath] = modules::svn::detectedSvnExePath().absolutePath();

   // System terminal path (Linux) -------------------------------------------
   layer[kTerminalPath] = detectedTerminalPath().absolutePath();

   // Initial working directory ----------------------------------------------
   layer[kInitialWorkingDirectory] = session::options().defaultWorkingDir();

   // SSH key ----------------------------------------------------------------
   FilePath sshKeyDir = modules::source_control::defaultSshKeyDir();
   FilePath rsaSshKeyPath = sshKeyDir.childPath("id_rsa");
   layer[kRsaKeyPath] = rsaSshKeyPath.absolutePath();
   layer["have_rsa_key"] = rsaSshKeyPath.exists();

   // Crash reporting --------------------------------------------------------
   layer["enable_crash_reporting"] = crash_handler::isHandlerEnabled();

   // R versions -------------------------------------------------------------
   RVersionSettings versionSettings(module_context::userScratchPath(),
                                    FilePath(options().getOverlayOption(
                                                kSessionSharedStoragePath)));
   json::Object defaultRVersionJson;
   defaultRVersionJson["version"] = versionSettings.defaultRVersion();
   defaultRVersionJson["r_home"] = versionSettings.defaultRVersionHome();
   layer["default_r_version"] = defaultRVersionJson;

   // Spelling ----------------------------------------------------------------
   layer["spelling"] =
         session::modules::spelling::spellingPrefsContextAsJson();

   cache_ = boost::make_shared<core::json::Object>(layer);
   return Success();
}

core::Error UserPrefsComputedLayer::validatePrefs()
{
   return Success();
}

core::Error UserPrefsComputedLayer::writePrefs(const core::json::Object &prefs)
{
   Error error;

   // enable or disable crash reporting - we only do this in
   // desktop mode (as it should not be overrideable in server mode)
   // and only if we are currently configured to use the user setting
   // (meaning no admin settings exist)
   if (crash_handler::configSource() == crash_handler::ConfigSource::User &&
       session::options().programMode() == kSessionProgramModeDesktop)
   {
      auto it = prefs.find("enable_crash_reporting");
      if (it != prefs.end())
      {
         error = crash_handler::setUserHandlerEnabled((*it).value().get_bool());
         if (error)
            return error;
      }
   }
   return Success();
}

// Try to detect a terminal on linux desktop
FilePath UserPrefsComputedLayer::detectedTerminalPath()
{
#if defined(_WIN32) || defined(__APPLE__)
   return FilePath();
#else
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      std::vector<FilePath> terminalPaths;
      terminalPaths.push_back(FilePath("/usr/bin/gnome-terminal"));
      terminalPaths.push_back(FilePath("/usr/bin/konsole"));
      terminalPaths.push_back(FilePath("/usr/bin/xfce4-terminal"));
      terminalPaths.push_back(FilePath("/usr/bin/xterm"));

      for (const FilePath& terminalPath : terminalPaths)
      {
         if (terminalPath.exists())
            return terminalPath;
      }

      return FilePath();
   }
   else
   {
      return FilePath();
   }
#endif
}

} // namespace prefs
} // namespace session
} // namespace rstudio

