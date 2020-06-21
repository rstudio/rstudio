/*
 * SessionFiles.cpp
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


#include "SessionFiles.hpp"

#include <csignal>

#include <vector>
#include <iostream>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <gsl/gsl>

#include <boost/lexical_cast.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/FileUtils.hpp>
#include <core/Settings.hpp>
#include <core/Exec.hpp>
#include <core/DateTime.hpp>

#include <core/http/Util.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <shared_core/json/Json.hpp>

#include <core/system/ShellUtils.hpp>
#include <core/system/Process.hpp>
#include <core/system/RecycleBin.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>

#include <session/SessionClientEvent.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionSourceDatabase.hpp>

#include <session/projects/SessionProjects.hpp>

#include "SessionFilesQuotas.hpp"
#include "SessionFilesListingMonitor.hpp"
#include "SessionGit.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace modules { 
namespace files {

namespace {

// monitor for file listings
FilesListingMonitor s_filesListingMonitor;

// make sure that monitoring persists accross suspended sessions
const char * const kFilesMonitoredPath = "files.monitored-path";

void onSuspend(Settings* pSettings)
{
   // get monitored path and alias it
   std::string monitoredPath = s_filesListingMonitor.currentMonitoredPath().getAbsolutePath();
   if (!monitoredPath.empty())
   {
      monitoredPath = FilePath::createAliasedPath(FilePath(monitoredPath),
                                                  module_context::userHomePath());
   }

   // set it
   pSettings->set(kFilesMonitoredPath, monitoredPath);
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
      s_filesListingMonitor.start(resolvedPath, false, &jsonFiles);
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
   for(json::Array::Iterator
         it = files.begin();
         it != files.end();
         ++it)
   {
      if ((*it).getType() != json::Type::STRING)
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

      std::string file = (*it).getString();
      pFilePaths->push_back(module_context::resolveAliasedPath(file));
   }

   return Success();
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

core::Error isGitDirectory(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParams(request.params, &path);
   if (error)
      return error;

   FilePath targetPath = module_context::resolveAliasedPath(path);

   pResponse->setResult(git::isGitDirectory(targetPath));

   return Success();
}

core::Error isPackageDirectory(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParams(request.params, &path);
   if (error)
      return error;

   FilePath targetPath = module_context::resolveAliasedPath(path);

   pResponse->setResult(r_util::isPackageDirectory(targetPath));

   return Success();
}
                         
core::Error getFileContents(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   std::string path, encoding;
   Error error = json::readParams(request.params, &path, &encoding);
   if (error)
      return error;

   FilePath targetPath = module_context::resolveAliasedPath(path);
   if (!module_context::isPathViewAllowed(targetPath))
   {
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);
   }

   std::string contents;
   error = module_context::readAndDecodeFile(targetPath,
                                             encoding,
                                             true,
                                             &contents);
   if (error)
      return error;

   pResponse->setResult(contents);

   return Success();
}
   
Error listFiles(const json::JsonRpcRequest& request, json::JsonRpcResponse* pResponse)
{
   // get args
   std::string path;
   bool monitor;
   bool includeHidden;
   Error error = json::readParams(request.params, &path, &monitor, &includeHidden);
   if (error)
      return error;
   FilePath targetPath = module_context::resolveAliasedPath(path);

   json::Object result;
   
   // if this includes a request for monitoring
   core::json::Array jsonFiles;
   if (monitor)
   {
      // always stop existing if we have one
      s_filesListingMonitor.stop();

      // install a monitor only if we aren't already covered by the project monitor
      if (!session::projects::projectContext().isMonitoringDirectory(targetPath))
      {
         error = s_filesListingMonitor.start(targetPath, includeHidden, &jsonFiles);
         if (error)
            return error;
      }
      else
      {
         error = FilesListingMonitor::listFiles(targetPath, includeHidden, &jsonFiles);
         if (error)
            return error;
      }
   }
   else
   {
      error = FilesListingMonitor::listFiles(targetPath, includeHidden, &jsonFiles);
      if (error)
         return error;
   }

   result["files"] = jsonFiles;

   bool browseable = true;

#ifndef _WIN32
   // on *nix systems, see if browsing above this path is possible
   error = targetPath.getParent().isReadable(browseable);
   if (error && !core::isPathNotFoundError(error))
      LOG_ERROR(error);
#endif

   result["is_parent_browseable"] = browseable;

   pResponse->setResult(result);
   return Success();
}


// IN: String path
core::Error createFolder(const core::json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParam(request.params, 0, &path);
   if (error)
      return error;
   
   // create the directory
   FilePath folderPath = module_context::resolveAliasedPath(path);
   if (folderPath.exists())
   {
      return fileExistsError(ERROR_LOCATION);
   }
   else
   {
      Error createError = folderPath.ensureDirectory();
      if (createError)
         return createError;
   }

   return Success();
}


core::Error deleteFile(const FilePath& filePath)
{
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      Error error = core::system::recycle_bin::sendTo(filePath);
      if (error)
      {
         LOG_ERROR(error);
         return filePath.remove();
      }
      else
      {
         return Success();
      }
   }
   else
   {
      return filePath.remove();
   }
}

// IN: Array<String> paths
core::Error deleteFiles(const core::json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   json::Array files;
   Error error = json::readParam(request.params, 0, &files);
   if (error)
      return error;
   
   // extract vector of FilePath
   std::vector<FilePath> filePaths;
   Error extractError = extractFilePaths(files, &filePaths);
   if (extractError)
      return extractError;

   // delete each file
   Error deleteError;
   for (std::vector<FilePath>::const_iterator 
         it = filePaths.begin();
         it != filePaths.end();
         ++it)
   {    
      // attempt to send the file to the recycle bin
      deleteError = deleteFile(*it);
      if (deleteError)
         return deleteError;
   }

   return Success();
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
   Error copyError;
   if (sourceFilePath.isDirectory())
   {
      copyError = file_utils::copyDirectory(sourceFilePath, targetFilePath);
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
      return error;
   
   // extract vector of FilePath
   std::vector<FilePath> filePaths;
   Error extractError = extractFilePaths(files, &filePaths);
   if (extractError)
      return extractError;

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
      FilePath targetPath = targetDirPath.completeChildPath(it->getFilename());
      Error moveError = it->move(targetPath);
      if (moveError)
         return moveError;
   }

   return Success();
}

// IN: String path, String targetPath
core::Error renameFile(const core::json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // read params
   std::string path, targetPath;
   Error error = json::readParams(request.params, &path, &targetPath);
   if (error)
      return error;

   // detect case-only name changes
   bool isCaseOnlyChange =
         path != targetPath &&
         string_utils::toLower(path) == string_utils::toLower(targetPath);
   
   // if the destination already exists then send back file exists
   FilePath destPath = module_context::resolveAliasedPath(targetPath);
   if (!isCaseOnlyChange && destPath.exists())
      return fileExistsError(ERROR_LOCATION);
   
   // move the file
   FilePath sourcePath = module_context::resolveAliasedPath(path);
   Error renameError = sourcePath.move(destPath);
   if (renameError)
      return renameError;
   
   // propagate rename to source database (non fatal if this fails)
   error = source_database::rename(sourcePath, destPath);
   if (error)
      LOG_ERROR(error);

   return Success();
}

void handleFilesRequest(const http::Request& request, 
                        http::Response* pResponse)
{   
   Options& options = session::options();
   if (options.programMode() != kSessionProgramModeServer)
   {
      pResponse->setNotFoundError(request);
      return;
   }
   
   // get prefix and uri (strip query string)
   std::string prefix = "/files/";
   std::string uri = request.uri();
   std::size_t pos = uri.find("?");
   if (pos != std::string::npos)
      uri.erase(pos);
   
   // validate the uri
   if (prefix.length() >= uri.length() ||    // prefix longer than uri
       uri.find(prefix) != 0 ||              // uri doesn't start with prefix
       uri.find("..") != std::string::npos)  // uri has inavlid char sequence
   {
      pResponse->setNotFoundError(request);
      return;
   }
   
   // compute path to file
   int prefixLen = gsl::narrow_cast<int>(prefix.length());
   std::string relativePath = http::util::urlDecode(uri.substr(prefixLen));
   if (relativePath.empty())
   {
      pResponse->setNotFoundError(request);
      return;
   }

   // complete path to file
   FilePath filePath = module_context::userHomePath().completePath(relativePath);

   // no directory listing available
   if (filePath.isDirectory())
   {
      // if there is an index.html then serve that
      filePath = filePath.completeChildPath("index.html");
      if (!filePath.exists())
      {
         pResponse->setNotFoundError(request);
         return;
      }
   }

   pResponse->setNoCacheHeaders();
   pResponse->setFile(filePath, request);
}
   
const char * const kUploadFilename = "filename";
const char * const kUploadedTempFile = "uploadedTempFile";
const char * const kUploadTargetDirectory = "targetDirectory";

Error writeTmpData(const FilePath& tmpFile,
                   const char* buffer,
                   size_t beginOffset,
                   size_t endOffset)
{
   Error error = tmpFile.ensureFile();
   if (error)
      return error;

   std::shared_ptr<std::ostream> pOfs;
   error = tmpFile.openForWrite(pOfs, false);
   if (error)
      return error;

   pOfs->seekp(tmpFile.getSize());

   if (!pOfs->write(buffer + beginOffset, endOffset - beginOffset + 1))
   {
      return systemError(boost::system::errc::io_error,
                         "Could not write to destination file: " + tmpFile.getAbsolutePath(),
                         ERROR_LOCATION);
   }

   return Success();
}

Error completeUpload(const core::json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // read params
   json::Object token;
   bool commit;
   Error error = json::readParams(request.params, &token, &commit);
   if (error)
      return error;
   
   // parse fields out of token object
   std::string filename, uploadedTempFile, targetDirectory;
   error = json::readObject(token, 
                            kUploadFilename, filename,
                            kUploadedTempFile, uploadedTempFile,
                            kUploadTargetDirectory, targetDirectory);
   if (error)
      return error;
   
   // get path to temp file
   FilePath uploadedTempFilePath(uploadedTempFile);
   
   // commit or cancel
   if (commit)
   {
      FilePath targetDirectoryPath(targetDirectory);

      if (boost::ends_with(filename, "zip"))
      {
         // expand the archive
         r::exec::RFunction unzip("unzip");
         unzip.addParam("zipfile", uploadedTempFilePath.getAbsolutePath());
         unzip.addParam("exdir", targetDirectoryPath.getAbsolutePath());
         Error unzipError = unzip.call();
         if (unzipError)
         {
            error = uploadedTempFilePath.remove();
            if (error)
               LOG_ERROR(error);

            return unzipError;
         }
         
         // remove the __MACOSX folder if it exists
         const std::string kMacOSXFolder("__MACOSX");
         FilePath macOSXPath = targetDirectoryPath.completePath(kMacOSXFolder);
         Error removeError = macOSXPath.removeIfExists();
         if (removeError)
            LOG_ERROR(removeError);

         // remove the uploaded temp file
         error = uploadedTempFilePath.removeIfExists();
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         // calculate target path
         FilePath targetPath = targetDirectoryPath.completeChildPath(filename);
         
         // move the source to the destination, falling back to a copy
         // if the move cannot be completed
         Error copyError = uploadedTempFilePath.move(targetPath, FilePath::MoveCrossDevice, true);
         if (copyError)
            return copyError;
      }
      
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
   core::system::ProcessResult result;
   Error error = core::system::runCommand("unzip -Z1 " + uploadedZipFile.getAbsolutePath(),
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
      return error;

   if (result.exitStatus == 0)
   {
      std::vector<std::string> zipFileListing;
      boost::split(zipFileListing, result.stdOut, boost::is_any_of("\n"));

      // check for overwrites
      for (std::vector<std::string>::const_iterator
           it = zipFileListing.begin();
           it != zipFileListing.end();
           ++it)
      {
         // don't count empty lines
         if ((*it).empty()) continue;

         FilePath filePath = destDir.completePath(*it);
         if (filePath.exists())
            pOverwritesJson->push_back(module_context::createFileSystemItem(filePath));
      }
   }
   else
   {
      return systemError(result.exitStatus,
                         "Unexpected result for unzip command",
                         ERROR_LOCATION);
   }
   
   return Success();
}

bool validateUploadedFile(uintmax_t fileSize, http::Response* pResponse)
{
   // get limit
   size_t mbLimit = session::options().limitFileUploadSizeMb();

   // don't enforce if no limit specified
   if (mbLimit <= 0)
      return true;

   // convert limit to bytes
   uintmax_t byteLimit = mbLimit * 1024 * 1024;

   // compare to file size
   if (fileSize > byteLimit)
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

bool validateUploadedFile(const http::Request& request, http::Response* pResponse)
{
   // because we don't have the actual file yet (as it is being streamed)
   // we do not yet know how big it truly is - this heuristic checks the total
   // content length (which includes form metadata) against the limit and subtracts
   // a tolerance of 10k, which should be more than enough to account for the
   // metadata overhead. a final check will be done once the file is fully received
   // and written to disk
   constexpr uintmax_t tolerance = 10 * 1024;
   if (request.contentLength() < tolerance)
      return true;
   return validateUploadedFile(request.contentLength() - tolerance, pResponse);
}

bool validateUploadedFile(const FilePath& file, http::Response* pResponse)
{
   return validateUploadedFile(file.getSize(), pResponse);
}

struct UploadState
{
   UploadState() :
      totalWritten(0),
      fileStartPos(0),
      fileEndPos(0)
   {
   }

   uintmax_t totalWritten;
   size_t fileStartPos;
   size_t fileEndPos;
   std::string fileName;
   std::string targetDirectory;
   FilePath tmpFile;
};

boost::mutex s_uploadMutex;
std::map<const http::Request*, boost::shared_ptr<UploadState>> s_uploadStateMap;

void parseContentBuffer(const std::string& buffer,
                        const boost::shared_ptr<UploadState>& pUploadState)
{
   std::string nameRegex("form-data; name=\"(.*)\"");
   boost::smatch nameMatch;
   if (regex_utils::search(buffer, nameMatch, boost::regex(nameRegex)))
   {
      std::string filenameRegex(nameRegex + "; filename=\"");
      boost::smatch fileMatch;
      if (regex_utils::search(buffer, fileMatch, boost::regex(filenameRegex)))
      {
         std::string searchStr = "filename=\"";
         size_t pos = buffer.find(searchStr) + searchStr.size();
         size_t endPos = buffer.find("\"", pos);
         pUploadState->fileName = buffer.substr(pos, endPos - pos);
      }
      else
      {
         if (nameMatch[1] == "targetDirectory")
         {
            // skip over to and record the value
            size_t pos = buffer.find("targetDirectory");
            size_t endPos = buffer.find("\r\n\r\n", pos);

            size_t valueStartPos = endPos + 4;
            size_t valueEndPos = buffer.find("\r\n", valueStartPos);

            pUploadState->targetDirectory = buffer.substr(valueStartPos, valueEndPos - valueStartPos);
         }
      }
   }
}

std::string getBoundary(const http::Request& request)
{
   std::string boundaryPrefix("boundary=");
   std::string boundary;
   size_t prefixLoc = request.contentType().find(boundaryPrefix);
   if (prefixLoc != std::string::npos)
   {
      boundary = request.contentType().substr(prefixLoc + boundaryPrefix.size(),
                                              std::string::npos);
      boundary = "--" + boundary;
      boost::algorithm::trim(boundary);
   }

   return boundary;
}

// note: this function is invoked on the thread pool and is not handled in an R context
// therefore, no R methods may be invoked within this function!!
bool handleFileUploadRequestAsync(const http::Request& request,
                                  const std::string& formData,
                                  bool complete,
                                  const http::UriHandlerFunctionContinuation& cont)
{
   const http::Request* pRequest = &request;
   http::Response response;

   // get upload state
   boost::shared_ptr<UploadState> pUploadState;
   LOCK_MUTEX(s_uploadMutex)
   {
      auto iter = s_uploadStateMap.find(pRequest);
      if (iter == s_uploadStateMap.end())
      {
         // no state exists for this request

         // preliminary file size validation - a final check will be performed
         // once the file is actually written to disk and we know for sure how big it is
         http::Response response;
         if (!validateUploadedFile(request, &response))
         {
            cont(&response);
            return false;
         }

         // create new upload state
         pUploadState = boost::make_shared<UploadState>();
         s_uploadStateMap[pRequest] = pUploadState;
      }
      else
      {
         pUploadState = iter->second;
      }
   }
   END_LOCK_MUTEX

   auto cleanupState = [=]()
   {
      LOCK_MUTEX(s_uploadMutex)
      {
         s_uploadStateMap.erase(pRequest);
      }
      END_LOCK_MUTEX
   };

   auto writeError = [&](const Error& error)
   {
      LOG_ERROR(error);
      json::setJsonRpcError(error, &response);
      cleanupState();
      cont(&response);
   };

   auto writeParamError = [&]()
   {
      json::setJsonRpcError(Error(json::errc::ParamInvalid, ERROR_LOCATION), &response);
      cleanupState();
      cont(&response);
   };

   if (pUploadState->tmpFile.isEmpty())
   {
      // create a temporary file to store the form's file data
      // we store this temporary file under the user's home directory to increase the odds
      // that we can perform a fast move on the tmp file when the user confirms, as most
      // uploads are within the user's home directory
      FilePath tmpDir = module_context::userUploadedFilesScratchPath();
      Error error = tmpDir.ensureDirectory();
      if (error)
      {
         writeError(error);
         return false;
      }

      error = FilePath::uniqueFilePath(tmpDir.getAbsolutePath(), ".bin", pUploadState->tmpFile);
      if (error)
      {
         writeError(error);
         return false;
      }
   }

   if (pUploadState->totalWritten == 0)
   {
      // check the first set of headers within the first chunk
      // because of the buffering done upstream, we are guaranteed
      // to have all of the necessary data within this chunk

      // skip to the end of the first form header
      size_t pos = formData.find("\r\n\r\n");
      if (pos == std::string::npos)
      {
         writeError(systemError(boost::system::errc::protocol_error,
                                "Invalid form data received - first end of header not found",
                                ERROR_LOCATION));
         return false;
      }

      // determine which form field we just read, file data or target directory
      parseContentBuffer(formData, pUploadState);

      if (pUploadState->targetDirectory.empty())
      {
         // we just read to the start of the file
         pUploadState->fileStartPos = pos + 4;
      }
      else
      {
         // we just read the target directory
         // skip over to the next headers and parse the filename
         size_t posEnd = formData.find("\r\n\r\n", pos + 4);
         if (posEnd == std::string::npos)
         {
            writeError(systemError(boost::system::errc::protocol_error,
                                   "Invalid form data received - second end of header not found",
                                   ERROR_LOCATION));
            return false;
         }

         std::string headers = formData.substr(pos, posEnd - pos);
         parseContentBuffer(headers, pUploadState);

         if (pUploadState->fileName.empty())
         {
            // didn't find the file name - return an error
            writeParamError();
            return false;
         }

         pUploadState->fileStartPos += posEnd + 4;
      }
   }

   if (complete)
   {
      if (pUploadState->targetDirectory.empty())
      {
         // now we need to read the target directory metadata field by reading backwords from the buffer
         std::string searchStr = "\r\n" + getBoundary(request) + "\r\n";
         size_t pos = formData.rfind(searchStr);
         if (pos == std::string::npos)
         {
            pUploadState->tmpFile.removeIfExists();
            writeError(systemError(boost::system::errc::protocol_error,
                                   "Invalid form data received - final end of header not found",
                                   ERROR_LOCATION));
            return false;
         }

         std::string headers = formData.substr(pos + searchStr.size());
         pUploadState->fileEndPos = pos - 1;

         // read the target directory field
         parseContentBuffer(headers, pUploadState);

         if (pUploadState->targetDirectory.empty())
         {
            // didn't find target directory - return an error
            pUploadState->tmpFile.removeIfExists();
            writeParamError();
            return false;
         }
      }
      else
      {
         // read to the end of the file portion by reading to the final boundary start
         std::string searchStr = "\r\n" + getBoundary(request) + "--\r\n";
         size_t pos = formData.rfind(searchStr);
         if (pos == std::string::npos)
         {
            pUploadState->tmpFile.removeIfExists();
            writeError(systemError(boost::system::errc::protocol_error,
                                   "Invalid form data received - final end of form not found",
                                   ERROR_LOCATION));
            return false;
         }

         pUploadState->fileEndPos = pos - 1;
      }

      // write the last chunk of file data
      Error saveError = writeTmpData(pUploadState->tmpFile,
                                     formData.c_str(),
                                     pUploadState->fileStartPos,
                                     pUploadState->fileEndPos);
      if (saveError)
      {
         pUploadState->tmpFile.removeIfExists();
         writeError(saveError);
         return false;
      }

      pUploadState->totalWritten += pUploadState->fileEndPos - pUploadState->fileStartPos + 1;
   }
   else
   {
      // write a chunk of data
      Error saveError = writeTmpData(pUploadState->tmpFile,
                                     formData.c_str(),
                                     pUploadState->fileStartPos,
                                     formData.size() - 1);
      if (saveError)
      {
         pUploadState->tmpFile.removeIfExists();
         writeError(saveError);
         return false;
      }

      pUploadState->totalWritten += formData.size() - pUploadState->fileStartPos;
      pUploadState->fileStartPos = 0;
      return true;
   }

   // the full file has been written - validate it
   if (!validateUploadedFile(pUploadState->tmpFile, &response))
   {
      // user cannot upload files this large - delete the temp file
      Error error = pUploadState->tmpFile.removeIfExists();
      if (error)
         LOG_ERROR(error);

      cleanupState();
      cont(&response);
      return false;
   }

   // detect any potential overwrites
   bool isZip = boost::ends_with(pUploadState->fileName, "zip");
   FilePath destDir = module_context::resolveAliasedPath(pUploadState->targetDirectory);
   FilePath destPath = destDir.completeChildPath(pUploadState->fileName);

   json::Array overwritesJson;
   if (isZip)
   {
      Error error = detectZipFileOverwrites(pUploadState->tmpFile, destDir, &overwritesJson);
      if (error)
      {
         writeError(error);
         return false;
      }
   }
   else
   {
      if (destPath.exists())
         overwritesJson.push_back(module_context::createFileSystemItem(destPath));
   }

   // set the upload information as the result
   json::Object uploadTokenJson;
   uploadTokenJson[kUploadFilename] = pUploadState->fileName;
   uploadTokenJson[kUploadedTempFile] = pUploadState->tmpFile.getAbsolutePath();
   uploadTokenJson[kUploadTargetDirectory] = destDir.getAbsolutePath();

   json::Object uploadJson;
   uploadJson["token"] = uploadTokenJson;
   uploadJson["overwrites"] = overwritesJson;
   json::setJsonRpcResult(uploadJson, &response);

   // response content type must always be text/html to be handled
   // properly by the browser/gwt on the client side
   response.setContentType("text/html");

   cont(&response);
   cleanupState();

   return true;
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
   pResponse->setStreamFile(attachmentPath, request);
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
      FilePath filePath = parentPath.completePath(file);
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
                                    tempZipFilePath.getAbsolutePath(),
                                    parentPath.getAbsolutePath(),
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
         pResponse->setNotFoundError(request);
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
      if (filePath.isEmpty())
         throw r::exec::RErrorException("invalid path: " + path);

      // create path info vector (use json repsesentation to force convertion
      // to VECSXP rather than STRSXP)
      json::Object pathInfo;
      pathInfo["path"] = filePath.getAbsolutePath();
      std::string parent = filePath.getAbsolutePath();
      FilePath parentPath = filePath.getParent();
      if (!parentPath.isEmpty())
         parent = parentPath.getAbsolutePath();
      pathInfo["directory"] = parent;
      pathInfo["name"] = filePath.getFilename();
      pathInfo["stem"] = filePath.getStem();
      pathInfo["extension"] = filePath.getExtension();

      // return it
      r::sexp::Protect rProtect;
      return r::sexp::create(pathInfo, &rProtect);
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

SEXP rs_readLines(SEXP filePathSEXP)
{
   FilePath filePath(r::sexp::asString(filePathSEXP));
   if (!filePath.exists())
   {
      LOG_ERROR(core::fileNotFoundError(ERROR_LOCATION));
      return R_NilValue;
   }
   
   std::string contents;
   Error error = core::readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   
   r::sexp::Protect protect;
   if (contents.empty())
      return r::sexp::create(contents, &protect);
   
   std::vector<std::string> splat = core::algorithm::split(contents, "\n");
   if (splat.size() && splat[splat.size() - 1].empty())
      splat.pop_back();
   
   for (std::size_t i = 0, n = splat.size(); i < n; ++i)
   {
      std::string& rElement = splat[i];
      if (rElement.size() && rElement[rElement.size() - 1] == '\r')
         rElement.erase(rElement.size() - 1);
   }
   
   return r::sexp::create(splat, &protect);
}

} // anonymous namespace

bool isMonitoringDirectory(const FilePath& directory)
{
   FilePath monitoredPath = s_filesListingMonitor.currentMonitoredPath();
   return !monitoredPath.isEmpty() && (directory == monitoredPath);
}

Error initialize()
{
   // register suspend handler
   using boost::bind;
   using namespace module_context;
   addSuspendHandler(SuspendHandler(bind(onSuspend, _2), onResume));
   
   // subscribe to events
   events().onClientInit.connect(bind(onClientInit));

   RS_REGISTER_CALL_METHOD(rs_readLines, 1);
   RS_REGISTER_CALL_METHOD(rs_pathInfo, 1);

   // install handlers
   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "stat", stat))
      (bind(registerRpcMethod, "is_text_file", isTextFile))
      (bind(registerRpcMethod, "is_git_directory", isGitDirectory))
      (bind(registerRpcMethod, "is_package_directory", isPackageDirectory))
      (bind(registerRpcMethod, "get_file_contents", getFileContents))
      (bind(registerRpcMethod, "list_files", listFiles))
      (bind(registerRpcMethod, "create_folder", createFolder))
      (bind(registerRpcMethod, "delete_files", deleteFiles))
      (bind(registerRpcMethod, "copy_file", copyFile))
      (bind(registerRpcMethod, "move_files", moveFiles))
      (bind(registerRpcMethod, "rename_file", renameFile))
      (bind(registerUriHandler, "/files", handleFilesRequest))
      (bind(registerUploadHandler, "/upload", handleFileUploadRequestAsync))
      (bind(registerUriHandler, "/export", handleFileExportRequest))
      (bind(registerRpcMethod, "complete_upload", completeUpload))
      (bind(sourceModuleRFile, "SessionFiles.R"))
      (bind(quotas::initialize));
   return initBlock.execute();
}


} // namespace files
} // namespace modules
} // namespace session
} // namespace rstudio

