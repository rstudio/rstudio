
/*
 * SessionUserState.cpp
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

#include <core/system/Xdg.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/json/rapidjson/schema.h>

#include <core/Exec.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionUserState.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace state {
namespace {

static boost::shared_ptr<json::Object> s_pUserState;

Error setState(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   std::string key;
   json::Value val;
   Error error = json::readParams(request.params, &key, &val);
   if (error)
      return error;

   auto it = s_pUserState->find(key);
   if (it != s_pUserState->end() && (*it).value() == val)
   {
      return Success();
   }

   (*s_pUserState)[key] = val;

   return Success();
}

} // anonymous namespace

json::Object userState()
{
   if (s_pUserState)
      return *s_pUserState;
   return json::Object();
}

Error initialize()
{
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_state", setState));
   Error error = initBlock.execute();
   if (error)
      return error;

   // Load schema for validation
   FilePath schemaFile = 
      options().rResourcesPath().complete("schema").complete("user-state-schema.json");
   std::string schemaContents;
   error = readStringFromFile(schemaFile, &schemaContents);
   if (error)
      return error;

   // Extract default values from schema
   json::Object defaults;
   error = json::getSchemaDefaults(schemaContents, &defaults);
   if (error)
      return error;

   // Use the defaults unless there's a valid user state file.
   s_pUserState = boost::make_shared<json::Object>(defaults);

   FilePath stateFile = core::system::xdg::userDataDir().complete(kUserStateFile);
   if (!stateFile.exists())
   {
      // No user state file, just use whatever defaults we've accumulated.
      return Success();
   }

   std::string stateConents;
   error = readStringFromFile(stateFile, &stateConents);
   if (error)
   {
      // Don't fail here since it will cause startup to fail; use defaults
      LOG_ERROR(error);
      return Success();
   }

   json::Value userState;
   error = json::parseAndValidate(stateConents, schemaContents, ERROR_LOCATION, &userState);
   if (error)
   {
      // Invalid or non-conforming user state; use defaults.
      LOG_ERROR(error);
      return Success();
   }

   *s_pUserState = userState.get_obj();

   return Success();
}

} // namespace state
} // namespace modules
} // namespace session
} // namespace rstudio
