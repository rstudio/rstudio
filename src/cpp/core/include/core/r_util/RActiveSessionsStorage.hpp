
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
         virtual core::Error create() = 0;
         virtual std::vector<boost::shared_ptr<ActiveSession> > listSessions() = 0;
         virtual size_t sessionCount(const FilePath& userHomePath, bool projectSharingEnabled) const = 0;
         virtual boost::shared_ptr<ActiveSession> getSession(const std::string& id) const = 0;
      protected:
         virtual ~IActiveSessionsStorage() = default;
         IActiveSessionsStorage() = default;
   };

   class FileActiveSessionsStorage : IActiveSessionsStorage
   {
      public:
         explicit FileActiveSessionsStorage(FilePath rootStoragePath);
         core::Error create();
         std::vector<boost::shared_ptr<ActiveSession> > listSessions();
         size_t sessionCount() const;
         boost::shared_ptr<ActiveSession> getSession(const std::string& id) const;
      private:
         FilePath storagePath_;
   };
   
   class ActiveSessionsStorageFactory
   {
   public:
      static std::shared_ptr<IActiveSessionsStorage> getFileActiveSessionsStorage(const FilePath& scratchPath);
   };

}
}
}

#endif