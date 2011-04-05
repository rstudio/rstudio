/*
 * SessionSource.cpp
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

#include "SessionSource.hpp"

#include <string>

#include <boost/bind.hpp>

#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RInternal.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RUtil.hpp>

#include <session/SessionSourceDatabase.hpp>

#include <session/SessionModuleContext.hpp>


using namespace core;

namespace session {
namespace modules { 
namespace source {

namespace {

using namespace session::source_database;
   
Error newDocument(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   // params
   std::string type;
   std::string encoding;
   json::Object properties;
   Error error = json::readParams(request.params,
                                  &type,
                                  &encoding,
                                  &properties);
   if (error)
      return error ;

   // create the new doc and write it to the database
   SourceDocument doc(type) ;
   doc.setEncoding(encoding);

   doc.editProperties(properties);

   error = source_database::put(doc);
   if (error)
      return error;
   
   // return the doc
   json::Object jsonDoc;
   doc.writeToJson(&jsonDoc);
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
   
   // ensure the file exists
   FilePath documentPath = module_context::resolveAliasedPath(path);
   if (!documentPath.exists())
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         ERROR_LOCATION);
   }
   
   // set the doc contents to the specified file
   SourceDocument doc(type) ;
   doc.setEncoding(encoding);
   error = doc.setPathAndContents(path);
   if (error)
      return error ;

   // recover durable properties if they are available
   json::Object properties;
   error = source_database::getDurableProperties(path, &properties);
   if (!error)
      doc.editProperties(properties);
   else
      LOG_ERROR(error);
   
   // write the file to the database
   error = source_database::put(doc);
   if (error)
      return error ;
   
   // return the doc
   json::Object jsonDoc;
   doc.writeToJson(&jsonDoc);
   pResponse->setResult(jsonDoc);
   return Success();
} 
   
Error listDocuments(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // get the docs
   json::Array jsonDocs ;
   Error error = getSourceDocumentsJson(&jsonDocs);
   if (error)
      return error;
   
   // return them
   pResponse->setResult(jsonDocs);
   return Success();
}

Error saveDocumentCore(const std::string& contents,
                       const json::Value& jsonPath,
                       const json::Value& jsonType,
                       const json::Value& jsonEncoding,
                       SourceDocument* pDoc)
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

   // handle document (varies depending upon whether we have a path)
   if (hasPath)
   {
      std::string encoded;
      error = r::util::iconvstr(contents,
                                "UTF-8",
                                pDoc->encoding(),
                                true,
                                &encoded);
      if (error)
         return error;

      // write the contents to the file
      error = writeStringToFile(fullDocPath, encoded,
                                options().sourcePersistLineEnding());
      if (error)
         return error ;

      // set the new path and contents for the document
      error = pDoc->setPathAndContents(path);
      if (error)
         return error ;
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
   json::Value jsonPath, jsonType, jsonEncoding;
   Error error = json::readParams(request.params, 
                                  &id, 
                                  &jsonPath, 
                                  &jsonType, 
                                  &jsonEncoding,
                                  &contents);
   if (error)
      return error ;
   
   // get the doc
   SourceDocument doc;
   error = source_database::get(id, &doc);
   if (error)
      return error ;
   
   error = saveDocumentCore(contents, jsonPath, jsonType, jsonEncoding, &doc);
   if (error)
      return error;
   
   // write the source doc
   error = source_database::put(doc);
   if (error)
      return error ;
   
   // return the hash
   pResponse->setResult(doc.hash());
   return Success();
}

Error saveDocumentDiff(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   using namespace core::string_utils;

   pResponse->setSuppressDetectChanges(true);

   // unique id and jsonPath (can be null for auto-save)
   std::string id;
   json::Value jsonPath, jsonType, jsonEncoding;
   
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
                                  &replacement,
                                  &offset,
                                  &length,
                                  &hash);
   if (error)
      return error ;
   
   // get the doc
   SourceDocument doc;
   error = source_database::get(id, &doc);
   if (error)
      return error ;
   
   // Don't even attempt anything if we're not working off the same original
   if (doc.hash() == hash)
   {
      std::string contents(doc.contents());

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
                               &doc);
      if (error)
         return error;
      
      error = source_database::put(doc);
      if (error)
         return error;
      
      pResponse->setResult(doc.hash());
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

   SourceDocument doc ;
   error = source_database::get(id, &doc);
   if (error)
      return error ;

   json::Object result;
   result["modified"] = false;
   result["deleted"] = false;

   // Only check if this document has ever been saved
   if (!doc.path().empty())
   {
      FilePath docFile = module_context::resolveAliasedPath(doc.path());
      if (!docFile.exists() || docFile.isDirectory())
      {
         result["deleted"] = true;

         doc.setDirty(true);
         error = source_database::put(doc);
         if (error)
            return error;
      }
      else
      {
         std::time_t lastWriteTime ;
         doc.checkForExternalEdit(&lastWriteTime);

         if (lastWriteTime)
         {
            FilePath filePath = module_context::resolveAliasedPath(doc.path()) ;
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
   SourceDocument doc ;
   Error error = source_database::get(id, &doc);
   if (error)
      return error ;

   if (!encoding.empty())
      doc.setEncoding(encoding);

   if (!fileType.empty())
      doc.setType(fileType);

   error = doc.setPathAndContents(doc.path());
   if (error)
      return error ;
   doc.setDirty(false);

   error = source_database::put(doc);
   if (error)
      return error ;

   json::Object resultObj;
   doc.writeToJson(&resultObj);
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

   SourceDocument doc;
   error = source_database::get(id, &doc);
   if (error)
      return error;

   doc.updateLastKnownWriteTime();

   error = source_database::put(doc);
   if (error)
      return error;

   return Success();
}
   
Error setSourceDocumentOnSave(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   // params
   std::string id ;
   bool value ;
   Error error = json::readParams(request.params, &id, &value);
   if (error)
      return error ;
   
   // get the doc
   SourceDocument doc ;
   error = source_database::get(id, &doc);
   if (error)
      return error ;
   
   // set source on save and then write it
   doc.setSourceOnSave(value);
   return source_database::put(doc);
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
   SourceDocument doc ;
   error = source_database::get(id, &doc);
   if (error)
      return error ;

   // edit properties and write the document
   doc.editProperties(properties);
   return source_database::put(doc);
}

Error closeDocument(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // params
   std::string id ;
   Error error = json::readParam(request.params, 0, &id);
   if (error)
      return error ;
   
   return source_database::remove(id);
}
   
Error closeAllDocuments(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   return source_database::removeAll();
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

void onShutdown(bool terminatedNormally)
{
   FilePath activeDocumentFile =
         module_context::resolveAliasedPath("~/.active-rstudio-document");
   Error error = activeDocumentFile.removeIfExists();
   if (error)
      LOG_ERROR(error);
}

} // anonymous namespace

Error initialize()
{   
   // connect to shutdown event
   module_context::events().onShutdown.connect(onShutdown);

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   using namespace r::function_hook;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerReplaceHook, "file.edit", fileEditHook, (CCODE*)NULL))
      (bind(registerRpcMethod, "new_document", newDocument))
      (bind(registerRpcMethod, "open_document", openDocument))
      (bind(registerRpcMethod, "list_documents", listDocuments))
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
      (bind(sourceModuleRFile, "SessionSource.R"));
   return initBlock.execute();

}


} // namespace source
} // namespace modules
} // namesapce session

