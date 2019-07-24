/*
 * SessionConfigFile.cpp
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


#include "SessionConfigFile.hpp"

#include <core/Error.hpp>
#include <core/FileSerializer.hpp>
#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/FileUtils.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/Json.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace config_file {

namespace {

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

   // First, check for the file at the XDG location

   
   FilePath filePath = module_context::resolveAliasedPath(path);
   error = filePath.parent().ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   std::string contents = json::writeFormatted(object);
   error = writeStringToFile(filePath, contents);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   pResponse->setResult(true);
   return Success();
}

Error readJSON(const core::json::JsonRpcRequest& request,
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
   
   FilePath filePath = module_context::resolveAliasedPath(path);
   if (!filePath.exists())
   {
      Error error = logErrorIfNotFound ?
               fileNotFoundError(ERROR_LOCATION) :
               Success();
      
      if (error)
         LOG_ERROR(error);
      
      return error;
   }
   
   std::string contents;
   error = readStringFromFile(filePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   json::Value valueJson;
   bool success = json::parse(contents, &valueJson);
   if (!success)
   {
      Error error(json::errc::ParseError, ERROR_LOCATION);
      LOG_ERROR(error);
      return error;
   }
   
   pResponse->setResult(valueJson);
   return Success();
}

Error initialize()
{
   // install handlers
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "write_config_json", writeConfigJSON))
      (bind(registerRpcMethod, "read_config_json", readConfigJSON));
   return initBlock.execute();
}


} // namespace files
} // namespace modules
} // namespace session
} // namespace rstudio

