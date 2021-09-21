#include <core/r_util/RActiveSessionStorage.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/system/Xdg.hpp>



namespace rstudio {
namespace core {
namespace r_util {

    LegacySessionStorage::LegacySessionStorage(const FilePath& activeSessionsDir) :
        activeSessionsDir_ (activeSessionsDir)
    {
        Error error = activeSessionsDir_.ensureDirectory();
        if(error)
            LOG_ERROR(error);
    }

    Error LegacySessionStorage::readProperty(const std::string& id, const std::string& name, std::string* pValue)
    {
        const std::string& legacyName = getLegacyName(name);

        using namespace rstudio::core;
        *pValue = "";

        FilePath readPath = buildPropertyPath(id, legacyName);
        if (readPath.exists())
        {
            Error error = core::readStringFromFile(readPath, pValue);
            if (error)
                return error;

            boost::algorithm::trim(*pValue);
        }
        return Success();
    }

    Error LegacySessionStorage::writeProperty(const std::string& id, const std::string& name, const std::string& value)
    {
        FilePath writePath = buildPropertyPath(id, name);
        return core::writeStringToFile(writePath, value);
    }

    FilePath LegacySessionStorage::buildPropertyPath(const std::string& id, const std::string& name)
    {
        FilePath propertiesDir = activeSessionsDir_.completeChildPath(legacySessionDirPrefix_ + id + "/" + propertiesDirName_);
        propertiesDir.ensureDirectory();
        return propertiesDir.completeChildPath(name);
    }

    std::shared_ptr<IActiveSessionStorage> ActiveSessionStorageFactory::getActiveSessionStorage()
    {
        return getLegacyActiveSessionStorage();
    }

    std::shared_ptr<IActiveSessionStorage> ActiveSessionStorageFactory::getLegacyActiveSessionStorage()
    {
        FilePath dataDir = ActiveSessions::storagePath(core::system::xdg::userDataDir());
        return std::make_shared<LegacySessionStorage>(LegacySessionStorage(dataDir));
    }
} // namespace r_util
} // namepsace core
} // namespace rstudio