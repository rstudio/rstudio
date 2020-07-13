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

const char * const kKeySchema = R"(
{
  "$id": "http://rstudio.org/schemas/zotero-key.json",
  "$schema": "http://json-schema.org/schema#",
  "title": "Zotero Key Metadata Schema",
  "type": "object",
  "properties": {
    "key": {
      "type": "string"
    },
    "userID": {
      "type": "number"
    },
    "username": {
      "type": "string"
    },
    "access": {
      "type": "object",
      "properties": {
        "user": {
          "type": "object",
          "properties": {
            "library": {
              "type": "boolean"
            },
            "files": {
              "type": "boolean"
            }
          }
        },
        "groups": {
          "type": "object",
          "properties": {
            "all": {
              "type": "object",
              "properties": {
                "library": {
                  "type": "boolean"
                },
                "write": {
                  "type": "boolean"
                }
              }
            }
          }
        }
      }
    }
  }
})";

const char * const kCollectionSchema = R"(
{
  "$id": "http://rstudio.org/schemas/zotero-collections.json",
  "$schema": "http://json-schema.org/schema#",
  "title": "Zotero Collections Metadata Schema",
  "type": "array",
  "items": {
    "type": "object",
    "properties" : {
      	"key" : {
          	"type": "string"
        },
        "version": {
          	"type": "number"
        },
        "library": {
          "type": "object",
          "properties": {
            	"type": {
                  	"type": "string"
                },
                "id": {
                  "type": "number"
                },
                "name": {
                  "type": "string"
                }
          }
        },
        "data": {
          "type": "object",
          "properties": {
            "key" : {
              "type": "string"
            },
            "version": {
              "type": "number"
            },
            "name": {
              "type": "string"
            }
          }
        }
    }
  }
})";

const char * const kItemSchema = R"(
{
  "$id": "http://rstudio.org/schemas/zotero-collections.json",
  "$schema": "http://json-schema.org/schema#",
  "title": "Zotero Collections Metadata Schema",
  "type": "array",
  "items": {
    "type": "object",
    "properties" : {
      	"key" : {
          	"type": "string"
        },
        "version": {
          	"type": "number"
        },
        "library": {
          "type": "object",
          "properties": {
            	"type": {
                  	"type": "string"
                },
                "id": {
                  "type": "number"
                },
                "name": {
                  "type": "string"
                }
          }
        },
        "data": {
          "type": "object",
          "properties": {
            "key" : {
              "type": "string"
            },
            "version": {
              "type": "number"
            },
            "itemType": {
              "type": "string"
            }
          }
        }
    }
  }
})";

std::string zoteroApiKey()
{
   return prefs::userPrefs().zoteroApiKey();
}

typedef boost::function<void(const Error&,int,json::Value)> ZoteroJsonResponseHandler;

void zoteroJsonRequest(const std::string& resource,
                       http::Fields queryParams,
                       bool authenticated,
                       const std::string& schema,
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
   asyncDownloadFile(url, headers, [handler, schema](const core::system::ProcessResult& result) {
      if (result.exitStatus == EXIT_SUCCESS)
      {
         json::Value resultJson;
         Error error;
         if (schema.length() > 0)
         {
            error = resultJson.parseAndValidate(result.stdOut, schema);
         } else {
            error = resultJson.parse(result.stdOut);
         }

         if (error)
         {
            handler(error, 500, json::Value());
         }
         else
         {
            handler(Success(), 200, resultJson);
         }
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
   zoteroJsonRequest(resource, queryParams, true, "", handler);
}



void zoteroKeyInfo(const ZoteroJsonResponseHandler& handler)
{
   boost::format fmt("keys/%s");
   zoteroJsonRequest(boost::str(fmt % zoteroApiKey()),
                     http::Fields(),
                     false,
                     kKeySchema,
                     handler);
}

void zoteroCollections(int userID, const ZoteroJsonResponseHandler& handler)
{
   http::Fields params;
   params.push_back(std::make_pair("format", "json"));

   boost::format fmt("users/%d/collections");
   zoteroJsonRequest(boost::str(fmt % userID),
                     params,
                     true,
                     kCollectionSchema,
                     handler);
}

void zoteroItemRequest(const std::string& path, const ZoteroJsonResponseHandler& handler) {

   http::Fields params;
   params.push_back(std::make_pair("format", "json"));
   params.push_back(std::make_pair("itemType", "-attachment"));

   zoteroJsonRequest(path,
                     params,
                     true,
                     kItemSchema,
                     handler);
}

void zoteroItemsForCollection(int userID, const std::string& collectionID, const ZoteroJsonResponseHandler& handler)
{
   boost::format fmt("users/%d/collections/%s/items");
   zoteroItemRequest(boost::str(fmt % userID % collectionID),
                     handler);
}

void zoteroDeletedData(int userID, int since, const ZoteroJsonResponseHandler& handler)
{
   http::Fields params;
   params.push_back(std::make_pair("since", std::to_string(since)));

   boost::format fmt("users/%d/deleted");
   zoteroJsonRequest(boost::str(fmt % userID),
                     params,
                     true,
                     "", // TODO: deleted schema
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


      zoteroItemsForCollection(userID, "HFB52FJF", [](const Error& error,int status,json::Value jsonValue) {
          if (error)
          {
             LOG_ERROR(error);
             return;
          }
          jsonValue.writeFormatted(std::cerr);
        });

/*     zoteroItems(userID, [](const Error& error,int status,json::Value jsonValue) {
       if (error)
       {
          LOG_ERROR(error);
          return;
       }

       jsonValue.writeFormatted(std::cerr);
     });


      zoteroCollections(userID, [](const Error& error,int status,json::Value jsonValue) {
         if (error)
         {
            LOG_ERROR(error);
            return;
         }
         jsonValue.writeFormatted(std::cerr);

      });

         */

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
