/*
 * RActiveSessionsStorage.hpp
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

#ifndef CORE_R_UTIL_R_ACTIVE_SESSIONS_STORAGE
#define CORE_R_UTIL_R_ACTIVE_SESSIONS_STORAGE

#include <core/json/JsonRpc.hpp>
#include <core/r_util/RActiveSessionStorage.hpp>
#include <core/r_util/RActiveSessions.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace r_util {
   class ActiveSession;

   class IActiveSessionsStorage
   {
   public:
      virtual std::vector< std::string > listSessionIds() const = 0;
      virtual size_t getSessionCount() const = 0;
      virtual std::shared_ptr<IActiveSessionStorage> getSessionStorage(const std::string& id) const = 0;
      virtual Error hasSessionId(const std::string& sessionId, bool* pHasSessionId) const = 0;

   protected:
      virtual ~IActiveSessionsStorage() = default;
      IActiveSessionsStorage() = default;
   };

   class FileActiveSessionsStorage : public IActiveSessionsStorage
   {
   public:
      explicit FileActiveSessionsStorage(const FilePath& rootStoragePath);
      ~FileActiveSessionsStorage() = default;
      std::vector< std::string > listSessionIds() const override;
      size_t getSessionCount() const override;
      std::shared_ptr<IActiveSessionStorage> getSessionStorage(const std::string& id) const override;
      Error hasSessionId(const std::string& sessionId, bool* pHasSessionId) const override;
      
   private:
      FilePath storagePath_;
   };

   class RpcActiveSessionsStorage : public core::r_util::IActiveSessionsStorage
   {
   public:
      explicit RpcActiveSessionsStorage(const core::system::User& user, InvokeRpc invokeRpcFunc);

      static std::shared_ptr<r_util::IActiveSessionsStorage> createDefaultStorage(const core::system::User& user);

      std::vector<std::string> listSessionIds() const override;
      size_t getSessionCount() const  override;
      std::shared_ptr<core::r_util::IActiveSessionStorage> getSessionStorage(const std::string& id)  const override;
      core::Error hasSessionId(const std::string& sessionId, bool* pHasSessionId)  const override;

   private:
      const core::system::User user_;
      const InvokeRpc invokeRpcFunc_;
   };

   constexpr const char* kSessionDirPrefix = "session-";
   FilePath buildActiveSessionStoragePath(const FilePath& rootStoragePath);

} // namespace r_util
} // namespace core 
} // namespace rstudio

#endif // CORE_R_UTIL_R_ACTIVE_SESSIONS_STORAGE
