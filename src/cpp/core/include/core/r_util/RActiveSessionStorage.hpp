#ifndef CORE_R_UTIL_R_ACTIVE_SESSION_STORAGE
#define CORE_R_UTIL_R_ACTIVE_SESSION_STORAGE

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace r_util {

   class IActiveSessionStorage 
   {
   public:
      Error virtual readProperty(const std::string& id, const std::string& name, std::string* pValue) = 0;
      Error virtual writeProperty(const std::string& id, const std::string& name, const std::string& value) = 0;

   protected:
      virtual ~IActiveSessionStorage() = default;
      IActiveSessionStorage() = default;
   };

   class LegacySessionStorage : public IActiveSessionStorage
   {
   public:
      explicit LegacySessionStorage(const FilePath& location);

      Error readProperty(const std::string& id, const std::string& name, std::string* pValue) override;   
      Error writeProperty(const std::string& id, const std::string& name, const std::string& value) override;

   private:
      FilePath activeSessionsDir_;
      const std::string propertiesDirName_ = "properites";
      const std::string legacySessionDirPrefix_ = "session-";

      FilePath buildPropertyPath(const std::string& id, const std::string& name);

      static const std::string& getLegacyName(const std::string& name)
      {
         static const std::map<std::string, std::string> legacyNames = 
         {
            { "last_used" , "last-used" },
            { "r_version" , "r-version" },
            { "r_version_label" , "r-version-label" },
            { "r_version_home" , "r-version-home" },
            { "working_directory" , "working-dir" },
            { "launch_parameters" , "launch-parameters" }
         };

         if (legacyNames.find(name) != legacyNames.end())
            return legacyNames.at(name);

         return name;
      }
   };

   class ActiveSessionStorageFactory
   {
   public:
      static std::shared_ptr<IActiveSessionStorage> getActiveSessionStorage();
      static std::shared_ptr<IActiveSessionStorage> getLegacyActiveSessionStorage();
   };
} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_ACTIVE_SESSIONS_STORAGE_HPP