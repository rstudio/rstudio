/*
 * DBActiveSessionStorage.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef DB_ACTIVE_SESSION_STORAGE_HPP
#define DB_ACTIVE_SESSION_STORAGE_HPP

#include <core/Database.hpp>
#include <core/r_util/RActiveSessionStorage.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>

namespace rstudio {
namespace server {
namespace storage {

class DBActiveSessionStorage : public core::r_util::IActiveSessionStorage 
{
public:
   explicit DBActiveSessionStorage(const std::string& sessionId, const core::system::User& user);
   explicit DBActiveSessionStorage(
      const std::string& sessionId,
      const core::system::User& user,
      boost::shared_ptr<core::database::IConnection> overrideConnection);
   ~DBActiveSessionStorage() = default;
   core::Error readProperty(const std::string& name, std::string* pValue) override;   
   core::Error readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues) override;
   core::Error readProperties(std::map<std::string, std::string>* pValues) override;
   core::Error writeProperty(const std::string& name, const std::string& value) override;
   core::Error writeProperties(const std::map<std::string, std::string>& properties) override;
   core::Error destroy() override;
   core::Error isValid(bool* pValue) override;

private:
   std::string sessionId_;
   core::system::User user_;

   boost::shared_ptr<core::database::IConnection> overrideConnection_;

   core::Error getConnectionOrOverride(boost::shared_ptr<core::database::IConnection>* connection);
};

core::Error getConn(boost::shared_ptr<core::database::IConnection>* connection);

namespace errc
{
   enum errc_t {
      Success = 0,
      DBError = 1,
      SessionNotFound = 2,
      TooManySessionsReturned = 3,
      ConnectionFailed = 4
   };
}

} // namespace storage
} // namespace server
} // namespace rstudio

#endif
