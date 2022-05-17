/*
 * DBActiveSessionStorage.cpp
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

#include <server/DBActiveSessionStorage.hpp>

#include <core/Database.hpp>
#include <server_core/ServerDatabase.hpp>

#include <numeric>

namespace rstudio {
namespace server {
namespace storage {

using namespace server_core::database;
namespace {

// This is the column name of the foreign key between the active_session_metadata
// And the licensed user table. The only column that isn't a string
const std::string kUserId = "user_id";

// Constants for the table and column names
const std::string kTableName = "active_session_metadata";
const std::string kSessionIdColumnName = "session_id";


std::string getKeyString(const std::map<std::string, std::string>& sourceMap)
{
   std::string keys = std::accumulate(++sourceMap.begin(), sourceMap.end(), 
               sourceMap.begin()->first, [](std::string a, std::pair<std::string, std::string> b) {
                  return a + ", " + b.first;
               });
   return keys;
}

std::string getValueString(const std::map<std::string, std::string>& sourceMap)
{
   std::string values = std::accumulate(++sourceMap.begin(), sourceMap.end(), 
               "'" + sourceMap.begin()->second + "'", [](std::string a, std::pair<std::string, std::string> b) {
                  std::string str{a};
                  if(b.first == kUserId)
                  {
                     a += ", " + b.second;
                  }
                  else
                  {
                     a += ", '" + b.second + "'";
                  }
                  return a;
               });
   return values;

}

std::string getUpdateString(const std::map<std::string, std::string>& sourceMap)
{
   std::string setValuesString = std::accumulate(++sourceMap.begin(), sourceMap.end(),
               sourceMap.begin()->first + " = '" + sourceMap.begin()->second + "'", [](std::string a, std::pair<std::string, std::string> iter){
                  return a + ", " + iter.first + " = '" + iter.second + "'";
               });
   return setValuesString;
}

std::string getColumnNameList(const std::set<std::string>& colNames)
{
   std::string cols = std::accumulate(++colNames.begin(), colNames.end(), 
               *(colNames.begin()), [](std::string a, std::string b) {
               return a + ", " + b;
               });
   return cols;
}

void populateMapWithRow(database::RowsetIterator iter, std::map<std::string, std::string> *pTargetMap)
{
   for(size_t i=0; i < iter->size(); i++)
   {
      std::string key = iter->get_properties(i).get_name();
      if(key != kUserId)
      {
         pTargetMap->insert(
            std::pair<std::string, std::string>{
               key,
               iter->get<std::string>(key, "")
            }
         );
      }
      else
      {
         pTargetMap->insert(
            std::pair<std::string, std::string>{
               key,
               std::to_string(iter->get<int>(key))
            }
         );
      }
   }
}
} // anonymous namespace

Error getConn(boost::shared_ptr<database::IConnection>* connection) {
   bool success = server_core::database::getConnection(boost::posix_time::milliseconds(500), connection);

   if(!success)
   {
      return Error{"FailedToAcquireConnection", errc::ConnectionFailed, "Failed to acquire a connection in 500 milliseconds.", ERROR_LOCATION};
   }

   return Success();
}

Error DBActiveSessionStorage::getConnectionOrOverride(boost::shared_ptr<database::IConnection>* connection)
{
   if(overrideConnection_ == nullptr)
      return getConn(connection);
   else
   {
      *connection = overrideConnection_;
      return Success();
   }
}

DBActiveSessionStorage::DBActiveSessionStorage(const std::string& sessionId)
   : sessionId_(sessionId)
{
   
}

DBActiveSessionStorage::DBActiveSessionStorage(const std::string& sessionId, boost::shared_ptr<core::database::IConnection> overrideConnection)
   : sessionId_(sessionId), overrideConnection_(overrideConnection)
{

}

Error DBActiveSessionStorage::readProperty(const std::string& name, std::string* pValue)
{
   *pValue = "";
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);
   
   if(error)
   {
      return error;
   }


   database::Query query = connection->query("SELECT "+name+" FROM "+kTableName+" WHERE "+kSessionIdColumnName+" = :id")
      .withInput(sessionId_);

   database::Rowset results{};
   error = connection->execute(query, results);

   if(!error)
   {
      database::RowsetIterator iter = results.begin();
      if(iter != results.end())
      {
         if(name != kUserId)
         {
            *pValue = iter->get<std::string>(name, "");
         }
         else
         {
            *pValue = std::to_string(iter->get<int>(name));
         }
         if(++iter != results.end()) 
         {
            return Error{"Too many sessions returned", errc::TooManySessionsReturned, ERROR_LOCATION};
         }
      }
      else
      {
         return Error{"Session does not exist", errc::SessionNotFound, ERROR_LOCATION};
      }
   }
   else
   {
      return Error{"DatabaseException", errc::DBError, "Database exception during property read [ session:"+sessionId_+" property:"+name+" ]", error, ERROR_LOCATION};
   }
   
   return error;
}

Error DBActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{
   pValues->clear();
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if(error)
   {
      return error;
   }
   
   std::string namesString = getColumnNameList(names);
   database::Query query = connection->query("SELECT "+namesString+" FROM "+kTableName+" WHERE "+kSessionIdColumnName+"=:id")
      .withInput(sessionId_);

   database::Rowset rowset{};
   error = connection->execute(query, rowset);

   if(!error)
   {
      database::RowsetIterator iter = rowset.begin();
      if(iter != rowset.end())
      {
         populateMapWithRow(iter, pValues);
         // Sanity check number of returned rows, by using the pk in the where clause we should only get 1 row
         if(++iter != rowset.end())
         {
            return Error{"Too many sessions returned", errc::TooManySessionsReturned, ERROR_LOCATION};
         }
      }
      else
      {
         return Error{"Session does not exist", errc::SessionNotFound, ERROR_LOCATION};
      }
   }
   else
   {
      return Error{"DatabaseException", errc::DBError, "Database exception during proprerties read [ session:"+sessionId_+" properties:"+namesString+" ]", error, ERROR_LOCATION};
   }

   return error;
}

Error DBActiveSessionStorage::readProperties(std::map<std::string, std::string>* pValues)
{
   std::set<std::string> all{"*"};
   return readProperties(all, pValues);
}

Error DBActiveSessionStorage::writeProperty(const std::string& name, const std::string& value)
{
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if(error)
   {
      return error;
   }

   database::Query query = connection->query("UPDATE "+kTableName+" SET "+name+" = :value WHERE "+kSessionIdColumnName+" = :id")
      .withInput(value)
      .withInput(sessionId_);

   error = connection->execute(query);

   if(error){
      return Error{"DatabaseException", errc::DBError, "Database error while updating session metadata [ session: "+sessionId_+" property: " + name + " ]", error, ERROR_LOCATION};
   }

   return error;
}

Error DBActiveSessionStorage::writeProperties(const std::map<std::string, std::string>& properties)
{
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if(error)
   {
      return error;
   }

   database::Query query = connection->query("SELECT * FROM "+kTableName+" WHERE "+kSessionIdColumnName+" = :id")
      .withInput(sessionId_);
   database::Rowset rowset{};

   if(error)
   {
      return error;
   }

   error = connection->execute(query, rowset);
   if(!error)
   {
      database::RowsetIterator iter = rowset.begin();
      if(iter != rowset.end())
      {
         // Sanity check number of returned rows, by using the pk in the where clause we should only get 1 row
         if(++iter != rowset.end())
         {
            return Error{"Too many sessions returned", errc::TooManySessionsReturned, ERROR_LOCATION};
         }

         database::Query updateQuery = connection->query("UPDATE "+kTableName+" SET "+getUpdateString(properties)+" WHERE session_id = :id")
            .withInput(sessionId_);
         
         error = connection->execute(updateQuery);
         if(error)
         {
            return Error{"DatabaseException", errc::DBError, "Error while updating properties [ session:"+sessionId_+" properties:"+getKeyString(properties)+" ]", error, ERROR_LOCATION};
         }
      }
      else
      {
         database::Query insertQuery = connection->query("INSERT INTO "+kTableName+" ("+kSessionIdColumnName+", "+getKeyString(properties)+") VALUES (:id, "+getValueString(properties)+")")
            .withInput(sessionId_);

         error = connection->execute(insertQuery);
         if(error)
         {
            return Error{"DatabaseException", errc::DBError, "Error while updating properties [ session:"+sessionId_+" properties:"+getKeyString(properties)+" ]", error, ERROR_LOCATION};
         }
      }
   }
   return error;
}

Error DBActiveSessionStorage::destroy()
{
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if(error)
   {
      return error;
   }

   database::Query query = connection->query("DELETE FROM "+kTableName+" WHERE "+kSessionIdColumnName+" = :id")
      .withInput(sessionId_);
   database::Rowset rowset{};

   error = connection->execute(query, rowset);

   if(!error)
   {
      return Error{"DatabaseException", errc::DBError, "Error while deleting session metadata [ session:"+sessionId_+" ]", error, ERROR_LOCATION};
   }
   else
   {
      return error;
   }
}


Error DBActiveSessionStorage::isValid(bool* pValue)
{
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if(error)
   {
      return error;
   }

   database::Query query = connection->query("SELECT COUNT(*) FROM "+kTableName+" WHERE "+kSessionIdColumnName+" = :id")
      .withInput(sessionId_);
   database::Rowset rowset{};

   *pValue = false;

   error = connection->execute(query, rowset);

   if(error)
   {
      return Error{"DatabaseException", errc::DBError, "Error while deleting session metadata [ session:"+sessionId_+" ]", error, ERROR_LOCATION};
   }
   database::RowsetIterator iter = rowset.begin();

   // Sanity checking, but should always have 1 row containing the count of rows
   if(iter != rowset.end())
   {
      int count = iter->get<int>("COUNT(*)");

      // ensure one and only one
      if(count > 1)
      {
         return Error{"Too Many Sessions Returned", errc::TooManySessionsReturned, ERROR_LOCATION};
      }
      else
      {
         if(count == 1)
         {
            *pValue = true;
         }
      }
   }
   return Success();
}

Error DBActiveSessionStorage::isEmpty(bool* pValue)
{
   bool val;
   Error error = isValid(&val);
   *pValue = !val;
   return error;
}

} // Namespace storage
} // Namespace server
} // Namespace rstudio
