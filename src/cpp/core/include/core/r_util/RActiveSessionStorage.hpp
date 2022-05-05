/*
 * RActiveSessionStorage.hpp
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

#ifndef CORE_R_UTIL_R_ACTIVE_SESSION_STORAGE
#define CORE_R_UTIL_R_ACTIVE_SESSION_STORAGE

#include <shared_core/FilePath.hpp>
#include <set>

namespace rstudio {
namespace core {
namespace r_util {

   class IActiveSessionStorage 
   {
   public:
      Error virtual readProperty(const std::string& name, std::string* pValue) = 0;
      Error virtual readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues) = 0;
      Error virtual readProperties(std::map<std::string, std::string>* pValues) = 0;
      Error virtual writeProperty(const std::string& name, const std::string& value) = 0;
      Error virtual writeProperties(const std::map<std::string, std::string>& properties) = 0;

   protected:
      virtual ~IActiveSessionStorage() = default;
      IActiveSessionStorage() = default;
   };

   class FileActiveSessionStorage : public IActiveSessionStorage
   {
   public:
      explicit FileActiveSessionStorage(const FilePath& location);
      ~FileActiveSessionStorage() = default;
      Error readProperty(const std::string& name, std::string* pValue) override;   
      Error readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues) override;
      Error readProperties(std::map<std::string, std::string>* pValues) override;
      Error writeProperty(const std::string& name, const std::string& value) override;
      Error writeProperties(const std::map<std::string, std::string>& properties) override;

   private:
      FilePath scratchPath_;
      const std::string propertiesDirName_ = "properites";

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
} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_ACTIVE_SESSIONS_STORAGE_HPP
