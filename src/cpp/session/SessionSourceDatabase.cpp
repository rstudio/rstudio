/*
 * SessionSourceDatabase.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include <core/FileUtils.hpp>
#include <core/DateTime.hpp>

#include <core/system/System.hpp>

#include <core/http/Util.hpp>

#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "SessionSourceDatabaseSupervisor.hpp"

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
   pDatabase->path = module_context::scopedScratchPath().complete("sdb/prop");
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
      FilePath propFile = file_utils::uniqueFilePath(propertiesDB.path);
      propertiesFile = propFile.filename();
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
   FilePath srcDBPath = source_database::path();
   FilePath docPath = file_utils::uniqueFilePath(srcDBPath);
   id_ = docPath.filename();
   type_ = type;
   setContents("");
   dirty_ = false;
   created_ = date_time::millisecondsSinceEpoch();
   sourceOnSave_ = false;
}
   

std::string SourceDocument::getProperty(const std::string& name) const
{
   json::Object::const_iterator it = properties_.find(name);
   if (it != properties_.end())
   {
      json::Value valueJson = it->second;
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

bool SourceDocument::isUntitled() const
{
   return path().empty() && !getProperty("tempName").empty();
}

// set contents from string
void SourceDocument::setContents(const std::string& contents)
{
   contents_ = contents;
   hash_ = hash::crc32Hash(contents_);
}

// set contents from file
Error SourceDocument::setPathAndContents(const std::string& path,
                                         bool allowSubstChars)
{
   // resolve aliased path
   FilePath docPath = module_context::resolveAliasedPath(path);

   std::string contents;
   Error error = module_context::readAndDecodeFile(docPath,
                                                   encoding(),
                                                   allowSubstChars,
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
         Error error = module_context::readAndDecodeFile(docPath,
                                                         encoding(),
                                                         true,
                                                         &contents);
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

   try
   {
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

      json::Value folds = docJson["folds"];
      folds_ = !folds.is_null() ? folds.get_str() : std::string();

      return Success();
   }
   catch(const std::exception& e)
   {
      return systemError(boost::system::errc::protocol_error,
                         e.what(),
                         ERROR_LOCATION);
   }
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
   jsonDoc["folds"] = folds();
   jsonDoc["lastKnownWriteTime"] = json::Value(
         static_cast<boost::int64_t>(lastKnownWriteTime_));
   jsonDoc["encoding"] = encoding_;
}

Error SourceDocument::writeToFile(const FilePath& filePath) const
{
   // get json representation
   json::Object jsonDoc ;
   writeToJson(&jsonDoc);
   std::ostringstream ostr ;
   json::writeFormatted(jsonDoc, ostr);

   // write to file
   return writeStringToFile(filePath, ostr.str());
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

namespace {

FilePath s_sourceDBPath;

} // anonymous namespace

FilePath path()
{
   return s_sourceDBPath;
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
   else if (filePath.filename() == "lock_file")
      return false;
   else
      return true;
}

void logUnsafeSourceDocument(const FilePath& filePath,
                             const std::string& reason)
{
   std::string msg = "Excluded unsafe source document";
   if (!filePath.empty())
      msg += " (" + filePath.absolutePath() + ")";
   msg += ": " + reason;
   LOG_WARNING_MESSAGE(msg);
}

bool isSafeSourceDocument(const FilePath& docDbPath,
                          boost::shared_ptr<SourceDocument> pDoc)
{
   // get a filepath and use it for filtering if we can
   FilePath filePath;
   if (!pDoc->path().empty())
   {
      filePath = FilePath(pDoc->path());
      if (filePath.extensionLowerCase() == ".rdata")
      {
         logUnsafeSourceDocument(filePath, ".RData file");
         return false;
      }
   }

   // get the size of the file in KB
   uintmax_t docSizeKb = docDbPath.size() / 1024;
   std::string kbStr = boost::lexical_cast<std::string>(docSizeKb);

   // if it's larger than 2MB then always drop it (that's the limit
   // enforced by the editor)
   if (docSizeKb > (2 * 1024))
   {
      logUnsafeSourceDocument(filePath, "File too large (" + kbStr + ")");
      return false;
   }

   // if it's larger then 500K and not dirty then drop it as well
   // (that's the file size considered "large" on the client)
   else if (!pDoc->dirty() && (docSizeKb > 512))
   {
      logUnsafeSourceDocument(filePath, "File too large (" + kbStr + ")");
      return false;
   }

   else
   {
      return true;
   }
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
         {
            // safety filter
            if (isSafeSourceDocument(filePath, pDoc))
               pDocs->push_back(pDoc);
         }
         else
            LOG_ERROR(error);
      }
   }
   
   return Success();
}
   
Error put(boost::shared_ptr<SourceDocument> pDoc)
{   
   // write to file
   FilePath filePath = source_database::path().complete(pDoc->id());
   Error error = pDoc->writeToFile(filePath);
   if (error)
      return error ;

   // write properties to durable storage (if there is a path)
   if (!pDoc->path().empty())
   {
      error = putProperties(pDoc->path(), pDoc->properties());
      if (error)
         LOG_ERROR(error);
   }

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

namespace {

void onShutdown(bool)
{
   Error error = supervisor::detachFromSourceDatabase();
   if (error)
      LOG_ERROR(error);
}

} // anonymous namespace

Error initialize()
{
   // provision a source database directory
   Error error = supervisor::attachToSourceDatabase(&s_sourceDBPath);
   if (error)
      return error;

   // signup for the shutdown event
   module_context::events().onShutdown.connect(onShutdown);

   return Success();
}

} // namespace source_database
} // namesapce session

