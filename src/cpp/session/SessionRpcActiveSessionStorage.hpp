/*
 * SessionRpcActiveSessionStorage.hpp
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

#ifndef SESSION_RPC_ACTIVE_SESSION_STORAGE_HPP
#define SESSION_RPC_ACTIVE_SESSION_STORAGE_HPP

#include <core/r_util/RActiveSessionStorage.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>

namespace rstudio {
namespace session {
namespace storage {

class RpcActiveSessionStorage : public core::r_util::IActiveSessionStorage 
{
   public:
      RpcActiveSessionStorage(const core::system::User& user, std::string sessionId);
      
      core::Error readProperty(const std::string& name, std::string* pValue) override;
      core::Error readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues) override;
      core::Error readProperties(std::map<std::string, std::string>* pValues) override;
      core::Error writeProperty(const std::string& name, const std::string& value) override;
      core::Error writeProperties(const std::map<std::string, std::string>& properties) override;

      core::Error destroy() override;
      core::Error isValid(bool* pValue) override;
      core::Error isEmpty(bool* pValue) override;

   private:
      const core::system::User _user;

      const std::string _id;
};

} // namespace storage
} // namespace session
} // namespace rstudio

#endif
