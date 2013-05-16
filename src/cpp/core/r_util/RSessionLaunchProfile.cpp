/*
 * RSessionLaunchProfile.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/r_util/RSessionLaunchProfile.hpp>

#include <boost/foreach.hpp>

#include <core/json/JsonRpc.hpp>

namespace core {
namespace r_util {

namespace {

json::Object optionsAsJson(const core::system::Options& options)
{
   json::Object optionsJson;
   BOOST_FOREACH(const core::system::Option& option, options)
   {
      optionsJson[option.first] = option.second;
   }
   return optionsJson;
}

core::system::Options optionsFromJson(const json::Object& optionsJson)
{
   core::system::Options options;
   BOOST_FOREACH(const json::Member& member, optionsJson)
   {
      std::string name = member.first;
      json::Value value = member.second;
      if (value.type() == json::StringType)
         options.push_back(std::make_pair(name, value.get_str()));
   }
   return options;
}

} // anonymous namespace


json::Object sessionLaunchProfileToJson(const SessionLaunchProfile& profile)
{
   json::Object profileJson;
   profileJson["username"] = profile.username;
   profileJson["executablePath"] = profile.executablePath;
   json::Object configJson;
   configJson["args"] = optionsAsJson(profile.config.args);
   configJson["environment"] = optionsAsJson(profile.config.environment);
   configJson["stdStreamBehavior"] = profile.config.stdStreamBehavior;
   configJson["memoryLimitBytes"] = profile.config.memoryLimitBytes;
   configJson["stackLimitBytes"] = profile.config.stackLimitBytes;
   configJson["userProcessesLimit"] = profile.config.userProcessesLimit;
   profileJson["config"] = configJson;
   return profileJson;
}

SessionLaunchProfile sessionLaunchProfileFromJson(
                                           const json::Object& jsonProfile)
{
   SessionLaunchProfile profile;

   // read top level fields
   json::Object configJson;
   Error error = json::readObject(jsonProfile,
                                  "username", &profile.username,
                                  "executablePath", &profile.executablePath,
                                  "config", &configJson);
   if (error)
      LOG_ERROR(error);


   // read config object
   json::Object argsJson, envJson;
   int stdStreamBehavior = 0,
       memoryLimitBytes = -1,
       stackLimitBytes = -1,
       userProcessesLimit = -1;
   error = json::readObject(configJson,
                           "args", &argsJson,
                           "environment", &envJson,
                           "stdStreamBehavior", &stdStreamBehavior,
                           "memoryLimitBytes", &memoryLimitBytes,
                           "stackLimitBytes", &stackLimitBytes,
                           "userProcessesLimit", &userProcessesLimit);
   if (error)
      LOG_ERROR(error);

   // populate config
   profile.config.args = optionsFromJson(argsJson);
   profile.config.environment = optionsFromJson(envJson);
   profile.config.stdStreamBehavior =
            static_cast<core::system::StdStreamBehavior>(stdStreamBehavior);
   profile.config.memoryLimitBytes = memoryLimitBytes;
   profile.config.stackLimitBytes = stackLimitBytes;
   profile.config.userProcessesLimit = userProcessesLimit;

   // return profile
   return profile;
}



} // namespace r_util
} // namespace core 



