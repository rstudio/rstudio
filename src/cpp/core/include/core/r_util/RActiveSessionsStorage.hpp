
#ifndef CORE_R_UTIL_R_ACTIVE_SESSIONS_STORAGE
#define CORE_R_UTIL_R_ACTIVE_SESSIONS_STORAGE


#include <core/r_util/RActiveSession.hpp>
#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace r_util {

   class IActiveSessionsStorage
   {
      public:
         virtual core::Error createSession(const std::string& id, std::map<std::string, std::string> initialProperties) = 0;
         virtual std::vector< boost::shared_ptr<ActiveSession> > listSessions(FilePath userHomePath, bool projectSharingEnabled) const = 0;
         virtual size_t getSessionCount(const FilePath& userHomePath, bool projectSharingEnabled) const = 0;
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
         std::vector< boost::shared_ptr<ActiveSession> > listSessions(FilePath userHomePath, bool projectSharingEnabled) const override;
         size_t getSessionCount(const FilePath& userHomePath, bool projectSharingEnabled) const override;
         boost::shared_ptr<ActiveSession> getSession(const std::string& id) const override;
         bool hasSessionId(const std::string& sessionId) const override;
      private:
         FilePath storagePath_;
   };

} // namespace r_util
} // namespace core 
} // namespace rstudio

#endif // CORE_R_UTIL_R_ACTIVE_SESSIONS_STORAGE
