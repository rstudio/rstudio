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
const char * const kContextId ="contextId";
const char * const kAgreementHash = kAgreementPrefix "agreedToHash";
const char * const kAutoCreatedProfile = "autoCreatedProfile";
const char * const kUiPrefs = "uiPrefs";
const char * const kAlwaysRestoreLastProject = "restoreLastProject";
const char * const kLastProjectPath = "lastProjectPath";
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
   FilePath scratchPath = session::options().userScratchPath();
   FilePath settingsPath = scratchPath.complete("user-settings");

   Error error = settings_.initialize(settingsPath);
   if (error)
      return error;

   // make sure we have a context id
   if (contextId().empty())
      setContextId(core::system::generateUuid());

   return Success();
}

std::string UserSettings::contextId() const
{
   return settings_.get(kContextId);
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

bool UserSettings::alwaysRestoreLastProject() const
{
   return settings_.getBool(kAlwaysRestoreLastProject, true);
}

void UserSettings::setAlwaysRestoreLastProject(bool alwaysRestore)
{
   settings_.set(kAlwaysRestoreLastProject, alwaysRestore);
}


FilePath UserSettings::lastProjectPath() const
{
   std::string path = settings_.get(kLastProjectPath);
   if (!path.empty())
      return FilePath(path);
   else
      return FilePath();
}

void UserSettings::setLastProjectPath(const FilePath& lastProjectPath)
{
   settings_.set(kLastProjectPath, lastProjectPath.absolutePath());
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
