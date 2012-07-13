/*
 * SessionSource.cpp
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

#include "SessionSource.hpp"

#include <string>
#include <map>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/utility.hpp>

#include <core/r_util/RSourceIndex.hpp>

#include <R_ext/rlocale.h>

#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/text/TemplateFilter.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/FileChangeEvent.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RInternal.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RUtil.hpp>

#include <session/SessionSourceDatabase.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionVCS.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace source {

using namespace session::source_database;

namespace {

// maintain an in-memory list of R source document indexes (for fast
// code searching)
class RSourceIndexes : boost::noncopyable
{
private:
   friend RSourceIndexes& rSourceIndexes();
   RSourceIndexes() {}

public:
   virtual ~RSourceIndexes() {}

   // COPYING: boost::noncopyable

   void update(boost::shared_ptr<SourceDocument> pDoc)
   {
      // is this indexable? if not then bail
      if ( pDoc->path().empty() ||
           (FilePath(pDoc->path()).extensionLowerCase() != ".r") )
      {
         return;
      }

      // index the source
      boost::shared_ptr<r_util::RSourceIndex> pIndex(
                 new r_util::RSourceIndex(pDoc->path(), pDoc->contents()));

      // insert it
      indexes_[pDoc->id()] = pIndex;
   }

   void remove(const std::string& id)
   {
      indexes_.erase(id);
   }

   void removeAll()
   {
      indexes_.clear();
   }

   std::vector<boost::shared_ptr<r_util::RSourceIndex> > indexes()
   {
      std::vector<boost::shared_ptr<r_util::RSourceIndex> > indexes;
      BOOST_FOREACH(const IndexMap::value_type& index, indexes_)
      {
         indexes.push_back(index.second);
      }
      return indexes;
   }

private:
   typedef std::map<std::string, boost::shared_ptr<r_util::RSourceIndex> >
                                                                    IndexMap;
   IndexMap indexes_;
};

RSourceIndexes& rSourceIndexes()
{
   static RSourceIndexes instance;
   return instance;
}

// wrap source_database::put for situations where there are new contents
// (so we can index the contents)
Error sourceDatabasePutWithUpdatedContents(
                              boost::shared_ptr<SourceDocument> pDoc)
{
   // write the file to the database
   Error error = source_database::put(pDoc);
   if (error)
      return error ;

   // update index
   rSourceIndexes().update(pDoc);

   return Success();
}
   
Error newDocument(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   // params
   std::string type;
   json::Value jsonContents;
   json::Object properties;
   Error error = json::readParams(request.params,
                                  &type,
                                  &jsonContents,
                                  &properties);
   if (error)
      return error ;

   // create the new doc and write it to the database
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument(type)) ;

   if (json::isType<std::string>(jsonContents))
      pDoc->setContents(jsonContents.get_str());

   pDoc->editProperties(properties);

   error = source_database::put(pDoc);
   if (error)
      return error;

   // return the doc
   json::Object jsonDoc;
   pDoc->writeToJson(&jsonDoc);
   pResponse->setResult(jsonDoc);
   return Success();
}

Error openDocument(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   // params
   std::string path;
   Error error = json::readParam(request.params, 0, &path);
   if (error)
      return error ;
   
   std::string type;
   error = json::readParam(request.params, 1, &type);
   if (error)
      return error ;

   std::string encoding;
   error = json::readParam(request.params, 2, &encoding);
   if (error && error.code() != core::json::errc::ParamTypeMismatch)
      return error ;
   if (encoding.empty())
      encoding = ::locale2charset(NULL);
   
   // ensure the file exists
   FilePath documentPath = module_context::resolveAliasedPath(path);
   if (!documentPath.exists())
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         ERROR_LOCATION);
   }
   
   // set the doc contents to the specified file
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument(type)) ;
   pDoc->setEncoding(encoding);
   error = pDoc->setPathAndContents(path, false);
   if (error)
   {
      error = pDoc->setPathAndContents(path, true);
      if (error)
         return error ;

      r::exec::warning("Not all characters in " + documentPath.absolutePath() +
                       " could be decoded using " + encoding + ". To try a "
                       "different encoding, choose \"File | Reopen with "
                       "Encoding...\" from the main menu.");
      r::exec::printWarnings();
   }

   // recover durable properties if they are available
   json::Object properties;
   error = source_database::getDurableProperties(path, &properties);
   if (!error)
      pDoc->editProperties(properties);
   else
      LOG_ERROR(error);
   
   // write to the source_database
   error = sourceDatabasePutWithUpdatedContents(pDoc);
   if (error)
      return error;

   // return the doc
   json::Object jsonDoc;
   pDoc->writeToJson(&jsonDoc);
   pResponse->setResult(jsonDoc);
   return Success();
} 

Error saveDocumentCore(const std::string& contents,
                       const json::Value& jsonPath,
                       const json::Value& jsonType,
                       const json::Value& jsonEncoding,
                       const json::Value& jsonFoldSpec,
                       int scrollPosition,
                       const std::string& selection,
                       boost::shared_ptr<SourceDocument> pDoc)
{
   // check whether we have a path and if we do get/resolve its value
   std::string path;
   FilePath fullDocPath;
   bool hasPath = json::isType<std::string>(jsonPath);
   if (hasPath)
   {
      path = jsonPath.get_str();
      fullDocPath = module_context::resolveAliasedPath(path);
   }
   
   // update dirty state: dirty if there was no path (and was thus an autosave)
   pDoc->setDirty(!hasPath);
   
   bool hasType = json::isType<std::string>(jsonType);
   if (hasType)
   {
      pDoc->setType(jsonType.get_str());
   }
   
   Error error;
   
   bool hasEncoding = json::isType<std::string>(jsonEncoding);
   if (hasEncoding)
   {
      pDoc->setEncoding(jsonEncoding.get_str());
   }

   bool hasFoldSpec = json::isType<std::string>(jsonFoldSpec);
   if (hasFoldSpec)
   {
      pDoc->setFolds(jsonFoldSpec.get_str());
   }

   pDoc->setScrollPosition(scrollPosition);
   pDoc->setSelection(selection);

   // handle document (varies depending upon whether we have a path)
   if (hasPath)
   {
      std::string encoded;
      error = r::util::iconvstr(contents,
                                "UTF-8",
                                pDoc->encoding(),
                                false,
                                &encoded);
      if (error)
      {
         error = r::util::iconvstr(contents,
                                   "UTF-8",
                                   pDoc->encoding(),
                                   true,
                                   &encoded);
         if (error)
            return error;

         r::exec::warning("Not all of the characters in " + path +
                          " could be encoded using " + pDoc->encoding() +
                          ". To save using a different encoding, choose \"File | "
                          "Save with Encoding...\" from the main menu.");
         r::exec::printWarnings();
      }

      // note whether the file existed prior to writing
      bool newFile = !fullDocPath.exists();

      // write the contents to the file
      error = writeStringToFile(fullDocPath, encoded,
                                options().sourcePersistLineEnding());
      if (error)
         return error ;

      // set the new path and contents for the document
      error = pDoc->setPathAndContents(path);
      if (error)
         return error ;

      // enque file changed event if we need to
      if (!module_context::isDirectoryMonitored(fullDocPath.parent()))
      {
         using core::system::FileChangeEvent;
         FileChangeEvent changeEvent(newFile ? FileChangeEvent::FileAdded :
                                               FileChangeEvent::FileModified,
                                     FileInfo(fullDocPath));
         module_context::enqueFileChangedEvent(changeEvent);
      }
   }

   // always update the contents so it holds the original UTF-8 data
   pDoc->setContents(contents);

   return Success();
}

Error saveDocument(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)

{
   // params
   std::string id, contents;
   json::Value jsonPath, jsonType, jsonEncoding, jsonFoldSpec;
   int scrollPosition;
   std::string selection;
   Error error = json::readParams(request.params, 
                                  &id, 
                                  &jsonPath, 
                                  &jsonType, 
                                  &jsonEncoding,
                                  &jsonFoldSpec,
                                  &scrollPosition,
                                  &selection,
                                  &contents);
   if (error)
      return error ;
   
   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error ;
   
   error = saveDocumentCore(contents, jsonPath, jsonType, jsonEncoding,
                            jsonFoldSpec, scrollPosition, selection, pDoc);
   if (error)
      return error;
   
   // write to the source_database
   error = sourceDatabasePutWithUpdatedContents(pDoc);
   if (error)
      return error;

   // return the hash
   pResponse->setResult(pDoc->hash());
   return Success();
}

Error saveDocumentDiff(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   using namespace core::string_utils;

   // unique id and jsonPath (can be null for auto-save)
   std::string id;
   json::Value jsonPath, jsonType, jsonEncoding, jsonFoldSpec;

   // scroll position
   int scrollPosition;

   // selection
   std::string selection;
   
   // This is a chunk of text that should be inserted into the
   // current document. It replaces the subrange [offset, offset+length).
   std::string replacement;
   int offset, length;
   
   // This is the expected hash of the current document. If the
   // current hash value is different than this value, then the
   // document cannot be patched and the request should be discarded.
   std::string hash;
   
   // read params
   Error error = json::readParams(request.params,
                                  &id,
                                  &jsonPath,
                                  &jsonType,
                                  &jsonEncoding,
                                  &jsonFoldSpec,
                                  &scrollPosition,
                                  &selection,
                                  &replacement,
                                  &offset,
                                  &length,
                                  &hash);
   if (error)
      return error ;
   
   // if this has no path then it is an autosave, in this case
   // suppress change detection
   bool hasPath = json::isType<std::string>(jsonPath);
   if (!hasPath)
       pResponse->setSuppressDetectChanges(true);

   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error ;
   
   // Don't even attempt anything if we're not working off the same original
   if (pDoc->hash() == hash)
   {
      std::string contents(pDoc->contents());

      // Offset and length are specified in characters, but contents
      // is in UTF8 bytes. Convert before using.
      std::string::iterator rangeBegin = contents.begin();
      error = utf8Advance(rangeBegin, offset, contents.end(), &rangeBegin);
      if (error)
         return Success(); // UTF8 decoding failed. Abort differential save.

      std::string::iterator rangeEnd = rangeBegin;
      error = utf8Advance(rangeEnd, length, contents.end(), &rangeEnd);
      if (error)
         return Success(); // UTF8 decoding failed. Abort differential save.

      contents.erase(rangeBegin, rangeEnd);
      contents.insert(rangeBegin, replacement.begin(), replacement.end());
      
      error = saveDocumentCore(contents, jsonPath, jsonType, jsonEncoding,
                               jsonFoldSpec, scrollPosition, selection, pDoc);
      if (error)
         return error;
      
      // write to the source_database
      error = sourceDatabasePutWithUpdatedContents(pDoc);
      if (error)
         return error;

      pResponse->setResult(pDoc->hash());
   }
   
   return Success();
}

Error checkForExternalEdit(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   pResponse->setSuppressDetectChanges(true);

   // params
   std::string id ;
   Error error = json::readParams(request.params, &id);
   if (error)
      return error;

   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument()) ;
   error = source_database::get(id, pDoc);
   if (error)
      return error ;

   json::Object result;
   result["modified"] = false;
   result["deleted"] = false;

   // Only check if this document has ever been saved
   if (!pDoc->path().empty())
   {
      FilePath docFile = module_context::resolveAliasedPath(pDoc->path());
      if (!docFile.exists() || docFile.isDirectory())
      {
         result["deleted"] = true;

         pDoc->setDirty(true);
         error = source_database::put(pDoc);
         if (error)
            return error;
      }
      else
      {
         std::time_t lastWriteTime ;
         pDoc->checkForExternalEdit(&lastWriteTime);

         if (lastWriteTime)
         {
            FilePath filePath = module_context::resolveAliasedPath(pDoc->path()) ;
            json::Object fsItem = module_context::createFileSystemItem(filePath);
            result["item"] = fsItem;
            result["modified"] = true;
         }
      }
   }

   pResponse->setResult(result);

   return Success();
}

namespace {

Error reopen(std::string id, std::string fileType, std::string encoding,
             json::JsonRpcResponse* pResponse)
{
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument()) ;
   Error error = source_database::get(id, pDoc);
   if (error)
      return error ;

   if (!encoding.empty())
      pDoc->setEncoding(encoding);

   if (!fileType.empty())
      pDoc->setType(fileType);

   error = pDoc->setPathAndContents(pDoc->path(), false);
   if (error)
   {
      error = pDoc->setPathAndContents(pDoc->path(), true);
      if (error)
         return error ;

      r::exec::warning("Not all characters in " + pDoc->path() +
                       " could be decoded using " + encoding + ".");
      r::exec::printWarnings();
   }
   pDoc->setDirty(false);

   // write to the source_database
   error = sourceDatabasePutWithUpdatedContents(pDoc);
   if (error)
      return error;

   json::Object resultObj;
   pDoc->writeToJson(&resultObj);
   pResponse->setResult(resultObj);

   return Success();
}

} // anonymous namespace

Error revertDocument(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string id, fileType ;
   Error error = json::readParams(request.params, &id, &fileType) ;
   if (error)
      return error;

   return reopen(id, fileType, std::string(), pResponse);
}

Error reopenWithEncoding(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string id, encoding;
   Error error = json::readParams(request.params, &id, &encoding);
   if (error)
      return error;

   return reopen(id, std::string(), encoding, pResponse);
}

Error ignoreExternalEdit(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string id ;
   Error error = json::readParams(request.params, &id) ;
   if (error)
      return error;

   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error;

   pDoc->updateLastKnownWriteTime();

   error = source_database::put(pDoc);
   if (error)
      return error;

   return Success();
}
   
Error setSourceDocumentOnSave(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   // params
   std::string id ;
   bool value = false;
   Error error = json::readParams(request.params, &id, &value);
   if (error)
      return error ;
   
   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error ;
   
   // set source on save and then write it
   pDoc->setSourceOnSave(value);
   return source_database::put(pDoc);
}   
   

Error modifyDocumentProperties(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   // params
   std::string id ;
   json::Object properties;
   Error error = json::readParams(request.params, &id, &properties);
   if (error)
      return error ;

   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error ;

   // edit properties and write the document
   pDoc->editProperties(properties);
   return source_database::put(pDoc);
}

Error closeDocument(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // params
   std::string id ;
   Error error = json::readParam(request.params, 0, &id);
   if (error)
      return error ;
   
   error = source_database::remove(id);
   if (error)
      return error;

   rSourceIndexes().remove(id);

   return Success();
}
   
Error closeAllDocuments(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   Error error = source_database::removeAll();
   if (error)
      return error;

   rSourceIndexes().removeAll();

   return Success();
}

Error getSourceTemplate(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // read params
   std::string name, templateName;
   Error error = json::readParams(request.params, &name, &templateName);
   if (error)
      return error;

   // setup template filter
   std::map<std::string,std::string> vars;
   vars["name"] = name;
   core::text::TemplateFilter filter(vars);

   // read file with template filter
   FilePath templatePath = session::options().rResourcesPath().complete(
                                             "templates/" +  templateName);
   std::string contents;
   error = core::readStringFromFile(templatePath,
                                    filter,
                                    &contents,
                                    string_utils::LineEndingPosix);
   if (error)
      return error;

   pResponse->setResult(contents);

   return Success();
}

void enqueFileEditEvent(const std::string& file)
{
   // ignore if no file passed
   if (file.empty())
      return;

   // calculate full path
   FilePath filePath = module_context::safeCurrentPath().complete(file);

   // if it doesn't exist then create it
   if (!filePath.exists())
   {
      Error error = core::writeStringToFile(filePath, "",
                                            options().sourcePersistLineEnding());
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }

   // fire event
   json::Object fileJson = module_context::createFileSystemItem(filePath);
   ClientEvent event(client_events::kFileEdit, fileJson);
   module_context::enqueClientEvent(event);
}

SEXP fileEditHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   // file.edit(..., title = file, editor = getOption("editor"))
   // see do_fileedit in platform.c
   r::function_hook::checkArity(op, args, call);

   try
   {
      // read and validate file name (we ignore all other parameters)
      SEXP filenamesSEXP = CAR(args);
      if (!r::sexp::isString(filenamesSEXP))
         throw r::exec::RErrorException("invalid filename specification");

      // extract string vector
      std::vector<std::string> filenames;
      Error error = r::sexp::extract(filenamesSEXP, &filenames);
      if (error)
         throw r::exec::RErrorException(error.summary());

      // fire events
      std::for_each(filenames.begin(), filenames.end(), enqueFileEditEvent);
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::errorCall(call, e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

void onSuspend(Settings*)
{
}

// update the source database index on resume

// TODO: a resume followed by a client_init will cause us to call
// source_database::list twice (which will cause us to read all of
// the files twice). find a way to prevent this.

void onResume(const Settings&)
{
   rSourceIndexes().removeAll();

   // get the docs and sort them by created
   std::vector<boost::shared_ptr<SourceDocument> > docs ;
   Error error = source_database::list(&docs);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   std::sort(docs.begin(), docs.end(), sortByCreated);

   // update the indexes
   std::for_each(docs.begin(),
                 docs.end(),
                 boost::bind(&RSourceIndexes::update, &rSourceIndexes(), _1));
}

void onShutdown(bool terminatedNormally)
{
   FilePath activeDocumentFile =
         module_context::resolveAliasedPath("~/.active-rstudio-document");
   Error error = activeDocumentFile.removeIfExists();
   if (error)
      LOG_ERROR(error);
}

} // anonymous namespace

Error clientInitDocuments(core::json::Array* pJsonDocs)
{
   // remove all items from the source index database
   rSourceIndexes().removeAll();

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

      // update the source index
      rSourceIndexes().update(pDoc);
   }

   return Success();
}

std::vector<boost::shared_ptr<core::r_util::RSourceIndex> > rIndexes()
{
   return rSourceIndexes().indexes();
}

Error initialize()
{   
   // connect to events
   using namespace module_context;
   events().onShutdown.connect(onShutdown);

   // add suspend/resume handler
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   // install rpc methods
   using boost::bind;
   using namespace r::function_hook;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerReplaceHook, "file.edit", fileEditHook, (CCODE*)NULL))
      (bind(registerRpcMethod, "new_document", newDocument))
      (bind(registerRpcMethod, "open_document", openDocument))
      (bind(registerRpcMethod, "save_document", saveDocument))
      (bind(registerRpcMethod, "save_document_diff", saveDocumentDiff))
      (bind(registerRpcMethod, "check_for_external_edit", checkForExternalEdit))
      (bind(registerRpcMethod, "ignore_external_edit", ignoreExternalEdit))
      (bind(registerRpcMethod, "set_source_document_on_save", setSourceDocumentOnSave))
      (bind(registerRpcMethod, "modify_document_properties", modifyDocumentProperties))
      (bind(registerRpcMethod, "revert_document", revertDocument))
      (bind(registerRpcMethod, "reopen_with_encoding", reopenWithEncoding))
      (bind(registerRpcMethod, "close_document", closeDocument))
      (bind(registerRpcMethod, "close_all_documents", closeAllDocuments))
      (bind(registerRpcMethod, "get_source_template", getSourceTemplate))
      (bind(sourceModuleRFile, "SessionSource.R"));
   return initBlock.execute();

}


} // namespace source
} // namespace modules
} // namesapce session

