#ifndef DB_ACTIVE_SESSIONS_STORAGE_HPP
#define DB_ACTIVE_SESSIONS_STORAGE_HPP

#include "core/r_util/RActiveSession.hpp"
#include "core/r_util/RActiveSessionsStorage.hpp"
#include "shared_core/Error.hpp"
#include "shared_core/FilePath.hpp"

namespace rstudio
{
namespace server
{
namespace storage
{

   using namespace core;
   using namespace core::r_util;

   class DBActiveSessionsStorage : public IActiveSessionsStorage
   {
      public:
         explicit DBActiveSessionsStorage(const std::string& userId);
         ~DBActiveSessionsStorage() = default;
         core::Error createSession(const std::string& id, std::map<std::string, std::string> initialProperties) override;
         std::vector< boost::shared_ptr<ActiveSession> > listSessions(FilePath userHomePath, bool projectSharingEnabled) const override;
         size_t getSessionCount(const FilePath& userHomePath, bool projectSharingEnabled) const override;
         boost::shared_ptr<ActiveSession> getSession(const std::string& id) const override;
         bool hasSessionId(const std::string& sessionId) const override;
      private:
         std::string userId_;
   };
} // namespace storage
} // namespace server
} // namespace rstudio

#endif
