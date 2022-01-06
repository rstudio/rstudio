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

#include <boost/algorithm/algorithm.hpp>
#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Algorithm.hpp>
#include <core/FileSerializer.hpp>
#include <core/Database.hpp>
#include <shared_core/Hash.hpp>

#include <core/system/Process.hpp>

#include <session/prefs/UserState.hpp>
#include <session/SessionModuleContext.hpp>

#include "ZoteroCSL.hpp"
#include "ZoteroUtil.hpp"
#include "ZoteroBetterBibTeX.hpp"

#include "session-config.h"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {

Error execQuery(boost::shared_ptr<database::IConnection> pConnection,
                const std::string& sql,
                boost::function<void(const database::Row&)> rowHandler)
{
   database::Rowset rows;
   database::Query query = pConnection->query(sql);
   Error error = pConnection->execute(query, rows);
   if (error)
      return error;

   std::size_t rowCount = 0;
   for (database::RowsetIterator it = rows.begin(); it != rows.end(); ++it)
   {
      const database::Row& row = *it;
      rowHandler(row);
      rowCount++;
   }

   TRACE("SQL Executed", rowCount);
   TRACE(sql);

   return Success();
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


std::string creatorsSQL(const ZoteroCollectionSpec& spec)
{
   boost::format fmt(R"(
      SELECT
        items.key as key,
        strftime('%%s', items.clientDateModified) AS version,
        creators.firstName as firstName,
        creators.LastName as lastName,
        itemCreators.orderIndex,
        creatorTypes.creatorType as creatorType
      FROM
        items
        join itemTypes on items.itemTypeID = itemTypes.itemTypeID
        join libraries on items.libraryID = libraries.libraryID
        %1%
        join itemCreators on items.itemID = itemCreators.itemID
        join creators on creators.creatorID = itemCreators.creatorID
        join creatorTypes on itemCreators.creatorTypeID = creatorTypes.creatorTypeID
        left join deletedItems on items.itemId = deletedItems.itemID
      WHERE
        itemTypes.typeName <> 'attachment'
        AND itemTypes.typeName <> 'note'
        AND deletedItems.dateDeleted IS NULL
        %2%
      ORDER BY
        items.key ASC,
        itemCreators.orderIndex
   )");
   return boost::str(fmt %
        (spec.parentKey.empty() ? "" :  R"(join collectionItems on items.itemID = collectionItems.itemID
         join collections on collectionItems.collectionID = collections.collectionID)") %
        (spec.parentKey.empty() ? "AND libraries.libraryID = " + spec.key : "AND collections.key = '" + spec.key + "'"));
}

std::string collectionSQL(const ZoteroCollectionSpec& spec)
{
   boost::format fmt(R"(
      SELECT
         items.key as key,
         fields.fieldName as name,
         itemDataValues.value as value,
         itemTypeFields.orderIndex as fieldOrder
      FROM
         items
         join libraries on items.libraryID = libraries.libraryID
         %1%
         join itemTypes on items.itemTypeID = itemTypes.itemTypeID
         join itemData on items.itemID = itemData.itemID
         join itemDataValues on itemData.valueID = itemDataValues.valueID
         join fields on itemData.fieldID = fields.fieldID
         join itemTypeFields on (itemTypes.itemTypeID = itemTypeFields.itemTypeID
            AND fields.fieldID = itemTypeFields.fieldID)
         left join deletedItems on items.itemId = deletedItems.itemID
      WHERE
         itemTypes.typeName <> 'attachment'
         AND itemTypes.typeName <> 'note'
         AND deletedItems.dateDeleted IS NULL
         %2%
   UNION
      SELECT
         items.key as key,
         'type' as name,
         itemTypes.typeName as  value,
         0 as fieldOrder
      FROM
         items
         join itemTypes on items.itemTypeID = itemTypes.itemTypeID
         join libraries on items.libraryID = libraries.libraryID
         left join deletedItems on items.itemId = deletedItems.itemID
         %1%
      WHERE
         itemTypes.typeName <> 'attachment'
         AND itemTypes.typeName <> 'note'
         AND deletedItems.dateDeleted IS NULL
         %2%
   UNION
      SELECT
         items.key as key,
         'libraryID' as name,
         CAST(items.libraryID as text) as value,
         500 as fieldOrder
      FROM
         items
         join itemTypes on items.itemTypeID = itemTypes.itemTypeID
         join libraries on items.libraryID = libraries.libraryID
         left join deletedItems on items.itemId = deletedItems.itemID
         %1%
      WHERE
         itemTypes.typeName <> 'attachment'
         AND itemTypes.typeName <> 'note'
         AND deletedItems.dateDeleted IS NULL
         %2%
   UNION
      SELECT
         items.key as key,
         'collectionKeys' as name,
         libraries.libraryID || ',' || IFNULL(group_concat(collections.key), '') as value,
         10000 as fieldOrder
      FROM
         items
         join itemTypes on items.itemTypeID = itemTypes.itemTypeID
         left join collectionItems on items.itemID = collectionItems.itemID
         left join collections on collectionItems.collectionID = collections.collectionID
         join libraries on items.libraryID = libraries.libraryID
         left join deletedItems on items.itemId = deletedItems.itemID
      WHERE
         itemTypes.typeName <> 'attachment'
         AND itemTypes.typeName <> 'note'
         AND deletedItems.dateDeleted IS NULL
         %2%
      GROUP BY items.key
   ORDER BY
      key ASC,
      fieldOrder ASC   )");

   if (spec.parentKey.empty())
   {
      return boost::str(fmt % "" % ("AND libraries.libraryID = " + spec.key));
   }
   else
   {
      return boost::str(fmt %
         ("join collectionItems on items.itemID = collectionItems.itemID\n"
          "join collections on collectionItems.collectionID = collections.collectionID") %
         ("AND collections.key = '" + spec.key + "'")
      );
   }
}

double getCollectionVersion(boost::shared_ptr<database::IConnection> pConnection, const ZoteroCollectionSpec& spec) {
    // This is a library
    std::string query;
    if (spec.parentKey.empty()) {
        query = R"(
                   SELECT
                       IFNULL(strftime('%%s', MAX(MAX(items.clientDateModified), MAX(collections.clientDateModified))), '0') AS version
                   FROM
                       libraries
                       left join items on libraries.libraryID = items.libraryID
                       left join collections on libraries.libraryID = collections.libraryID
                       join itemTypes on items.itemTypeID = itemTypes.itemTypeID
                       left join deletedItems on items.itemId = deletedItems.itemID
                   WHERE
                       libraries.libraryID = %1%
                   AND
                       itemTypes.typeName <> 'attachment'
                   AND
                       itemTypes.typeName <> 'note'
                   AND
                       deletedItems.dateDeleted IS NULL
                )";
    } else {
        query = R"(
                SELECT
                    IFNULL(strftime('%%s', MAX(MAX(items.clientDateModified), MAX(collections.clientDateModified))), '0') AS version
                FROM
                    collections
                    left join collectionItems on collections.collectionID = collectionItems.collectionID
                    left join items on collectionItems.itemID = items.itemID
                    left join itemTypes on items.itemTypeID = itemTypes.itemTypeID
                    left join collections as parentCollections on collections.parentCollectionID = parentCollections.collectionID
                WHERE
                    collections.collectionID = %1%
                   )";

    }

    double version = 0;
    Error error = execQuery(pConnection, boost::str(boost::format(query) % spec.key),
                      [&version](const database::Row& row) {
        try
        {
           std::string versionStr = readString(row, "version", "0");
           version = safe_convert::stringTo<double>(versionStr, 0);
        }
        CATCH_UNEXPECTED_EXCEPTION

    });
    return version;
}

ZoteroCollection getCollection(boost::shared_ptr<database::IConnection> pConnection, const ZoteroCollectionSpec& spec)
{
   // default to return in case of error
   ZoteroCollection collection(spec);

   // get creators
   ZoteroCreatorsByKey creators;
   Error error = execQuery(pConnection, creatorsSQL(spec), [&creators](const database::Row& row) {
     std::string key = row.get<std::string>("key");
     ZoteroCreator creator;

     soci::indicator firstIndicator = row.get_indicator("firstName");
     if (firstIndicator == soci::i_ok)
     {
        creator.firstName = row.get<std::string>("firstName");
     }

     soci::indicator lastIndicator = row.get_indicator("lastName");
     if (lastIndicator == soci::i_ok)
     {
        creator.lastName = row.get<std::string>("lastName");
     }

     soci::indicator typeIndicator = row.get_indicator("creatorType");
     if (typeIndicator == soci::i_ok)
     {
        creator.creatorType = row.get<std::string>("creatorType");
     }
     creators[key].push_back(creator);
   });
   if (error)
   {
      LOG_ERROR(error);
      return collection;
   }

   std::map<std::string,std::string> currentItem;
   json::Array itemsJson;
   error = execQuery(pConnection, collectionSQL(spec),
                     [&creators, &currentItem, &itemsJson](const database::Row& row) {

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
         itemsJson.push_back(sqliteItemToCSL(currentItem, creators));
         currentItem.clear();
         currentItem["key"] = key;
      }

      // read the csl name value pairs
      soci::indicator nameIndicator = row.get_indicator("name");
      soci::indicator valueIndicator = row.get_indicator("value");
      if (nameIndicator == soci::i_ok && valueIndicator == soci::i_ok)
      {
        std::string name = row.get<std::string>("name");
        std::string value = readString(row, "value");

        // the data was returned without problems
        currentItem[name] = value;
      }
   });

   // add the final item (if we had one)
   if (currentItem.count("key"))
      itemsJson.push_back(sqliteItemToCSL(currentItem, creators));

   if (error)
   {
      LOG_ERROR(error);
      return collection;
   }

   // Read the collection version
   double version = getCollectionVersion(pConnection, spec);

   // success!
   collection.version = version;
   collection.items = itemsJson;
   return collection;
}

struct LibraryInfo
{
   LibraryInfo() : version(0) {}
   std::string name;
   double version;
};


ZoteroCollectionSpecs getCollections(boost::shared_ptr<database::IConnection> pConnection, bool librariesOnly = false)
{
   std::string librariesSql = R"(
                              SELECT
                                  CAST(libraries.libraryID as text) as collectionKey,
                                  IFNULL(groups.name, 'My Library') as collectionName,
                                  NULL as parentCollectionKey,
                                  IFNULL(strftime('%s', MAX(MAX(items.clientDateModified), MAX(collections.clientDateModified))), '0') AS version
                              FROM
                                  libraries
                                  left join items on libraries.libraryID = items.libraryID
                                  left join collections on libraries.libraryID = collections.libraryID
                                  join itemTypes on items.itemTypeID = itemTypes.itemTypeID
                                  left join deletedItems on items.itemId = deletedItems.itemID
                                  left join groups as groups on libraries.libraryID = groups.libraryID
                              WHERE
                                  libraries.type in ('user', 'group')
                              AND
                                  itemTypes.typeName <> 'attachment'
                              AND
                                  itemTypes.typeName <> 'note'
                              AND
                                  deletedItems.dateDeleted IS NULL
                              GROUP
                                  BY libraries.libraryID
                          )";

   std::string collectionsSql = R"(
                                SELECT
                                    collections.key as collectionKey,
                                    collections.collectionName as collectionName,
                                    IFNULL(parentCollections.key, libraries.libraryId) as parentCollectionKey,
                                    IFNULL(strftime('%s', MAX(MAX(items.clientDateModified), MAX(collections.clientDateModified))), '0') AS version
                                FROM
                                    collections
                                    join libraries on libraries.libraryID = collections.libraryID
                                    left join collectionItems on collections.collectionID = collectionItems.collectionID
                                    left join items on collectionItems.itemID = items.itemID
                                    left join itemTypes on items.itemTypeID = itemTypes.itemTypeID
                                    left join collections as parentCollections on collections.parentCollectionID = parentCollections.collectionID
                                    left join groups as groups on libraries.libraryID = groups.libraryID
                                GROUP BY
                                    collections.key
                            )";

    // If this is libraries only, just read the libraries, otherwise
    // union the library and collection SQL
    std::string sql = librariesSql;
    if (!librariesOnly) {
        sql = boost::str(boost::format("%1% UNION %2%") % librariesSql % collectionsSql);
    }

   ZoteroCollectionSpecs specs;
   Error error = execQuery(pConnection, sql, [&specs](const database::Row& row) {

      // had this issue: https://github.com/rstudio/rstudio/issues/8861
      // perhaps a corrupted collection or a collection using an older
      // schema that was unversioned?
      try
      {
         ZoteroCollectionSpec spec;
         spec.name = row.get<std::string>("collectionName");
         spec.key = readString(row, "collectionKey", "-1");

         std::string versionStr = readString(row, "version", "0");
         spec.version = safe_convert::stringTo<double>(versionStr, 0);

         const soci::indicator indicator = row.get_indicator("parentCollectionKey");
         if (indicator == soci::i_ok)
         {
            // If the parent key is not null, this is a child collection
            spec.parentKey = row.get<std::string>("parentCollectionKey");
         }
         specs.push_back(spec);
      }
      CATCH_UNEXPECTED_EXCEPTION
   });

   if (error)
      LOG_ERROR(error);

   return specs;
}


FilePath zoteroSqliteDir()
{
   FilePath sqliteDir = module_context::userScratchPath()
        .completeChildPath("zotero")
        .completeChildPath("sqlite");

   Error error = sqliteDir.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return sqliteDir;
}

FilePath zoteroSqliteCopyPath(std::string dataDir)
{
   std::string sqliteFile = hash::crc32HexHash(dataDir) + ".sqlite";
   return zoteroSqliteDir().completePath(sqliteFile);
}

Error connect(std::string dataDir, boost::shared_ptr<database::IConnection>* ppConnection)
{
   // get path to actual sqlite db
   FilePath dbFile(dataDir + "/zotero.sqlite");

   // get path to copy of file we will use for queries
   FilePath dbCopyFile = zoteroSqliteCopyPath(dataDir);

   // if the copy file doesn't exist or is older than the dbFile then make another copy
   bool databaseIsStale = dbCopyFile.getLastWriteTime() < dbFile.getLastWriteTime();
   if (databaseIsStale)
   {
      TRACE("Copying " + dbFile.getAbsolutePath());
      std::time_t writeTime = dbFile.getLastWriteTime();
      Error error = dbFile.copy(dbCopyFile, true);
      if (error)
         return error;
      dbCopyFile.setLastWriteTime(writeTime);
   }

   // create connection
   database::SqliteConnectionOptions options;
   options.file = string_utils::systemToUtf8(dbCopyFile.getAbsolutePath());
   options.readonly = true;
   Error error = database::connect(options, ppConnection);
   if (error)
   {
      // if there is an error connecting then delete the copy (perhaps it's corrupted?)
      Error removeError = dbCopyFile.remove();
      if (removeError)
         LOG_ERROR(error);
      return error;
   }

   // try a simple query to ensure there are no other problems (delete the copy if there are)
   error = execQuery(*ppConnection, "SELECT * FROM libraries", [](const database::Row&) {});
   if (error)
   {
      // if there is an error running the query then delete the copy (perhaps it's corrupted?)
      Error removeError = dbCopyFile.remove();
      if (removeError)
         LOG_ERROR(error);
      return error;
   }

   // success!
   return Success();
}


void getLocalCollectionSpecs(std::string key, std::vector<std::string> collections, ZoteroCollectionSpecsHandler handler)
{
   // connect to the database (log and return cache on error)
   boost::shared_ptr<database::IConnection> pConnection;
   Error error = connect(key, &pConnection);
   if (error)
   {
      LOG_ERROR(error);
      handler(error, std::vector<ZoteroCollectionSpec>());
      return;
   }

   // get all collections
   ZoteroCollectionSpecs specs = getCollections(pConnection);

   // filter the specs if specific connections are being queried
   ZoteroCollectionSpecs filteredSpecs;
   if (collections.size() > 0)
   {
      std::copy_if(specs.begin(), specs.end(), std::back_inserter(filteredSpecs),
                   [collections, specs](const ZoteroCollectionSpec& spec) {

         // find the top-level library of the spec (when the loop terminates
         // the targetSpec will be the library spec)
         ZoteroCollectionSpec targetSpec = spec;
         while (true)
         {
             ZoteroCollectionSpec parentSpec = findParentSpec(targetSpec, specs);
             if (parentSpec.empty())
                break;
             else
                targetSpec = parentSpec;
         }

         // include if the library is within the list of collections
         return std::count_if(collections.begin(), collections.end(), [targetSpec](const std::string& name) {
            return name == targetSpec.name;
         }) > 0;

      });
   }
   else
   {
      filteredSpecs = specs;
   }

   handler(Success(), filteredSpecs);
}

void getLocalCollections(std::string key,
                         std::vector<std::string> collections,
                         ZoteroCollectionSpecs cacheSpecs,
                         ZoteroCollectionsHandler handler)
{
   // connect to the database (log and return cache on error)
   boost::shared_ptr<database::IConnection> pConnection;
   Error error = connect(key, &pConnection);
   if (error)
   {
      LOG_ERROR(error);
      ZoteroCollections cachedCollections;
      std::transform(cacheSpecs.begin(), cacheSpecs.end(), std::back_inserter(cachedCollections),
                     [](const ZoteroCollectionSpec& spec) { return ZoteroCollection(spec); });
      handler(error, cachedCollections, "");
      return;
   }

   // divide collections into ones we need to do a download for, and one that
   // we already have an up to date version for
   ZoteroCollections upToDateCollections;
   std::vector<std::pair<std::string, ZoteroCollectionSpec>> downloadCollections;

   // get all of the user's libraries
   ZoteroCollectionSpecs userCollections = getCollections(pConnection, true);

   for (auto userCollection : userCollections)
   {  
      std::string name = userCollection.name;
      double version = userCollection.version;
      std::string key = userCollection.key;
      std::string parentKey = userCollection.parentKey;

      // see if this is a requested collection
      bool requested =
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
            ZoteroCollectionSpec collectionSpec(name, key, parentKey, version);
            // If the version is 0 this is a local instance that isn't incrementing version numbers, do not cache
            if (it->version != version || it->version == 0)
            {
               TRACE("Need to update collection " + name);
               downloadCollections.push_back(std::make_pair(name, collectionSpec));
            }
            else {
               TRACE("Collection " + name + " is up to date.");
               upToDateCollections.push_back(ZoteroCollection(collectionSpec));
            }
         }
         else
         {
            downloadCollections.push_back(std::make_pair(name, ZoteroCollectionSpec(name, key, parentKey, version)));
         }
      }
   }

   // read collections we need to then append them to already up to date collections
   ZoteroCollections resultCollections = upToDateCollections;
   for (auto downloadSpec : downloadCollections)
   {
      ZoteroCollection coll = getCollection(pConnection, downloadSpec.second);
      resultCollections.push_back(coll);
   }

   if (betterBibtexEnabled())
      betterBibtexProvideIds(resultCollections, handler);
   else
      handler(Success(), resultCollections, "");
}

void getLocalLibraryNames(std::string key, ZoteroLibrariesHandler handler)
{
   // connect to the database (log and return cache on error)
   boost::shared_ptr<database::IConnection> pConnection;
   Error error = connect(key, &pConnection);
   if (error)
   {
      LOG_ERROR(error);
      handler(error, std::vector<std::string>());
      return;
   }

   // get all collections
   ZoteroCollectionSpecs specs = getCollections(pConnection);

   // build library names
   std::vector<std::string> libraries;
   for (auto spec : specs)
   {
      if (spec.parentKey.empty())
         libraries.push_back(spec.name);
   }

   // return them
   handler(Success(), libraries);
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

FilePath platformProfileDir(FilePath profilePath)
{
#if defined(_WIN32) || defined(__APPLE__)
    return profilePath.getParent();
#else
    return profilePath;
#endif
}

FilePath defaultProfileDir()
{
   // read the lines
   FilePath profilesDir = zoteroProfilesDir();
   FilePath profileIni = platformProfileDir(profilesDir).completeChildPath("profiles.ini");
   if (profileIni.exists())
   {

       std::vector<std::string> lines;
       Error error = core::readStringVectorFromFile(profileIni, &lines);
       if (error)
       {
          LOG_ERROR(error);
          return FilePath();
       }

       // find the default profile
       boost::regex regex("^\\[.*?\\]$");
       boost::regex valueRegex("^(.*)=(.*)$");
       std::string sectionPath;
       bool sectionPathIsRelative = false;
       bool sectionIsDefault = false;
       for (auto line : lines)
       {
          boost::smatch match;
          if (boost::regex_search(line, match, regex))
          {
             sectionPath = "";
             sectionPathIsRelative = false;
             sectionIsDefault = false;
          }
          else if (boost::regex_search(line, match, valueRegex))
          {
             std::string key = string_utils::toLower(boost::algorithm::trim_copy(std::string(match[1])));
             std::string value = boost::algorithm::trim_copy(std::string(match[2]));
             if (key == "path")
                sectionPath = value;
             else if (key == "isrelative")
                sectionPathIsRelative = value == "1";
             else if (key == "default")
                sectionIsDefault = value == "1";
          }

          if (sectionIsDefault && !sectionPath.empty())
          {
             if (sectionPathIsRelative)
                return platformProfileDir(profilesDir).completeChildPath(sectionPath);
             else
                return FilePath(sectionPath);
          }
       }

    }
    return FilePath();

}

DetectedLocalZoteroConfig detectLocalZoteroConfig()
{
   // we'll fall back to the default if we can't find another dir in the profile
   DetectedLocalZoteroConfig config;
   config.dataDirectory = defaultZoteroDataDir();

   // find the prefs for the default profile
   FilePath profileDir = defaultProfileDir();
   FilePath prefsFile = profileDir.completeChildPath("prefs.js");
   if (prefsFile.exists())
   {
      // read the prefs.js file
      std::string prefs;
      Error error = core::readStringFromFile(prefsFile, &prefs);
      if (error)
         LOG_ERROR(error);

      // look for the zotero.dataDir pref
      boost::smatch match;
      boost::regex regex("user_pref\\(\"extensions.zotero.dataDir\",\\s*\"([^\"]+)\"\\);");
      if (boost::regex_search(prefs, match, regex))
      {
         // prefs file escapes backslahes (it's javascript) so convert them
         std::string dataDirMatch = match[1];
         dataDirMatch = boost::algorithm::replace_all_copy(dataDirMatch, "\\\\", "/");

         // set dataDir only if the path exists
         FilePath profileDataDir(dataDirMatch);
         if (profileDataDir.exists())
            config.dataDirectory = profileDataDir;
      }

      // look for better bibtex in the config
      config.betterBibtex = betterBibtexInConfig(prefs);
   }


   // no data directory if it doesn't exist
   if (!config.dataDirectory.exists())
      config.dataDirectory = FilePath();

   // return the config
   return config;
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
DetectedLocalZoteroConfig detectedLocalZoteroConfig()
{
   if (localZoteroAvailable())
   {
      return detectLocalZoteroConfig();
   }
   else
   {
      return DetectedLocalZoteroConfig();
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
      return detectedLocalZoteroConfig().dataDirectory;
}


ZoteroCollectionSource localCollections()
{
   ZoteroCollectionSource source;
   source.getCollections = getLocalCollections;
   source.getLibraryNames = getLocalLibraryNames;
   source.getCollectionSpecs = getLocalCollectionSpecs;
   return source;
}

} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
