/*
 * SessionFiles.cpp
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

#include "SessionFiles.hpp"

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

#include <core/system/DirectoryMonitor.hpp>

#include <core/http/Util.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/Json.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>

#include <session/SessionClientEvent.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include "SessionFilesQuotas.hpp"

using namespace core ;

namespace session {

namespace modules { 
namespace files {

namespace {
   
// constants for enquing file changed events
const char * const kType = "type";
const char * const kFile = "file";
const char * const kTargetFile = "targetFile";
   
void enqueFileChangeEvent(const core::system::FileChangeEvent& event)
{
   json::Object fileChange ;
   fileChange[kType] = event.type();
   fileChange[kFile] = module_context::createFileSystemItem(event.fileInfo());
   ClientEvent clientEvent(client_events::kFileChanged, fileChange);
   module_context::enqueClientEvent(clientEvent);
}
   
// NOTE: we explicitly fire removed events for directories because of
// limitations in the way we use inotify in DirectoryMonitor. once we 
// lift these restrictions we need to get rid of these explicit calls
void enqueFileRemovedEvent(const FileInfo& fileInfo)
{
   using core::system::FileChangeEvent;
   enqueFileChangeEvent(FileChangeEvent(FileChangeEvent::FileRemoved, 
                                        fileInfo));
}

bool isVisible(const FilePath& file)
{
   // check extension for special file types which are always visible
   std::string ext = file.extensionLowerCase();
   if (ext == ".rprofile" ||
       ext == ".rdata"    ||
       ext == ".rhistory" )
   {
      return true;
   }
   else
   {
      return !file.isHidden();
   }
}  

// directory monitor instance. the DirectoryMonitor is started when a 
// call to list_files includes monitor=true or when a session which was
// previously monitoring files is resumed
core::system::DirectoryMonitor s_directoryMonitor;
 
bool fileEventFilter(const FileInfo& fileInfo)
{
   return isVisible(FilePath(fileInfo.absolutePath()));
}
   
void startMonitoring(const std::string& path)
{
   Error error = s_directoryMonitor.start(path,
                                          boost::bind(fileEventFilter, 
                                                         _1));
   if (error)
      LOG_ERROR(error);
}

// make sure that monitoring persists accross suspended sessions
const char * const kMonitoredPath = "files.monitored-path";   

void onSuspend(Settings* pSettings)
{
   pSettings->set(kMonitoredPath, s_directoryMonitor.path());
}

void onResume(const Settings& settings)
{
   std::string monitoredPath = settings.get(kMonitoredPath);
   if (!monitoredPath.empty())
      startMonitoring(monitoredPath);
   
   quotas::checkQuotaStatus();
}
   
void onClientInit()
{
   quotas::checkQuotaStatus();
}
   
void onDetectChanges(module_context::ChangeSource source)
{
   // poll for events
   std::vector<core::system::FileChangeEvent> events;
   Error error = s_directoryMonitor.checkForEvents(&events);
   if (error)
      LOG_ERROR(error);

   // fire client events as necessary
   std::for_each(events.begin(), events.end(), enqueFileChangeEvent);
}
   
void onShutdown(bool terminatedNormally)
{
   Error error = s_directoryMonitor.stop();
   if (error)
      LOG_ERROR(error);
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

   
// IN: String path
// OUT: Array<FileEntry> files
core::Error listFiles(const json::JsonRpcRequest& request, 
                      json::JsonRpcResponse* pResponse)
{
   // get path
   std::string path;
   bool monitor;
   Error error = json::readParams(request.params, &path, &monitor);
   if (error)
      return error ;
   
   // retreive list of files
   FilePath targetPath = module_context::resolveAliasedPath(path) ;
   std::vector<FilePath> files ;
   Error listingError = targetPath.children(&files) ;
   if (listingError)
      return listingError ;

   // sort the files by name
   std::sort(files.begin(), files.end(), compareAbsolutePathNoCase);
   
   // produce json listing
   json::Array jsonFiles ;
   BOOST_FOREACH( FilePath& filePath, files )
   {
      // files which may have been deleted after the listing or which
      // are not end-user visible
      if (filePath.exists() && isVisible(filePath))
      {
         json::Object fileObject = module_context::createFileSystemItem(filePath);
         jsonFiles.push_back(fileObject) ;
      }
   }

   // return listing
   pResponse->setResult(jsonFiles) ;
   
   // setup monitoring if requested (merely log errors so if something
   // unexpected happens the user still gets the file listing)
   if (monitor)
      startMonitoring(targetPath.absolutePath());
   
   // success
   return Success();
}
   

// IN: String path
core::Error createFile(const core::json::JsonRpcRequest& request, 
                       json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParam(request.params, 0, &path);
   if (error)
      return error ;   
   
   // calculate file path
   FilePath filePath = module_context::resolveAliasedPath(path) ;
   if (filePath.exists())
   {
      return systemError(boost::system::errc::file_exists, ERROR_LOCATION);
   }
   else
   {
      // create the file
      std::ofstream ofs(filePath.absolutePath().c_str(), 
                  std::ios_base::out | std::ios_base::trunc ) ;
      if (!ofs)
         return systemError(boost::system::errc::io_error, ERROR_LOCATION);

      ofs.close();
   }

   return Success() ;
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
      return systemError(boost::system::errc::file_exists, ERROR_LOCATION);
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
      // cache file info before we delete
      FileInfo fileInfo(*it);
     
      // remove the file
      deleteError = it->remove();
      if (deleteError)
         return deleteError ;
      
      // post delete event (inotify doesn't pick up folder deletes)
      if (fileInfo.isDirectory())
         enqueFileRemovedEvent(fileInfo);
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
   Error error = json::readParams(request.params, &sourcePath, &targetPath);
   if (error)
      return error ;
   
   // make sure the target path doesn't exist
   FilePath targetFilePath = module_context::resolveAliasedPath(targetPath);
   if (targetFilePath.exists())
   {
      return systemError(boost::system::errc::file_exists, 
                         ERROR_LOCATION);
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
      // cache FileInfo before deleting
      FileInfo fileInfo(*it);
      
      // move the file
      FilePath targetPath = targetDirPath.childPath(it->filename()) ;
      Error moveError = it->move(targetPath) ;
      if (moveError)
         return moveError ;
      
      // enque delete event if necessary
      if (fileInfo.isDirectory())
         enqueFileRemovedEvent(fileInfo);
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
  
   // create file info now before we remove
   FilePath sourcePath = module_context::resolveAliasedPath(path);
   FileInfo sourceFileInfo(sourcePath);
      
   // move the file
   FilePath destPath = module_context::resolveAliasedPath(targetPath) ;
   Error renameError = sourcePath.move(destPath);
   if (renameError)
      return renameError ;
                           
   // generate delete event for folders (inotify doesn't do this right now)
   if (sourceFileInfo.isDirectory())
      enqueFileRemovedEvent(sourceFileInfo);
   
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
   
   // compute path to file and validate that it exists
   int prefixLen = prefix.length();
   std::string relativePath = http::util::urlDecode(uri.substr(prefixLen));
   FilePath filePath = module_context::userHomePath().complete(relativePath);
   if (filePath.empty())
   {
      pResponse->setError(http::status::NotFound, 
                          request.uri() + " not found");
      return;
   }
   
   // return the file
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
   }
   pResponse->setHeader("Content-Disposition",
                        "attachment; filename=" + 
                        http::util::urlEncode(filename, true));
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
      std::string fileParam = "file" + boost::lexical_cast<std::string>(i);
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

} // anonymous namespace

Error initialize()
{
   // register suspend handler
   using namespace module_context;
   addSuspendHandler(SuspendHandler(onSuspend, onResume));
   
   // subscribe to events
   using boost::bind;
   events().onClientInit.connect(bind(onClientInit));
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onShutdown.connect(bind(onShutdown, _1));

   // install handlers
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "list_files", listFiles))
      (bind(registerRpcMethod, "create_file", createFile))
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

