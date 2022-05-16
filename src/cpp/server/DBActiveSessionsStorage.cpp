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


namespace rstudio {
namespace server {
namespace storage {

using namespace server_core::database;

DBActiveSessionsStorage::DBActiveSessionsStorage(const std::string& userId, const FilePath& rootStoragePath)
   : userId_(userId), rootStoragePath_(rootStoragePath)
{
}

core::Error DBActiveSessionsStorage::createSession(const std::string& id, std::map<std::string, std::string> initialProperties)
{
   DBActiveSessionStorage storage{id};
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
      .withInput(userId_);
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
      Error logError{"DatabaseException", errc::DBError, "Exception occured while ", error, ERROR_LOCATION};
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
         .withInput(userId_);
      database::Rowset rowset{};
      error = connection->execute(query, rowset);

      if(!error)
      {
         return rowset.begin()->get<size_t>("COUNT(*)");
      }
   }

   return (size_t) 0;
}

boost::shared_ptr<ActiveSession> DBActiveSessionsStorage::getSession(const std::string& id) const
{
   FilePath scratchPath = rootStoragePath_.completeChildPath(kSessionDirPrefix + id);
   if(hasSessionId(id))
   {
      DBActiveSessionStorage storage{id};
      return boost::shared_ptr<ActiveSession>(new ActiveSession(id, scratchPath, std::make_shared<DBActiveSessionStorage>(storage)));
   }
   else
   {
      return boost::shared_ptr<ActiveSession>(new ActiveSession(id));
   }
}

bool DBActiveSessionsStorage::hasSessionId(const std::string& sessionId) const
{
   boost::shared_ptr<database::IConnection> connection;
   bool hasId = false;
   Error error = getConn(&connection);

   if(!error)
   {
      database::Query query = connection->query("SELECT * FROM active_session_metadata WHERE session_id=:id")
         .withInput(sessionId);
      database::Rowset rowset{};
      connection->execute(query, rowset);

      database::RowsetIterator iter = rowset.begin();
      if(iter != rowset.end())
      {
         hasId = true;
         iter++;
         if(iter != rowset.end())
         {
            Error logError{"Too many sessions were returned when checking for the presence of an ID", errc::TooManySessionsReturned, ERROR_LOCATION};
            LOG_ERROR(logError);
         }
      }
   }
   else
   {
      LOG_ERROR(error);
   }
   return hasId;
}

} // namespace storage
} // namespace server
} // namespace rstudio
