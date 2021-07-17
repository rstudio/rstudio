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

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionQuarto.hpp>

#include "SessionQuarto.hpp"

using namespace rstudio::core;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {

using namespace quarto;

namespace modules {
namespace quarto {
namespace xrefs {

namespace {


json::Array readXRefIndex(const FilePath& srcFile, const FilePath& indexPath)
{
   // read the index as a string
   std::string index;
   Error error = core::readStringFromFile(indexPath, &index);
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }

   // parse json w/ validation
   json::Object quartoIndexJson;
   error = quartoIndexJson.parseAndValidate(
      index,
      resourceFileAsString("schema/quarto-xref.json")
   );
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }

   // read xrefs (already validated so don't need to dance around types/existence)
   json::Array xrefs;
   boost::regex keyRegex("^(\\w+)-(.*?)(?:-(\\d+))?$");
   json::Array entries = quartoIndexJson["entries"].getArray();
   for (const json::Value& entry : entries)
   {
      json::Object valObject = entry.getObject();
      std::string key, caption;
      json::readObject(valObject, "key", key, "caption", caption);
      boost::smatch match;
      if (boost::regex_search(key, match, keyRegex))
      {
         json::Object xref;
         xref["file"] = srcFile.getFilename();
         xref["type"] = match[1].str();
         xref["id"] = match[2].str();
         xref["suffix"] = (match.length() > 3) ? match[3].str() : "";
         xref["title"] = caption;
         xrefs.push_back(xref);
      }
   }
   return xrefs;
}

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

   // is this file in a project and is it a book project?
   FilePath projectDir;
   bool isBook = false;
   FilePath projectConfig = quartoProjectConfigFile(filePath);
   if (!projectConfig.isEmpty())
   {
      // set project dir
      projectDir = projectConfig.getParent();

      // short circuit for this being in the current project context (so we already have the config)
      if (isFileInSessionQuartoProject(filePath))
      {
         isBook = quartoConfig().project_type == kQuartoProjectBook;
      }
      else
      {
         std::string type;
         readQuartoProjectConfig(projectConfig, &type);
         isBook = type == kQuartoProjectBook;
      }
   }

   // handle projects one way, and standalone files another way
   if (!projectDir.isEmpty())
   {

   }
   else
   {
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
         indexJson["refs"] = readXRefIndex(filePath, indexPath);
      }
   }

   // return success
   pResponse->setResult(indexJson);
   return Success();
}

Error xrefForId(const json::JsonRpcRequest& request,
                json::JsonRpcResponse*)
{
   // read params
   std::string file, id;
   Error error = json::readParams(request.params, &file, &id);
   if (error)
      return error;



   return Success();
}

} // anonymous namespace

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
