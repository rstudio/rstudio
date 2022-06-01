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

#include <core/r_util/RActiveSessions.hpp>
#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace r_util {

   class ActiveSession;

   class IActiveSessionsStorage
   {
   public:
      virtual core::Error createSession(const std::string& id, std::map<std::string, std::string> initialProperties) = 0;
      virtual std::vector< std::string > listSessionIds() const = 0;
      virtual size_t getSessionCount() const = 0;
      virtual boost::shared_ptr<ActiveSession> getSession(const std::string& id) const = 0;
      virtual bool hasSessionId(const std::string& sessionId) const = 0;

   protected:
      virtual ~IActiveSessionsStorage() = default;
      IActiveSessionsStorage() = default;
   };

   class FileActiveSessionsStorage : public IActiveSessionsStorage
   {
   public:
      explicit FileActiveSessionsStorage(const FilePath& rootStoragePath);
      ~FileActiveSessionsStorage() = default;
      core::Error createSession(const std::string& id, std::map<std::string, std::string> initialProperties) override;
      std::vector< std::string > listSessionIds() const override;
      size_t getSessionCount() const override;
      boost::shared_ptr<ActiveSession> getSession(const std::string& id) const override;
      bool hasSessionId(const std::string& sessionId) const override;
      
   private:
      FilePath storagePath_;
   };

   constexpr const char* kSessionDirPrefix = "session-";
   FilePath buildActiveSessionStoragePath(const FilePath& rootStoragePath);

} // namespace r_util
} // namespace core 
} // namespace rstudio

#endif // CORE_R_UTIL_R_ACTIVE_SESSIONS_STORAGE
