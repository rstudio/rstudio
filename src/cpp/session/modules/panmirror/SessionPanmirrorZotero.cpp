/*
 * SessionPanmirrorZotero.cpp
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

#include "SessionPanmirrorZotero.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>

#include <core/system/Process.hpp>

#include <session/prefs/UserPrefs.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace zotero {

namespace {

const char * const kZoteroApiHost = "https://api.zotero.org";

std::string zoteroApiKey()
{
   return prefs::userPrefs().zoteroApiKey();
}

typedef boost::function<void(const Error&,int,json::Value)> ZoteroJsonResponseHandler;

void zoteroJsonRequest(const std::string& resource,
                       http::Fields queryParams,
                       bool authenticated,
                       const ZoteroJsonResponseHandler& handler)
{
   // authorize using header or url param as required
   http::Fields headers;
   if (authenticated)
   {
      if (module_context::hasMinimumRVersion("3.6"))
      {
         boost::format fmt("Bearer %s");
         headers.push_back(std::make_pair("Authorization", "Bearer " + zoteroApiKey()));
      }
      else
      {
         queryParams.push_back(std::make_pair("key", zoteroApiKey()));
      }
   }

   // build query string
   std::string queryString;
   core::http::util::buildQueryString(queryParams, &queryString);
   if (queryString.length() > 0)
      queryString = "?" + queryString;


   // build the url and make the request
   boost::format fmt("%s/%s%s");
   const std::string url = boost::str(fmt % kZoteroApiHost % resource % queryString);
   asyncDownloadFile(url, headers, [handler](const core::system::ProcessResult& result) {
      if (result.exitStatus == EXIT_SUCCESS)
      {
         json::Value resultJson;
         Error error = resultJson.parse(result.stdOut);
         if (error)
            handler(error, 500, json::Value());
         else
            handler(Success(), 200, resultJson);
      }
      else
      {
         Error error = systemError(boost::system::errc::state_not_recoverable, result.stdErr, ERROR_LOCATION);
         handler(error, 500, json::Value());
      }
   });
}

void zoteroJsonRequest(const std::string& resource,
                      http::Fields queryParams,
                      const ZoteroJsonResponseHandler& handler)
{
   zoteroJsonRequest(resource, queryParams, true, handler);
}



void zoteroKeyInfo(const ZoteroJsonResponseHandler& handler)
{
   boost::format fmt("keys/%s");
   zoteroJsonRequest(boost::str(fmt % zoteroApiKey()),
                     http::Fields(),
                     false,
                     handler);
}

void zoteroCollections(int userID, const ZoteroJsonResponseHandler& handler)
{
   http::Fields params;
   params.push_back(std::make_pair("format", "json"));

   boost::format fmt("users/%d/collections");
   zoteroJsonRequest(boost::str(fmt % userID),
                     params,
                     handler);
}



void onDeferredInit(bool)
{
   zoteroKeyInfo([](const Error& error,int status,json::Value jsonValue) {


      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      int userID;
      json::Object jsonResult = jsonValue.getObject();
      Error jsonError = core::json::readObject(jsonResult, "userID", userID);
      if (jsonError)
      {
         LOG_ERROR(jsonError);
         return;
      }

      zoteroCollections(userID, [](const Error& error,int status,json::Value jsonValue) {
         if (error)
         {
            LOG_ERROR(error);
            return;
         }

          jsonValue.writeFormatted(std::cerr);

      });

   });




}


} // end anonymous namespace

Error initialize()
{
   module_context::events().onDeferredInit.connect(onDeferredInit);

   ExecBlock initBlock;
   initBlock.addFunctions()

   ;
   return initBlock.execute();
}

} // end namespace zotero
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
