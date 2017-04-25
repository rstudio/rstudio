/*
 * SessionSourceDatabase.cpp
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
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "SessionSourceDatabaseSupervisor.hpp"

#define kContentsSuffix "-contents"

// NOTE: if a file is deleted then its properties database entry is not
// deleted. this has two implications:
//
//   - storage is not reclaimed
//   - the properties can be "resurrected" and re-attached to another
//     file with the same path
//
// One way to overcome this might be to use filesystem metadata to store
// properties rather than a side-database

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace source_database {

// static members
const char * const SourceDocument::SourceDocumentTypeSweave    = "sweave";
const char * const SourceDocument::SourceDocumentTypeRSource   = "r_source";
const char * const SourceDocument::SourceDocumentTypeRMarkdown = "r_markdown";
const char * const SourceDocument::SourceDocumentTypeRHTML     = "r_html";
const char * const SourceDocument::SourceDocumentTypeCpp       = "cpp";

namespace {

// cached mapping of document id to document path (facilitates efficient path
// lookup)
std::map<std::string, std::string> s_idToPath;

struct PropertiesDatabase
{
   FilePath path;
   FilePath indexFile;
   std::map<std::string,std::string> index;
};

Error getPropertiesDatabase(PropertiesDatabase* pDatabase)
{
   pDatabase->path = module_context::scopedScratchPath().complete(kSessionSourceDatabasePrefix "/prop");
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

json::Value pathToProjectPath(const std::string& path)
{
   // no project
   projects::ProjectContext& projectContext = projects::projectContext();
   if (!projectContext.hasProject())
      return json::Value();

   // no path
   if (path.empty())
      return json::Value();

   // return relative path if we are within the project directory
   FilePath filePath = module_context::resolveAliasedPath(path);
   if (filePath.isWithin(projectContext.directory()))
      return filePath.relativePath(projectContext.directory());
   else
      return json::Value();
}

std::string pathFromProjectPath(json::Value projPathJson)
{
   // no project
   projects::ProjectContext& projectContext = projects::projectContext();
   if (!projectContext.hasProject())
      return std::string();

   // no proj path
   std::string projPath = !projPathJson.is_null() ? projPathJson.get_str() :
                                                    std::string();
   if (projPath.empty())
      return std::string();

   // interpret path relative to project directory
   FilePath filePath = projectContext.directory().childPath(projPath);
   if (filePath.exists())
      return module_context::createAliasedPath(filePath);
   else
      return std::string();
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
   relativeOrder_ = 0;
   lastContentUpdate_ = date_time::millisecondsSinceEpoch();
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
   lastContentUpdate_ = date_time::millisecondsSinceEpoch();
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

   // rewind the last content update to the file's write time
   lastContentUpdate_ = lastKnownWriteTime_;

   return Success();
}

Error SourceDocument::contentsMatchDisk(bool *pMatches)
{
   *pMatches = false;
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

      *pMatches = contents_.length() == contents.length() && 
                  hash_ == hash::crc32Hash(contents);
   }

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

      bool matches = false;
      Error error = contentsMatchDisk(&matches);
      if (error)
         return error;
      if (matches)
         dirty_ = false;
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

void SourceDocument::setLastKnownWriteTime(std::time_t time)
{
   lastKnownWriteTime_ = time;
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

      // if we have a project_path field then it supercedes the path field
      // (since it would correctly survive a moved project folder)
      std::string projPath = pathFromProjectPath(docJson["project_path"]);
      if (!projPath.empty())
         path_ = projPath;

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

      json::Value order = docJson["relative_order"];
      relativeOrder_ = !order.is_null() ? order.get_int() : 0;

      json::Value lastContentUpdate = docJson["last_content_update"];
      lastContentUpdate_ = !lastContentUpdate.is_null() ? 
                               lastContentUpdate.get_int64() : 0;

      json::Value collabServer = docJson["collab_server"];
      collabServer_ = !collabServer.is_null() ? collabServer.get_str() : 
                                                std::string();

      json::Value sourceWindow = docJson["source_window"];
      sourceWindow_ = !sourceWindow.is_null() ? sourceWindow.get_str() :
                                                std::string();

      return Success();
   }
   catch(const std::exception& e)
   {
      return systemError(boost::system::errc::protocol_error,
                         e.what(),
                         ERROR_LOCATION);
   }
}
   
void SourceDocument::writeToJson(json::Object* pDocJson, bool includeContents) const
{
   json::Object& jsonDoc = *pDocJson;
   jsonDoc["id"] = id();
   jsonDoc["path"] = !path().empty() ? path_ : json::Value();
   jsonDoc["project_path"] = pathToProjectPath(path_);
   jsonDoc["type"] = !type().empty() ? type_ : json::Value();
   jsonDoc["hash"] = hash();
   jsonDoc["contents"] = includeContents ? contents() : std::string();
   jsonDoc["dirty"] = dirty();
   jsonDoc["created"] = created();
   jsonDoc["source_on_save"] = sourceOnSave();
   jsonDoc["relative_order"] = relativeOrder();
   jsonDoc["properties"] = properties();
   jsonDoc["folds"] = folds();
   jsonDoc["lastKnownWriteTime"] = json::Value(
         static_cast<boost::int64_t>(lastKnownWriteTime_));
   jsonDoc["encoding"] = encoding_;
   jsonDoc["collab_server"] = collabServer();
   jsonDoc["source_window"] = sourceWindow_;
   jsonDoc["last_content_update"] = json::Value(
         static_cast<boost::int64_t>(lastContentUpdate_));
}

SEXP SourceDocument::toRObject(r::sexp::Protect* pProtect, bool includeContents) const
{
   json::Object object;
   writeToJson(&object, includeContents);
   return r::sexp::create(object, pProtect);
}

Error SourceDocument::writeToFile(const FilePath& filePath, bool writeContents) const
{
   // NOTE: in a previous implementation, the document properties and
   // document contents were encoded together in the same file -- we
   // now use the original file as the properties file (for backwards
   // compatibility), and write the contents to '<id>-contents'. this
   // allows newer versions of RStudio to remain backwards-compatible
   // with older formats for the source database
   
   // write contents to file
   if (writeContents)
   {
      FilePath contentsPath(filePath.absolutePath() + kContentsSuffix);
      Error error = writeStringToFile(contentsPath, contents_);
      if (error)
         return error;
   }
   
   // get document properties as json
   json::Object jsonProperties;
   writeToJson(&jsonProperties, false);
   
   // write properties to file
   std::ostringstream oss;
   json::writeFormatted(jsonProperties, oss);
   Error error = writeStringToFile(filePath, oss.str());
   return error;
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

bool sortByRelativeOrder(const boost::shared_ptr<SourceDocument>& pDoc1,
                         const boost::shared_ptr<SourceDocument>& pDoc2)
{
   // if both documents are unordered, sort by creation time
   if (pDoc1->relativeOrder() == 0 && pDoc2->relativeOrder() == 0)
   {
      return sortByCreated(pDoc1, pDoc2);
   }
   // unordered documents go at the end 
   if (pDoc1->relativeOrder() == 0) 
   {
      return false;
   }
   return pDoc1->relativeOrder() < pDoc2->relativeOrder();
}

FilePath path()
{
   return supervisor::sessionDirPath();
}

Error get(const std::string& id, boost::shared_ptr<SourceDocument> pDoc)
{
   return get(id, true, pDoc);
}
   
Error get(const std::string& id, bool includeContents, boost::shared_ptr<SourceDocument> pDoc)
{
   FilePath propertiesPath = source_database::path().complete(id);
   
   // attempt to read file contents from sidecar file if available
   std::string contents;
   if (includeContents)
   {
      FilePath contentsPath(propertiesPath.absolutePath() + kContentsSuffix);
      if (contentsPath.exists())
      {
         Error error = readStringFromFile(contentsPath,
                                          &contents,
                                          options().sourceLineEnding());
         if (error)
            LOG_ERROR(error);
      }
   }
   
   if (propertiesPath.exists())
   {
      // read the contents of the file
      std::string properties;
      Error error = readStringFromFile(propertiesPath,
                                       &properties,
                                       options().sourceLineEnding());
      if (error)
         return error;
   
      // parse the json
      json::Value value;
      if (!json::parse(properties, &value))
      {
         return systemError(boost::system::errc::invalid_argument,
                            ERROR_LOCATION);
      }
      
      // initialize doc from json
      json::Object jsonDoc = value.get_obj();
      
      if (includeContents && !contents.empty())
         jsonDoc["contents"] = contents;
      
      if (!jsonDoc.count("contents"))
         jsonDoc["contents"] = std::string();
      
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
   
   std::string filename = filePath.filename();
   if (filename == ".DS_Store" ||
       filename == "lock_file" ||
       filename == "suspend_file" ||
       filename == "restart_file" ||
       boost::algorithm::ends_with(filename, kContentsSuffix))
   {
      return false;
   }
   
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

bool hasNullByteSequence(const std::string& contents)
{
   std::string nullBytes;
   nullBytes.push_back('\0');
   nullBytes.push_back('\0');
   return boost::algorithm::contains(contents, nullBytes);
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
   std::string kbStr = safe_convert::numberToString(docSizeKb);

   // if it's larger than 5MB then always drop it (that's the limit
   // enforced by the editor)
   if (docSizeKb > (5 * 1024))
   {
      logUnsafeSourceDocument(filePath, "File too large (" + kbStr + ")");
      return false;
   }

   // if it's larger then 2MB and not dirty then drop it as well
   // (that's the file size considered "large" on the client)
   else if (!pDoc->dirty() && (docSizeKb > (2 * 1024)))
   {
      logUnsafeSourceDocument(filePath, "File too large (" + kbStr + ")");
      return false;
   }

   // if it has a sequence of 2 null bytes then drop it
   else if (hasNullByteSequence(pDoc->contents()))
   {
      logUnsafeSourceDocument(filePath,
                              "File is binary (has null byte sequence)");
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

Error list(std::vector<FilePath>* pPaths)
{
   // list children
   std::vector<FilePath> children;
   Error error = source_database::path().children(&children);
   if (error)
      return error;
   
   // filter to actual source documents
   core::algorithm::copy_if(
            children.begin(),
            children.end(),
            std::back_inserter(*pPaths),
            isSourceDocument);
   
   return Success();
}
   
Error put(boost::shared_ptr<SourceDocument> pDoc, bool writeContents)
{   
   // write to file
   FilePath filePath = source_database::path().complete(pDoc->id());
   Error error = pDoc->writeToFile(filePath, writeContents);
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

Error getPath(const std::string& id, std::string* pPath)
{
   std::map<std::string, std::string>::iterator it = s_idToPath.find(id);
   if (it == s_idToPath.end())
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         ERROR_LOCATION);
   }
   *pPath = it->second;
   return Success();
}

Error getPath(const std::string& id, core::FilePath* pPath)
{
   std::string path;
   Error error = getPath(id, &path);
   if (error) 
      return error;
   *pPath = module_context::resolveAliasedPath(path);
   return Success();
}

Error getId(const std::string& path, std::string* pId)
{
   for (std::map<std::string, std::string>::iterator it = s_idToPath.begin();
        it != s_idToPath.end();
        it++)
   {
      if (it->second == path)
      {
         *pId = it->first;
         return Success();
      }
   }
   return systemError(boost::system::errc::no_such_file_or_directory,
                      ERROR_LOCATION);
}

Error getId(const FilePath& path, std::string* pId)
{
   return getId(module_context::createAliasedPath(FileInfo(path)), pId);
}

Error rename(const FilePath& from, const FilePath& to)
{
   // ensure the destination exists
   if (!to.exists())
      return Success();

   // ensure the file is in the source database
   std::string id;
   Error error = getId(from, &id);
   if (error)
   {
      // rename of a file not in the sdb is a no-op
      return Success();
   }

   // find the file in the sdb and update it with the new path
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error;
   error = pDoc->setPathAndContents(
         module_context::createAliasedPath(FileInfo(to)));
   if (error)
      return error;
   error = source_database::put(pDoc);
   if (error)
      return error;

   // success! fire event for other modules to pick up
   events().onDocRenamed(
         module_context::createAliasedPath(FileInfo(from)), pDoc);

   return error;
}

namespace {

void onQuit()
{
   Error error = supervisor::saveMostRecentDocuments();
   if (error)
      LOG_ERROR(error);

   error = supervisor::detachFromSourceDatabase();
   if (error)
      LOG_ERROR(error);
}

void onSuspend(const r::session::RSuspendOptions& options, core::Settings*)
{
   supervisor::suspendSourceDatabase(options.status);
}

void onResume(const Settings&)
{
   supervisor::resumeSourceDatabase();
}

void onDocUpdated(boost::shared_ptr<SourceDocument> pDoc)
{
   s_idToPath[pDoc->id()] = pDoc->path();
}

void onDocRemoved(const std::string& id, const std::string& path)
{
   std::map<std::string, std::string>::iterator it = s_idToPath.find(id);
   if (it != s_idToPath.end())
      s_idToPath.erase(it);
}

void onDocRenamed(const std::string &, 
                  boost::shared_ptr<SourceDocument> pDoc)
{
   s_idToPath[pDoc->id()] = pDoc->path();
}

void onRemoveAll()
{
   s_idToPath.clear();
}

SEXP rs_getDocumentProperties(SEXP pathSEXP, SEXP includeContentsSEXP)
{
   Error error;
   FilePath path = module_context::resolveAliasedPath(r::sexp::safeAsString(pathSEXP));
   bool includeContents = r::sexp::asLogical(includeContentsSEXP);

   std::string id;
   error = source_database::getId(path, &id);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }

   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument);
   error = source_database::get(id, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }

   r::sexp::Protect protect;
   SEXP object = pDoc->toRObject(&protect, includeContents);
   return object;
}

} // anonymous namespace

Events& events()
{
   static Events instance;
   return instance;
}

Error initialize()
{
   // provision a source database directory
   Error error = supervisor::attachToSourceDatabase();
   if (error)
      return error;

   RS_REGISTER_CALL_METHOD(rs_getDocumentProperties, 2);

   events().onDocUpdated.connect(onDocUpdated);
   events().onDocRemoved.connect(onDocRemoved);
   events().onDocRenamed.connect(onDocRenamed);
   events().onRemoveAll.connect(onRemoveAll);

   // signup for session end/suspend events
   module_context::events().onQuit.connect(onQuit);
   module_context::addSuspendHandler(
         module_context::SuspendHandler(onSuspend, onResume));

   return Success();
}

} // namespace source_database
} // namespace session
} // namespace rstudio

