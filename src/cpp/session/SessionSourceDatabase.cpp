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
#include <boost/regex.hpp>
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

#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

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

namespace {

Error readAndDecode(const FilePath& docPath,
                    const std::string& encoding,
                    bool allowSubstChars,
                    std::string* pContents)
{
   // read contents
   std::string encodedContents;
   Error error = readStringFromFile(docPath, &encodedContents,
                                    options().sourceLineEnding());

   if (error)
      return error ;

   error = r::util::iconvstr(encodedContents, encoding, "UTF-8",
                             allowSubstChars, pContents);
   if (error)
      return error;

   stripBOM(pContents);
   // Detect invalid UTF-8 sequences and recover
   error = string_utils::utf8Clean(pContents->begin(),
                                   pContents->end(),
                                   '?');
   return error ;
}

} // anonymous namespace

// set contents from file
Error SourceDocument::setPathAndContents(const std::string& path,
                                         bool allowSubstChars)
{
   // resolve aliased path
   FilePath docPath = module_context::resolveAliasedPath(path);

   std::string contents;
   Error error = readAndDecode(docPath, encoding(), allowSubstChars,
                               &contents);
   if (error)
      return error ;

   // update path and contents
   path_ = path;
   setContents(contents);
   lastKnownWriteTime_ = docPath.lastWriteTime();

   return Success();
}

Error SourceDocument::updateDirty()
{
   if (path().empty())
   {
      dirty_ = !contents_.empty();
   }
   else if (dirty_)
   {
      // This doesn't actually guarantee that dirty state is correct. All
      // it does, at the most, is take a dirty document and mark it clean
      // if the contents are the same as on disk. This is important because
      // the client now has logic to detect when undo/redo causes a document
      // to be reverted to its previous state (i.e. a dirty document can
      // become clean through undo/redo), but that state doesn't get sent
      // back to the server.

      // We don't make a clean document dirty here, even if the contents
      // on disk are different, because we will do that on the client side
      // and the UI logic is a little complicated.

      FilePath docPath = module_context::resolveAliasedPath(path());
      if (docPath.exists() && docPath.size() <= (1024*1024))
      {
         std::string contents;
         Error error = readAndDecode(docPath, encoding(), true, &contents);
         if (error)
            return error;

         if (contents_.length() == contents.length() && hash_ == hash::crc32Hash(contents))
            dirty_ = false;
      }
   }
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

   json::Value encoding = docJson["encoding"];
   encoding_ = !encoding.is_null() ? encoding.get_str() : std::string();

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
   jsonDoc["encoding"] = encoding_;
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

bool sortByCreated(const boost::shared_ptr<SourceDocument>& pDoc1,
                   const boost::shared_ptr<SourceDocument>& pDoc2)
{
   return pDoc1->created() < pDoc2->created();
}

FilePath path()
{
   return module_context::scopedScratchPath().complete("source_database");
}
   
Error get(const std::string& id, boost::shared_ptr<SourceDocument> pDoc)
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

bool isSourceDocument(const FilePath& filePath)
{
   if (filePath.isDirectory())
      return false;
   else if (filePath.filename() == ".DS_Store")
      return false;
   else
      return true;
}

Error list(std::vector<boost::shared_ptr<SourceDocument> >* pDocs)
{
   std::vector<FilePath> files ;
   Error error = source_database::path().children(&files);
   if (error)
      return error ;
   
   BOOST_FOREACH( FilePath& filePath, files )
   {
      if (isSourceDocument(filePath))
      {
         // get the source doc
         boost::shared_ptr<SourceDocument> pDoc(new SourceDocument()) ;
         Error error = source_database::get(filePath.filename(), pDoc);
         if (!error)
            pDocs->push_back(pDoc);
         else
            LOG_ERROR(error);
      }
   }
   
   return Success();
}
   
Error put(boost::shared_ptr<SourceDocument> pDoc)
{
   // get json representation
   json::Object jsonDoc ;
   pDoc->writeToJson(&jsonDoc);
   std::ostringstream ostr ;
   json::writeFormatted(jsonDoc, ostr);
   
   // write to file
   FilePath filePath = source_database::path().complete(pDoc->id());
   Error error = writeStringToFile(filePath, ostr.str());
   if (error)
      return error ;

   // write properties to durable storage
   error = putProperties(pDoc->path(), pDoc->properties());
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
   std::vector<boost::shared_ptr<SourceDocument> > docs ;
   Error error = source_database::list(&docs);
   if (error)
      return error ;
   std::sort(docs.begin(), docs.end(), sortByCreated);
   
   // populate the array
   pJsonDocs->clear();
   BOOST_FOREACH( boost::shared_ptr<SourceDocument>& pDoc, docs )
   {
      // Force dirty state to be checked.
      // Client and server dirty state can get out of sync because
      // undo/redo on the client side can make dirty documents
      // become clean again. I tried pushing the client dirty state
      // back to the server but couldn't convince myself that I
      // got all the edge cases. This approach is simpler--just
      // compare the contents in the doc database to the contents
      // on disk, and only do it when listing documents. However
      // it does mean that reloading the client may cause a dirty
      // document to become clean (if the contents are identical
      // to what's on disk).
      error = pDoc->updateDirty();
      if (error)
         LOG_ERROR(error);

      json::Object jsonDoc ;
      pDoc->writeToJson(&jsonDoc);
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

