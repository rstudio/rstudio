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

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/FileSerializer.hpp>
#include <core/Database.hpp>

#include <core/system/Process.hpp>

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

struct ZoteroCollectionDBSpec : public ZoteroCollectionSpec
{
   int id;
};
typedef std::vector<ZoteroCollectionDBSpec> ZoteroCollectionDBSpecs;

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

template <typename T>
std::string readString(const database::Row& row, T accessor, std::string defaultValue = "")
{
   std::ostringstream ostr;
   auto& props = row.get_properties(accessor);
   switch(props.get_data_type())
   {
   case soci::dt_string:
      ostr << row.get<std::string>(accessor);
      break;
   case soci::dt_double:
      ostr << row.get<double>(accessor);
      break;
   case soci::dt_integer:
      ostr << row.get<int>(accessor);
      break;
   case soci::dt_long_long:
      ostr << row.get<long long>(accessor);
      break;
   case soci::dt_unsigned_long_long:
      ostr << row.get<unsigned long long>(accessor);
      break;
   default:
      ostr << defaultValue;
   }
   return ostr.str();
}

json::Object itemToCSLJson(std::map<std::string,std::string> item)
{
   json::Object cslJson;
   for (auto field : item)
   {
      cslJson[field.first] = field.second;
   }
   return cslJson;
}


ZoteroCollection getCollection(boost::shared_ptr<database::IConnection> pConnection,
                               const std::string &name,
                               const std::string& sql)
{
   int version = 0;
   std::map<std::string,std::string> currentItem;
   json::Array itemsJson;
   execQuery(pConnection, sql, [&version, &currentItem, &itemsJson](const database::Row& row) {

      std::string key = row.get<std::string>("key");
      std::string currentKey = currentItem.count("key") ? currentItem["key"] : "";

      // inception
      if (currentKey.empty())
      {
         currentItem["key"] = key;
      }

      // finished an item
      else if (key != currentKey)
      {
         itemsJson.push_back(itemToCSLJson(currentItem));
         currentItem.clear();
         currentItem["key"] = key;
      }

      // update version
      int rowVersion = row.get<int>("version");
      if (rowVersion > version)
         version = rowVersion;

      // read the csl
      std::string name = row.get<std::string>("name");
      std::string value = readString(row, "value");
      currentItem[name] = value;
   });

   // add the final item (if we had one)
   if (currentItem.count("key"))
      itemsJson.push_back(itemToCSLJson(currentItem));


   // TODO: run creators query

   // success!
   ZoteroCollection collection(ZoteroCollectionSpec(name, version));
   collection.items = itemsJson;
   return collection;
}


ZoteroCollectionDBSpecs getCollections(boost::shared_ptr<database::IConnection> pConnection)
{
   ZoteroCollectionDBSpecs specs;
   execQuery(pConnection, "select collectionID, collectionName, version from collections", [&specs](const database::Row& row) {
      ZoteroCollectionDBSpec spec;
      spec.id = row.get<int>("collectionID");
      spec.name = row.get<std::string>("collectionName");
      spec.version = row.get<int>("version");
      specs.push_back(spec);
   });
   return specs;
}


std::string collectionSQL(const std::string& name = "")
{
   boost::format fmt(R"(
      SELECT
         items.key as key,
         items.version,
         fields.fieldName as name,
         itemDataValues.value as value
      FROM
         items
         join itemTypes on items.itemTypeID = itemTypes.itemTypeID
         join itemTypeFields on itemTypes.itemTypeID = itemTypeFields.itemTypeID
         join libraries on items.libraryID = libraries.libraryID
         join itemData on items.itemID = itemData.itemID
         join itemDataValues on itemData.valueID = itemDataValues.valueID
         join fields on itemData.fieldID = fields.fieldID
         %1%
      WHERE
        libraries.type = 'user'
        AND itemTypes.typeName <> 'attachment'
        %2%
      ORDER BY
        items.key ASC,
        itemTypeFields.orderIndex
   )");
   return boost::str(fmt %
      (!name.empty() ? "join collections on libraries.libraryID = collections.libraryID" : "") %
      (!name.empty() ? "AND collections.collectionName = '" + name + "'" : ""));
}

ZoteroCollection getLibrary(boost::shared_ptr<database::IConnection> pConnection)
{
   return getCollection(pConnection, kMyLibrary, collectionSQL());
}


int getLibraryVersion(boost::shared_ptr<database::IConnection> pConnection)
{
   int version = 0;
   execQuery(pConnection, "SELECT MAX(version) AS version from items", [&version](const database::Row& row) {
      std::string versionStr = readString(row, 0, "0");
      version = safe_convert::stringTo<int>(versionStr, 0);
   });
   return version;
}


Error connect(std::string dataDir, boost::shared_ptr<database::IConnection>* ppConnection)
{
   std::string db = dataDir + "/zotero.sqlite";
   database::SqliteConnectionOptions options = { db };
   return database::connect(options, ppConnection);
}

void getLocalLibrary(std::string key,
                     ZoteroCollectionSpec cacheSpec,
                     ZoteroCollectionsHandler handler)
{
   // connect to the database
   boost::shared_ptr<database::IConnection> pConnection;
   Error error = connect(key, &pConnection);
   if (error)
   {
      handler(error, std::vector<ZoteroCollection>());
   }

   // get the library version and reflect the cache back if it's up to date
   int version = getLibraryVersion(pConnection);
   if (version >= cacheSpec.version)
   {
      handler(error, { cacheSpec });
   }
   else
   {
      ZoteroCollection library = getLibrary(pConnection);
      handler(Success(), std::vector<ZoteroCollection>{ library });
   }
}

void getLocalCollections(std::string key,
                         std::vector<std::string> collections,
                         ZoteroCollectionSpecs cacheSpecs,
                         ZoteroCollectionsHandler handler)
{
   // connect to the database
   boost::shared_ptr<database::IConnection> pConnection;
   Error error = connect(key, &pConnection);
   if (error)
   {
      handler(error, std::vector<ZoteroCollection>());
   }

   // divide collections into ones we need to do a download for, and one that
   // we already have an up to date version for
   ZoteroCollections upToDateCollections;
   std::vector<std::pair<int, ZoteroCollectionSpec>> downloadCollections;

   // get all of the user's collections
   ZoteroCollectionDBSpecs userCollections = getCollections(pConnection);
   for (auto userCollection : userCollections)
   {
      std::string name = userCollection.name;
      int version = userCollection.version;
      int collectionId = userCollection.id;

      // see if this is a requested collection
      bool requested =
        collections.size() == 0 || // all collections requested
        std::count_if(collections.begin(),
                      collections.end(),
                      [name](const std::string& str) { return boost::algorithm::iequals(name, str); }) > 0 ;

      if (requested)
      {
         // see if we need to do a download for this collection
         ZoteroCollectionSpecs::const_iterator it = std::find_if(
           cacheSpecs.begin(), cacheSpecs.end(), [name](ZoteroCollectionSpec spec) { return boost::algorithm::iequals(spec.name,name); }
         );
         if (it != cacheSpecs.end())
         {
            ZoteroCollectionSpec collectionSpec(name, version);
            if (it->version < version)
               downloadCollections.push_back(std::make_pair(collectionId, collectionSpec));
            else
               upToDateCollections.push_back(collectionSpec);
         }
         else
         {
            downloadCollections.push_back(std::make_pair(collectionId, ZoteroCollectionSpec(name, version)));
         }
      }
   }

   // read collections we need to then append them to already up to date collections
   ZoteroCollections resultCollections = upToDateCollections;
   for (auto downloadSpec : downloadCollections)
   {
      std::string name = downloadSpec.second.name;
      resultCollections.push_back(getCollection(pConnection, name, collectionSQL(name)));
   }
   handler(Success(), resultCollections);
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
