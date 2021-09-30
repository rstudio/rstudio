/*
 * RActiveSessionStorage.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include <core/r_util/RActiveSessionStorage.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/system/Xdg.hpp>

namespace rstudio {
namespace core {
namespace r_util {

    FileActiveSessionStorage::FileActiveSessionStorage(const FilePath& activeSessionsDir) :
        activeSessionsDir_ (activeSessionsDir)
    {
        Error error = activeSessionsDir_.ensureDirectory();
        if(error)
            LOG_ERROR(error);
    }

    Error FileActiveSessionStorage::readProperty(const std::string& id, const std::string& name, std::string* pValue)
    {
        const std::string& fileName = getPropertyFileName(name);

        using namespace rstudio::core;
        *pValue = "";

        FilePath readPath = buildPropertyPath(id, fileName);
        if (readPath.exists())
        {
            Error error = core::readStringFromFile(readPath, pValue);
            if (error)
                return error;

            boost::algorithm::trim(*pValue);
        }

        return Success();
    }

    Error FileActiveSessionStorage::writeProperty(const std::string& id, const std::string& name, const std::string& value)
    {
        FilePath writePath = buildPropertyPath(id, name);
        return core::writeStringToFile(writePath, value);
    }

    FilePath FileActiveSessionStorage::buildPropertyPath(const std::string& id, const std::string& name)
    {
        FilePath propertiesDir = activeSessionsDir_.completeChildPath(fileSessionDirPrefix_ + id + "/" + propertiesDirName_);
        propertiesDir.ensureDirectory();
        return propertiesDir.completeChildPath(name);
    }

    std::shared_ptr<IActiveSessionStorage> ActiveSessionStorageFactory::getActiveSessionStorage()
    {
        return getFileActiveSessionStorage();
    }

    std::shared_ptr<IActiveSessionStorage> ActiveSessionStorageFactory::getFileActiveSessionStorage()
    {
        FilePath dataDir = ActiveSessions::storagePath(core::system::xdg::userDataDir());
        return std::make_shared<FileActiveSessionStorage>(FileActiveSessionStorage(dataDir));
    }
} // namespace r_util
} // namepsace core
} // namespace rstudio
