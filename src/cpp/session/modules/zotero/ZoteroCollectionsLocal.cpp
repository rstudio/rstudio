/*
 * ZoteroCollectionsLocal.cpp
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

#include "ZoteroCollectionsLocal.hpp"

#include <boost/bind.hpp>
#include <boost/algorithm/algorithm.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/FileSerializer.hpp>
#include <core/Database.hpp>

#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/prefs/UserState.hpp>
#include <session/SessionModuleContext.hpp>

#include "session-config.h"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {

SEXP createCacheSpecSEXP( ZoteroCollectionSpec cacheSpec, r::sexp::Protect* pProtect)
{
   std::vector<std::string> names;
   names.push_back(kName);
   names.push_back(kVersion);
   SEXP cacheSpecSEXP = r::sexp::createList(names, pProtect);
   r::sexp::setNamedListElement(cacheSpecSEXP, kName, cacheSpec.name);
   r::sexp::setNamedListElement(cacheSpecSEXP, kVersion, cacheSpec.version);
   return cacheSpecSEXP;
}

void execQuery(boost::shared_ptr<database::IConnection> pConnection,
               const std::string& sql,
               boost::function<void(const database::Row&)> rowHandler)
{
   database::Rowset rows;
   database::Query query = pConnection->query(sql);
   Error error = pConnection->execute(query, rows);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   for (database::RowsetIterator it = rows.begin(); it != rows.end(); ++it)
   {
      const database::Row& row = *it;
      rowHandler(row);
   }
}

ZoteroCollectionSpecs getCollections(boost::shared_ptr<database::IConnection> pConnection)
{
   ZoteroCollectionSpecs specs;
   execQuery(pConnection, "select collectionName, version from collections", [&specs](const database::Row& row) {
      ZoteroCollectionSpec spec;
      spec.name = row.get<std::string>("collectionName");
      spec.version = row.get<int>("version");
      specs.push_back(spec);
   });
   return specs;
}

int getLibraryVersion(boost::shared_ptr<database::IConnection> pConnection)
{
   int version = 0;
   execQuery(pConnection, "SELECT MAX(version) AS version from items", [&version](const database::Row& row) {
      std::string versionStr = row.get<std::string>("version");
      version = safe_convert::stringTo<int>(versionStr, 0);
   });
   return version;
}


void testZoteroSQLite(std::string dataDir)
{
   // connect to sqlite
   std::string db = dataDir + "/zotero.sqlite";
   database::SqliteConnectionOptions options = { db };
   boost::shared_ptr<database::IConnection> pConnection;
   Error error = database::connect(options, &pConnection);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   std::cerr << "library: " << getLibraryVersion(pConnection) << std::endl;

   ZoteroCollectionSpecs specs = getCollections(pConnection);
   for (auto spec : specs)
      std::cerr << spec.name << ": " << spec.version << std::endl;



}

void getLocalLibrary(std::string key,
                     ZoteroCollectionSpec cacheSpec,
                     ZoteroCollectionsHandler handler)
{
   testZoteroSQLite(key);

   r::sexp::Protect protect;
   std::string libraryJsonStr;
   Error error = r::exec::RFunction(".rs.zoteroGetLibrary", key,
                                    createCacheSpecSEXP(cacheSpec, &protect)).call(&libraryJsonStr);
   if (error)
   {
      handler(error, std::vector<ZoteroCollection>());
   }
   else
   {
      json::Object libraryJson;
      error = libraryJson.parse(libraryJsonStr);
      if (error)
      {
         handler(error, std::vector<ZoteroCollection>());
      }
      else
      {
         ZoteroCollection collection;
         collection.name = libraryJson[kName].getString();
         collection.version = libraryJson[kVersion].getInt();
         collection.items = libraryJson[kItems].getArray();
         handler(Success(), std::vector<ZoteroCollection>{ collection });
      }
   }
}


void getLocalCollections(std::string key,
                         std::vector<std::string> collections,
                         ZoteroCollectionSpecs cacheSpecs,
                         ZoteroCollectionsHandler handler)
{
    json::Array cacheSpecsJson;
    std::transform(cacheSpecs.begin(), cacheSpecs.end(), std::back_inserter(cacheSpecsJson), [](ZoteroCollectionSpec spec) {
       json::Object specJson;
       specJson[kName] = spec.name;
       specJson[kVersion] = spec.version;
       return specJson;
    });

    r::sexp::Protect protect;
    std::string collectionJsonStr;
    Error error = r::exec::RFunction(".rs.zoteroGetCollections", key, collections, cacheSpecsJson)
                                     .call(&collectionJsonStr);
    if (error)
    {
       handler(error, std::vector<ZoteroCollection>());
    }
    else
    {
       json::Array collectionsJson;
       error = collectionsJson.parse(collectionJsonStr);
       if (error)
       {
          handler(error, std::vector<ZoteroCollection>());
       }
       else
       {
           ZoteroCollections collections;
           std::transform(collectionsJson.begin(), collectionsJson.end(), std::back_inserter(collections), [](json::Value json) {
              ZoteroCollection collection;
              json::Object collectionJson = json.getObject();
              collection.name = collectionJson[kName].getString();
              collection.version = collectionJson[kVersion].getInt();
              collection.items = collectionJson[kItems].getArray();
              return collection;
           });
           handler(Success(), collections);
       }
    }
}

FilePath userHomeDir()
{
   std::string homeEnv;
#ifdef _WIN32
   homeEnv = "USERPROFILE";
#else
   homeEnv = "HOME";
#endif
   return FilePath(string_utils::systemToUtf8(core::system::getenv(homeEnv)));
}

// https://www.zotero.org/support/kb/profile_directory
FilePath zoteroProfilesDir()
{
   FilePath homeDir = userHomeDir();
   std::string profilesDir;
#if defined(_WIN32)
   profilesDir = "AppData\\Roaming\\Zotero\\Zotero\\Profiles";
#elif defined(__APPLE__)
   profilesDir = "Library/Application Support/Zotero/Profiles";
#else
   profilesDir = ".zotero/zotero";
#endif
   return homeDir.completeChildPath(profilesDir);
}

// https://www.zotero.org/support/zotero_data
FilePath defaultZoteroDataDir()
{
   FilePath homeDir = userHomeDir();
   return homeDir.completeChildPath("Zotero");
}

FilePath detectZoteroDataDir()
{
   // we'll fall back to the default if we can't find another dir in the profile
   FilePath dataDir = defaultZoteroDataDir();

   // find the zotero profiles dir
   FilePath profilesDir = zoteroProfilesDir();
   if (profilesDir.exists())
   {
      // there will be one path in the directory
      std::vector<FilePath> children;
      Error error = profilesDir.getChildren(children);
      if (error)
         LOG_ERROR(error);
      if (children.size() > 0)
      {
         // there will be a single directory inside the profiles dir
         FilePath profileDir = children[0];
         FilePath prefsFile = profileDir.completeChildPath("prefs.js");
         if (prefsFile.exists())
         {
            // read the prefs.js file
            std::string prefs;
            error = core::readStringFromFile(prefsFile, &prefs);
            if (error)
               LOG_ERROR(error);

            // look for the zotero.dataDir pref
            boost::smatch match;
            boost::regex regex("user_pref\\(\"extensions.zotero.dataDir\",\\s*\"([^\"]+)\"\\);");
            if (boost::regex_search(prefs, match, regex))
            {
               // set dataDiroly if the path exists
               FilePath profileDataDir(match[1]);
               if (profileDataDir.exists())
                  dataDir = profileDataDir;
            }
         }
      }
   }

   // return the data dir
   return dataDir;
}


} // end anonymous namespace


bool localZoteroAvailable()
{
   // availability based on server vs. desktop
#ifdef RSTUDIO_SERVER
   bool local = false;
#else
   bool local = true;
#endif

   // however, also make it available in debug mode for local dev/test
#ifndef NDEBUG
   local = true;
#endif

   return local;
}

// Detect the Zotero data directory and return it if it exists
FilePath detectedZoteroDataDirectory()
{
   if (localZoteroAvailable())
   {
      FilePath dataDir = detectZoteroDataDir();
      if (dataDir.exists())
         return dataDir;
      else
         return FilePath();
   }
   else
   {
      return FilePath();
   }
}


// Returns the zoteroDataDirectory (if any). This will return a valid FilePath
// if the user has specified a zotero data dir in the preferences; OR if
// a zotero data dir was detected on the system. In the former case the
// path may not exist (and this should be logged as an error)
FilePath zoteroDataDirectory()
{
   std::string dataDir = prefs::userState().zoteroDataDir();
   if (!dataDir.empty())
      return module_context::resolveAliasedPath(dataDir);
   else
      return detectedZoteroDataDirectory();
}


ZoteroCollectionSource localCollections()
{
   ZoteroCollectionSource source;
   source.getLibrary = getLocalLibrary;
   source.getCollections = getLocalCollections;
   return source;
}

} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
