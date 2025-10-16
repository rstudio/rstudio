/*
 * UserPrefsComputedLayer.cpp
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

#include "UserPrefsComputedLayer.hpp"

#include <shared_core/json/Json.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserPrefValues.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/RVersionSettings.hpp>

#include <r/session/RSession.hpp>

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
   layer[kGitExePath] = modules::git::detectedGitExePath().getAbsolutePath();
   layer[kSvnExePath] = modules::svn::detectedSvnExePath().getAbsolutePath();

   // System terminal path (Linux) -------------------------------------------
   layer[kTerminalPath] = detectedTerminalPath().getAbsolutePath();

   // Initial working directory ----------------------------------------------
   layer[kInitialWorkingDirectory] = session::options().defaultWorkingDir();

   // SSH key ----------------------------------------------------------------
   FilePath sshKeyDir = modules::source_control::defaultSshKeyDir();

   // Github recommends using ed25519, so look for that first
   std::string keyFile("id_ed25519");
   FilePath rsaSshKeyPath = sshKeyDir.completeChildPath(keyFile);
   if (!rsaSshKeyPath.exists())
   {
      keyFile = "id_rsa";
      rsaSshKeyPath = sshKeyDir.completeChildPath(keyFile);
   }
   layer[kRsaKeyPath] = rsaSshKeyPath.getAbsolutePath();
   layer["have_rsa_key"] = rsaSshKeyPath.exists();

   // provide name of public key file
   layer["rsa_key_file"] = keyFile + ".pub";

   // R versions -------------------------------------------------------------
   RVersionSettings versionSettings(module_context::userScratchPath(),
                                    FilePath(options().getOverlayOption(
                                                kSessionSharedStoragePath)));
   json::Object defaultRVersionJson;
   defaultRVersionJson["version"] = versionSettings.defaultRVersion();
   defaultRVersionJson["r_home"] = versionSettings.defaultRVersionHome();
   defaultRVersionJson["label"] = versionSettings.defaultRVersionLabel();
   layer[kDefaultRVersion] = defaultRVersionJson;

   #ifdef __linux__
   // uncomment this to default to web-based file dialogs on Linux Desktop
   // layer[kNativeFileDialogs] = false;
   #endif

   // Synctex viewer ----------------------------------------------------------
#ifdef __APPLE__
# define kDefaultDesktopPdfPreviewer kPdfPreviewerRstudio
#else
# define kDefaultDesktopPdfPreviewer kPdfPreviewerDesktopSynctex
#endif
   
   layer[kPdfPreviewer] = (session::options().programMode() == kSessionProgramModeDesktop)
         ? kDefaultDesktopPdfPreviewer
         : kPdfPreviewerRstudio;

   // Spelling ----------------------------------------------------------------
   layer["spelling"] =
         session::modules::spelling::spellingPrefsContextAsJson();

   // Other session defaults from rsession.conf -------------------------------

   int saveAction = session::options().saveActionDefault();
   if (saveAction == r::session::kSaveActionSave)
      layer[kSaveWorkspace] = kSaveWorkspaceAlways;
   else if (saveAction == r::session::kSaveActionNoSave)
      layer[kSaveWorkspace] = kSaveWorkspaceNever;
   else if (saveAction == r::session::kSaveActionAsk)
      layer[kSaveWorkspace] = kSaveWorkspaceAsk;

   layer[kRunRprofileOnResume] = session::options().rProfileOnResumeDefault();
   
   cache_ = boost::make_shared<core::json::Object>(layer);
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


