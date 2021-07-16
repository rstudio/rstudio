/*
 * SessionQuartoXRefs.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include "SessionQuartoXRefs.hpp"

#include <algorithm>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>


#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {

namespace {

Error xrefIndexForFile(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // read params
   std::string file;
   Error error = json::readParams(request.params, &file);
   if (error)
      return error;

   // resolve path
   FilePath filePath = resolveAliasedPath(file);

   // index entries
   json::Object indexJson;
   indexJson["baseDir"] = createAliasedPath(filePath.getParent());
   indexJson["refs"] = json::Array();

   // get storage for this file
   FilePath indexPath;
   error = perFilePathStorage(kQuartoCrossrefScope, filePath, false, &indexPath);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   if (indexPath.exists())
   {
      std::string index;
      error = core::readStringFromFile(indexPath, &index);
      if (error)
      {
         LOG_ERROR(error);
         return error;
      }

      json::Object quartoIndexJson;
      error = quartoIndexJson.parse(index);
      if (error)
      {
         LOG_ERROR(error);
         return error;
      }

      json::Array xrefs;
      json::Array entries = quartoIndexJson["entries"].getArray();
      std::transform(entries.begin(), entries.end(), std::back_inserter(xrefs), [&filePath](const json::Value& val) {
         json::Object valObject = val.getObject();
         std::string key, caption;
         Error error = json::readObject(valObject, "key", key, "caption", caption);
         if (error)
            LOG_ERROR(error);
         json::Object xref;
         // TODO: More robust parsing and json validation + support suffixes
         xref["file"] = filePath.getFilename();
         std::size_t dashPos = key.find_first_of('-');
         xref["type"] = key.substr(0,dashPos);
         xref["id"] = key.substr(dashPos + 1);
         xref["suffix"] = "";
         xref["title"] = caption;
         return xref;
      });
      indexJson["refs"] = xrefs;

   }

   // return success
   pResponse->setResult(indexJson);
   return Success();
}

Error xrefForId(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   // read params
   std::string file, id;
   Error error = json::readParams(request.params, &file, &id);
   if (error)
      return error;



   return Success();
}

} // anonymous namespace

namespace modules {
namespace quarto {
namespace xrefs {

Error initialize()
{


   // register rpc functions
   ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(registerRpcMethod, "quarto_xref_index_for_file", xrefIndexForFile))
     (boost::bind(registerRpcMethod, "quarto_xref_for_id", xrefForId))
   ;
   return initBlock.execute();


}

} // namespace xrefs
} // namespace quarto
} // namespace modules

namespace module_context {

core::json::Value quartoXRefIndex()
{

    return json::Value();
}

} // namespace module_context

} // namespace session
} // namespace rstudio
