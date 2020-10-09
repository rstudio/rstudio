/*
 * SessionZotero.cpp
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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

#include "SessionZotero.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>
#include <session/prefs/Preferences.hpp>
#include <session/projects/SessionProjects.hpp>

#include "ZoteroCollections.hpp"
#include "ZoteroCollectionsWeb.hpp"
#include "ZoteroCollectionsLocal.hpp"
#include "ZoteroBetterBibTeX.hpp"
#include "ZoteroUtil.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

using namespace collections;

namespace {

const char * const kStatus = "status";
const char * const kMessage = "message";
const char * const kWarning = "warning";
const char * const kError = "error";

const char * const kStatusOK = "ok";
const char * const kStatusNoHost = "nohost";
const char * const kStatusNotFound = "notfound";
const char * const kStatusError = "error";

Error zoteroDetectLocalConfig(const json::JsonRpcRequest&,
                             json::JsonRpcResponse* pResponse)
{
   auto detectedConfig = collections::detectedLocalZoteroConfig();
   json::Object configJson;
   configJson["dataDirectory"] = !detectedConfig.dataDirectory.isEmpty()
      ?  module_context::createAliasedPath(detectedConfig.dataDirectory)
      : "";
   configJson["betterBibtex"] = detectedConfig.betterBibtex;
   pResponse->setResult(configJson);
   return Success();
}

void zoteroValidateWebApiKey(const json::JsonRpcRequest& request,
                             const json::JsonRpcFunctionContinuation& cont)
{
   // extract params
   std::string key;
   Error error = json::readParams(request.params, &key);
   if (error)
   {
      json::JsonRpcResponse response;
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   validateWebApiKey(key, [cont](bool valid) {
      json::JsonRpcResponse response;
      response.setResult(valid);
      cont(Success(), &response);
   });
}

std::string errorResultStatus(Error error, const ErrorLocation& location)
{
   std::string err = core::errorDescription(error);
   if (is404Error(err))
   {
      return kStatusNotFound;
   }
   else if (isHostError(err))
   {
      return kStatusNoHost;
   }
   else
   {
      core::log::logErrorMessage(err, location);
      return kStatusError;
   }
}

void handleGetCollectionSpecs(Error error, ZoteroCollectionSpecs collectionSpecs, const json::JsonRpcFunctionContinuation& cont)
{
   // result defaults
   json::Object resultJson;
   resultJson[kMessage] = json::Value();
   resultJson[kError] = "";

   // handle success & error
   if (!error)
   {
      json::Array collectionSpecsJson;
      for (auto collectionSpec : collectionSpecs)
      {
         json::Object collectionSpecJson;
         collectionSpecJson[kName] = collectionSpec.name;
         collectionSpecJson[kVersion] = collectionSpec.version;
         collectionSpecJson[kKey] = collectionSpec.key;
         collectionSpecJson[kParentKey] = collectionSpec.parentKey;
         collectionSpecsJson.push_back(collectionSpecJson);
      }
      resultJson[kStatus] = kStatusOK;
      resultJson[kMessage] = collectionSpecsJson;
   }
   else
   {
      resultJson[kStatus] = errorResultStatus(error, ERROR_LOCATION);
   }

   json::JsonRpcResponse response;
   response.setResult(resultJson);
   cont(Success(), &response);

}

void handleGetLibraries(Error error, std::vector<std::string> libraries, const json::JsonRpcFunctionContinuation& cont)
{
   // result defaults
   json::Object resultJson;
   resultJson[kMessage] = json::Value();
   resultJson[kWarning] = "";
   resultJson[kError] = "";

   // handle success & error
   if (!error)
   {
      json::Array collectionsJson = json::toJsonArray(libraries);
      resultJson[kStatus] = kStatusOK;
      resultJson[kMessage] = collectionsJson;
   }
   else
   {
      resultJson[kStatus] = errorResultStatus(error, ERROR_LOCATION);
   }

   json::JsonRpcResponse response;
   response.setResult(resultJson);
   cont(Success(), &response);
}

void handleGetCollections(Error error, ZoteroCollections collections, std::string warning, const json::JsonRpcFunctionContinuation& cont)
{
   // result defaults
   json::Object resultJson;
   resultJson[kMessage] = json::Value();
   resultJson[kWarning] = "";
   resultJson[kError] = "";

   // handle success & error
   if (!error)
   {
      json::Array collectionsJson;
      for (auto collection : collections)
      {
         json::Object collectionJson;
         collectionJson[kName] = collection.name;
         collectionJson[kVersion] = collection.version;
         collectionJson[kKey] = collection.key;
         collectionJson[kParentKey] = collection.parentKey;
         collectionJson[kItems] = collection.items;
         collectionsJson.push_back(collectionJson);
      }
      resultJson[kStatus] = kStatusOK;
      resultJson[kWarning] = warning;
      resultJson[kMessage] = collectionsJson;
   }
   else
   {
      resultJson[kStatus] = errorResultStatus(error, ERROR_LOCATION);
   }

   json::JsonRpcResponse response;
   response.setResult(resultJson);
   cont(Success(), &response);
}

void zoteroGetLibraryNames(const json::JsonRpcRequest&,
                           const json::JsonRpcFunctionContinuation& cont)
{
   json::JsonRpcResponse response;
   auto handler =  boost::bind(handleGetLibraries, _1, _2, cont);
   getLibraryNames(handler);
}

bool getConfiguredCollections(const std::string& file, std::vector<std::string>* pCollections)
{
   // determine whether the file the zotero collections are requested for is part of the current project
   bool isProjectFile = false;
   if (!file.empty() && projects::projectContext().hasProject())
   {
      FilePath filePath = module_context::resolveAliasedPath(file);
      if (filePath.isWithin(projects::projectContext().buildTargetPath()))
      {
         isProjectFile = true;
      }
   }

   // if it's a project, check for global bookdown disable of zotero
   if (isProjectFile)
   {
      // check for global disable of zotero for bookdown
      const std::string kLogicalPrefix = "LOGICAL:";
      std::vector<std::string> bookdownCollections = module_context::bookdownZoteroCollections();

      // logical value. non-TRUE value means zotero has been disabled in YAML, TRUE value means all collections
      if (bookdownCollections.size() == 1 && boost::algorithm::starts_with(bookdownCollections[0], kLogicalPrefix))
      {
         std::string value = bookdownCollections[0].substr(kLogicalPrefix.length());
         if (value == "TRUE")
         {
             session::prefs::userPrefs().zoteroLibraries().toVectorString(*pCollections);
         }
         else
         {
            return false;
         }
      }
      else if (bookdownCollections.size() > 0)
      {
         *pCollections = bookdownCollections;
      }
      else
      {
         session::prefs::userPrefs().zoteroLibraries().toVectorString(*pCollections);
      }
   }

   // read global pref (ignore project b/c this file isn't in the project)
   else
   {
      auto libsPref = session::prefs::userPrefs().readValue(kUserStateUserLayer, "zotero_libraries");
      if (libsPref.has_value() && libsPref.get().isArray())
         libsPref->getArray().toVectorString(*pCollections);
   }

   return true;

}

void zoteroGetActiveCollectionSpecs(const json::JsonRpcRequest& request,
                                    const json::JsonRpcFunctionContinuation& cont)
{
   // extract params
   std::string file;
   json::Array collectionsJson;
   Error error = json::readParams(request.params, &file, &collectionsJson);
   if (error)
   {
      json::JsonRpcResponse response;
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // collections filter (empty means all collections)
   std::vector<std::string> collections;

   // see if the user provided some collections
   collectionsJson.toVectorString(collections);

   // if they didn't, see if there are project level or global prefs that provide collections
   if (collections.size() == 0)
   {
      if (!getConfiguredCollections(file, &collections))
      {
         ZoteroCollectionSpecs noCollections;
         handleGetCollectionSpecs(Success(), noCollections, cont);
         return;
      }
   }

   // if at this point we still don't have any collections (likely due to migrating
   // from a dev version that had support for 'All Collections') then just default
   // to 'My Library'
   if (collections.size() == 0)
      collections.push_back(kMyLibrary);

   // get the specs
   getCollectionSpecs(collections, [cont, collections](core::Error error, ZoteroCollectionSpecs specs) {
      json::JsonRpcResponse response;
      if (error)
      {
         json::setErrorResponse(error, &response);
         cont(Success(), &response);
      }
      else
      {
         handleGetCollectionSpecs(Success(), specs, cont);
      }
   });

}

void zoteroGetCollections(const json::JsonRpcRequest& request,
                          const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   std::string file;
   json::Array collectionsJson, cachedJson;
   bool useCache;
   Error error = json::readParams(request.params, &file, &collectionsJson, &cachedJson, &useCache);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // read user request
   std::vector<std::string> collections;
   collectionsJson.toVectorString(collections);

   // if there is no user request, see if there are project level or global prefs that provide collections
   if (collections.size() == 0)
   {
      if (!getConfiguredCollections(file, &collections))
      {
         ZoteroCollections noCollections;
         handleGetCollections(Success(), noCollections, "", cont);
         return;
      }
   }

   // if at this point we still don't have any collections (likely due to migrating
   // from a dev version that had support for 'All Collections') then just default
   // to 'My Library'
   if (collections.size() == 0)
      collections.push_back(kMyLibrary);

   // extract client cache specs
   ZoteroCollectionSpecs cacheSpecs;
   std::transform(cachedJson.begin(), cachedJson.end(), std::back_inserter(cacheSpecs), [](const json::Value json) {
      auto jsonSpec = json.getObject();
      ZoteroCollectionSpec cacheSpec;
      cacheSpec.name = jsonSpec[kName].getString();
      cacheSpec.version = jsonSpec[kVersion].getInt();
      cacheSpec.key = jsonSpec[kKey].getString();
      cacheSpec.parentKey = jsonSpec[kParentKey].getString();
      return cacheSpec;
   });

   // create handler for request
   auto handler =  boost::bind(handleGetCollections, _1, _2, _3, cont);

   // get the collections
   getCollections(collections, cacheSpecs, useCache, handler);

}


} // end anonymous namespace


Error initialize()
{
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
       (boost::bind(registerAsyncRpcMethod, "zotero_get_collections", zoteroGetCollections))
       (boost::bind(registerAsyncRpcMethod, "zotero_get_library_names", zoteroGetLibraryNames))
       (boost::bind(registerAsyncRpcMethod, "zotero_get_active_collection_specs", zoteroGetActiveCollectionSpecs))
       (boost::bind(registerAsyncRpcMethod, "zotero_validate_web_api_key", zoteroValidateWebApiKey))
       (boost::bind(registerRpcMethod, "zotero_detect_local_config", zoteroDetectLocalConfig))
       (boost::bind(registerRpcMethod, "zotero_better_bibtex_export", betterBibtexExport))
       (betterBibtexInit)
   ;
   return initBlock.execute();
}

} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio


