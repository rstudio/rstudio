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

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace pubmed {

namespace {

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

   // return value
   json::Array docsJson;
   PubMedDocument doc("doi");
   doc.title = query;
   docsJson.push_back(pubMedDocumentJson(doc));

   json::JsonRpcResponse response;
   response.setResult(docsJson);
   cont(Success(), &response);
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
