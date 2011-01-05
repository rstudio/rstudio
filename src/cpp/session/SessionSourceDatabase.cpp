/*
 * SessionSourceDatabase.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <session/SessionSourceDatabase.hpp>

#include <string>
#include <vector>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Hash.hpp>
#include <core/FileSerializer.hpp>
#include <core/DateTime.hpp>

#include <core/system/System.hpp>

#include <core/http/Util.hpp>

#include <session/SessionModuleContext.hpp>

// NOTE: if a file is deleted then its properties database entry is not
// deleted. this has two implications:
//
//   - storage is not reclaimed
//   - the properties can be "resurreced" and re-attached to another
//     file with the same path
//
// One way to overcome this might be to use filesystem metadata to store
// properties rather than a side-database

using namespace core;

namespace session {
namespace source_database {

namespace {

struct PropertiesDatabase
{
   FilePath path;
   FilePath indexFile;
   std::map<std::string,std::string> index;
};

Error getPropertiesDatabase(PropertiesDatabase* pDatabase)
{
   pDatabase->path = path().complete("properties");
   Error error = pDatabase->path.ensureDirectory();
   if (error)
      return error;

   pDatabase->indexFile = pDatabase->path.complete("INDEX");

   if (pDatabase->indexFile.exists())
      return readStringMapFromFile(pDatabase->indexFile, &(pDatabase->index));
   else
      return Success();
}

Error putProperties(const std::string& path, const json::Object& properties)
{
   // url escape path (so we can use key=value persistence)
   std::string escapedPath = http::util::urlEncode(path);

   // get properties database
   PropertiesDatabase propertiesDB;
   Error error = getPropertiesDatabase(&propertiesDB);
   if (error)
      return error;

   // use existing properties file if it exists, otherwise create new
   bool updateIndex = false;
   std::string propertiesFile = propertiesDB.index[escapedPath];
   if (propertiesFile.empty())
   {
      propertiesFile = core::system::generateUuid(false);
      propertiesDB.index[escapedPath] = propertiesFile;
      updateIndex = true;
   }

   // write the file
   std::ostringstream ostr ;
   json::writeFormatted(properties, ostr);
   FilePath propertiesFilePath = propertiesDB.path.complete(propertiesFile);
   error = writeStringToFile(propertiesFilePath, ostr.str());
   if (error)
      return error;

   // update the index if necessary
   if (updateIndex)
      return writeStringMapToFile(propertiesDB.indexFile, propertiesDB.index);
   else
      return Success();
}

Error getProperties(const std::string& path, json::Object* pProperties)
{
   // url escape path (so we can use key=value persistence)
   std::string escapedPath = http::util::urlEncode(path);

   // get properties database
   PropertiesDatabase propertiesDB;
   Error error = getPropertiesDatabase(&propertiesDB);
   if (error)
      return error;

   // check for properties file
   std::string propertiesFile = propertiesDB.index[escapedPath];
   if (propertiesFile.empty())
   {
      // return empty object if there is none
      *pProperties = json::Object();
      return Success();
   }

   // read the properties file
   std::string contents ;
   FilePath propertiesFilePath = propertiesDB.path.complete(propertiesFile);
   error = readStringFromFile(propertiesFilePath, &contents,
                              options().sourceLineEnding());
   if (error)
      return error;

   // parse the json
   json::Value value;
   if ( !json::parse(contents, &value) )
      return systemError(boost::system::errc::bad_message, ERROR_LOCATION);

   // return it
   if (json::isType<json::Object>(value))
      *pProperties = value.get_obj();
   return Success();
}

}  // anonymous namespace

SourceDocument::SourceDocument(const std::string& type)
{
   id_ = core::system::generateUuid();
   type_ = type;
   setContents("");
   dirty_ = false;
   created_ = date_time::millisecondsSinceEpoch();
   sourceOnSave_ = false;
}
   

std::string SourceDocument::getProperty(const std::string& name)
{
   if (properties_.find(name) != properties_.end())
   {
      json::Value valueJson = properties_[name];
      if (json::isType<std::string>(valueJson))
         return valueJson.get_str();
      else
         return "";
   }
   else
   {
      return "";
   }
}
      
// set contents from string
void SourceDocument::setContents(const std::string& contents)
{
   contents_ = contents;
   hash_ = hash::crc32Hash(contents_);
}

// set contents from file
Error SourceDocument::setPathAndContents(const std::string& path)
{
   // resolve aliased path
   FilePath docPath = module_context::resolveAliasedPath(path);

   // read contents
   std::string contents;
   Error error = readStringFromFile(docPath, &contents,
                                    options().sourceLineEnding());
   if (error)
      return error ;

   // update path and contents
   path_ = path;
   setContents(contents);
   lastKnownWriteTime_ = docPath.lastWriteTime();

   return Success();
}


void SourceDocument::editProperties(json::Object& properties)
{
   std::for_each(properties.begin(),
                 properties.end(),
                 boost::bind(&SourceDocument::editProperty, this, _1));
}

void SourceDocument::checkForExternalEdit(std::time_t* pTime)
{
   *pTime = 0;

   if (path_.empty())
      return;

   if (lastKnownWriteTime_ == 0)
      return;

   core::FilePath filePath = module_context::resolveAliasedPath(path_);
   if (!filePath.exists())
      return;

   std::time_t newTime = filePath.lastWriteTime();
   if (newTime != lastKnownWriteTime_)
      *pTime = newTime;
}

void SourceDocument::updateLastKnownWriteTime()
{
   lastKnownWriteTime_ = 0;
   if (path_.empty())
      return;

   core::FilePath filePath = module_context::resolveAliasedPath(path_);
   if (!filePath.exists())
      return;

   lastKnownWriteTime_ = filePath.lastWriteTime();
}
   
Error SourceDocument::readFromJson(json::Object* pDocJson)
{
   // NOTE: since this class is the one who presumably persisted the
   // json values in the first place we don't do "checked" access to
   // the json data elements. if the persistence format differs from
   // what we expect things will blow up. therefore if we change the
   // persistence format we need to make sure this code is robust
   // in the presence of the old format

   json::Object& docJson = *pDocJson;

   id_ = docJson["id"].get_str();
   json::Value path = docJson["path"];
   path_ = !path.is_null() ? path.get_str() : std::string();

   json::Value type = docJson["type"];
   type_ = !type.is_null() ? type.get_str() : std::string();

   setContents(docJson["contents"].get_str());
   dirty_ = docJson["dirty"].get_bool();
   created_ = docJson["created"].get_real();
   sourceOnSave_ = docJson["source_on_save"].get_bool();

   // read safely (migration)
   json::Value properties = docJson["properties"];
   properties_ = !properties.is_null() ? properties.get_obj() : json::Object();

   json::Value lastKnownWriteTime = docJson["lastKnownWriteTime"];
   lastKnownWriteTime_ = !lastKnownWriteTime.is_null()
                            ? lastKnownWriteTime.get_int64()
                            : 0;

   return Success();
}
   
void SourceDocument::writeToJson(json::Object* pDocJson) const
{
   json::Object& jsonDoc = *pDocJson;
   jsonDoc["id"] = id();
   jsonDoc["path"] = !path().empty() ? path_ : json::Value();
   jsonDoc["type"] = !type().empty() ? type_ : json::Value();
   jsonDoc["hash"] = hash();
   jsonDoc["contents"] = contents();
   jsonDoc["dirty"] = dirty();
   jsonDoc["created"] = created();
   jsonDoc["source_on_save"] = sourceOnSave();
   jsonDoc["properties"] = properties();
   jsonDoc["lastKnownWriteTime"] = json::Value(
         static_cast<boost::int64_t>(lastKnownWriteTime_));
}

void SourceDocument::editProperty(const json::Object::value_type& property)
{
   if (property.second.is_null())
   {
      properties_.erase(property.first);
   }
   else
   {
      properties_[property.first] = property.second;
   }
}

bool sortByCreated(const SourceDocument& doc1, const SourceDocument& doc2)
{
   return doc1.created() < doc2.created();
}

FilePath path()
{
   return module_context::userScratchPath().complete("source_database");
}
   
Error get(const std::string& id, SourceDocument* pDoc)
{
   FilePath filePath = source_database::path().complete(id);
   if (filePath.exists())
   {
      // read the contents of the file
      std::string contents ;
      Error error = readStringFromFile(filePath, &contents,
                                       options().sourceLineEnding());
      if (error)
         return error;
   
      // parse the json
      json::Value value;
      if ( !json::parse(contents, &value) )
      {
         return systemError(boost::system::errc::invalid_argument,
                            ERROR_LOCATION);
      }
      
      // initialize doc from json
      json::Object jsonDoc = value.get_obj();
      return pDoc->readFromJson(&jsonDoc);
   }
   else
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         ERROR_LOCATION);
   }
}

Error getDurableProperties(const std::string& path, json::Object* pProperties)
{
   return getProperties(path, pProperties);
}
   
Error list(std::vector<SourceDocument>* pDocs)
{
   std::vector<FilePath> files ;
   Error error = source_database::path().children(&files);
   if (error)
      return error ;
   
   BOOST_FOREACH( FilePath& filePath, files )
   {
      if (!filePath.isDirectory())
      {
         // get the source doc
         SourceDocument doc ;
         Error error = source_database::get(filePath.filename(), &doc);
         if (error)
            return error ;

         // add it to the list
         pDocs->push_back(doc);
      }
   }
   
   return Success();
}
   
Error put(const SourceDocument& doc)
{
   // get json representation
   json::Object jsonDoc ;
   doc.writeToJson(&jsonDoc);
   std::ostringstream ostr ;
   json::writeFormatted(jsonDoc, ostr);
   
   // write to file
   FilePath filePath = source_database::path().complete(doc.id());
   Error error = writeStringToFile(filePath, ostr.str());
   if (error)
      return error ;

   // write properties to durable storage
   error = putProperties(doc.path(), doc.properties());
   if (error)
      LOG_ERROR(error);

   return Success();
}
   
Error remove(const std::string& id)
{
   return source_database::path().complete(id).removeIfExists();
}
   
Error removeAll()
{
   std::vector<FilePath> files ;
   Error error = source_database::path().children(&files);
   if (error)
      return error ;
   
   BOOST_FOREACH( FilePath& filePath, files )
   {
      Error error = filePath.remove();
      if (error)
         return error ;
   }
   
   return Success();
}


Error getSourceDocumentsJson(core::json::Array* pJsonDocs)
{
   // get the docs and sort them by created
   std::vector<SourceDocument> docs ;
   Error error = source_database::list(&docs);
   if (error)
      return error ;
   std::sort(docs.begin(), docs.end(), sortByCreated);
   
   // populate the array
   pJsonDocs->clear();
   BOOST_FOREACH( SourceDocument& doc, docs )
   {
      json::Object jsonDoc ;
      doc.writeToJson(&jsonDoc);
      pJsonDocs->push_back(jsonDoc);
   }
   
   return Success();
}
      
Error initialize()
{
   // make sure the source database exists
   return source_database::path().ensureDirectory();
}

} // namespace source_database
} // namesapce session

