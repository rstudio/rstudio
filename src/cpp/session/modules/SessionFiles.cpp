/*
 * SessionFiles.cpp
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


#include "SessionFiles.hpp"

#include <csignal>

#include <vector>
#include <iostream>
#include <fstream>
#include <sstream>
#include <algorithm>

#include <boost/foreach.hpp>
#include <boost/lexical_cast.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/Settings.hpp>
#include <core/Exec.hpp>
#include <core/DateTime.hpp>

#include <core/http/Util.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/Json.hpp>

#include <core/system/ShellUtils.hpp>
#include <core/system/Process.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>

#include <session/SessionClientEvent.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>

#include <session/projects/SessionProjects.hpp>

#include "SessionFilesQuotas.hpp"
#include "SessionFilesListingMonitor.hpp"

using namespace core ;

namespace session {

namespace modules { 
namespace files {

namespace {

// monitor for file listings
FilesListingMonitor s_filesListingMonitor;

// make sure that monitoring persists accross suspended sessions
const char * const kMonitoredPath = "files.monitored-path";   

void onSuspend(Settings* pSettings)
{
   // get monitored path and alias it
   std::string monitoredPath = s_filesListingMonitor.currentMonitoredPath().absolutePath();
   if (!monitoredPath.empty())
   {
      monitoredPath = FilePath::createAliasedPath(FilePath(monitoredPath),
                                                  module_context::userHomePath());
   }

   // set it
   pSettings->set(kMonitoredPath, monitoredPath);
}

void onResume(const Settings& settings)
{
   // get the monitored path
   std::string monitoredPath = settings.get(kMonitoredPath);
   if (!monitoredPath.empty())
   {
      // resolve aliases
      FilePath resolvedPath = FilePath::resolveAliasedPath(
                                            monitoredPath,
                                            module_context::userHomePath());

      // start monitoriing
      json::Array jsonFiles;
      s_filesListingMonitor.start(resolvedPath, &jsonFiles);
   }

   quotas::checkQuotaStatus();
}
   
void onClientInit()
{
   quotas::checkQuotaStatus();
}
   

// extract a set of FilePath object from a list of home path relative strings
Error extractFilePaths(const json::Array& files, 
                       std::vector<FilePath>* pFilePaths)
{   
   for(json::Array::const_iterator 
         it = files.begin(); 
         it != files.end();
         ++it)
   {
      if (it->type() != json::StringType)
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

      std::string file = it->get_str() ;
      pFilePaths->push_back(module_context::resolveAliasedPath(file)) ;
   }

   return Success() ;
}

core::Error stat(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParams(request.params, &path);
   if (error)
      return error;

   FilePath targetPath = module_context::resolveAliasedPath(path);

   pResponse->setResult(module_context::createFileSystemItem(targetPath));
   return Success();
}

core::Error isTextFile(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParams(request.params, &path);
   if (error)
      return error;

   FilePath targetPath = module_context::resolveAliasedPath(path);

   pResponse->setResult(module_context::isTextFile(targetPath));

   return Success();
}

   
Error listFiles(const json::JsonRpcRequest& request, json::JsonRpcResponse* pResponse)
{
   // get args
   std::string path;
   bool monitor;
   Error error = json::readParams(request.params, &path, &monitor);
   if (error)
      return error;
   FilePath targetPath = module_context::resolveAliasedPath(path) ;
   
   // if this includes a request for monitoring
   core::json::Array jsonFiles;
   if (monitor)
   {
      // always stop existing if we have one
      s_filesListingMonitor.stop();

      // install a monitor only if we aren't already covered by the project monitor
      if (!session::projects::projectContext().isMonitoringDirectory(targetPath))
      {
         error = s_filesListingMonitor.start(targetPath, &jsonFiles);
         if (error)
            return error;
      }
      else
      {
         error = FilesListingMonitor::listFiles(targetPath, &jsonFiles);
         if (error)
            return error;
      }
   }
   else
   {
      error = FilesListingMonitor::listFiles(targetPath, &jsonFiles);
      if (error)
         return error;
   }

   pResponse->setResult(jsonFiles);
   return Success();
}


// IN: String path
core::Error createFolder(const core::json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParam(request.params, 0, &path);
   if (error)
      return error ;   
   
   // create the directory
   FilePath folderPath = module_context::resolveAliasedPath(path) ;
   if (folderPath.exists())
   {
      return fileExistsError(ERROR_LOCATION);
   }
   else
   {
      Error createError = folderPath.ensureDirectory() ;
      if (createError)
         return createError ;
   }

   return Success() ;
}

// IN: Array<String> paths
core::Error deleteFiles(const core::json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   json::Array files;
   Error error = json::readParam(request.params, 0, &files);
   if (error)
      return error ;
   
   // extract vector of FilePath
   std::vector<FilePath> filePaths ;
   Error extractError = extractFilePaths(files, &filePaths) ;
   if (extractError)
      return extractError ;

   // delete each file
   Error deleteError ;
   for (std::vector<FilePath>::const_iterator 
         it = filePaths.begin();
         it != filePaths.end();
         ++it)
   {    
      // remove the file
      deleteError = it->remove();
      if (deleteError)
         return deleteError ;
   }

   return Success() ;
}
   
   
void copySourceFile(const FilePath& sourceDir, 
                    const FilePath& destDir,
                    int level,
                    const FilePath& sourceFilePath)
{
   // compute the target path
   std::string relativePath = sourceFilePath.relativePath(sourceDir);
   FilePath targetPath = destDir.complete(relativePath);
   
   // if the copy item is a directory just create it
   if (sourceFilePath.isDirectory())
   {
      Error error = targetPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }
   // otherwise copy it
   else
   {
      Error error = sourceFilePath.copy(targetPath);
      if (error)
         LOG_ERROR(error);
   }
}
   
// IN: String sourcePath, String targetPath
Error copyFile(const core::json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   // read params
   std::string sourcePath, targetPath;
   bool overwrite;
   Error error = json::readParams(request.params,
                                  &sourcePath,
                                  &targetPath,
                                  &overwrite);
   if (error)
      return error;
   FilePath targetFilePath = module_context::resolveAliasedPath(targetPath);
   
   // make sure the target path doesn't exist
   if (targetFilePath.exists())
   {
      if (overwrite)
      {
         Error error = targetFilePath.remove();
         if (error)
         {
            LOG_ERROR(error);
            return fileExistsError(ERROR_LOCATION);
         }
      }
      else
      {
         return fileExistsError(ERROR_LOCATION);
      }
   }

   // compute the source file path
   FilePath sourceFilePath = module_context::resolveAliasedPath(sourcePath);
   
   // copy directories recursively
   Error copyError ;
   if (sourceFilePath.isDirectory())
   {
      // create the target directory
      Error error = targetFilePath.ensureDirectory();
      if (error)
         return error ;
      
      // iterate over the source
      copyError = sourceFilePath.childrenRecursive(
        boost::bind(copySourceFile, sourceFilePath, targetFilePath, _1, _2));
   }
   else
   {
      copyError = sourceFilePath.copy(targetFilePath);
   }
   
   // check quota after copies
   quotas::checkQuotaStatus();
   
   // return error status
   return copyError;
}
      

// IN: Array<String> paths, String targetPath
Error moveFiles(const core::json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   json::Array files;
   std::string targetPath;
   Error error = json::readParams(request.params, &files, &targetPath);
   if (error)
      return error ;
   
   // extract vector of FilePath
   std::vector<FilePath> filePaths ;
   Error extractError = extractFilePaths(files, &filePaths) ;
   if (extractError)
      return extractError ;

   // create FilePath for target directory
   FilePath targetDirPath = module_context::resolveAliasedPath(targetPath);
   if (!targetDirPath.isDirectory())
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   // move the files
   for (std::vector<FilePath>::const_iterator 
         it = filePaths.begin();
         it != filePaths.end();
         ++it)
   {      
      // move the file
      FilePath targetPath = targetDirPath.childPath(it->filename()) ;
      Error moveError = it->move(targetPath) ;
      if (moveError)
         return moveError ;
   }

   return Success() ;
}

// IN: String path, String targetPath
core::Error renameFile(const core::json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // read params
   std::string path, targetPath;
   Error error = json::readParams(request.params, &path, &targetPath);
   if (error)
      return error ;

   // if the destination already exists then send back file exists
    FilePath destPath = module_context::resolveAliasedPath(targetPath) ;
    if (destPath.exists())
       return fileExistsError(ERROR_LOCATION);

   // move the file
   FilePath sourcePath = module_context::resolveAliasedPath(path);
   Error renameError = sourcePath.move(destPath);
   if (renameError)
      return renameError ;
   
   return Success() ;
}

void handleFilesRequest(const http::Request& request, 
                        http::Response* pResponse)
{   
   Options& options = session::options();
   if (options.programMode() != kSessionProgramModeServer)
   {
      pResponse->setError(http::status::NotFound,
                          request.uri() + " not found");
      return;
   }
   
   // get prefix and uri
   std::string prefix = "/files/";
   std::string uri = request.uri();
   
   // validate the uri
   if (prefix.length() >= uri.length() ||    // prefix longer than uri
       uri.find(prefix) != 0 ||              // uri doesn't start with prefix
       uri.find("..") != std::string::npos)  // uri has inavlid char sequence
   {
      pResponse->setError(http::status::NotFound, 
                          request.uri() + " not found");
      return;
   }
   
   // compute path to file
   int prefixLen = prefix.length();
   std::string relativePath = http::util::urlDecode(uri.substr(prefixLen));
   if (relativePath.empty())
   {
      pResponse->setError(http::status::NotFound, request.uri() + " not found");
      return;
   }

   // complete path to file
   FilePath filePath = module_context::userHomePath().complete(relativePath);

   // no directory listing available
   if (filePath.isDirectory())
   {
      pResponse->setError(http::status::NotFound,
                          "No listing available for " + request.uri());
      return;
   }


   pResponse->setNoCacheHeaders();
   pResponse->setFile(filePath, request);
}
   
const char * const kUploadFilename = "filename";
const char * const kUploadedTempFile = "uploadedTempFile";
const char * const kUploadTargetDirectory = "targetDirectory";
   
Error completeUpload(const core::json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // read params
   json::Object token;
   bool commit ;
   Error error = json::readParams(request.params, &token, &commit);
   if (error)
      return error ;
   
   // parse fields out of token object
   std::string filename, uploadedTempFile, targetDirectory;
   error = json::readObject(token, 
                            kUploadFilename, &filename,
                            kUploadedTempFile, &uploadedTempFile,
                            kUploadTargetDirectory, &targetDirectory);
   if (error)
      return error;
   
   // get path to temp file
   FilePath uploadedTempFilePath(uploadedTempFile);
   
   // commit or cancel
   if (commit)
   {
      FilePath targetDirectoryPath(targetDirectory);
      if (uploadedTempFilePath.extensionLowerCase() == ".zip")
      {
         // expand the archive
         r::exec::RFunction unzip("unzip");
         unzip.addParam("zipfile", uploadedTempFilePath.absolutePath());
         unzip.addParam("exdir", targetDirectoryPath.absolutePath());
         Error unzipError = unzip.call();
         if (unzipError)
            return unzipError;
         
         // remove the __MACOSX folder if it exists
         const std::string kMacOSXFolder("__MACOSX");
         FilePath macOSXPath = targetDirectoryPath.complete(kMacOSXFolder);
         Error removeError = macOSXPath.removeIfExists();
         if (removeError)
            LOG_ERROR(removeError);
      }
      else
      {
         // calculate target path
         FilePath targetPath = targetDirectoryPath.childPath(filename);
         
         // remove existing target path
         Error removeError = targetPath.removeIfExists();
         if (removeError)
            return removeError;
         
         // copy the source to the destination
         Error copyError = uploadedTempFilePath.copy(targetPath);
         if (copyError)
            return copyError;
      }

      // remove the uploaded temp file
      error = uploadedTempFilePath.remove();
      if (error)
         LOG_ERROR(error);
      
      // check quota after uploads
      quotas::checkQuotaStatus();
      
      return Success();
   }
   else
   {
      // merely log failures to remove the temp file (not critical to the user)
      Error error = uploadedTempFilePath.removeIfExists();
      if (error)
         LOG_ERROR(error);
      
      return Success();
   }
}
   
   
Error detectZipFileOverwrites(const FilePath& uploadedZipFile,
                              const FilePath& destDir,
                              json::Array* pOverwritesJson)
{
   // query for all of the paths in the zip file
   std::vector<std::string> zipFileListing;
   r::exec::RFunction listZipFile(".rs.listZipFile",
                                  uploadedZipFile.absolutePath());
   Error unzipError = listZipFile.call(&zipFileListing);
   if (unzipError)
      return unzipError;
   
   // check for overwrites
   for (std::vector<std::string>::const_iterator 
        it = zipFileListing.begin();
        it != zipFileListing.end();
        ++it)
   {
      FilePath filePath = destDir.complete(*it);
      if (filePath.exists())
         pOverwritesJson->push_back(module_context::createFileSystemItem(filePath));
   }
   
   return Success();
}
   
bool validateUploadedFile(const http::File& file, http::Response* pResponse)
{
   // get limit
   size_t mbLimit = session::options().limitFileUploadSizeMb();

   // don't enforce if no limit specified
   if (mbLimit <= 0)
      return true;

   // convert limit to bytes
   size_t byteLimit = mbLimit * 1024 * 1024;
   
   // compare to file size
   if (file.contents.size() > byteLimit)
   {
      Error fileTooLargeError = systemError(boost::system::errc::file_too_large,
                                            ERROR_LOCATION);
   
      json::setJsonRpcError(fileTooLargeError, pResponse);
   
      return false;
   }
   else
   {
      return true;
   }
}
   
void handleFileUploadRequest(const http::Request& request, 
                             http::Response* pResponse) 
{
   // response content type must always be text/html to be handled
   // properly by the browser/gwt on the client side
   pResponse->setContentType("text/html");
   
   // get fields
   const http::File& file = request.uploadedFile("file");
   std::string targetDirectory = request.formFieldValue("targetDirectory");
   
   // first validate that we got the required fields
   if (file.name.empty() || targetDirectory.empty())
   {
      json::setJsonRpcError(json::errc::ParamInvalid, pResponse);
      return;
   }
   
   // now validate the file
   if ( !validateUploadedFile(file, pResponse) )
      return ;
   
   // form destination path
   FilePath destDir = module_context::resolveAliasedPath(targetDirectory);
   FilePath destPath = destDir.childPath(file.name);
   
   // establish whether this is a zip file and create appropriate temp file path
   bool isZip = destPath.extensionLowerCase() == ".zip";
   FilePath tempFilePath = module_context::tempFile("upload", 
                                                    isZip ? "zip" : "bin");
   
   // attempt to write the temp file
   Error saveError = core::writeStringToFile(tempFilePath, file.contents);
   if (saveError)
   {
      LOG_ERROR(saveError);
      json::setJsonRpcError(saveError, pResponse);
      return;
   }
   
   // detect any potential overwrites 
   json::Array overwritesJson;
   if (isZip)
   {
      Error error = detectZipFileOverwrites(tempFilePath, 
                                            destDir, 
                                            &overwritesJson);
      if (error)
      {
         LOG_ERROR(error);
         json::setJsonRpcError(error, pResponse);
         return;
      }
   }
   else
   {
      if (destPath.exists())
         overwritesJson.push_back(module_context::createFileSystemItem(destPath));
   }
   
   // set the upload information as the result
   json::Object uploadTokenJson;
   uploadTokenJson[kUploadFilename] = file.name;
   uploadTokenJson[kUploadedTempFile] = tempFilePath.absolutePath();
   uploadTokenJson[kUploadTargetDirectory] = destDir.absolutePath();
   json::Object uploadJson;
   uploadJson["token"] = uploadTokenJson;
   uploadJson["overwrites"] = overwritesJson;
   json::setJsonRpcResult(uploadJson, pResponse);   
}
   
void setAttachmentResponse(const http::Request& request,
                           const std::string& filename,
                           const FilePath& attachmentPath,
                           http::Response* pResponse)
{
   if (request.headerValue("User-Agent").find("MSIE") == std::string::npos)
   {
      pResponse->setNoCacheHeaders();
   }
   else
   {
      // Can't set full no-cache headers because this breaks downloads in IE
      pResponse->setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
      pResponse->setHeader("Cache-Control", "private");
   }
   // Can't rely on "filename*" in Content-Disposition header because not all
   // browsers support non-ASCII characters here (e.g. Safari 5.0.5). If
   // possible, make the requesting URL contain the UTF-8 byte escaped filename
   // as the last path element.
   pResponse->setHeader("Content-Disposition",
                        "attachment; filename*=UTF-8''"
                        + http::util::urlEncode(filename, false));
   pResponse->setHeader("Content-Type", "application/octet-stream");
   pResponse->setBody(attachmentPath);
}
   
void handleMultipleFileExportRequest(const http::Request& request, 
                                     http::Response* pResponse)
{
   // name parameter
   std::string name = request.queryParamValue("name");
   if (name.empty())
   {
      pResponse->setError(http::status::BadRequest, "name not specified");
      return;
   }
   
   // parent parameter
   std::string parent = request.queryParamValue("parent");
   if (parent.empty())
   {
      pResponse->setError(http::status::BadRequest, "parent not specified");
      return;
   }
   FilePath parentPath = module_context::resolveAliasedPath(parent);
   if (!parentPath.exists())
   {
      pResponse->setError(http::status::BadRequest, "parent doesn't exist");
      return;
   }
   
   // files parameters (paths relative to parent)
   std::vector<std::string> files;
   for (int i=0; ;i++)
   {
      // get next file (terminate when we stop finding files)
      std::string fileParam = "file" + safe_convert::numberToString(i);
      std::string file = request.queryParamValue(fileParam);
      if (file.empty())
         break;
      
      // verify that the file exists
      FilePath filePath = parentPath.complete(file);
      if (!filePath.exists())
      {
         pResponse->setError(http::status::BadRequest, 
                             "file " + file + " doesn't exist");
         return;
      }
      
      // add it
      files.push_back(file);
   }
   
   // create the zip file
   FilePath tempZipFilePath = module_context::tempFile("export", "zip");
   Error error = r::exec::RFunction(".rs.createZipFile",
                                    tempZipFilePath.absolutePath(),
                                    parentPath.absolutePath(),
                                    files).call();
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setError(error);
      return;
   }
   
   // return attachment
   setAttachmentResponse(request, name, tempZipFilePath, pResponse);
}
   
void handleFileExportRequest(const http::Request& request, 
                             http::Response* pResponse) 
{
   // see if this is a single or multiple file request
   std::string file = request.queryParamValue("file");
   if (!file.empty())
   {
      // resolve alias and ensure that it exists
      FilePath filePath = module_context::resolveAliasedPath(file);
      if (!filePath.exists())
      {
         pResponse->setError(http::status::NotFound, "file doesn't exist");
         return;
      }
      
      // get the name
      std::string name = request.queryParamValue("name");
      if (name.empty())
      {
         pResponse->setError(http::status::BadRequest, "name not specified");
         return;
      }
      
      // download as attachment
      setAttachmentResponse(request, name, filePath, pResponse);
   }
   else
   {
      handleMultipleFileExportRequest(request, pResponse);
   }
}

SEXP rs_pathInfo(SEXP pathSEXP)
{
   try
   {
      // validate
      if (r::sexp::length(pathSEXP) != 1)
      {
         throw r::exec::RErrorException(
                        "must pass a single file to get path info for");
      }

      std::string path;
      Error error = r::sexp::extract(pathSEXP, &path);
      if (error)
         throw r::exec::RErrorException(r::endUserErrorMessage(error));

      // resolve aliased path
      FilePath filePath = module_context::resolveAliasedPath(path);
      if (filePath.empty())
         throw r::exec::RErrorException("invalid path: " + path);

      // create path info vector (use json repsesentation to force convertion
      // to VECSXP rather than STRSXP)
      json::Object pathInfo;
      pathInfo["path"] = filePath.absolutePath();
      std::string parent = filePath.absolutePath();
      FilePath parentPath = filePath.parent();
      if (!parentPath.empty())
         parent = parentPath.absolutePath();
      pathInfo["directory"] = parent;
      pathInfo["name"] = filePath.filename();
      pathInfo["stem"] = filePath.stem();
      pathInfo["extension"] = filePath.extension();

      // return it
      r::sexp::Protect rProtect;
      return r::sexp::create(pathInfo, &rProtect);
   }
   catch(r::exec::RErrorException e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

} // anonymous namespace

bool isMonitoringDirectory(const FilePath& directory)
{
   FilePath monitoredPath = s_filesListingMonitor.currentMonitoredPath();
   return !monitoredPath.empty() && (directory == monitoredPath);
}

Error initialize()
{
   // register suspend handler
   using namespace module_context;
   addSuspendHandler(SuspendHandler(onSuspend, onResume));
   
   // subscribe to events
   using boost::bind;
   events().onClientInit.connect(bind(onClientInit));

   // register path info function
   R_CallMethodDef pathInfoMethodDef ;
   pathInfoMethodDef.name = "rs_pathInfo" ;
   pathInfoMethodDef.fun = (DL_FUNC) rs_pathInfo ;
   pathInfoMethodDef.numArgs = 1;
   r::routines::addCallMethod(pathInfoMethodDef);

   // install handlers
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "stat", stat))
      (bind(registerRpcMethod, "is_text_file", isTextFile))
      (bind(registerRpcMethod, "list_files", listFiles))
      (bind(registerRpcMethod, "create_folder", createFolder))
      (bind(registerRpcMethod, "delete_files", deleteFiles))
      (bind(registerRpcMethod, "copy_file", copyFile))
      (bind(registerRpcMethod, "move_files", moveFiles))
      (bind(registerRpcMethod, "rename_file", renameFile))
      (bind(registerUriHandler, "/files", handleFilesRequest))
      (bind(registerUriHandler, "/upload", handleFileUploadRequest))
      (bind(registerUriHandler, "/export", handleFileExportRequest))
      (bind(registerRpcMethod, "complete_upload", completeUpload))
      (bind(sourceModuleRFile, "SessionFiles.R"))
      (bind(quotas::initialize));
   return initBlock.execute();
}


} // namepsace files
} // namespace modules
} // namesapce session

