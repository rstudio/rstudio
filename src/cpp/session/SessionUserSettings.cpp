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

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>

#include <r/session/RSession.hpp>

using namespace core ;

namespace session {  
   
#define kAgreementPrefix "agreement."
   
namespace {
const char * const kAgreementHash = kAgreementPrefix "agreedToHash";
const char * const kAutoCreatedProfile = "autoCreatedProfile";
const char * const kUiPrefs = "uiPrefs";
const char * const kSaveAction = "saveAction";
const char * const kLoadRData = "loadRData";
const char * const kInitialWorkingDirectory = "initialWorkingDirectory";
const char * const kCRANMirrorName = "cranMirrorName";
const char * const kCRANMirrorHost = "cranMirrorHost";
const char * const kCRANMirrorUrl = "cranMirrorUrl";
const char * const kCRANMirrorCountry = "cranMirrorCountry";
}
   
UserSettings& userSettings()
{
   static UserSettings instance ;
   return instance;
}
   
Error UserSettings::initialize()
{
   FilePath scratchPath = session::options().userScratchPath();
   FilePath settingsPath = scratchPath.complete("user-settings");
   return settings_.initialize(settingsPath);
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
}

int UserSettings::saveAction() const
{
   return settings_.getInt(kSaveAction, -1);
}

void UserSettings::setSaveAction(int saveAction)
{
   settings_.set(kSaveAction, saveAction);
   applySaveAction();
}

void UserSettings::applySaveAction() const
{
   r::session::setSaveAction(saveAction());
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
}

void UserSettings::setInitialWorkingDirectory(const core::FilePath& filePath)
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
