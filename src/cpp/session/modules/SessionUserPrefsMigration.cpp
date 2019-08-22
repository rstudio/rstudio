/*
 * SessionUserPrefsMigration.cpp
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

#include "SessionUserPrefsMigration.hpp"

#include <core/json/Json.hpp>
#include <core/Settings.hpp>

#include <session/SessionOptions.hpp>
#include <session/prefs/UserPrefs.hpp>

#include <r/session/RSession.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

void migrateUiPrefs(const json::Object& uiPrefs, json::Object* dest)
{
   std::vector<std::string> keys = userPrefs().allKeys();
   json::Value val;
   for (const auto &key: keys)
   {
      if (key == kPanes)
      {
         // Migrate pane locations
         json::Object::iterator it = uiPrefs.find("pane_config");
         if (it != uiPrefs.end())
         {
            json::Object old = (*it).value().get_obj();
            json::Object panes;
            panes[kPanesQuadrants] = old["panes"];
            panes[kPanesTabSet1] = old["tabSet1"];
            panes[kPanesTabSet2] = old["tabSet2"];
            panes[kPanesConsoleLeftOnTop] = old["consoleLeftOnTop"];
            panes[kPanesConsoleRightOnTop] = old["consoleRightOnTop"];

            (*dest)[key] = panes;
         }
      }
      else if (key == kEditorKeybindings)
      {
         // Migrate editor keybindings (was a bunch of booleans)
         std::string keybindings;
         bool vim, emacs, sublime;
         json::getOptionalParam(uiPrefs, "use_vim_mode", false, &vim);
         json::getOptionalParam(uiPrefs, "enable_emacs_keybindings", false, &emacs);
         json::getOptionalParam(uiPrefs, "enable_sublime_keybindings", false, &sublime);
         if (vim)
         {
            keybindings = kEditorKeybindingsVim;
         }
         else if (emacs)
         {
            keybindings = kEditorKeybindingsEmacs;
         }
         else if (sublime)
         {
            keybindings = kEditorKeybindingsSublime;
         }

         if (!keybindings.empty())
         {
            (*dest)[key] = keybindings;
         }
      }
      else if (key == kFoldStyle ||
               key == kJobsTabVisibility ||
               key == kLauncherJobsSort ||
               key == kBusyDetection ||
               key == kDocOutlineShow)
      {
         // These preferences don't migrate
         continue;
      }
      else if (key == kEditorTheme)
      {
         json::Object::iterator it = uiPrefs.find("rstheme");
         if (it != uiPrefs.end())
         {
            std::string theme;
            json::getOptionalParam((*it).value().get_obj(), "name", std::string(), &theme);
            if (!theme.empty())
            {
               // We only need to preserve the name of the theme; the other theme properties will be
               // automatically derived
               (*dest)[key] = theme;
            }
         }
      }
      else if (key == kGlobalTheme)
      {
         // Migrate the global theme (formerly called "flat theme")
         std::string theme;
         json::getOptionalParam(uiPrefs, "flat_theme", std::string(), &theme);
         if (!theme.empty())
         {
            (*dest)[key] = theme;
         }
      }
      else
      {
         // Most UI prefs can just be copied to the new system without modification.
         json::Object::iterator it = uiPrefs.find(key);
         if (it != uiPrefs.end())
         {
            (*dest)[key] = (*it).value();
         }
      }
   }
}

} // anonymous namespace


core::Error migratePrefs(const FilePath& src)
{
   json::Object srcPrefs;
   json::Object destPrefs;

   // Read the old settings file
   Settings settings; 
   Error err = settings.initialize(src);
   if (err)
      return err;

   // Migrate UI prefs if present.
   std::string uiPrefs = settings.get("uiPrefs");
   if (!uiPrefs.empty())
   {
      json::Value val;
      err = json::parse(uiPrefs, ERROR_LOCATION, &val);
      if (err)
      {
         LOG_ERROR(err);
      }
      else if (val.type() == json::ObjectType)
      {
         migrateUiPrefs(val.get_obj(), &destPrefs);
      }
   }

   // Migrate .rprofile execution
   bool rprofileOnResume = settings.getBool("rprofileOnResume",
         session::options().rProfileOnResumeDefault());
   userPrefs().setRunRprofileOnResume(rprofileOnResume);

   // Migrate save action
   int saveAction = settings.getInt("saveAction", 
         session::options().saveActionDefault());
   if (saveAction == r::session::kSaveActionSave)
      destPrefs[kSaveWorkspace] = kSaveWorkspaceAlways;
   else if (saveAction == r::session::kSaveActionNoSave)
      destPrefs[kSaveWorkspace] = kSaveWorkspaceNever;
   else if (saveAction == r::session::kSaveActionAsk)
      destPrefs[kSaveWorkspace] = kSaveWorkspaceAsk;

   // Migrate profile run setting
   destPrefs[kRunRprofileOnResume] = settings.getBool("rprofileOnResume",
         session::options().rProfileOnResumeDefault());

   // Migrate line options
   int endings = settings.getInt("lineEndingConversion", string_utils::LineEndingNative);
   switch (endings)
   {
      case string_utils::LineEndingWindows:
         destPrefs[kLineEndingConversion] = kLineEndingConversionWindows;
         break;
      case string_utils::LineEndingPosix:
         destPrefs[kLineEndingConversion] = kLineEndingConversionPosix;
         break;
      case string_utils::LineEndingNative:
         destPrefs[kLineEndingConversion] = kLineEndingConversionNative;
         break;
      case string_utils::LineEndingPassthrough:
         destPrefs[kLineEndingConversion] = kLineEndingConversionPassthrough;
         break;
   }
   destPrefs[kUseNewlinesInMakefiles] = settings.getBool("newlineInMakefiles", true);

   // Migrate history options
   destPrefs[kAlwaysSaveHistory] = settings.getBool("alwaysSaveHistory", true);
   destPrefs[kRemoveHistoryDuplicates] = settings.getBool("removeHistoryDuplicates", false);

   // Migrate RStudio Server Pro options
   destPrefs[kShowUserHomePage] = settings.get("showUserHomePage", kShowUserHomePageSessions);
   destPrefs[kReuseSessionsForProjectLinks] = 
      settings.getBool("reuseSessionsForProjectLinks", true);

   // Migrate version control settings
   std::string terminalPath = settings.get("vcsTerminalPath");
   if (!terminalPath.empty())
      destPrefs[kTerminalPath] = terminalPath;
   std::string gitPath = settings.get("vcsGitExePath");
   if (!gitPath.empty())
      destPrefs[kGitExePath] = gitPath;
   std::string svnPath = settings.get("vcsSvnExePath");
   if (!svnPath.empty())
      destPrefs[kSvnExePath] = svnPath;
   destPrefs[kVcsEnabled] = settings.getBool("vcsEnabled", true);

   // Migrate other settings
   destPrefs[kUseDevtools] = settings.getBool("useDevtools", true);
   destPrefs[kClangVerbose] = settings.getInt("clangVerbose", 0);
   destPrefs[kHideObjectFiles] = settings.getBool("hideObjectFiles", true);
   destPrefs[kViewDirAfterRCmdCheck] = settings.getBool("viewDirAfterRCmdCheck", false);
   destPrefs[kUseInternet2] = settings.getBool("useInternet2", true);
   destPrefs[kLatexShellEscape] = settings.getBool("enableLaTeXShellEscape", false);
   destPrefs[kCleanTexi2dviOutput] = settings.getBool("cleanTexi2DviOutput", true);
   destPrefs[kUseSecureDownload] = settings.getBool("securePackageDownload", true);

   return userPrefs().writeLayer(PREF_LAYER_USER, destPrefs); 
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

