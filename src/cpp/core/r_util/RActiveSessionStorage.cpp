/*
 * RActiveSessionStorage.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <boost/current_function.hpp>

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

FileActiveSessionStorage::FileActiveSessionStorage(const FilePath& scratchPath) :
   scratchPath_ (scratchPath)
{
   Error error = scratchPath_.ensureDirectory();
   if(error)
      LOG_ERROR(error);
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

Error FileActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{
   std::vector<FilePath> failedFiles;
   pValues->clear();
   for (const std::string& name : names)
   {
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
   propertyDir.getChildren(files);

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
      FilePath writePath = getPropertyFile(prop.first);
      Error error = core::writeStringToFile(writePath, prop.second);
      if (error)
         failedFiles.push_back(writePath);
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

Error FileActiveSessionStorage::isValid(bool* pValue)
{
   *pValue = scratchPath_.exists();
   return Success();
}

FilePath FileActiveSessionStorage::getPropertyDir() const
{
   FilePath propertiesDir = scratchPath_.completeChildPath(propertiesDirName_);
   propertiesDir.ensureDirectory();
   return propertiesDir;
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
   LOG_DEBUG_MESSAGE("Reading property " + name + " from server for session " + id_);

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
      error = Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unexpected type for result field in response when reading fields for session " + id_ + " owned by user " + user_.getUsername());
      error.addProperty("response", response.result().write());

      LOG_ERROR(error);
   if (error)
      return error;

   // No need to wait for the response here.
   return Success();
      return error;
   }


   return json::readObject(response.result().getObject(), name, *pValue);
}

Error RpcActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{   
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

   return Success();
}

Error RpcActiveSessionStorage::readProperties(std::map<std::string, std::string>* pValues)
{
   LOG_DEBUG_MESSAGE("Reading properties all properties from server for session " + id_);
   return readProperties({}, pValues);
}

Error RpcActiveSessionStorage::writeProperty(const std::string& name, const std::string& value)
{
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
}

Error RpcActiveSessionStorage::writeProperties(const std::map<std::string, std::string>& properties)
{
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
}

Error RpcActiveSessionStorage::destroy()
{
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

   // Deletion was successful on the server side.
   if (response.result().getBool())
      return scratchPath_.removeIfExists();

   error = Error(json::errc::ExecutionError, ERROR_LOCATION);
   error.addProperty(
      "descritpion",
      "Server was unable to destroy the session " + id_ + ".");
      
   LOG_ERROR(error);
   return error;
}

Error RpcActiveSessionStorage::isValid(bool* pValue)
{
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

   return Success();
}

} // namespace r_util
} // namespace core
} // namespace rstudio
