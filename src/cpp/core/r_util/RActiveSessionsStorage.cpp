#include <core/r_util/RActiveSessionsStorage.hpp>


namespace rstudio {
namespace core {
namespace r_util {

   static FilePath buildStoragePath(const FilePath& rootStoragePath)
   {
      return rootStoragePath.completeChildPath("sessions/active");
   }

   FileActiveSessionsStorage::FileActiveSessionsStorage(FilePath rootStoragePath)
   {
      storagePath_ = buildStoragePath(rootStoragePath);
      Error error = storagePath_.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
      }
   }
}
}
}