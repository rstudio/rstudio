/*
 * SessionConfigFile.cpp
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


#include "SessionConfigFile.hpp"

#include <shared_core/Error.hpp>
#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>
#include <core/Exec.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

#include <core/system/Xdg.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace config_file {

namespace {

enum class ConfigErrorCode
{
   SUCCESS = 0,
   READ_ERROR = 1,
   WRITE_ERROR = 2
};

Error configError(ConfigErrorCode code, const Error& cause, const ErrorLocation& location)
{
   static constexpr const char* errorName = { "config_error" };
   switch (code)
   {
      case ConfigErrorCode::SUCCESS:
         return Success();
      case ConfigErrorCode::READ_ERROR:
         return Error(errorName, static_cast<int>(code), "Error while reading config file.", cause, location);
      case ConfigErrorCode::WRITE_ERROR:
         return Error(errorName, static_cast<int>(code), "Error while writing config file.", cause, location);
      default:
         return Error(errorName, static_cast<int>(code), "Unknown config error.", cause, location);
   }
}

// Writes JSON configuration data to the user configuration folder.
Error writeConfigJSON(const core::json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(false);
   
   std::string path;
   json::Object object;
   Error error = json::readParams(request.params, &path, &object);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // Compute the user config directory path, and ensure it exists.
   FilePath filePath = core::system::xdg::userConfigDir().completePath(path);
   error = filePath.getParent().ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // Write the new configuration data.
   error = writeStringToFile(filePath, object.writeFormatted());
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   pResponse->setResult(true);
   return Success();
}

// Reads and merges JSON configuration data from the user and system folders.
Error readConfigJSON(const core::json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(json::Object());
   
   std::string path;
   bool logErrorIfNotFound;
   Error error = json::readParams(request.params, &path, &logErrorIfNotFound);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // First, check for the user config file at the XDG location
   FilePath userConfig = core::system::xdg::userConfigDir().completePath(path);
   if (!userConfig.exists())
   {
      // If it's not there, check the legacy location (RStudio 1.2 and prior stored per-user data in
      // this hardcoded location)
      userConfig = module_context::resolveAliasedPath("~/.R/rstudio").completePath(path);
   }

   // System config is always in the XDG location (if it exists at all)
   FilePath systemConfig = core::system::xdg::systemConfigFile(path);

   // If neither config file exists, no work to do; raise an error if requested.
   if (!userConfig.exists() && !systemConfig.exists())
   {
      Error error = logErrorIfNotFound ?
               fileNotFoundError(ERROR_LOCATION) :
               Success();
      
      if (error)
         LOG_ERROR(error);
      
      return error;
   }
   
   std::string contents;
   json::Value configJson;

   // Read the user config first. 
   if (userConfig.exists())
   {
      error = readStringFromFile(userConfig, &contents);
      if (error)
      {
         LOG_ERROR(error);
         if (!systemConfig.exists())
         {
            // If there's no system config and the user config can't be read, we can't do anything
            // more.
            return error;
         }
      }
      
      error = configJson.parse(contents);
      if (error)
      {
         error = configError(ConfigErrorCode::READ_ERROR, error, ERROR_LOCATION);
         LOG_ERROR(error);
         if (!systemConfig.exists())
         {
            return error;
         }
      }
   }

   // Read the system config, if present
   if (systemConfig.exists())
   {
      error = readStringFromFile(systemConfig, &contents);
      if (error)
      {
         LOG_ERROR(error);
         if (!userConfig.exists())
         {
            return error;
         }
      }
      else
      {
         json::Value systemJson;
         error = systemJson.parse(contents);
         if (error)
         {
            error = configError(ConfigErrorCode::READ_ERROR, error, ERROR_LOCATION);
            LOG_ERROR(error);
            if (!userConfig.exists())
            {
               return error;
            }
         }
         else if (systemJson.getType() == json::Type::OBJECT)
         {
            if (configJson.getType() == json::Type::OBJECT)
            {
               // We have successfully read two config sources; merge them
               configJson = json::Object::mergeObjects(configJson.getObject(), systemJson.getObject());
            }
            else
            {
               // We only have one config source (the system); use it
               configJson = systemJson;
            }
         }
      }
   }
   
   pResponse->setResult(configJson);
   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "write_config_json", writeConfigJSON))
      (bind(registerRpcMethod, "read_config_json", readConfigJSON));
   return initBlock.execute();
}


} // namespace config_file
} // namespace modules
} // namespace session
} // namespace rstudio

