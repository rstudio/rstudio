/*
 * RActiveSessionStorage.hpp
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

#ifndef CORE_R_UTIL_R_ACTIVE_SESSION_STORAGE
#define CORE_R_UTIL_R_ACTIVE_SESSION_STORAGE

#include <set>

#include <core/json/JsonRpc.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <shared_core/FilePath.hpp>


namespace rstudio {
namespace core {
namespace r_util {
typedef std::function<Error(const json::JsonRpcRequest& request, json::JsonRpcResponse* pResponse)> InvokeRpc;

class IActiveSessionStorage 
{
public:
   Error virtual readProperty(const std::string& name, std::string* pValue) = 0;
   Error virtual readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues) = 0;
   Error virtual readProperties(std::map<std::string, std::string>* pValues) = 0;
   Error virtual writeProperty(const std::string& name, const std::string& value) = 0;
   Error virtual writeProperties(const std::map<std::string, std::string>& properties) = 0;
   Error virtual destroy() = 0;
   Error virtual clearScratchPath() = 0;

   // Returns true if the session storage exists (e.g. scratch path exists, or row in DB).
   // Does NOT check whether the session metadata is complete/valid.
   Error virtual isEmpty(bool* pIsEmpty) = 0;

   // Returns true if the session is valid — storage exists AND has required metadata
   // (e.g. for R sessions: editor and project are present). Implementations that cannot
   // check metadata (like RPC) may delegate to the server-side validate.
   // Default: delegates to isEmpty for backward compatibility.
   Error virtual isValid(bool* pValue)
   {
      bool isEmptyVal = true;
      Error error = isEmpty(&isEmptyVal);
      if (error)
         return error;
      *pValue = !isEmptyVal;
      return Success();
   }

   // Implemented for File and Rpc storage to look at scratchPath suspended-data for size
   uintmax_t virtual computeSuspendSize() { return 0; }

protected:
   virtual ~IActiveSessionStorage() = default;
   IActiveSessionStorage() = default;
};

class FileActiveSessionStorage : public IActiveSessionStorage
{
public:
   explicit FileActiveSessionStorage(const FilePath& location, const core::r_util::FilePathToProjectId& projectToIdFunction);
   ~FileActiveSessionStorage() override = default;
   Error readProperty(const std::string& name, std::string* pValue) override;   
   Error readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues) override;
   Error readProperties(std::map<std::string, std::string>* pValues) override;
   Error writeProperty(const std::string& name, const std::string& value) override;
   Error writeProperties(const std::map<std::string, std::string>& properties) override;
   Error destroy() override;
   Error clearScratchPath() override;
   Error isEmpty(bool* pIsEmpty) override;
   Error isValid(bool* pValue) override;

   uintmax_t computeSuspendSize() override;

private:
   // Scratch Path Example : ~/.local/share/rstudio/sessions/active/session-6d0bdd18
   // This contains the properties directory, as well as susspended session data, session-persistence-state etc
   // Baked into this path is the session id

   // Properties Path Example : ~/.local/share/rstudio/sessions/active/session-6d0bdd18/properites
   FilePath scratchPath_;
   const core::r_util::FilePathToProjectId projectToIdFunction_;

   const std::string propertiesDirName_ = "properites";

   Error ensurePropertyDir() const;

   FilePath getPropertyDir() const;

   FilePath getPropertyFile(const std::string& name) const;
   
   static const std::map<std::string, std::string> fileNames;
   
   static const std::string& getPropertyFileName(const std::string& propertyName)
   {
      if (fileNames.find(propertyName) != fileNames.end())
         return fileNames.at(propertyName);

      return propertyName;
   }

   static const std::string& getFileNameProperty(const std::string& fileName)
   {

      for(auto iter = fileNames.begin(); iter != fileNames.end(); iter++)
      {
         if(iter->second == fileName)
         {
            return iter->first;
         }
      }

      return fileName;
   }
};

class RpcActiveSessionStorage : public core::r_util::IActiveSessionStorage 
{
   public:
      explicit RpcActiveSessionStorage(const core::system::User& user, const std::string& sessionId, const FilePath& scratchPath, const InvokeRpc& invokeRpcFunc);
      ~RpcActiveSessionStorage() override = default;
      
      core::Error readProperty(const std::string& name, std::string* pValue) override;
      core::Error readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues) override;
      core::Error readProperties(std::map<std::string, std::string>* pValues) override;
      core::Error writeProperty(const std::string& name, const std::string& value) override;
      core::Error writeProperties(const std::map<std::string, std::string>& properties) override;

      core::Error destroy() override;
      Error clearScratchPath() override;
      core::Error isEmpty(bool* pIsEmpty) override;
      core::Error isValid(bool* pValue) override;

      uintmax_t computeSuspendSize() override;

   private:
      const core::system::User user_;
      const std::string id_;
      FilePath scratchPath_;
      const InvokeRpc invokeRpcFunc_;
};

} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_ACTIVE_SESSIONS_STORAGE_HPP
