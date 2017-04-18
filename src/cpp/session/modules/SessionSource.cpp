/*
 * SessionSource.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionSource.hpp"
#include "rmarkdown/NotebookChunkDefs.hpp"

#include <string>
#include <map>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/utility.hpp>

#include <core/r_util/RSourceIndex.hpp>

#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/r_util/RPackageInfo.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/FileChangeEvent.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RInternal.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RUtil.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSessionUtils.hpp>

extern "C" const char *locale2charset(const char *);

#include <session/SessionSourceDatabase.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace source {

using namespace session::source_database;

namespace {

void writeDocToJson(boost::shared_ptr<SourceDocument> pDoc,
                    core::json::Object* pDocJson)
{
   // write the doc
   pDoc->writeToJson(pDocJson);

   // derive the extended type property
   (*pDocJson)["extended_type"] = module_context::events()
                                   .onDetectSourceExtendedType(pDoc);

   // amend with chunk definitions if an R Markdown document
   json::Object notebook;
   if (pDoc->isRMarkdownDocument())
   {
      Error error = rmarkdown::notebook::getChunkValues(
            pDoc->path(), pDoc->id(), &notebook);
      if (error)
         LOG_ERROR(error);
   }
   (*pDocJson)["notebook"] = notebook;
}

void detectExtendedType(boost::shared_ptr<SourceDocument> pDoc)
{
   // detect the extended type of the document by calling any registered
   // extended type detection handlers
   std::string extendedType =
                  module_context::events().onDetectSourceExtendedType(pDoc);

   // notify the client
   json::Object jsonData;
   jsonData["doc_id"] = pDoc->id();
   jsonData["extended_type"] = extendedType;
   ClientEvent event(client_events::kSourceExtendedTypeDetected, jsonData);
   module_context::enqueClientEvent(event);
}

int numSourceDocuments()
{
   std::vector<boost::shared_ptr<SourceDocument> > docs;
   source_database::list(&docs);
   return docs.size();
}

// wrap source_database::put for situations where there are new contents
// (so we can index the contents)
Error sourceDatabasePutWithUpdatedContents(boost::shared_ptr<SourceDocument> pDoc,
                                           bool writeContents = true)
{
   // write the file to the database
   Error error = source_database::put(pDoc, writeContents);
   if (error)
      return error ;

   source_database::events().onDocUpdated(pDoc);

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

   // set relative order (client will receive docs in relative order on init)
   pDoc->setRelativeOrder(numSourceDocuments() + 1);

   error = source_database::put(pDoc);
   if (error)
      return error;

   // return the doc
   json::Object jsonDoc;
   writeDocToJson(pDoc, &jsonDoc);
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
   
   // ensure the file is not binary
   if (!module_context::isTextFile(documentPath))
   {
      Error error = systemError(boost::system::errc::illegal_byte_sequence,
                                ERROR_LOCATION);
      pResponse->setError(error, "File is binary rather than text so cannot "
                                 "be opened by the source editor.");
      return Success();
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

      module_context::consoleWriteError(
                 "Not all characters in " + documentPath.absolutePath() +
                 " could be decoded using " + encoding + ". To try a "
                 "different encoding, choose \"File | Reopen with "
                 "Encoding...\" from the main menu.");
   }

   // recover durable properties if they are available
   json::Object properties;
   error = source_database::getDurableProperties(path, &properties);
   if (!error)
      pDoc->editProperties(properties);
   else
      LOG_ERROR(error);
   
   // set relative order (client will receive docs in relative order on init)
   pDoc->setRelativeOrder(numSourceDocuments() + 1);

   // write to the source_database
   error = sourceDatabasePutWithUpdatedContents(pDoc);
   if (error)
      return error;

   // broadcast doc added event -- it's important to do this after it's in the 
   // database but before we serialize it so hooks can operate on the doc 
   // before it's written to the client
   events().onDocAdded(pDoc->id());

   // create JSON representation of doc
   json::Object jsonDoc;
   writeDocToJson(pDoc, &jsonDoc);

   pResponse->setResult(jsonDoc);

   return Success();
} 

Error saveDocumentCore(const std::string& contents,
                       const json::Value& jsonPath,
                       const json::Value& jsonType,
                       const json::Value& jsonEncoding,
                       const json::Value& jsonFoldSpec,
                       const json::Value& jsonChunkOutput,
                       boost::shared_ptr<SourceDocument> pDoc)
{
   // check whether we have a path and if we do get/resolve its value
   std::string oldPath, path;
   FilePath fullDocPath;
   bool hasPath = json::isType<std::string>(jsonPath);
   if (hasPath)
   {
      oldPath = pDoc->path();
      path = jsonPath.get_str();
      fullDocPath = module_context::resolveAliasedPath(path);
   }

   // update dirty state: dirty if there was no path AND the new contents
   // are different from the old contents (and was thus a content autosave
   // as distinct from a fold-spec or scroll-position/selection autosave)
   pDoc->setDirty(!hasPath && (contents != pDoc->contents()));
   
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

   // note that it's entirely possible for the chunk output to be null if the
   // document is unrendered (in which case we want to leave the chunk output
   // as-is)
   bool hasChunkOutput = json::isType<json::Array>(jsonChunkOutput);
   if (hasChunkOutput && pDoc->isRMarkdownDocument())
   {
      error = rmarkdown::notebook::setChunkDefs(pDoc, 
            jsonChunkOutput.get_array());
      if (error)
         LOG_ERROR(error);
   }

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


         module_context::consoleWriteError(
                          "Not all of the characters in " + path +
                          " could be encoded using " + pDoc->encoding() +
                          ". To save using a different encoding, choose \"File | "
                          "Save with Encoding...\" from the main menu.");
      }

      // note whether the file existed prior to writing
      bool newFile = !fullDocPath.exists();

      // write the contents to the file
      error = writeStringToFile(fullDocPath, encoded,
                                module_context::lineEndings(fullDocPath));
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

      // notify other server modules of the file save
      module_context::events().onSourceEditorFileSaved(fullDocPath);

      // save could change the extended type of the file so check it
      detectExtendedType(pDoc);

      // if we changed the path, notify other modules
      if (oldPath != pDoc->path())
      {
         source_database::events().onDocRenamed(oldPath, pDoc);
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
   json::Value jsonPath, jsonType, jsonEncoding, jsonFoldSpec, jsonChunkOutput;
   Error error = json::readParams(request.params, 
                                  &id, 
                                  &jsonPath, 
                                  &jsonType, 
                                  &jsonEncoding,
                                  &jsonFoldSpec,
                                  &jsonChunkOutput,
                                  &contents);
   if (error)
      return error ;
   
   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error;
   
   // check if the document contents have changed
   bool hasChanges = contents != pDoc->contents();
   error = saveDocumentCore(contents, jsonPath, jsonType, jsonEncoding,
                            jsonFoldSpec, jsonChunkOutput, pDoc);
   if (error)
      return error;
   
   // write to the source_database
   error = sourceDatabasePutWithUpdatedContents(pDoc, hasChanges);
   if (error)
      return error;

   // return the hash
   pResponse->setResult(pDoc->hash());
   return Success();
}

Error saveDocumentDiff(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   using namespace rstudio::core::string_utils;

   // unique id and jsonPath (can be null for auto-save)
   std::string id;
   json::Value jsonPath, jsonType, jsonEncoding, jsonFoldSpec, jsonChunkOutput;
   
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
                                  &jsonChunkOutput,
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
      
      // track if we're updating the document contents
      bool hasChanges = contents != pDoc->contents();
      error = saveDocumentCore(contents, jsonPath, jsonType, jsonEncoding,
                               jsonFoldSpec, jsonChunkOutput, pDoc);
      if (error)
         return error;
      
      // write to the source database (don't worry about writing document
      // contents if those have not changed)
      error = sourceDatabasePutWithUpdatedContents(pDoc, hasChanges);
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

      module_context::consoleWriteError(
                       "Not all characters in " + pDoc->path() +
                       " could be decoded using " + encoding + ".");
   }
   pDoc->setDirty(false);

   // write to the source_database
   error = sourceDatabasePutWithUpdatedContents(pDoc);
   if (error)
      return error;

   json::Object resultObj;
   writeDocToJson(pDoc, &resultObj);
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
   error = source_database::get(id, false, pDoc);
   if (error)
      return error ;

   // edit properties and write the document
   pDoc->editProperties(properties);
   return source_database::put(pDoc, false);
}

Error getDocumentProperties(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   std::string path; 
   json::Object properties;
   Error error = json::readParams(request.params, &path);
   if (error)
      return error;

   error = source_database::getDurableProperties(path, &properties);
   if (error)
      return error;

   pResponse->setResult(properties);

   return Success();
}

Error closeDocument(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // params
   std::string id ;
   Error error = json::readParam(request.params, 0, &id);
   if (error)
      return error ;

   // get the path (it's okay if this fails, unsaved docs don't have a path)
   std::string path;
   source_database::getPath(id, &path);

   // retrieve document from source database
   boost::shared_ptr<source_database::SourceDocument> pDoc(
               new source_database::SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      // do any cleanup necessary prior to removal
      source_database::events().onDocPendingRemove(pDoc);
   }

   // actually remove from the source database
   error = source_database::remove(id);
   if (error)
      return error;

   source_database::events().onDocRemoved(id, path);

   return Success();
}
   
Error closeAllDocuments(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   Error error = source_database::removeAll();
   if (error)
      return error;

   source_database::events().onRemoveAll();

   return Success();
}

Error processSourceTemplate(const std::string& name,
                            const std::string& templateName,
                            std::string* pContents)
{
   // setup template filter
   std::map<std::string,std::string> vars;
   vars["name"] = name;
   core::text::TemplateFilter filter(vars);

   // read file with template filter
   FilePath templatePath = session::options().rResourcesPath().complete(
                                             "templates/" +  templateName);
   return core::readStringFromFile(templatePath,
                                   filter,
                                   pContents,
                                   string_utils::LineEndingPosix);
}

Error getSourceTemplate(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // read params
   std::string name, templateName;
   Error error = json::readParams(request.params, &name, &templateName);
   if (error)
      return error;

   std::string contents;
   error =  processSourceTemplate(name, templateName, &contents);
   if (error)
      return error;

   pResponse->setResult(contents);
   return Success();
}

Error defaultRdResponse(const std::string& name,
                        const std::string& type,
                        json::JsonRpcResponse* pResponse)
{
   std::string filePath;
   Error error = r::exec::RFunction(".rs.createDefaultShellRd", name, type)
                                              .call(&filePath);
   if (error)
      return error;

   std::string contents;
   error = core::readStringFromFile(
                        FilePath(string_utils::systemToUtf8(filePath)),
                        &contents,
                        string_utils::LineEndingPosix);
   if (error)
      return error;


   json::Object resultJson;
   resultJson["path"] = json::Value();
   resultJson["contents"] = contents;
   pResponse->setResult(resultJson);
   return Success();
}


Error createRdShell(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string name, type;
   Error error = json::readParams(request.params, &name, &type);
   if (error)
      return error;

   // suppress output so that R doesn't write the Rd message to the console
   r::session::utils::SuppressOutputInScope suppressOutputScope;

   // if we are within a package development environment then use that
   // as the basis for the new document
   if (projects::projectContext().config().buildType ==
       r_util::kBuildTypePackage)
   {
      // read package info
      FilePath packageDir = projects::projectContext().buildTargetPath();
      r_util::RPackageInfo pkgInfo;
      Error error = pkgInfo.read(packageDir);
      if (error)
      {
         LOG_ERROR(error);
         return defaultRdResponse(name, type, pResponse);
      }

      // lookup the object in the package first
      std::string filePath;
      error = r::exec::RFunction(".rs.createShellRd",
                               name, type, pkgInfo.name()).call(&filePath);
      if (error)
         return error;

      // if it was found then read it
      if (!filePath.empty())
      {
         FilePath rdFilePath(string_utils::systemToUtf8(filePath));
         FilePath manFilePath = packageDir.childPath("man").childPath(
                                                      rdFilePath.filename());
         if (!manFilePath.exists())
         {
            Error error = rdFilePath.copy(manFilePath);
            if (error)
               return error;

            json::Object resultJson;
            resultJson["path"] = module_context::createAliasedPath(manFilePath);
            resultJson["contents"] = json::Value();
            pResponse->setResult(resultJson);
         }
         else
         {
            std::string contents;
            error = core::readStringFromFile(rdFilePath,
                                             &contents,
                                             string_utils::LineEndingPosix);
            if (error)
               return error;

            json::Object resultJson;
            resultJson["path"] = json::Value();
            resultJson["contents"] = contents;
            pResponse->setResult(resultJson);
         }

         return Success();
      }
      else
      {
         return defaultRdResponse(name, type, pResponse);
      }

   }
   else
   {
      return defaultRdResponse(name, type, pResponse);
   }
}

Error isReadOnlyFile(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // params
   std::string path;
   Error error = json::readParam(request.params, 0, &path);
   if (error)
      return error ;
   FilePath filePath = module_context::resolveAliasedPath(path);

   pResponse->setResult(filePath.exists() &&
                        core::system::isReadOnly(filePath));

   return Success();
}

Error getMinimalSourcePath(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   // params
   std::string path;
   Error error = json::readParams(request.params, &path);
   if (error)
      return error ;
   FilePath filePath = module_context::resolveAliasedPath(path);

   // calculate path
   pResponse->setResult(module_context::pathRelativeTo(
            module_context::safeCurrentPath(),
            filePath));

   return Success();
}


Error getScriptRunCommand(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // params
   std::string interpreter, path;
   Error error = json::readParams(request.params, &interpreter, &path);
   if (error)
      return error ;
   FilePath filePath = module_context::resolveAliasedPath(path);

   // use as minimal a path as possible
   FilePath currentPath = module_context::safeCurrentPath();
   if (filePath.isWithin(currentPath))
   {
      path = filePath.relativePath(currentPath);
      if (interpreter.empty())
      {
#ifndef _WINDOWS
         if (path.find_first_of('/') == std::string::npos)
            path = "./" + path;
      }
#endif
   }
   else
   {
      path = filePath.absolutePath();
   }

   // quote if necessary
   if (path.find_first_of(' ') != std::string::npos)
      path = "\\\"" + path + "\\\"";

   // if there's no interpreter then we may need to do a chmod
#ifndef _WINDOWS
   if (interpreter.empty())
   {
      error = r::exec::RFunction(
                 "system",
                  "chmod +x " +
                  string_utils::utf8ToSystem(path)).call();
      if (error)
         return error;
   }
#endif

   // now build and return the command
   std::string command;
   if (interpreter.empty())
      command = path;
   else
      command = interpreter + " " + path;
   command = "system(\"" + command + "\")";

   pResponse->setResult(command);

   return Success();
}

Error setDocOrder(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   json::Array ids;
   std::vector<boost::shared_ptr<SourceDocument> > docs;
   Error error = json::readParams(request.params, &ids);
   if (error)
      return error;
   source_database::list(&docs);

   BOOST_FOREACH( boost::shared_ptr<SourceDocument>& pDoc, docs )
   {
      for (unsigned i = 0; i < ids.size(); i++) 
      {
         // docs are ordered starting at 1; the special value 0 indicates a
         // document with no order
         if (pDoc->id() == ids[i].get_str() && 
             pDoc->relativeOrder() != static_cast<int>(i + 1))
         {
            pDoc->setRelativeOrder(i + 1);
            source_database::put(pDoc);
         }
      }
   }

   return Success();
}

Error getSourceDocument(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   std::string id;
   Error error = json::readParams(request.params, &id);
   if (error)
      return error;

   // get the document from the source database
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error;

   // write the doc to JSON and return it
   json::Object jsonDoc;
   writeDocToJson(pDoc, &jsonDoc);
   pResponse->setResult(jsonDoc);

   return Success();
}

void onDocUpdated(boost::shared_ptr<SourceDocument> pDoc)
{
   source_database::events().onDocUpdated(pDoc);
}

Error setSourceDocumentDirty(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   std::string id;
   bool dirty;
   Error error = json::readParams(request.params, &id, &dirty);
   if (error)
      return error;

   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error;
   
   pDoc->setDirty(dirty);

   // if marking clean, ignore external edits too
   if (!dirty)
   {
      // don't move the write time backwards (the intent is to sync an edit
      // which has just occurred)
      std::time_t writeTime =
            module_context::resolveAliasedPath(pDoc->path()).lastWriteTime();
      if (writeTime > pDoc->lastKnownWriteTime())
         pDoc->setLastKnownWriteTime(writeTime);
   }

   error = source_database::put(pDoc);
   if (error)
      return error;

   onDocUpdated(pDoc);

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
      Error error = core::writeStringToFile(
                               filePath, "",
                               module_context::lineEndings(filePath));
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

void onSuspend(Settings*)
{
}

// update the source database index on resume

// TODO: a resume followed by a client_init will cause us to call
// source_database::list twice (which will cause us to read all of
// the files twice). find a way to prevent this.

void onResume(const Settings&)
{
   source_database::events().onRemoveAll();

   // get the docs and sort them by created
   std::vector<boost::shared_ptr<SourceDocument> > docs ;
   Error error = source_database::list(&docs);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   std::sort(docs.begin(), docs.end(), sortByCreated);

   // notify listeners of updates
   std::for_each(docs.begin(), docs.end(), onDocUpdated);
}

void onShutdown(bool terminatedNormally)
{
   FilePath activeDocumentFile =
         module_context::resolveAliasedPath("~/.active-rstudio-document");
   Error error = activeDocumentFile.removeIfExists();
   if (error)
      LOG_ERROR(error);
}

SEXP rs_fileEdit(SEXP fileSEXP)
{
   try
   {
      // read and validate file name (we ignore all other parameters)
      if (!r::sexp::isString(fileSEXP))
         throw r::exec::RErrorException("invalid filename specification");

      // extract string vector
      std::vector<std::string> filenames;
      Error error = r::sexp::extract(fileSEXP, &filenames);
      if (error)
         throw r::exec::RErrorException(error.summary());

      // fire events
      std::for_each(filenames.begin(), filenames.end(), enqueFileEditEvent);

      // done
      return R_NilValue;
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION

   // keep compiler happy
   return R_NilValue;
}

} // anonymous namespace

Error clientInitDocuments(core::json::Array* pJsonDocs)
{
   source_database::events().onRemoveAll();

   // get the docs and sort them by relative order
   std::vector<boost::shared_ptr<SourceDocument> > docs ;
   Error error = source_database::list(&docs);
   if (error)
      return error ;
   std::sort(docs.begin(), docs.end(), sortByRelativeOrder);

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
      writeDocToJson(pDoc, &jsonDoc);
      pJsonDocs->push_back(jsonDoc);

      source_database::events().onDocUpdated(pDoc);
   }

   return Success();
}

Error initialize()
{   
   // connect to events
   using namespace module_context;
   module_context::events().onShutdown.connect(onShutdown);

   // add suspend/resume handler
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));

   // register fileEdit method
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_fileEdit" ;
   methodDef.fun = (DL_FUNC) rs_fileEdit ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   // install rpc methods
   using boost::bind;
   using namespace rstudio::r::function_hook;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "new_document", newDocument))
      (bind(registerRpcMethod, "open_document", openDocument))
      (bind(registerRpcMethod, "save_document", saveDocument))
      (bind(registerRpcMethod, "save_document_diff", saveDocumentDiff))
      (bind(registerRpcMethod, "check_for_external_edit", checkForExternalEdit))
      (bind(registerRpcMethod, "ignore_external_edit", ignoreExternalEdit))
      (bind(registerRpcMethod, "set_source_document_on_save", setSourceDocumentOnSave))
      (bind(registerRpcMethod, "modify_document_properties", modifyDocumentProperties))
      (bind(registerRpcMethod, "get_document_properties", getDocumentProperties))
      (bind(registerRpcMethod, "revert_document", revertDocument))
      (bind(registerRpcMethod, "reopen_with_encoding", reopenWithEncoding))
      (bind(registerRpcMethod, "close_document", closeDocument))
      (bind(registerRpcMethod, "close_all_documents", closeAllDocuments))
      (bind(registerRpcMethod, "get_source_template", getSourceTemplate))
      (bind(registerRpcMethod, "create_rd_shell", createRdShell))
      (bind(registerRpcMethod, "is_read_only_file", isReadOnlyFile))
      (bind(registerRpcMethod, "get_minimal_source_path", getMinimalSourcePath))
      (bind(registerRpcMethod, "get_script_run_command", getScriptRunCommand))
      (bind(registerRpcMethod, "set_doc_order", setDocOrder))
      (bind(registerRpcMethod, "get_source_document", getSourceDocument))
      (bind(registerRpcMethod, "set_source_document_dirty", setSourceDocumentDirty))
      (bind(sourceModuleRFile, "SessionSource.R"));
   Error error = initBlock.execute();
   if (error)
      return error;

   // init source
   error = r::exec::RFunction(".rs.initSource").call();
   if (error)
      LOG_ERROR(error);
   return Success();
}


} // namespace source
} // namespace modules
} // namespace session
} // namespace rstudio

