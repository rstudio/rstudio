/*
 * RActiveSessionStorage.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <boost/current_function.hpp>
#include <boost/thread/thread.hpp>

#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <core/r_util/RActiveSessionStorage.hpp>
#include <core/system/Xdg.hpp>

namespace rstudio {
namespace core {
namespace r_util {

namespace {
Error createError(const std::string& errorName, const std::string& preamble, 
   const std::vector<FilePath>& files, const ErrorLocation& errorLocation)
{
   std::string errorMessage = preamble + "[ ";
   auto iter = files.begin();
   while(iter != files.end())
   {
      errorMessage += "'" + iter->getAbsolutePath() + "'";
      iter++;
      if(iter != files.end())
         errorMessage += ", ";
   }
   errorMessage += " ]";
   return Error(errorName, 1, errorMessage, errorLocation);
}
} // anonymous namespace

FileActiveSessionStorage::FileActiveSessionStorage(const FilePath& scratchPath, const FilePathToProjectId& projectToIdFunction) :
   scratchPath_ (scratchPath),
   projectToIdFunction_ (projectToIdFunction)
{
   // Do not create the directory here or else any attempt to look for a session ends up creating it. The API is designed
   // to return an empty object if it does not exist.
   //Error error = scratchPath_.ensureDirectory();
   //if(error)
   //   LOG_ERROR(error);
}

const std::map<std::string, std::string> FileActiveSessionStorage::fileNames =
{
   { "last_used" , "last-used" },
   { "r_version" , "r-version" },
   { "r_version_label" , "r-version-label" },
   { "r_version_home" , "r-version-home" },
   { "working_directory" , "working-dir" },
   { "launch_parameters" , "launch-parameters" }
};

Error FileActiveSessionStorage::readProperty(const std::string& name, std::string* pValue)
{
   std::map<std::string, std::string> propertyValue{};
   *pValue = "";

   Error error = readProperties({ name }, &propertyValue);

   if (error)
      return error;

   std::map<std::string, std::string>::const_iterator iter = propertyValue.find(name);

   if(iter != propertyValue.end())
      *pValue = iter->second;
   
   return Success();
}

uintmax_t computeSuspendSizeImpl(const FilePath& scratchPath)
{
   FilePath suspendPath = scratchPath.completePath("suspended-session-data");
   if (!suspendPath.exists())
      return 0;

   return suspendPath.getSizeRecursive();
}

uintmax_t FileActiveSessionStorage::computeSuspendSize()
{
   return computeSuspendSizeImpl(scratchPath_);
}

Error FileActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{
   std::vector<FilePath> failedFiles;
   pValues->clear();
   for (const std::string& name : names)
   {
      // Convert project filename to projectId since we don't store project-id as a file in properites
      if (name == ActiveSession::kProjectId)
      {
         FilePath projPathFile = getPropertyFile(ActiveSession::kProject);
         std::string projPath;
         Error error = core::readStringFromFile(projPathFile, &projPath);
         if (error)
            failedFiles.push_back(projPathFile);
         boost::algorithm::trim(projPath);
         if (!projectToIdFunction_.empty())
         {
            ProjectId projId = projectToIdFunction_(projPath);
            pValues->insert(std::pair<std::string, std::string>{name,  projId.asString()});
         }
         else
            LOG_DEBUG_MESSAGE("Unable to read projectId from active session");
         continue;
      }

      // This is a computed property, though maybe we should just compute it after it suspend and save it to avoid this special case?
      if (name == ActiveSession::kSuspendSize)
      {
         pValues->insert(std::pair<std::string, std::string>{name, std::to_string(computeSuspendSize())});
         continue;
      }

      FilePath readPath = getPropertyFile(name);
      std::string value = "";

      if (readPath.exists())
      {
         Error error = core::readStringFromFile(readPath, &value);
         if (error)
            failedFiles.push_back(readPath);
         boost::algorithm::trim(value);
      }
      pValues->insert(std::pair<std::string, std::string>{name, value});
   }

   if(failedFiles.empty())
      return Success();
   else
      return createError("UnableToReadFiles", "Failed to read from the following files ", 
         failedFiles, ERROR_LOCATION);
}

Error FileActiveSessionStorage::readProperties(std::map<std::string, std::string>* pValues)
{

   FilePath propertyDir = getPropertyDir();
   std::vector<FilePath> files{};
   std::vector<FilePath> failedFiles{};
   pValues->clear();
   Error error = propertyDir.getChildren(files);
   if (isNotFoundError(error))
      return Success();

   for(FilePath file : files) {
      std::string value = "";
      Error error = core::readStringFromFile(file, &value);

      if(error)
         failedFiles.push_back(file);

      std::string propertyName = getFileNameProperty(file.getFilename());
      pValues->insert(std::pair<std::string, std::string>{propertyName, value});
   }

   if(!failedFiles.empty())
      return createError("UnableToReadFiles", "Failed to read from the following files ",
         failedFiles, ERROR_LOCATION);
      
   return Success();
}

Error FileActiveSessionStorage::writeProperty(const std::string& name, const std::string& value)
{
   std::map<std::string, std::string> property = {{name, value}};
   return writeProperties(property);
}

Error FileActiveSessionStorage::writeProperties(const std::map<std::string, std::string>& properties)
{
   std::vector<FilePath> failedFiles{};
   for (auto&& prop : properties)
   {
      if (prop.first == ActiveSession::kSuspendSize)
         continue; // Suspend-size is computed, not saved for file storage so ignore this in the off chance we get here
      FilePath writePath = getPropertyFile(prop.first);
      Error error = core::writeStringToFile(writePath, prop.second, string_utils::LineEndingPassthrough, true, 0, false);

      if (error)
      {
         if (error.getCode() == boost::system::errc::no_such_file_or_directory)
         {
            ensurePropertyDir();
            error = core::writeStringToFile(writePath, prop.second);
         }
         if (error)
            failedFiles.push_back(writePath);
      }
   }
   
   if (failedFiles.empty())
      return Success();
   else
      return createError("UnableToWriteFiles", "Failed to write to the following files ", 
         failedFiles, ERROR_LOCATION);
}

Error FileActiveSessionStorage::destroy()
{
   return scratchPath_.removeIfExists();
}

Error FileActiveSessionStorage::clearScratchPath()
{
   return scratchPath_.removeIfExists();
}

Error FileActiveSessionStorage::isEmpty(bool* pIsEmpty)
{
   *pIsEmpty = !scratchPath_.exists();
   return Success();
}

Error FileActiveSessionStorage::isValid(bool* pValue)
{
   *pValue = false;

   if (!scratchPath_.exists())
      return Success();

   // Check editor property - non-R sessions (VS Code, Positron) don't need project validation
   std::string editorVal;
   Error error = readProperty(ActiveSession::kEditor, &editorVal);
   if (error)
      return Success(); // Can't read properties, not valid

   bool isRSession = editorVal == kWorkbenchRStudio || editorVal.empty();
   if (!isRSession)
   {
      *pValue = true;
      return Success();
   }

   // R session: ensure project property is present and non-empty.
   // Retry logic handles NFS race conditions where the file may be briefly empty during writes.
   std::string projectVal;
   error = readProperty(ActiveSession::kProject, &projectVal);
   if (error)
      return Success(); // Can't read, not valid

   if (!projectVal.empty())
   {
      *pValue = true;
      return Success();
   }

   // Project is empty — check if property file exists and retry
   FilePath projectFile = getPropertyFile(ActiveSession::kProject);
   if (projectFile.exists())
   {
      int retryCount = 3;
      do {
         boost::this_thread::sleep(boost::posix_time::milliseconds(50));
         error = readProperty(ActiveSession::kProject, &projectVal);
         if (!error && !projectVal.empty())
         {
            *pValue = true;
            return Success();
         }
      } while (--retryCount > 0);
   }

   return Success();
}

FilePath FileActiveSessionStorage::getPropertyDir() const
{
   FilePath propertiesDir = scratchPath_.completeChildPath(propertiesDirName_);
   return propertiesDir;
}

Error FileActiveSessionStorage::ensurePropertyDir() const
{
   Error error = scratchPath_.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   FilePath propertiesDir = getPropertyDir();
   return propertiesDir.ensureDirectory();
}

FilePath FileActiveSessionStorage::getPropertyFile(const std::string& name) const
{
   FilePath propertiesDir = getPropertyDir();
   const std::string& fileName = getPropertyFileName(name);
   return propertiesDir.completeChildPath(fileName);
}


RpcActiveSessionStorage::RpcActiveSessionStorage(const system::User& user, const std::string& sessionId, const FilePath& scratchPath, const InvokeRpc& invokeRpcFunc) :
   user_(user),
   id_(std::move(sessionId)),
   scratchPath_(scratchPath),
   invokeRpcFunc_(invokeRpcFunc)
{
}

Error RpcActiveSessionStorage::readProperty(const std::string& name, std::string* pValue)
{
#ifdef _WIN32
   return Success();
#else
   json::Array fields;
   fields.push_back(name);
   
   json::Object body;
   body[kSessionStorageUserIdField] = user_.getUserId();
   body[kSessionStorageIdField] = id_;
   body[kSessionStorageFieldsField] = fields;
   body[kSessionStorageOperationField] = kSessionStorageReadOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::JsonRpcResponse response;
   Error error = invokeRpcFunc_(request, &response);
   if (error)
      return error;

   if (!response.result().isObject())
   {
      if (response.error().isObject())
      {
        // The JSON rpc returned an explicit error code so return that directly to the caller
        int code;
        error = json::readObject(response.error().getObject(), "code", code);
        if (!error)
        {
           std::string errorMessage;
           json::readObject(response.error().getObject(), "message", errorMessage);
           return Error("Read property response", code, errorMessage, ERROR_LOCATION);
        }
      }
      error = Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unexpected type for result field in response when reading fields for session " + id_ + " owned by user " + user_.getUsername());
      error.addProperty("response", response.result().write());

      LOG_ERROR(error);
      return error;
   }

   error = json::readObject(response.result().getObject(), name, *pValue);
   if (error)
      LOG_DEBUG_MESSAGE("Error reading session metadata property " + name + " from server for session " + id_ + " error: " + error.asString());
   else
      LOG_DEBUG_MESSAGE("Read session metadata property " + name + " from server for session " + id_ + " value: " + *pValue);
   return error;
#endif
}

Error RpcActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{   
#ifndef _WIN32
   if (!names.empty())
      LOG_DEBUG_MESSAGE("Reading properties { " + boost::join(names, ", ") + " } from server for session " + id_);
      
   json::Object body;
   body[kSessionStorageUserIdField] = user_.getUserId();
   body[kSessionStorageIdField] = id_;
   body[kSessionStorageFieldsField] = json::toJsonArray(names);
   body[kSessionStorageOperationField] = kSessionStorageReadOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::JsonRpcResponse response;
   Error error = invokeRpcFunc_(request, &response);
   if (error)
      return error;

   json::Object resultObj;
   if (!response.result().isObject())
   {
      error = Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unexpected type for result field in response when reading fields for session " + id_ + " owned by user " + user_.getUsername());
      error.addProperty("response", response.result().write());

      LOG_ERROR(error);
      return error;
   }

   if (!names.empty())
      LOG_DEBUG_MESSAGE("Response - properties read from server for session " + id_ + ": " + response.result().writeFormatted());

   resultObj = response.result().getObject();

   for (const auto& name: names)
   {
      std::string value;
      error = json::readObject(resultObj, name, value);
      if (error)
         LOG_ERROR(error);
      else
         (*pValues)[name] = value;
   }

   if (names.empty())
   {
      if (!response.result().isObject())
      {
         error = Error(json::errc::ParseError, ERROR_LOCATION);
         error.addProperty(
            "description",
            "Unable to parse the response from the server when reading the fields for session " + id_ + " owned by user " + user_.getUsername());
         error.addProperty("response", response.result().write());

         if (!names.empty())
            error.addProperty("fields", boost::algorithm::join(names, ", "));

         LOG_ERROR(error);
         return error;
      }

      json::Object resultObj = response.result().getObject();
      for (auto itr = resultObj.begin(); itr != resultObj.end(); ++itr)
         if ((*itr).getValue().isString())
            (*pValues)[(*itr).getName()] = (*itr).getValue().getString();
   }

#endif
   return Success();
}

Error RpcActiveSessionStorage::readProperties(std::map<std::string, std::string>* pValues)
{
   LOG_DEBUG_MESSAGE("Reading properties all properties from server for session " + id_);
   return readProperties({}, pValues);
}

uintmax_t RpcActiveSessionStorage::computeSuspendSize()
{
   return computeSuspendSizeImpl(scratchPath_);
}

Error RpcActiveSessionStorage::writeProperty(const std::string& name, const std::string& value)
{
#ifdef _WIN32
   return Success();
#else
   LOG_DEBUG_MESSAGE("Writing property " + name + " with value " + value + " from server for session " + id_);
   json::Object fields;
   fields[name] = value;

   json::Object body;
   body[kSessionStorageUserIdField] = user_.getUserId();
   body[kSessionStorageIdField] = id_;
   body[kSessionStorageFieldsField] = fields;
   body[kSessionStorageOperationField] = kSessionStorageWriteOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::JsonRpcResponse response;
   return invokeRpcFunc_(request, &response);
#endif
}

Error RpcActiveSessionStorage::writeProperties(const std::map<std::string, std::string>& properties)
{
#ifdef _WIN32
   return Success();
#else
   std::string strProps;
   #undef DEBUG
   if (log::isLogLevel(log::LogLevel::DEBUG))
   {
      for (auto itr = properties.begin(); itr != properties.end(); ++itr)
      {
         if (!strProps.empty())
            strProps.append(", ");

         strProps.append(itr->first)
            .append(" = ")
            .append(itr->second);
      }
   }

   LOG_DEBUG_MESSAGE("Writing properties { " + strProps + " } from server for session " + id_);
   json::Object body;
   body[kSessionStorageUserIdField] = user_.getUserId();
   body[kSessionStorageIdField] = id_;
   body[kSessionStorageFieldsField] = json::toJsonValue(properties);
   body[kSessionStorageOperationField] = kSessionStorageWriteOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::JsonRpcResponse response;
   return invokeRpcFunc_(request, &response);
#endif
}

Error RpcActiveSessionStorage::destroy()
{
#ifdef _WIN32
   return Success();
#else
   json::Object body;
   body[kSessionStorageUserIdField] = user_.getUserId();
   body[kSessionStorageIdField] = id_;
   body[kSessionStorageOperationField] = kSessionStorageDeleteOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::JsonRpcResponse response;
   Error error = invokeRpcFunc_(request, &response);
   if (error)
      return error;

   if (!response.result().isBool())
   {
      error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unexpected response from the server when validating session " + id_ + " owned by user " + user_.getUsername());
      error.addProperty("response", response.result().write());

      LOG_ERROR(error);
      return error;
   }

   // Deletion was successful on the server side - just in case we are in the session process, and are doing a migration, we should remove the
   // file path for the session here too.
   if (response.result().getBool() && !scratchPath_.isEmpty())
      return scratchPath_.removeIfExists();

   error = Error(json::errc::ExecutionError, ERROR_LOCATION);
   error.addProperty(
      "description",
      "Server was unable to destroy the session " + id_ + ".");
      
   LOG_ERROR(error);
   return error;
#endif
}

Error RpcActiveSessionStorage::clearScratchPath()
{
   if (!scratchPath_.isEmpty())
      return scratchPath_.removeIfExists();
   return Success();
}

Error RpcActiveSessionStorage::isEmpty(bool* pIsEmpty)
{
   // isValid returns true when the session exists and is valid,
   // but isEmpty should return true when the session does NOT exist.
   bool isValid = false;
   Error error = this->isValid(&isValid);
   if (error)
      return error;
   *pIsEmpty = !isValid;
   return Success();
}

Error RpcActiveSessionStorage::isValid(bool* pValue)
{
#ifndef _WIN32
   LOG_DEBUG_MESSAGE("Checking whether session is valid for id: " + id_);
   
   json::Object body;
   body[kSessionStorageUserIdField] = user_.getUserId();
   body[kSessionStorageIdField] = id_;
   body[kSessionStorageOperationField] = kSessionStorageValidateOp;
   
   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::JsonRpcResponse response;
   Error error = invokeRpcFunc_(request, &response);
   if (error)
      return error;

   if (!response.result().isBool())
   {
      error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unexpected response from the server when validating session " + id_ + " owned by user " + user_.getUsername());
      error.addProperty("response", response.result().write());

      LOG_ERROR(error);
      return error;
   }

   *pValue = response.result().getBool();
#endif

   return Success();
}

} // namespace r_util
} // namespace core
} // namespace rstudio
