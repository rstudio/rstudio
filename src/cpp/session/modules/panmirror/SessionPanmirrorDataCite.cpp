/*
 * SessionPanmirrorDataCite.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionPanmirrorDataCite.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/http/Util.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

#include "SessionPanmirrorUtils.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace datacite {

namespace {

const char * const kDataCiteApiHost = "https://api.datacite.org";

// convert json returned by API to json record expected by the client
json::Object asDataCiteRecord(const json::Object resultJson)
{
   resultJson.writeFormatted(std::cerr);


   // get attributes object
   json::Object attributesJson;
   json::readObject(resultJson, "attributes", attributesJson);

   // read some basic attributes
   std::string doi;
   json::Array titlesJson;
   json::readObject(attributesJson, "doi", doi, "titles", titlesJson);

   // bail if we don't have a doi and a title
   if (doi.empty() || titlesJson.isEmpty())
      return json::Object();

   // return a record w/ doi + other fields
   json::Object recordJson;
   recordJson["doi"] = doi;

   // read title
   if (titlesJson[0].isObject() && titlesJson[0].getObject().hasMember("title"))
      recordJson["title"] = titlesJson[0].getObject()["title"];

   // read publication info
   std::string publisher;
   json::readObject(attributesJson, "publisher", publisher);
   if (!publisher.empty())
      recordJson["publisher"] = publisher;
   if (attributesJson.hasMember("publicationYear"))
      recordJson["publicationYear"] = attributesJson["publicationYear"];

   // read creators
   json::Array creatorsJson;
   json::readObject(attributesJson, "creators", creatorsJson);
   if (!creatorsJson.isEmpty())
   {
      json::Array targetCreatorsJson;
      for (auto creatorJson : creatorsJson)
      {
         if (creatorJson.isObject() && creatorJson.getObject().hasMember("name"))
         {
             std::string familyName, givenName, fullName;
             json::readObject(creatorJson.getObject(), "name", fullName);
             json::readObject(creatorJson.getObject(), "givenName", givenName);
             json::readObject(creatorJson.getObject(), "familyName", familyName);

             json::Object targetJson;
             if (!fullName.empty())
                 targetJson["fullName"] = fullName;
             if (!givenName.empty())
                 targetJson["givenName"] = givenName;
             if (!familyName.empty())
                 targetJson["familyName"] = familyName;
             targetCreatorsJson.push_back(targetJson);

         }
      }
      recordJson["creators"] = targetCreatorsJson;
   }

   // read type
   json::Object typesJson;
   json::readObject(attributesJson, "types", typesJson);
   if (typesJson.hasMember("citeproc"))
      recordJson["type"] = typesJson["citeproc"];

   return recordJson;
}

void dataCiteRequest(const std::string& resource,
                     http::Fields params,
                     const json::JsonRpcFunctionContinuation& cont,
                     const JsonHandler& handler)
{
   // build query string
   std::string queryString;
   core::http::util::buildQueryString(params, &queryString);
   if (queryString.length() > 0)
      queryString = "?" + queryString;

   // build the url and make the request
   boost::format fmt("%s/%s%s");
   const std::string url = boost::str(fmt % kDataCiteApiHost % resource % queryString);

   asyncDownloadFile(url, boost::bind(jsonRpcDownloadHandler, cont, _1, ERROR_LOCATION,
                                      [handler](const core::json::JsonRpcFunctionContinuation& cont,
                                      core::json::Value json) {
      // validate top level JSON:API response
      if (!json.isObject() ||
          (!json.getObject().hasMember("data") && !json.getObject().hasMember("errors")))
      {
         resolveJsonRpcContinuation(cont, kStatusError, json::Value(),
                                    "Unexpected data format returned by DataCite API");
      }
      else if (json.getObject().hasMember("errors"))
      {
         json::Array errorsJson;
         json::readObject(json.getObject(), "errors", errorsJson);
         std::string code, title;
         if (errorsJson.getSize() > 0 && errorsJson[0].isObject())
         {
            json::readObject(errorsJson[0].getObject(),
                             "code", code,
                             "title", title);
         }
         if (code.empty())
            code = "-1";
         if (title.empty())
            title = "(Unknown Error)";

         resolveJsonRpcContinuation(cont, kStatusError, json::Value(),
                                    "DataCite API Error: " + code + " - " + title);
      }
      else
      {
         handler(cont, json.getObject()["data"]);
      }
   }));
}

void dataCiteSearch(const json::JsonRpcRequest& request,
                    const json::JsonRpcFunctionContinuation& cont)
{
   // extract query
   std::string query;
   Error error = json::readParams(request.params, &query);
   if (error)
   {
     json::JsonRpcResponse response;
     setErrorResponse(error, &response);
     cont(Success(), &response);
     return;
   }

   // build params
   core::http::Fields params;
   params.push_back(std::make_pair("query", query));

   // make request
   dataCiteRequest("dois", params, cont,
                   [](const core::json::JsonRpcFunctionContinuation& cont, core::json::Value json) {

      if (!json.isArray())
      {
         resolveJsonRpcContinuation(cont, kStatusError, json::Value(),
                                    "DataCite API returned incorrectly formatted response");
      }
      else
      {
         json::Array resultsJson = json.getArray();
         json::Array recordsJson;
         for (auto resultJson : resultsJson)
         {
            if (resultJson.isObject())
            {
                 json::Object jsonRecord = asDataCiteRecord(resultJson.getObject());
                 if (jsonRecord.getSize() > 0)
                    recordsJson.push_back(jsonRecord);
            }
         }
         resolveJsonRpcContinuation(cont, kStatusOK, recordsJson);
      }
   });
}

} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::registerAsyncRpcMethod, "datacite_search", dataCiteSearch))
   ;
   return initBlock.execute();
}

} // end namespace datacite
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
