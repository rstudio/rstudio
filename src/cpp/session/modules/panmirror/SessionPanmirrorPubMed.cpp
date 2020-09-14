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

#include <r/ROptions.hpp>

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
   PubMedDocument(const std::string& doi = "") : doi(doi) {}
   bool empty() { return doi.empty(); }
   std::string doi;
   std::vector<std::string> pubTypes;
   std::string title;
   std::vector<std::string> authors;
   std::string sortFirstAuthor;
   std::string source;
   std::string volume;
   std::string issue;
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

   // authors
   if (doc.authors.size() > 0)
   {
      json::Array authorsJson;
      for (auto author : doc.authors)
         authorsJson.push_back(author);
      docJson["authors"] = authorsJson;
   }

   // lastAuthor
   if (!doc.sortFirstAuthor.empty())
      docJson["sortFirstAuthor"] = doc.sortFirstAuthor;

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

   // pubDate
   if (!doc.pubDate.empty())
      docJson["pubDate"] = doc.pubDate;

   return docJson;

}

PubMedDocument resultToPubMedDocument(json::Object resultJson)
{
   // look for doi (the only required field)
   std::string doi;
   const char * const kArticleIds = "articleids";
   if (resultJson.hasMember(kArticleIds) && resultJson[kArticleIds].isArray())
   {
     json::Array articleIdsJson = resultJson[kArticleIds].getArray();
     for (json::Value articleIdJson : articleIdsJson)
     {
        if (articleIdJson.isObject())
        {
           std::string idtype, value;
           json::readObject(articleIdJson.getObject(),
                           "idtype", idtype,
                           "value", value);
           if (idtype == "doi")
           {
              doi = value;
              break;
           }
        }
     }
   }

   // if there is no doi then return an empty doc
   if (doi.empty())
      return PubMedDocument();

   // create doc with doi
   PubMedDocument doc(doi);

   // look for pubtypes
   json::Array pubTypesJson;
   json::readObject(resultJson, "pubtype", pubTypesJson);
   if (pubTypesJson.getSize() > 0)
   {
      for (auto pubTypeJson : pubTypesJson)
      {
         if (pubTypeJson.isString())
            doc.pubTypes.push_back(pubTypeJson.getString());
      }
   }

   // look for authors
   json::Array authorsJson;
   json::readObject(resultJson, "authors", authorsJson);
   if (authorsJson.getSize() > 0)
   {
      for (auto authorJson : authorsJson)
      {
         if (authorJson.isObject() && authorJson.getObject().hasMember("name"))
         {
            json::Value nameJson = authorJson.getObject()["name"];
            if (nameJson.isString())
               doc.authors.push_back(nameJson.getString());
         }
      }
   }

   // read sortfirstauthor, title, source, volume, issue, pubDate
   json::readObject(resultJson, "sortfirstauthor", doc.sortFirstAuthor);
   json::readObject(resultJson, "title", doc.title);
   json::readObject(resultJson, "source", doc.source);
   json::readObject(resultJson, "volume", doc.volume);
   json::readObject(resultJson, "issue", doc.issue);
   json::readObject(resultJson, "pubdate", doc.pubDate);

   return doc;
}


void pubMedRequest(const std::string& resource,
                   http::Fields params,
                   const json::JsonRpcFunctionContinuation& cont,
                   const JsonHandler& handler)
{

   // add standard params
   params.push_back(std::make_pair("tool", "rstudio"));
   params.push_back(std::make_pair("email", "pubmed@rstudio.com"));
   params.push_back(std::make_pair("retmode", "json"));

   // add apikey if specified
   std::string apiKey = r::options::getOption<std::string>("rstudio.pubmed_api_key", "", false);
   if (!apiKey.empty())
      params.push_back(std::make_pair("api_key", apiKey));

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
   params.push_back(std::make_pair("sort", "relevance"));
   params.push_back(std::make_pair("retmax", "25"));

   pubMedRequest("entrez/eutils/esearch.fcgi", params, cont,
                 [](const core::json::JsonRpcFunctionContinuation& cont, core::json::Value json) {

      // std::cerr << "query: " << json.getObject()["esearchresult"].getObject()["querytranslation"].getString() << std::endl;

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

      // if there were no results return an empty array
      if (ids.size() == 0)
      {
         resolveJsonRpcContinuation(cont, kStatusOK, json::Array());
         return;
      }
      
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
               PubMedDocument doc = resultToPubMedDocument(resultJson);
               if (!doc.empty())
                  docsJson.push_back(pubMedDocumentJson(doc));
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
