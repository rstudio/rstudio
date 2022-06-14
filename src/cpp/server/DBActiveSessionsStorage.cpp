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


DBActiveSessionsStorage::DBActiveSessionsStorage(const system::User& user) :
   user_(user)
{
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

   system::UidType uid = user_.getUserId();
   const std::string& uname = user_.getUsername();

   database::Query query = connection->query("SELECT session_id FROM active_session_metadata WHERE user_id=(SELECT id FROM licensed_users WHERE user_id=:id AND user_name=:name)")
      .withInput(uid)
      .withInput(uname);
   
   database::Rowset rowset{};
   error = connection->execute(query, rowset);

   if(!error)
   {
      database::RowsetIterator iter = rowset.begin();
      while(iter != rowset.end())
      {
         std::string sessionId = iter->get<std::string>("session_id");
         sessions.push_back(sessionId);
         ++iter;
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
      LOG_ERROR(error);
   else
   {
      system::UidType uid = user_.getUserId();
      const std::string& uname = user_.getUsername();

      int count;
      database::Query query = connection->query("SELECT COUNT(*) FROM active_session_metadata WHERE user_id=(SELECT id FROM licensed_users WHERE user_id=:id AND user_name=:name)")
         .withInput(uid)
         .withInput(uname)
         .withOutput(count);

      error = connection->execute(query);

      if(error)
         LOG_ERROR(error);
   }

   return 0;
}

std::shared_ptr<IActiveSessionStorage> DBActiveSessionsStorage::getSessionStorage(const std::string& id) const
{
   return std::make_shared<DBActiveSessionStorage>(id, user_);
}

Error DBActiveSessionsStorage::hasSessionId(const std::string& sessionId, bool* pHasSessionId) const
{
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConn(&connection);

   if (!error)
   {
      int count;
      database::Query query = connection->query("SELECT COUNT(*) FROM active_session_metadata WHERE session_id=:id")
         .withInput(sessionId)
         .withOutput(count);

      error = connection->execute(query);
      if (error)
         return error;

      *pHasSessionId = count > 0;
   }

   return error;
}

} // namespace storage
} // namespace server
} // namespace rstudio
