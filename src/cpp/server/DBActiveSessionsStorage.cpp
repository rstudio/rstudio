/*
 * DBActiveSessionsStorage.cpp
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

#include <server/DBActiveSessionsStorage.hpp>

#include <core/Database.hpp>
#include <server/DBActiveSessionStorage.hpp>
#include <server_core/ServerDatabase.hpp>

using namespace rstudio::core;
using namespace rstudio::core::r_util;
using namespace rstudio::server_core::database;

namespace rstudio {
namespace server {
namespace storage {


DBActiveSessionsStorage::DBActiveSessionsStorage(const system::User& user)
   : user_(user)
{
}

core::Error DBActiveSessionsStorage::initSessionProperties(const std::string& id, std::map<std::string, std::string> initialProperties)
{
   DBActiveSessionStorage storage(id, user_);
   return storage.writeProperties(initialProperties);
}

std::vector< std::string > DBActiveSessionsStorage::listSessionIds() const
{
   boost::shared_ptr<database::IConnection> connection;
   std::vector<std::string> sessions;
   Error error = getConn(&connection);

   if(error)
   {
      LOG_ERROR(error);
   }

   database::Query query = connection->query("SELECT session_id FROM active_session_metadata WHERE user_id=:id")
      .withInput(user_.getUserId());
   database::Rowset rowset{};
   error = connection->execute(query, rowset);

   if(!error)
   {
      database::RowsetIterator iter = rowset.begin();
      while(iter != rowset.end())
      {
         std::string sessionId = iter->get<std::string>("session_id");
         sessions.push_back(sessionId);
      }
   }
   else
   {
      Error logError("DatabaseException", errc::DBError, "Exception occured while ", error, ERROR_LOCATION);
      LOG_ERROR(logError);
   }
   return sessions;
}

size_t DBActiveSessionsStorage::getSessionCount() const
{
   boost::shared_ptr<database::IConnection> connection;
   std::vector<std::string> sessions;
   Error error = getConn(&connection);

   if(error)
   {
      LOG_ERROR(error);
   }
   else
   {
      database::Query query = connection->query("SELECT COUNT(*) FROM active_session_metadata WHERE user_id=:id")
         .withInput(user_.getUserId());
      database::Rowset rowset{};
      error = connection->execute(query, rowset);

      if(!error)
      {
         return rowset.begin()->get<size_t>("COUNT(*)");
      }
   }

   return 0;
}

std::shared_ptr<IActiveSessionStorage> DBActiveSessionsStorage::getSessionStorage(const std::string& id) const
{
   bool hasId = false;
   Error error = hasSessionId(id, &hasId);
   if (error)
      LOG_ERROR(error);

   if (!error && hasId)
   {
      return std::make_shared<DBActiveSessionStorage>(id, user_);
   }
   else
   {
      return std::shared_ptr<IActiveSessionStorage>();
   }
}

Error DBActiveSessionsStorage::hasSessionId(const std::string& sessionId, bool* pHasSessionId) const
{
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConn(&connection);

   if (!error)
   {
      database::Query query = connection->query("SELECT count(*) FROM active_session_metadata WHERE session_id=:id")
         .withInput(sessionId);
      error = connection->execute(query, pHasSessionId);
   }

   return error;
}

} // namespace storage
} // namespace server
} // namespace rstudio
