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

#include <core/Settings.hpp>

#include <core/json/Json.hpp>

namespace session {

// singleton
class UserSettings;
UserSettings& userSettings();   
   
class UserSettings : boost::noncopyable
{
private:
   UserSettings() {}
   friend UserSettings& userSettings();
   
public:
   // COPYING: boost::noncopyable
   
   // intialize
   core::Error initialize();
   
   // enable batch updates
   void beginUpdate() { settings_.beginUpdate(); }
   void endUpdate() { settings_.endUpdate(); }

   // agreement hash code
   std::string agreementHash() const;
   void setAgreementHash(const std::string& hash) ;
   
   // did we already auto-create the profile?
   bool autoCreatedProfile() const;
   void setAutoCreatedProfile(bool autoCreated) ;

   core::json::Object uiPrefs() const;
   void setUiPrefs(const core::json::Object& prefsObject);
   
   
private:
   core::Settings settings_;
};
   
} // namespace session

#endif // SESSION_USER_SETTINGS_HPP

