/*
 * SessionUserSettings.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_USER_SETTINGS_HPP
#define SESSION_USER_SETTINGS_HPP

#include <string>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/signal.hpp>

#include <core/Settings.hpp>
#include <core/FilePath.hpp>

#include <core/json/Json.hpp>

#include <core/system/FileChangeEvent.hpp>

namespace session {

// singleton
class UserSettings;
UserSettings& userSettings();   

struct CRANMirror
{
   std::string name;
   std::string host;
   std::string url;
   std::string country;
};

struct BioconductorMirror
{
   std::string name;
   std::string url;
};

class UserSettings : boost::noncopyable
{
private:
   UserSettings() {}
   friend UserSettings& userSettings();

public:
   boost::signal<void()> onChanged;

public:
   // COPYING: boost::noncopyable
   
   // intialize
   core::Error initialize();
   
   // enable batch updates
   void beginUpdate() { settings_.beginUpdate(); }
   void endUpdate() { settings_.endUpdate(); }

   // context id
   std::string contextId() const;
   void setContextId(const std::string& contextId);

   // old context-id (for migrating untitled files)
   std::string oldContextId() const;

   // agreement hash code
   std::string agreementHash() const;
   void setAgreementHash(const std::string& hash) ;
   
   // did we already auto-create the profile?
   bool autoCreatedProfile() const;
   void setAutoCreatedProfile(bool autoCreated) ;

   core::json::Object uiPrefs() const;
   void setUiPrefs(const core::json::Object& prefsObject);

   // readers for ui prefs
   bool useSpacesForTab() const;
   int numSpacesForTab() const;
   std::string defaultEncoding() const;
   std::string defaultSweaveEngine() const;
   std::string defaultLatexProgram() const;
   bool alwaysEnableRnwCorcordance() const;

   bool alwaysRestoreLastProject() const;
   void setAlwaysRestoreLastProject(bool alwaysRestore);

   int saveAction() const;
   void setSaveAction(int saveAction);

   bool loadRData() const;
   void setLoadRData(bool loadRData);

   core::FilePath initialWorkingDirectory() const;
   void setInitialWorkingDirectory(const core::FilePath& filePath);

   bool alwaysSaveHistory() const;
   void setAlwaysSaveHistory(bool alwaysSave);

   bool removeHistoryDuplicates() const;
   void setRemoveHistoryDuplicates(bool removeDuplicates);

   CRANMirror cranMirror() const;
   void setCRANMirror(const CRANMirror& cranMirror);

   BioconductorMirror bioconductorMirror() const;
   void setBioconductorMirror(const BioconductorMirror& bioconductorMirror);

   bool vcsEnabled() const;
   void setVcsEnabled(bool enabled);

   core::FilePath gitExePath() const;
   void setGitExePath(const core::FilePath& gitExePath);

   core::FilePath svnExePath() const;
   void setSvnExePath(const core::FilePath& svnExePath);

   core::FilePath vcsTerminalPath() const;
   void setVcsTerminalPath(const core::FilePath& terminalPath);

   bool vcsUseGitBash() const;
   void setVcsUseGitBash(bool useGitBash);

   bool useTexi2Dvi() const;
   void setUsetexi2Dvi(bool useTexi2Dvi);

   bool cleanTexi2DviOutput() const;
   void setCleanTexi2DviOutput(bool cleanTexi2DviOutput);

   bool enableLaTeXShellEscape() const;
   void setEnableLaTeXShellEscape(bool enableShellEscape);

   std::string spellingLanguage() const;
   std::vector<std::string> spellingCustomDictionaries() const;

private:

   void onSettingsFileChanged(
                        const core::system::FileChangeEvent& changeEvent);

   core::FilePath getWorkingDirectoryValue(const std::string& key) const;
   void setWorkingDirectoryValue(const std::string& key,
                                 const core::FilePath& filePath) ;

   void updatePrefsCache(const core::json::Object& uiPrefs) const;

   template <typename T>
   T readUiPref(const boost::scoped_ptr<T>& pPref) const
   {
      if (!pPref)
         updatePrefsCache(uiPrefs());

      return *pPref;
   }

private:
   core::FilePath settingsFilePath_;
   core::Settings settings_;

   // cached prefs values
   mutable boost::scoped_ptr<bool> pUseSpacesForTab_;
   mutable boost::scoped_ptr<int> pNumSpacesForTab_;
   mutable boost::scoped_ptr<std::string> pDefaultEncoding_;
   mutable boost::scoped_ptr<std::string> pDefaultSweaveEngine_;
   mutable boost::scoped_ptr<std::string> pDefaultLatexProgram_;
   mutable boost::scoped_ptr<bool> pAlwaysEnableRnwConcordance_;
   mutable boost::scoped_ptr<std::string> pSpellingLanguage_;
   mutable boost::scoped_ptr<core::json::Array> pSpellingCustomDicts_;
};
   
} // namespace session

#endif // SESSION_USER_SETTINGS_HPP

