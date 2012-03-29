/*
 * SessionUserSettings.cpp
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

#include <session/SessionUserSettings.hpp>

#include <iostream>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RConsoleHistory.hpp>

using namespace core ;

namespace session {  
   
#define kAgreementPrefix "agreement."
   
namespace {
const char * const kContextId ="contextIdentifier";
const char * const kAgreementHash = kAgreementPrefix "agreedToHash";
const char * const kAutoCreatedProfile = "autoCreatedProfile";
const char * const kUiPrefs = "uiPrefs";
const char * const kAlwaysRestoreLastProject = "restoreLastProject";
const char * const kSaveAction = "saveAction";
const char * const kLoadRData = "loadRData";
const char * const kInitialWorkingDirectory = "initialWorkingDirectory";
const char * const kCRANMirrorName = "cranMirrorName";
const char * const kCRANMirrorHost = "cranMirrorHost";
const char * const kCRANMirrorUrl = "cranMirrorUrl";
const char * const kCRANMirrorCountry = "cranMirrorCountry";
const char * const kBioconductorMirrorName = "bioconductorMirrorName";
const char * const kBioconductorMirrorUrl = "bioconductorMirrorUrl";
const char * const kAlwaysSaveHistory = "alwaysSaveHistory";
const char * const kRemoveHistoryDuplicates = "removeHistoryDuplicates";

void setCRANReposOption(const std::string& url)
{
   if (!url.empty())
   {
      Error error = r::exec::RFunction(".rs.setCRANRepos", url).call();
      if (error)
         LOG_ERROR(error);
   }
}

void setBioconductorReposOption(const std::string& mirror)
{
   if (!mirror.empty())
   {
      Error error = r::options::setOption("BioC_mirror", mirror);
      if (error)
         LOG_ERROR(error);
   }
}



} // anonymous namespace
   
UserSettings& userSettings()
{
   static UserSettings instance ;
   return instance;
}
   
Error UserSettings::initialize()
{
   // calculate settings file path
   FilePath settingsDir = module_context::registerMonitoredUserScratchDir(
              "user-settings",
              boost::bind(&UserSettings::onSettingsFileChanged, this, _1));
   settingsFilePath_ = settingsDir.complete("user-settings");

   // if it doesn't exist see if we can migrate an old user settings
   if (!settingsFilePath_.exists())
   {
      FilePath oldSettingsPath =
            module_context::userScratchPath().complete("user-settings");
      if (oldSettingsPath.exists())
         oldSettingsPath.move(settingsFilePath_);
   }

   // read the settings
   Error error = settings_.initialize(settingsFilePath_);
   if (error)
      return error;

   // make sure we have a context id
   if (contextId().empty())
      setContextId(module_context::generateShortenedUuid());

   return Success();
}

void UserSettings::onSettingsFileChanged(
                     const core::system::FileChangeEvent& changeEvent)
{
   // ensure this is for our target file
   if (settingsFilePath_.absolutePath() !=
       changeEvent.fileInfo().absolutePath())
   {
      return;
   }

   // re-read the settings from disk
   Error error = settings_.initialize(settingsFilePath_);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // update prefs cache
   updatePrefsCache(uiPrefs());

   // set underlying R repos options
   std::string cranMirrorURL = cranMirror().url;
   if (!cranMirrorURL.empty())
      setCRANReposOption(cranMirrorURL);
   std::string bioconductorMirrorURL = settings_.get(kBioconductorMirrorUrl);
   if (!bioconductorMirrorURL.empty())
      setBioconductorReposOption(bioconductorMirrorURL);

   // update remove dups in underlying R session
   using namespace r::session;
   consoleHistory().setRemoveDuplicates(removeHistoryDuplicates());

   // fire event so others can react appropriately
   onChanged();
}


std::string UserSettings::contextId() const
{
   return settings_.get(kContextId);
}

std::string UserSettings::oldContextId() const
{
   return settings_.get("contextId");
}

void UserSettings::setContextId(const std::string& contextId)
{
   settings_.set(kContextId, contextId);
}

std::string UserSettings::agreementHash() const
{
   return settings_.get(kAgreementHash, "");
}
   
void UserSettings::setAgreementHash(const std::string& hash)
{
   settings_.set(kAgreementHash, hash);
}

  
bool UserSettings::autoCreatedProfile() const
{
   return settings_.getBool(kAutoCreatedProfile, false);
}
   
void UserSettings::setAutoCreatedProfile(bool autoCreated)
{
   settings_.set(kAutoCreatedProfile, autoCreated);
}

core::json::Object UserSettings::uiPrefs() const
{
   std::string value = settings_.get(kUiPrefs, "{}");
   json::Value jsonValue;
   bool success = core::json::parse(value, &jsonValue);
   if (success)
      return jsonValue.get_obj();
   else
      return json::Object();
}

void UserSettings::setUiPrefs(const core::json::Object& prefsObject)
{
   std::ostringstream output;
   json::writeFormatted(prefsObject, output);
   settings_.set(kUiPrefs, output.str());

   updatePrefsCache(prefsObject);
}

namespace {

template <typename T>
T readPref(const json::Object& prefs,
           const std::string& name,
           const T& defaultValue)
{
   T value;
   Error error = json::readObject(prefs,
                                  name,
                                  defaultValue,
                                  &value);
   if (error)
   {
      value = defaultValue;
      error.addProperty("pref", name);
      LOG_ERROR(error);
   }

   return value;
}

} // anonymous namespace

void UserSettings::updatePrefsCache(const json::Object& prefs) const
{ 
   bool useSpacesForTab = readPref<bool>(prefs, "use_spaces_for_tab", true);
   pUseSpacesForTab_.reset(new bool(useSpacesForTab));

   int numSpacesForTab = readPref<int>(prefs, "num_spaces_for_tab", 2);
   pNumSpacesForTab_.reset(new int(numSpacesForTab));

   std::string enc = readPref<std::string>(prefs, "default_encoding", "");
   pDefaultEncoding_.reset(new std::string(enc));

   std::string sweave = readPref<std::string>(prefs, "default_sweave_engine", "Sweave");
   pDefaultSweaveEngine_.reset(new std::string(sweave));

   std::string latex = readPref<std::string>(prefs, "default_latex_program", "pdfLaTeX");
   pDefaultLatexProgram_.reset(new std::string(latex));

   bool alwaysEnableRnwConcordance = readPref<bool>(prefs, "always_enable_concordance", true);
   pAlwaysEnableRnwConcordance_.reset(new bool(alwaysEnableRnwConcordance));
}


// readers for ui prefs

bool UserSettings::useSpacesForTab() const
{
   return readUiPref<bool>(pUseSpacesForTab_);
}

int UserSettings::numSpacesForTab() const
{
   return readUiPref<int>(pNumSpacesForTab_);
}

std::string UserSettings::defaultEncoding() const
{
   return readUiPref<std::string>(pDefaultEncoding_);
}

std::string UserSettings::defaultSweaveEngine() const
{
   return readUiPref<std::string>(pDefaultSweaveEngine_);
}

std::string UserSettings::defaultLatexProgram() const
{
   return readUiPref<std::string>(pDefaultLatexProgram_);
}

bool UserSettings::alwaysEnableRnwCorcordance() const
{
   return readUiPref<bool>(pAlwaysEnableRnwConcordance_);
}

bool UserSettings::alwaysRestoreLastProject() const
{
   return settings_.getBool(kAlwaysRestoreLastProject, true);
}

void UserSettings::setAlwaysRestoreLastProject(bool alwaysRestore)
{
   settings_.set(kAlwaysRestoreLastProject, alwaysRestore);
}

int UserSettings::saveAction() const
{
   return settings_.getInt(kSaveAction, -1);
}

void UserSettings::setSaveAction(int saveAction)
{
   settings_.set(kSaveAction, saveAction);
}


bool UserSettings::loadRData() const
{
   return settings_.getBool(kLoadRData, true);
}

void UserSettings::setLoadRData(bool loadRData)
{
   settings_.set(kLoadRData, loadRData);
}

core::FilePath UserSettings::initialWorkingDirectory() const
{
   return getWorkingDirectoryValue(kInitialWorkingDirectory);
}

CRANMirror UserSettings::cranMirror() const
{
   CRANMirror mirror;
   mirror.name = settings_.get(kCRANMirrorName);
   mirror.host = settings_.get(kCRANMirrorHost);
   mirror.url = settings_.get(kCRANMirrorUrl);
   mirror.country = settings_.get(kCRANMirrorCountry);
   return mirror;
}

void UserSettings::setCRANMirror(const CRANMirror& mirror)
{
   settings_.set(kCRANMirrorName, mirror.name);
   settings_.set(kCRANMirrorHost, mirror.host);
   settings_.set(kCRANMirrorUrl, mirror.url);
   settings_.set(kCRANMirrorCountry, mirror.country);

   setCRANReposOption(mirror.url);
}

BioconductorMirror UserSettings::bioconductorMirror() const
{
   BioconductorMirror mirror ;

   mirror.name = settings_.get(kBioconductorMirrorName);
   if (!mirror.name.empty())
   {
      mirror.url = settings_.get(kBioconductorMirrorUrl);
   }
   else
   {
      mirror.name = "Seattle (USA)";
      mirror.url = "http://www.bioconductor.org";
   }

   return mirror;
}

void UserSettings::setBioconductorMirror(
                        const BioconductorMirror& bioconductorMirror)
{
   settings_.set(kBioconductorMirrorName, bioconductorMirror.name);
   settings_.set(kBioconductorMirrorUrl, bioconductorMirror.url);

   setBioconductorReposOption(bioconductorMirror.url);
}

bool UserSettings::vcsEnabled() const
{
   return settings_.getBool("vcsEnabled", true);
}

void UserSettings::setVcsEnabled(bool enabled)
{
   settings_.set("vcsEnabled", enabled);
}

FilePath UserSettings::gitExePath() const
{
   std::string dir = settings_.get("vcsGitExePath");
   if (!dir.empty())
      return FilePath(dir);
   else
      return FilePath();
}

void UserSettings::setGitExePath(const FilePath& gitExePath)
{
   settings_.set("vcsGitExePath", gitExePath.absolutePath());
}

FilePath UserSettings::svnExePath() const
{
   std::string dir = settings_.get("vcsSvnExePath");
   if (!dir.empty())
      return FilePath(dir);
   else
      return FilePath();
}

void UserSettings::setSvnExePath(const FilePath& svnExePath)
{
   settings_.set("vcsSvnExePath", svnExePath.absolutePath());
}

FilePath UserSettings::vcsTerminalPath() const
{
   std::string dir = settings_.get("vcsTerminalPath");
   if (!dir.empty())
      return FilePath(dir);
   else
      return FilePath();
}

void UserSettings::setVcsTerminalPath(const FilePath& terminalPath)
{
   settings_.set("vcsTerminalPath", terminalPath.absolutePath());
}

bool UserSettings::vcsUseGitBash() const
{
   return settings_.getBool("vcsUseGitBash", true);
}

void UserSettings::setVcsUseGitBash(bool useGitBash)
{
   settings_.set("vcsUseGitBash", useGitBash);
}

bool UserSettings::useTexi2Dvi() const
{
   return settings_.getBool("useTexi2Dvi", true);
}

void UserSettings::setUsetexi2Dvi(bool useTexi2Dvi)
{
   settings_.set("useTexi2Dvi", useTexi2Dvi);
}

bool UserSettings::cleanTexi2DviOutput() const
{
   return settings_.getBool("cleanTexi2DviOutput", true);
}

void UserSettings::setCleanTexi2DviOutput(bool cleanTexi2DviOutput)
{
   settings_.set("cleanTexi2DviOutput", cleanTexi2DviOutput);
}

bool UserSettings::enableLaTeXShellEscape() const
{
   return settings_.getBool("enableLaTeXShellEscape", false);
}

void UserSettings::setEnableLaTeXShellEscape(bool enableShellEscape)
{
   settings_.set("enableLaTeXShellEscape", enableShellEscape);
}

bool UserSettings::alwaysSaveHistory() const
{
   return settings_.getBool(kAlwaysSaveHistory, true);
}

void UserSettings::setAlwaysSaveHistory(bool alwaysSave)
{
   settings_.set(kAlwaysSaveHistory, alwaysSave);
}

bool UserSettings::removeHistoryDuplicates() const
{
   return settings_.getBool(kRemoveHistoryDuplicates, false);
}


void UserSettings::setRemoveHistoryDuplicates(bool removeDuplicates)
{
   settings_.set(kRemoveHistoryDuplicates, removeDuplicates);

   // update in underlying R session
   r::session::consoleHistory().setRemoveDuplicates(removeDuplicates);
}

void UserSettings::setInitialWorkingDirectory(const FilePath& filePath)
{
   setWorkingDirectoryValue(kInitialWorkingDirectory, filePath);
}

FilePath UserSettings::getWorkingDirectoryValue(const std::string& key) const
{
   return module_context::resolveAliasedPath(settings_.get(key, "~"));
}


void UserSettings::setWorkingDirectoryValue(const std::string& key,
                                            const FilePath& filePath)
{
   if (module_context::createAliasedPath(filePath) == "~")
      settings_.set(key, std::string("~"));
   else
      settings_.set(key, filePath.absolutePath());
}

} // namespace session
