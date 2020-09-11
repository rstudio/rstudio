/*
 * SessionPanmirrorPubMed.cpp
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

#include "SessionPanmirrorPubMed.hpp"

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
namespace pubmed {

namespace {

const char * const kPubMedEUtilsHost = "https://eutils.ncbi.nlm.nih.gov";

struct PubMedDocument
{
   PubMedDocument(const std::string& doi) : doi(doi) {}
   std::string doi;
   std::vector<std::string> pubTypes;
   std::string title;
   std::string source;
   std::string volume;
   std::string issue;
   std::vector<std::string> authors;
   std::string pubDate;
};


json::Object pubMedDocumentJson(const PubMedDocument& doc)
{
   // all fields are optional save for doi
   json::Object docJson;

   // doi (required)
   docJson["doi"] = doc.doi;

   // pubTypes
   if (doc.pubTypes.size() > 0)
   {
      json::Array pubTypesJson;
      for (auto pubType : doc.pubTypes)
         pubTypesJson.push_back(pubType);
      docJson["pubTypes"] = pubTypesJson;
   }

   // title
   if (!doc.title.empty())
      docJson["title"] = doc.title;

   // source
   if (!doc.source.empty())
   {
      docJson["source"] = doc.source;
      if (!doc.volume.empty())
         docJson["volume"] = doc.volume;
      if (!doc.issue.empty())
         docJson["issue"] = doc.issue;
   }

   // authors
   if (doc.authors.size() > 0)
   {
      json::Array authorsJson;
      for (auto author : doc.authors)
         authorsJson.push_back(author);
      docJson["authors"] = authorsJson;
   }

   // pubDate
   if (!doc.pubDate.empty())
      docJson["pubDate"] = doc.pubDate;

   return docJson;

}

json::Object transformPubMedResult(json::Object resultJson)
{
   PubMedDocument doc(resultJson["uid"].getString());

   return pubMedDocumentJson(doc);
}


void pubMedRequest(const std::string& resource,
                   http::Fields params,
                   const json::JsonRpcFunctionContinuation& cont,
                   const JsonHandler& handler)
{

   // add json retmode
   params.push_back(std::make_pair("retmode", "json"));

   // build query string
   std::string queryString;
   core::http::util::buildQueryString(params, &queryString);
   if (queryString.length() > 0)
      queryString = "?" + queryString;

   // build the url and make the request
   boost::format fmt("%s/%s%s");
   const std::string url = boost::str(fmt % kPubMedEUtilsHost % resource % queryString);

   asyncDownloadFile(url, boost::bind(jsonRpcDownloadHandler, cont, _1, ERROR_LOCATION, handler));
}


void pubMedSearch(const json::JsonRpcRequest& request,
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

   http::Fields params;
   params.push_back(std::make_pair("db", "pubmed"));
   params.push_back(std::make_pair("term", query));
   params.push_back(std::make_pair("retmax", "50"));

   pubMedRequest("entrez/eutils/esearch.fcgi", params, cont,
                 [](const core::json::JsonRpcFunctionContinuation& cont, core::json::Value json) {

      // validate json
      Error error =json.validate(module_context::resourceFileAsString("schema/pubmed-esearch.json"));
      if (error)
      {
         resolveJsonRpcContinuation(cont, kStatusError, json::Value(), core::errorMessage(error));
         return;
      }
      
      // read ids
      std::vector<std::string> ids;
      json.getObject()["esearchresult"]
          .getObject()["idlist"]
          .getArray()
          .toVectorString(ids);
      
      // fetch documents
      http::Fields summaryParams;
      summaryParams.push_back(std::make_pair("db", "pubmed"));
      summaryParams.push_back(std::make_pair("id", boost::algorithm::join(ids, ",")));
      pubMedRequest("entrez/eutils/esummary.fcgi", summaryParams, cont,
                    [](const core::json::JsonRpcFunctionContinuation& cont, core::json::Value json) {

         // validate json
         Error error =json.validate(module_context::resourceFileAsString("schema/pubmed-esummary.json"));
         if (error)
         {
            resolveJsonRpcContinuation(cont, kStatusError, json::Value(), core::errorMessage(error));
            return;
         }

         // read ids
         std::vector<std::string> ids;
         json.getObject()["result"]
             .getObject()["uids"]
             .getArray()
             .toVectorString(ids);

         // read docs
         json::Array docsJson;
         json::Object resultsJson = json.getObject()["result"].getObject();
         for (auto id : ids)
         {
            if (resultsJson.hasMember(id))
            {
               json::Object resultJson = resultsJson[id].getObject();
               docsJson.push_back(transformPubMedResult(resultJson));
            }
         }

         // resolve
         resolveJsonRpcContinuation(cont, kStatusOK, docsJson);

      });


   });

}


} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::registerAsyncRpcMethod, "pubmed_search", pubMedSearch))
   ;
   return initBlock.execute();
}

} // end namespace pubmed
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
