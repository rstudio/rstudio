/*
 * SessionSource.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <fstream>

#include <gsl/gsl>

#include <boost/bind.hpp>
#include <boost/utility.hpp>

#include <core/r_util/RSourceIndex.hpp>

#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RPackageInfo.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/FileChangeEvent.hpp>
#include <core/system/Xdg.hpp>

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

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/Preferences.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace source {

using namespace session::source_database;

namespace {

module_context::WaitForMethodFunction s_waitForRequestDocumentSave;
module_context::WaitForMethodFunction s_waitForRequestDocumentClose;

Error sourceDatabaseError(Error error)
{
   if (isFileNotFoundError(error))
   {
      // The regular message (no such file or directory) is not useful
      // to end users when attempting to save files (especially for autosaves),
      // since they typically have no knowledge that the source database exists.
      // Instead, log this as a generic 'internal error'.
      return Error(
               boost::system::errc::no_such_file_or_directory,
               "An internal error occurred",
               error.getLocation());
   }
   else
   {
      return error;
   }
}
std::string inferDocumentType(const FilePath& documentPath,
                              const std::string& defaultType)
{
   // read first line in document
   std::ifstream ifs(documentPath.getAbsolutePath());
   if (!ifs.is_open())
      return defaultType;
 
   // try to read the first line
   std::string line;
   std::getline(ifs, line);
   ifs.close();
   
   // check for a shebang line
   if (!boost::algorithm::starts_with(line, "#!"))
      return defaultType;
   
   // use heuristics to guess the file type
   boost::regex pattern("(?:\\s|/)([^\\s/]+)(?=\\s|$)");
   boost::sregex_token_iterator it(line.begin(), line.end(), pattern, 1);
   boost::sregex_token_iterator end;
   for (; it != end; ++it)
   {
      // skip things that look like flags
      if (boost::algorithm::starts_with(*it, "-"))
         continue;
      
      // check for common shells
      for (auto&& shell : {"bash", "csh", "fish", "ksh", "zsh"})
         if (*it == shell)
            return kSourceDocumentTypeShell;
      
      // check for R
      for (auto&& r : {"r", "R", "Rscript"})
         if (*it == r)
            return kSourceDocumentTypeRSource;
      
      // check for Python
      if (boost::algorithm::starts_with(*it, "python"))
         return kSourceDocumentTypePython;
   }
   
   return defaultType;

}

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
   
   // discover project-specific settings when available (only applied when
   // opening a file that belongs to a project separate from the active project)
   r_util::RProjectConfig projConfig;
   bool useConfig = false;
   
   if (!pDoc->path().empty())
   {
      FilePath docPath = module_context::resolveAliasedPath(pDoc->path());

      if (projects::projectContext().hasProject() &&
          docPath.isWithin(projects::projectContext().directory()))
      {
         // this file belongs to the active project: do nothing here and let
         // other machinery handle setting of project-specific options
         useConfig = false;
      }
      else
      {
         Error error = r_util::findProjectConfig(
                  docPath,
                  module_context::userHomePath(),
                  &projConfig);
         useConfig = !error;
      }
   }
   
   if (useConfig)
   {
      json::Object projConfigJson;
      
      projConfigJson["tab_size"] = projConfig.numSpacesForTab;
      projConfigJson["use_soft_tabs"] = projConfig.useSpacesForTab;
      projConfigJson["strip_trailing_whitespace"] = projConfig.stripTrailingWhitespace;
      projConfigJson["ensure_trailing_newline"] = projConfig.autoAppendNewline;
      
      (*pDocJson)["project_config"] = projConfigJson;
   }
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
   return gsl::narrow_cast<int>(docs.size());
}

// wrap source_database::put for situations where there are new contents
// (so we can index the contents)
Error sourceDatabasePutWithUpdatedContents(boost::shared_ptr<SourceDocument> pDoc,
                                           bool writeContents = true,
                                           bool retryWrite = false)
{
   // write the file to the database
   Error error = source_database::put(pDoc, writeContents, retryWrite);
   if (error)
      return error;

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
      return error;

   // create the new doc and write it to the database
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument(type));

   if (json::isType<std::string>(jsonContents))
      pDoc->setContents(jsonContents.getString());

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
      return error;
   
   std::string type;
   error = json::readParam(request.params, 1, &type);
   if (error)
      return error;

   std::string encoding;
   error = json::readParam(request.params, 2, &encoding);
   if (error && error != core::json::errc::ParamTypeMismatch)
      return error;

   if (encoding.empty())
   {
      // prefer UTF-8 encoding for R Markdown documents if no
      // encoding is set
      if (type == "r_markdown" && encoding == "")
         encoding = "UTF-8";
      else
         encoding = ::locale2charset(nullptr);
   }
   
   FilePath documentPath = module_context::resolveAliasedPath(path);
   if (!module_context::isPathViewAllowed(documentPath))
   {
      Error error = systemError(boost::system::errc::operation_not_permitted,
                                ERROR_LOCATION);
      pResponse->setError(error, "The file is in a restricted path and cannot "
                                 "be opened by the source editor.");
      
   }

   // ensure the file exists
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
   
   // infer type from the document if appropriate
   if (type == "text")
   {
      type = inferDocumentType(documentPath, type);
   }

   // set the doc contents to the specified file
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument(type));
   pDoc->setEncoding(encoding);
   error = pDoc->setPathAndContents(path, false);
   if (error)
   {
      error = pDoc->setPathAndContents(path, true);
      if (error)
         return error;

      module_context::consoleWriteError(
         "Not all characters in " + documentPath.getAbsolutePath() +
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
                       boost::shared_ptr<SourceDocument> pDoc,
                       bool retryWrite)
{
   // check whether we have a path and if we do get/resolve its value
   std::string oldPath, path;
   FilePath fullDocPath;
   bool hasPath = json::isType<std::string>(jsonPath);
   if (hasPath)
   {
      oldPath = pDoc->path();
      path = jsonPath.getString();
      fullDocPath = module_context::resolveAliasedPath(path);
   }

   // update dirty state: dirty if there was no path AND the new contents
   // are different from the old contents (and was thus a content autosave
   // as distinct from a fold-spec or scroll-position/selection autosave)
   pDoc->setDirty(!hasPath && (contents != pDoc->contents()));
   
   bool hasType = json::isType<std::string>(jsonType);
   if (hasType)
   {
      pDoc->setType(jsonType.getString());
   }
   
   Error error;
   
   bool hasEncoding = json::isType<std::string>(jsonEncoding);
   if (hasEncoding)
   {
      pDoc->setEncoding(jsonEncoding.getString());
   }

   bool hasFoldSpec = json::isType<std::string>(jsonFoldSpec);
   if (hasFoldSpec)
   {
      pDoc->setFolds(jsonFoldSpec.getString());
   }

   // note that it's entirely possible for the chunk output to be null if the
   // document is unrendered (in which case we want to leave the chunk output
   // as-is)
   bool hasChunkOutput = json::isType<json::Array>(jsonChunkOutput);
   if (hasChunkOutput && pDoc->isRMarkdownDocument())
   {
      error = rmarkdown::notebook::setChunkDefs(pDoc, 
            jsonChunkOutput.getArray());
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
                          "Save with Encoding...\" from the main menu.\n");
      }

      // note whether the file existed prior to writing
      bool newFile = !fullDocPath.exists();

      // write the contents to the file
      int writeTimeout = retryWrite ? session::prefs::userPrefs().saveRetryTimeout() : 0;
      error = writeStringToFile(fullDocPath, encoded,
                                module_context::lineEndings(fullDocPath),
                                true,
                                writeTimeout);
      if (error)
         return error;

      // set the new path and contents for the document
      error = pDoc->setPathAndContents(path);
      if (error)
         return error;

      // enque file changed event if we need to
      if (!module_context::isDirectoryMonitored(fullDocPath.getParent()))
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
   bool retryWrite = false;
   json::Value jsonPath, jsonType, jsonEncoding, jsonFoldSpec, jsonChunkOutput;
   Error error = json::readParams(request.params, 
                                  &id, 
                                  &jsonPath, 
                                  &jsonType, 
                                  &jsonEncoding,
                                  &jsonFoldSpec,
                                  &jsonChunkOutput,
                                  &contents,
                                  &retryWrite);
   if (error)
      return error;
   
   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return sourceDatabaseError(error);
   
   // check if the document contents have changed
   bool hasChanges = contents != pDoc->contents();
   error = saveDocumentCore(contents, jsonPath, jsonType, jsonEncoding,
                            jsonFoldSpec, jsonChunkOutput, pDoc, retryWrite);
   if (error)
      return error;
   
   // write to the source_database
   error = sourceDatabasePutWithUpdatedContents(pDoc, hasChanges, retryWrite);
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
   bool valid;
   
   // This is the expected hash of the current document. If the
   // current hash value is different than this value, then the
   // document cannot be patched and the request should be discarded.
   std::string hash;
   
   // indicated whether or not this is write operation should be retried
   // if the file handle cannot be acquired - this is used for
   // manual saves as they can take longer as they are user-initiated actions
   // autosaves need to be quick as they occur frequently
   bool retryWrite = false;

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
                                  &valid,
                                  &hash,
                                  &retryWrite);
   if (error)
      return error;
   
   // if this has no path then it is an autosave, in this case
   // suppress change detection and write retries
   bool hasPath = json::isType<std::string>(jsonPath);
   if (!hasPath)
      pResponse->setSuppressDetectChanges(true);

   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return sourceDatabaseError(error);
   
   // Don't even attempt anything if we're not working off the same original
   if (pDoc->hash() != hash)
      return Success();
   
   // attempt the document save. note that if this fails,
   // we won't set a response hash and RStudio will take this as a signal
   // to attempt a 'full' document save rather than just a diff-based save
   try
   {
      std::string contents(pDoc->contents());
      
      // NOTE: this flag denotes whether the front-end successfully
      // constructed a diff to be saved; we leave this in while still
      // going down this code path just to ensure that any code that
      // runs in response to a document save (even if that save fails)
      // still has a chance to run
      if (valid)
      {
         // the offsets we receive are in bytes, so we can replace the contents
         // of the string directly at the supplied offset + length (the contents
         // string itself is already UTF-8 encoded)
         contents.replace(offset, length, replacement);
      }

      // track if we're updating the document contents
      bool hasChanges = contents != pDoc->contents();
      error = saveDocumentCore(contents, jsonPath, jsonType, jsonEncoding,
                               jsonFoldSpec, jsonChunkOutput, pDoc, retryWrite);
      if (error)
         return error;

      // write to the source database (don't worry about writing document
      // contents if those have not changed)
      error = sourceDatabasePutWithUpdatedContents(pDoc, hasChanges, retryWrite);
      if (error)
         return error;

      // set document hash
      pResponse->setResult(pDoc->hash());
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   return Success();
}

Error checkForExternalEdit(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   pResponse->setSuppressDetectChanges(true);

   // params
   std::string id;
   Error error = json::readParams(request.params, &id);
   if (error)
      return error;

   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error;

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
         std::time_t lastWriteTime;
         pDoc->checkForExternalEdit(&lastWriteTime);

         if (lastWriteTime)
         {
            FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
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
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   Error error = source_database::get(id, pDoc);
   if (error)
      return error;

   if (!encoding.empty())
      pDoc->setEncoding(encoding);

   if (!fileType.empty())
      pDoc->setType(fileType);

   error = pDoc->setPathAndContents(pDoc->path(), false);
   if (error)
   {
      error = pDoc->setPathAndContents(pDoc->path(), true);
      if (error)
         return error;

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
   std::string id, fileType;
   Error error = json::readParams(request.params, &id, &fileType);
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
   std::string id;
   Error error = json::readParams(request.params, &id);
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
   std::string id;
   bool value = false;
   Error error = json::readParams(request.params, &id, &value);
   if (error)
      return error;
   
   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error;
   
   // set source on save and then write it
   pDoc->setSourceOnSave(value);
   return source_database::put(pDoc);
}   
   

Error modifyDocumentProperties(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   // params
   std::string id;
   json::Object properties;
   Error error = json::readParams(request.params, &id, &properties);
   if (error)
      return error;

   // get the doc
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, false, pDoc);
   if (error)
      return error;

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
   std::string id;
   Error error = json::readParam(request.params, 0, &id);
   if (error)
      return error;

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
   string_utils::LineEnding ending = string_utils::LineEndingNative;

   FilePath templatePath;

   // First, check user template path
   templatePath = core::system::xdg::userConfigDir().completePath("templates")
                                                    .completePath(templateName);
   if (!templatePath.exists())
   {
      // Next, check the system template path.
      templatePath = core::system::xdg::systemConfigFile("templates").completePath(templateName);
      if (!templatePath.exists())
      {
         // No user or system template; check for a built-in template.
         templatePath = session::options().rResourcesPath().completePath("templates")
                                          .completePath(templateName);

#ifdef __APPLE__
         // Special case: built-in templates can have an OSX variant; prefer that if it exists
         FilePath osxTemplatePath = templatePath.getParent().completePath(
               templatePath.getStem() + "_osx" + templatePath.getExtension());
         if (osxTemplatePath.exists())
            templatePath = osxTemplatePath;
#endif

         // Built-in templates always use posix line endings
         ending = string_utils::LineEndingPosix;
      }
   }

   if (!templatePath.exists())
   {
      // We didn't find a user, system, or built-in template, so use an empty one.
      *pContents = "";
      return Success();
   }

   // read file with template filter
   return core::readStringFromFile(templatePath,
                                   filter,
                                   pContents,
                                   ending);
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
   error = processSourceTemplate(name, templateName, &contents);
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
         FilePath manFilePath = packageDir.completeChildPath("man").completeChildPath(
            rdFilePath.getFilename());
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
      return error;
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
      return error;
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
      return error;
   FilePath filePath = module_context::resolveAliasedPath(path);

   // use as minimal a path as possible
   FilePath currentPath = module_context::safeCurrentPath();
   if (filePath.isWithin(currentPath))
   {
      path = filePath.getRelativePath(currentPath);
      if (interpreter.empty())
      {
#ifndef _WIN32
         if (path.find_first_of('/') == std::string::npos)
            path = "./" + path;
#endif
      }
   }
   else
   {
      path = filePath.getAbsolutePath();
   }

   // quote if necessary
   if (path.find_first_of(' ') != std::string::npos)
      path = "\\\"" + path + "\\\"";

   // if there's no interpreter then we may need to do a chmod
#ifndef _WIN32
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

   for (boost::shared_ptr<SourceDocument>& pDoc : docs)
   {
      for (unsigned i = 0; i < ids.getSize(); i++)
      {
         // docs are ordered starting at 1; the special value 0 indicates a
         // document with no order
         if (pDoc->id() == ids[i].getString() &&
             pDoc->relativeOrder() != gsl::narrow_cast<int>(i + 1))
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
            module_context::resolveAliasedPath(pDoc->path()).getLastWriteTime();
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

   // construct file path from full path
   FilePath filePath = (boost::algorithm::starts_with(file, "~"))
         ? module_context::resolveAliasedPath(file)
         : module_context::safeCurrentPath().completePath(file);

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

   // construct file system item (also tag with mime type)
   json::Object fileJson = module_context::createFileSystemItem(filePath);
   fileJson["mime_type"] = filePath.getMimeContentType();
   
   // fire event
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
   std::vector<boost::shared_ptr<SourceDocument> > docs;
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
      Error error = r::sexp::extract(fileSEXP, &filenames, true);
      if (error)
         throw r::exec::RErrorException(error.getSummary());

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

void fillIds(SEXP idsSEXP, json::Object *pJsonData) 
{
   json::Object& jsonData = *pJsonData;
   jsonData["ids"] = json::Value();
   if (TYPEOF(idsSEXP) == STRSXP)
   {
      std::vector<std::string> ids;
      r::sexp::fillVectorString(idsSEXP, &ids);
      jsonData["ids"] = json::toJsonArray(ids);
   }
}

bool waitForSuccess(ClientEvent& event, module_context::WaitForMethodFunction& waitMethod) 
{
   json::JsonRpcRequest request;
   if (!waitMethod(&request, event))
      return false;
   
   bool success = false;
   Error error = json::readParams(request.params, &success);
   if (error)
      LOG_ERROR(error);
   
   return success;
}

SEXP rs_requestDocumentSave(SEXP idsSEXP)
{
   r::sexp::Protect protect;
   
   json::Object jsonData;
   fillIds(idsSEXP, &jsonData);
   
   ClientEvent event(client_events::kRequestDocumentSave, jsonData);

   return r::sexp::create(waitForSuccess(event, s_waitForRequestDocumentSave), &protect);
}

SEXP rs_requestDocumentClose(SEXP idsSEXP, SEXP saveSXP) {
   r::sexp::Protect protect;
   
   json::Object jsonData;
   fillIds(idsSEXP, &jsonData);

   jsonData["save"] = r::sexp::asLogical(saveSXP);

   ClientEvent event(client_events::kRequestDocumentClose, jsonData);
   
   return r::sexp::create(waitForSuccess(event, s_waitForRequestDocumentClose), &protect);
}

SEXP rs_readSourceDocument(SEXP idSEXP)
{
   std::string id = r::sexp::asString(idSEXP);
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   Error error = source_database::get(id, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   r::sexp::Protect protect;
   return r::sexp::create(pDoc->contents(), &protect);
}

} // anonymous namespace

Error clientInitDocuments(core::json::Array* pJsonDocs)
{
   source_database::events().onRemoveAll();

   // get the docs and sort them by relative order
   std::vector<boost::shared_ptr<SourceDocument> > docs;
   Error error = source_database::list(&docs);
   if (error)
      return error;
   std::sort(docs.begin(), docs.end(), sortByRelativeOrder);

   // populate the array
   pJsonDocs->clear();
   for (boost::shared_ptr<SourceDocument>& pDoc : docs)
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

      json::Object jsonDoc;
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
   
   // register waitfor methods
   s_waitForRequestDocumentSave =
         module_context::registerWaitForMethod("request_document_save_completed");
   s_waitForRequestDocumentClose =
         module_context::registerWaitForMethod("request_document_close_completed");

   RS_REGISTER_CALL_METHOD(rs_fileEdit, 1);
   RS_REGISTER_CALL_METHOD(rs_requestDocumentSave, 1);
   RS_REGISTER_CALL_METHOD(rs_readSourceDocument, 1);
   RS_REGISTER_CALL_METHOD(rs_requestDocumentClose, 2);

   // install rpc methods
   using boost::bind;
   using namespace rstudio::r::function_hook;
   ExecBlock initBlock;
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

