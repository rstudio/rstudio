/*
 * SessionUserPrefsMigration.cpp
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

#include "SessionUserPrefsMigration.hpp"

#include <shared_core/json/Json.hpp>
#include <core/Settings.hpp>

#include <session/SessionOptions.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

#include <r/session/RSession.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

/**
 * Migrates the "UI prefs" from RStudio 1.2 (and older) to the RStudio 1.3+. Both are JSON objects,
 * so most values are just copied as-is, but a number of preferences require special treatment since
 * they were reworded or reorganized to be more user-friendly in RStudio 1.3.
 */
void migrateUiPrefs(const json::Object& uiPrefs, json::Object* dest)
{
   std::vector<std::string> keys = userPrefs().allKeys();
   json::Value val;
   for (const auto &key: keys)
   {
      if (key == kPanes)
      {
         // Migrate pane locations
         json::Object::Iterator it = uiPrefs.find("pane_config");
         if (it != uiPrefs.end())
         {
            json::Object old = (*it).getValue().getObject();
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
               key == kDocOutlineShow ||
               key == kRmdViewerType ||
               key == kShinyViewerType ||
               key == kPlumberViewerType ||
               key == kAnsiConsoleMode)
      {
         // These preferences don't migrate
         continue;
      }
      else if (key == kEditorTheme)
      {
         json::Object::Iterator it = uiPrefs.find("rstheme");
         if (it != uiPrefs.end())
         {
            std::string theme;
            json::getOptionalParam((*it).getValue().getObject(), "name", std::string(), &theme);
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
         json::Object::Iterator it = uiPrefs.find(key);
         if (it != uiPrefs.end())
         {
            (*dest)[key] = (*it).getValue();
         }
      }
   }
}

} // anonymous namespace


/**
 * Migrates RStudio 1.2 (or older) preferences from a user-settings file into the new RStudio 1.3+
 * preferences system. This is a one-time operation, performed only when we detect that no
 * preferences exist yet. We can remove this code if RStudio 1.2 and below usage ever drops below a
 * significant threshold.
 */
core::Error migratePrefs(const FilePath& src)
{
   json::Object srcPrefs;
   json::Object destPrefs;
   json::Object destState;

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
      err = val.parse(uiPrefs);
      if (err)
      {
         log::logError(err, ERROR_LOCATION);
      }
      else if (val.getType() == json::Type::OBJECT)
      {
         // Migrate UI prefs; guard against JSON exceptions (there's a lot of potential for these
         // due to malformed or invalid prefs files from the wild)
         try
         {
            migrateUiPrefs(val.getObject(), &destPrefs);
         }
         CATCH_UNEXPECTED_EXCEPTION
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

   // Migrate state 
   std::string contextId = settings.get("contextIdentifier");
   if (!contextId.empty())
      destState[kContextId] = contextId;
   int handlerType = settings.getInt("errorHandlerType", 1);
   if (handlerType == 0)
      destState[kErrorHandlerType] = kErrorHandlerTypeMessage;
   else if (handlerType == 1)
      destState[kErrorHandlerType] = kErrorHandlerTypeTraceback;
   else if (handlerType == 2)
      destState[kErrorHandlerType] = kErrorHandlerTypeBreak;
   else if (handlerType == 3)
      destState[kErrorHandlerType] = kErrorHandlerTypeCustom;
   else if (handlerType == 4)
      destState[kErrorHandlerType] = kErrorHandlerTypeNotebook;

   // Commit migrated state (state is runtime recoverable so don't hard fail)
   err = userState().writeLayer(STATE_LAYER_USER, destState);
   if (err)
      LOG_ERROR(err);

   // Write the accumulated preferences to our user prefs layer
   return userPrefs().writeLayer(PREF_LAYER_USER, destPrefs);
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

