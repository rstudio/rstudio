/*
 * ZoteroBetterBibTeX.cpp
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

#include "ZoteroBetterBibTeX.hpp"

#include <shared_core/json/Json.hpp>
#include <shared_core/json/Json.hpp>

#include <core/http/TcpIpBlockingClient.hpp>
#include <core/http/SocketUtils.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

#include <session/SessionModuleContext.hpp>

#include "ZoteroCollections.hpp"
#include "ZoteroCollectionsLocal.hpp"

namespace rstudio {

using namespace core;

namespace session {
namespace modules {
namespace zotero {

using namespace collections;

namespace  {

template<typename T>
bool betterBibtexJsonRpcRequest(const std::string& method, const json::Array& params, T* pResponse, std::string* pWarning)
{
   // build request
   http::Request request;
   request.setMethod("POST");
   request.setContentType("application/json");
   request.setHeader("Accept", "application/json");
   request.setUri("/better-bibtex/json-rpc");
   json::Object rpcRequest;
   rpcRequest["jsonrpc"] = "2.0";
   rpcRequest["method"] = method;
   rpcRequest["params"] = params;
   request.setBody(rpcRequest.writeFormatted());

   http::Response response;
   Error error = http::sendRequest("localhost", "23119", boost::posix_time::milliseconds(1000), request, &response);
   if (!error)
   {
      if (response.statusCode() == http::status::Ok)
      {
         json::Value responseValue;
         Error error = responseValue.parse(response.body());
         if (!error)
         {
            if (responseValue.isObject() && responseValue.getObject().hasMember("result"))
            {
               json::Value resultValue = responseValue.getObject()["result"];
               if (json::isType<T>(resultValue))
               {
                  *pResponse = resultValue.getValue<T>();
                  return true;
               }
            }

         }

         *pWarning = "Unexpected data format provided by Better BibTeX";
         LOG_ERROR_MESSAGE(*pWarning + " : " + response.body());

      }
      else
      {
         *pWarning = "Unexpected status " +
                     safe_convert::numberToString(response.statusCode()) + " from Better BibTeX";
         LOG_ERROR_MESSAGE(*pWarning);
      }

   }
   else if (http::isConnectionUnavailableError(error) ||
            (error = systemError(boost::system::errc::timed_out, ErrorLocation())))
   {
      *pWarning = "Unable to connect to Better BibTeX. Please ensure that Zotero is running.";
   }
   else
   {
      *pWarning = "Unexpected error communicating with Better BibTex";
      LOG_ERROR(error);
   }

   return false;
}


} // anonymous namespace


bool betterBibtexInConfig(const std::string& config)
{
   return config.find_first_of("extensions.zotero.translators.better-bibtex") != std::string::npos;
}

bool betterBibtexEnabled()
{
   return session::prefs::userState().zoteroUseBetterBibtex();
}

void betterBibtexProvideIds(const collections::ZoteroCollections& collections,
                            collections::ZoteroCollectionsHandler handler)
{
   // get zotero key for each item in all of the collections
   std::vector<std::string> zoteroKeys;
   for (auto collection : collections)
   {
      std::transform(collection.items.begin(), collection.items.end(), std::back_inserter(zoteroKeys), [](const json::Value& itemJson) {
         return itemJson.getObject()["key"].getString();
      });
   }

   // call better bibtex to create a map of zotero keys to bbt citation ids
   std::string warning;
   std::map<std::string,std::string> keyMap;
   json::Object keyMapJson;
   json::Array params;
   params.push_back(json::toJsonArray(zoteroKeys));
   if (betterBibtexJsonRpcRequest("item.citationkey", params, &keyMapJson, &warning))
   {
      for (auto member : keyMapJson)
      {
         if (member.getValue().isString())
            keyMap[member.getName()] = member.getValue().getString();
      }
   }

   // new set of collections with updated ids
   collections::ZoteroCollections updatedCollections;
   std::transform(collections.begin(), collections.end(), std::back_inserter(updatedCollections),
                  [&keyMap](const collections::ZoteroCollection& collection) {
      json::Array updatedItems;
      std::transform(collection.items.begin(), collection.items.end(), std::back_inserter(updatedItems),
                     [&keyMap](const json::Value& itemJson) {
          json::Object itemObject = itemJson.getObject();
          if (itemObject.hasMember("key"))
          {
             std::string zoteroKey = itemObject["key"].getString();
             std::map<std::string,std::string>::const_iterator it = keyMap.find(zoteroKey);
             if (it != keyMap.end())
             {
                itemObject["id"] = it->second;
             }
          }
          return itemObject;
      });
      collections::ZoteroCollection updatedCollection(collections::ZoteroCollectionSpec(collection.name, collection.key, collection.parentKey, collection.version));
      updatedCollection.items = updatedItems;
      return updatedCollection;
   });

   // return
   handler(Success(), updatedCollections, warning);
}

void setBetterBibtexNotFoundResult(const std::string& warning, json::JsonRpcResponse* pResponse)
{
   json::Object resultJson;
   resultJson["status"] = "nohost";
   resultJson["warning"] = warning;
   pResponse->setResult(resultJson);
}

Error betterBibtexExport(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // extract params
   json::Array itemKeysJson;
   std::string translatorId;
   int libraryId;
   Error error = json::readParams(request.params, &itemKeysJson, &translatorId, &libraryId);
   if (error)
      return error;

   // include library in item keys
   boost::format fmt("%1%:%2%");
   for (std::size_t i=0; i<itemKeysJson.getSize(); i++)
      itemKeysJson[i] = boost::str(fmt % libraryId % itemKeysJson[i].getString());

   // get citation keys
   std::string warning;
   json::Object keyMapJson;
   json::Array params;
   params.push_back(itemKeysJson);
   if (betterBibtexJsonRpcRequest("item.citationkey", params, &keyMapJson, &warning))
   {
      // extract keys
      json::Array citekeysJson;
      std::transform(keyMapJson.begin(), keyMapJson.end(), std::back_inserter(citekeysJson),
                     [](const json::Object::Member& member) {
                        return member.getValue();
                     });

      // perform export
      params.clear();
      params.push_back(citekeysJson);
      params.push_back(translatorId);
      params.push_back(libraryId);
      json::Array exportJson;
      if (betterBibtexJsonRpcRequest("item.export", params, &exportJson, &warning))
      {
         if (exportJson.getSize() >= 3 &&
             exportJson[0].isInt() && exportJson[0].getInt() == 200 &&
             exportJson[2].isString())
         {
            json::Object jsonResult;
            jsonResult["status"] = "ok";
            jsonResult["message"] = exportJson[2].getString();
            pResponse->setResult(jsonResult);
         }
         else
         {
            std::string warning = "Unexpected response from Better BibTeX";
            LOG_ERROR_MESSAGE(warning + " : " + exportJson.write());
            setBetterBibtexNotFoundResult(warning, pResponse);
         }
      }
      else
      {
         setBetterBibtexNotFoundResult(warning, pResponse);
      }
   }
   else
   {
      setBetterBibtexNotFoundResult(warning, pResponse);
   }

   return Success();
}

Error betterBibtexInit()
{
   // force better bibtex pref off if the config isn't found
   if (collections::localZoteroAvailable())
   {
      collections::DetectedLocalZoteroConfig config = collections::detectedLocalZoteroConfig();
      if (prefs::userState().zoteroUseBetterBibtex() && !config.betterBibtex)
         prefs::userState().setZoteroUseBetterBibtex(false);
   }

   return Success();
}


} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
