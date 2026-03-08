/*
 * DBActiveSessionStorage.cpp
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

#include <server/DBActiveSessionStorage.hpp>

#include <core/Database.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <shared_core/SafeConvert.hpp>
#include <server_core/ServerDatabase.hpp>

#include <numeric>

using namespace rstudio::core;
using namespace rstudio::core::r_util;
using namespace rstudio::server_core::database;

namespace rstudio {
namespace server {
namespace storage {

namespace {

// This is the column name of the foreign key between the active_session_metadata
// and the licensed user table key - an integer
const std::string kUserId = "user_id";

// Another integer column that needs special handling
static const std::string kSuspendSize = "suspend_size";

// Constants for the table and column names
const std::string kTableName = "active_session_metadata";
const std::string kSessionIdColumnName = "session_id";

static const std::string kEditorColumnName = "workbench";
static const std::string kWorkingDirColumnName = "working_directory";
static const std::string kProjectColumnName = "project";

inline const std::string& columnName(const std::string& propertyName)
{
   if (propertyName == ActiveSession::kEditor)
      return kEditorColumnName;

   if (propertyName == ActiveSession::kProjectId)
      return kProjectColumnName;

   return propertyName;
}

inline const std::string& propertyName(const std::string& columnName)
{
   if (columnName == kEditorColumnName)
      return ActiveSession::kEditor;

   return columnName;
}

std::string getPropNamesSql(const std::vector<std::string>& propNames)
{
   std::string keys = std::accumulate(
      ++propNames.begin(),
      propNames.end(),
      columnName(*(propNames.begin())),
      [](std::string a, std::string b)
      {
         return a + ", " + columnName(b);
      });
   return keys;
}

std::string getVarNamesSql(const std::vector<std::string>& propNames)
{
   std::string keys = std::accumulate(
      ++propNames.begin(),
      propNames.end(),
      ":" + columnName((*propNames.begin())),
      [](std::string a, std::string b)
      {
         return a + ", :" + columnName(b);
      });
   return keys;
}

std::string getKeyString(const std::map<std::string, std::string>& sourceMap)
{
   std::string keys = std::accumulate(
      ++sourceMap.begin(),
      sourceMap.end(),
      columnName(sourceMap.begin()->first),
      [](std::string a, std::pair<std::string, std::string> b)
      {
         return a + ", " + columnName(b.first);
      });
   return keys;
}

std::string convertTimestampProperty(const std::string& extTime)
{
   // Look for extended iso time string and do a conversion
   if (extTime.empty() || extTime.find("-") == std::string::npos)
      return extTime;
   if (extTime == "not-a-date-time")
      return "";
   try
   {
      boost::posix_time::ptime time = boost::posix_time::from_iso_extended_string(extTime);
      return boost::posix_time::to_iso_string(time);
   }
   catch(const std::bad_cast & e)
   {
      LOG_ERROR_MESSAGE("Invalid time string from session metadata: " + extTime);
   }
   return "";
}

void convertProperty(std::string* pName, std::string* pValue, const core::system::User& user,
                     boost::shared_ptr<database::IConnection> connection)
{
   // last_used, created use millis-since-epoch but these use ptime in extended format where our DB schema uses plain iso
   if (*pName == ActiveSession::kLastResumed || *pName == ActiveSession::kSuspendTimestamp) // suspend_timestamp here?
      *pValue = convertTimestampProperty(*pValue);

}

std::string getUpdateStringAndValues(const std::map<std::string, std::string>& sourceMap,
                                     const system::User& user,
                                     std::vector<std::string>* pNames,
                                     std::vector<std::string>* pValues,
                                     boost::shared_ptr<database::IConnection> connection)
{
   std::string firstPropName(sourceMap.begin()->first);
   std::string firstPropValue(sourceMap.begin()->second);
   convertProperty(&firstPropName, &firstPropValue, user, connection);
   (*pNames).push_back(firstPropName);
   (*pValues).push_back(firstPropValue);

   std::string setValuesString = std::accumulate(
      ++sourceMap.begin(),
      sourceMap.end(),
      columnName(firstPropName) + " = :" + columnName(firstPropName) + " ",
      [pNames, pValues, user, connection](std::string a, std::pair<std::string, std::string> iter)
      {
         std::string propName = iter.first;
         std::string propValue = iter.second;

         convertProperty(&propName, &propValue, user, connection);

         (*pNames).push_back(propName);
         (*pValues).push_back(propValue);
         return a + ", " + columnName(iter.first) + " = " + ":" + columnName(iter.first) + " ";
      });
   return setValuesString;
}

std::string getColumnNameList(const std::set<std::string>& colNames)
{
   std::string cols = std::accumulate(
      ++colNames.begin(),
      colNames.end(),
      columnName(*(colNames.begin())), [](std::string a, std::string b)
      {
         return a + ", " + columnName(b);
      });
   return cols;
}

// Temporary key used to store the raw projectId before resolving to path
static const std::string kTempProjectId = "__temp_project_id__";

void populateMapWithRow(database::RowsetIterator iter, std::map<std::string, std::string> *pTargetMap, const core::system::User& user)
{
   std::string projectId;
   std::string workingDir;
   for(size_t i=0; i < iter->size(); i++)
   {
      std::string key = iter->get_properties(i).get_name();

      try
      {
         if (key == kUserId || key == kSuspendSize)
            pTargetMap->emplace(key, std::to_string(iter->get<int>(key)));
         // Store the projectId temporarily - it will be resolved to a path after the connection is released
         else
            pTargetMap->emplace(propertyName(key), iter->get<std::string>(key, ""));
      }
      catch (const std::bad_cast& e)
      {
         LOG_ERROR_MESSAGE("bad_cast reading column '" + key + "': " + e.what());
      }
      catch (const std::exception& e)
      {
         LOG_ERROR_MESSAGE("Exception reading column '" + key + "': " + e.what());
      }
   }
}

Error getSessionCount(boost::shared_ptr<database::IConnection> connection, std::string sessionId, int* pCount)
{
   database::Query query = connection->query("SELECT COUNT(*) FROM " + kTableName + " WHERE " + kSessionIdColumnName + " = :id")
      .withInput(sessionId, "id")
      .withOutput(*pCount);

   Error error = connection->execute(query);

   if (error)
      return Error("DatabaseException", errc::DBError, "Error while retrieving session count for [ session:" + sessionId + " ]", error, ERROR_LOCATION);

   return Success();
}

} // anonymous namespace

Error getConn(boost::shared_ptr<database::IConnection>* connection) {
   bool success = server_core::database::getConnection(boost::posix_time::milliseconds(500), connection);

   if (!success)
   {
      return Error("FailedToAcquireConnection", errc::ConnectionFailed, "Failed to acquire a connection in 500 milliseconds.", ERROR_LOCATION);
   }

   return Success();
}

Error DBActiveSessionStorage::getConnectionOrOverride(boost::shared_ptr<database::IConnection>* connection)
{
   if (overrideConnection_ == nullptr)
      return getConn(connection);
   else
   {
      *connection = overrideConnection_;
      return Success();
   }
}

DBActiveSessionStorage::DBActiveSessionStorage(const std::string& sessionId, const system::User& user) :
   sessionId_(sessionId),
   user_(user)
{
}

DBActiveSessionStorage::DBActiveSessionStorage(const std::string& sessionId, const system::User& user, boost::shared_ptr<core::database::IConnection> overrideConnection) :
   sessionId_(sessionId),
   user_(user),
   overrideConnection_(overrideConnection)
{
}

Error DBActiveSessionStorage::readProperty(const std::string& name, std::string* pValue)
{
   static const std::string empty;

   *pValue = "";
   std::string projectId; // Store projectId if we need to resolve it after releasing connection

   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if (error)
      return error;

   std::string columnStr = columnName(name);

   std::string queryStr = "SELECT ";
   queryStr
      .append(columnStr)
      .append(" FROM ")
      .append(kTableName)
      .append(" WHERE ")
      .append(kSessionIdColumnName)
      .append(" = :id");

   database::Query query = connection->query(queryStr)
      .withInput(sessionId_, "id");

   database::Rowset rowset;
   error = connection->execute(query, rowset);

   if (error)
      return Error("DatabaseException", errc::DBError, "Database exception during property read [ session:" + sessionId_ + " property:" + name + " ]", error, ERROR_LOCATION);

   auto iter = rowset.begin();

   if (iter == rowset.end())
      return Error("Session does not exist", errc::SessionNotFound, ERROR_LOCATION);

   if (name != kUserId)
      *pValue = iter->get<std::string>(0, "");
   else
      *pValue = std::to_string(iter->get<int>(0));

   // Sanity check number of returned rows, by using the pk in the where clause we should only get 1 row
   if (++iter != rowset.end())
   {
      int count = 1;
      while (iter++ != rowset.end())
         ++count;
      return Error("Too many sessions returned", errc::TooManySessionsReturned, "Expected only one session returned, found " + std::to_string(count) + "[ session:" + sessionId_ + " ]", ERROR_LOCATION);
   }


   return Success();
}

Error DBActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{
   pValues->clear();

   // Use a scope block to ensure connection is released before resolving project path
   {
      boost::shared_ptr<database::IConnection> connection;
      Error error = getConnectionOrOverride(&connection);

      if (error)
         return error;

      std::string namesString = getColumnNameList(names);
      if (names.find(ActiveSession::kProject) != names.end())
         namesString = namesString + ", project";
      database::Query query = connection->query("SELECT " + namesString + " FROM " + kTableName + " WHERE " + kSessionIdColumnName + "=:id")
         .withInput(sessionId_, "id");

      database::Rowset rowset;
      error = connection->execute(query, rowset);

      if (error)
         return Error("DatabaseException", errc::DBError, "Database exception during proprerties read [ session:" + sessionId_ + " properties:" + namesString + " ]", error, ERROR_LOCATION);

      database::RowsetIterator iter = rowset.begin();
      if (iter == rowset.end())
         return Error("Session does not exist", errc::SessionNotFound, ERROR_LOCATION);

      populateMapWithRow(iter, pValues, user_);

      // Sanity check number of returned rows, by using the pk in the where clause we should only get 1 row
      if (++iter != rowset.end())
      {
         int count = 1;
         while (iter++ != rowset.end())
            ++count;
         return Error("Too many sessions returned", errc::TooManySessionsReturned, "Expected only one session returned, found " + std::to_string(count) + "[ session:" + sessionId_ + " ]", ERROR_LOCATION);
      }
   }

   return Success();
}

Error DBActiveSessionStorage::readProperties(std::map<std::string, std::string>* pValues)
{
   // Normally we avoid using * in select lists to avoid unexpected names,
   // or orders of columns. However in this case we explicitly want all columns,
   // and our readProperties uses the populateMapWithRow which discovers the
   // column names, so new or unexpected column names will not cause issues.

   std::set<std::string> all{"*"};
   return readProperties(all, pValues);
}

Error DBActiveSessionStorage::writeProperty(const std::string& inputName, const std::string& inputValue)
{
   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if (error)
      return error;

   std::string name = inputName;
   std::string value = inputValue;

   database::Query query = connection->query("UPDATE " + kTableName + " SET " + columnName(name) + " = :value WHERE " + kSessionIdColumnName + " = :id")
      .withInput(value, "value")
      .withInput(sessionId_, "id");

   error = connection->execute(query);

   if (error)
      return Error("DatabaseException", errc::DBError, "Database error while updating session metadata [ session: " + sessionId_ + " property: " + name + " ]", error, ERROR_LOCATION);

   return error;
}

Error DBActiveSessionStorage::writeProperties(const std::map<std::string, std::string>& properties)
{
   std::string propsStr;
   bool first = true;
   for (auto it = properties.begin(); it != properties.end(); it++)
   {
      if (!first)
         propsStr += ", ";
      propsStr += it->first + " = " + it->second;
      first = false;
   }
   LOG_DEBUG_MESSAGE("Writing session properties: " + sessionId_ + " props: " + propsStr);

   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if (error)
      return error;

   std::vector<std::string> propNames, propValues;

   std::string queryStr = "UPDATE " + kTableName + " SET " + getUpdateStringAndValues(properties, user_, &propNames, &propValues, connection) + " WHERE session_id = :session_id";

   database::Query updateQuery = connection->query(queryStr);
   for (unsigned int i = 0; i < propValues.size(); i++)
   {
      updateQuery.withInput(propValues[i], columnName(propNames[i]));
   }
   updateQuery.withInput(sessionId_, "session_id");

   error = connection->execute(updateQuery);

   if (error)
      return Error("DatabaseException", errc::DBError, "Error while updating properties [ session:" + sessionId_ + " properties:" + getKeyString(properties) + " ]", error, ERROR_LOCATION);

   if (updateQuery.getAffectedRows() == 0)
   {
      std::vector<std::string> propNames, propValues;

      // Populate propNames and propValues from the input properties, applying conversions
      for (const auto& prop : properties)
      {
         std::string name = prop.first;
         std::string value = prop.second;
         convertProperty(&name, &value, user_, connection);
         propNames.push_back(name);
         propValues.push_back(value);
      }

      std::string queryStr = "INSERT INTO " +
            kTableName +
            " (" +
            kSessionIdColumnName +
            ", " +
            kUserId +
            ", " +
            getPropNamesSql(propNames) +
            ") VALUES (:id, " +
            "(SELECT id FROM licensed_users WHERE user_name=:user_name AND user_id=:user_id), " +
            getVarNamesSql(propNames) +
            ")";

      std::string username = user_.getUsername();
      int userId = user_.getUserId();
      database::Query insertQuery = connection->query(queryStr)
         .withInput(sessionId_, "id")
         .withInput(username, "user_name")
         .withInput(userId, "user_id");

      for (unsigned int i = 0; i < propValues.size(); i++)
      {
         insertQuery.withInput(propValues[i], columnName(propNames[i]));
      }

      error = connection->execute(insertQuery);

      if (error)
         return Error("DatabaseException", errc::DBError, "Error while inserting new session with properties [ session:" + sessionId_ + " properties:" + getKeyString(properties) + " ]", error, ERROR_LOCATION);
   }
   return Success();
}

Error DBActiveSessionStorage::destroy()
{
   LOG_DEBUG_MESSAGE("Removing active session for: " + sessionId_ + " from database");

   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);

   if (error)
      return error;

   database::Query query = connection->query("DELETE FROM " + kTableName + " WHERE " + kSessionIdColumnName + " = :id")
      .withInput(sessionId_, "id");

   error = connection->execute(query);

   if (error)
      return Error("DatabaseException", errc::DBError, "Error while deleting session metadata [ session:" + sessionId_ + " ]", error, ERROR_LOCATION);

   if (!query.getAffectedRows())
      LOG_DEBUG_MESSAGE("Failed to delete active session from database - no rows removed for: " + sessionId_);

   return error;
}

Error DBActiveSessionStorage::clearScratchPath()
{
   return Success();
}


Error DBActiveSessionStorage::isEmpty(bool* pIsEmpty)
{
   *pIsEmpty = true;

   boost::shared_ptr<database::IConnection> connection;
   Error error = getConnectionOrOverride(&connection);
   int count;

   if (error)
      return error;

   error = getSessionCount(connection, sessionId_, &count);
   if (error)
      return error;

   // ensure one and only one
   if (count > 1)
   {
      LOG_WARNING_MESSAGE("More than one session with session id: " + sessionId_);
      return Error("Too Many Sessions Returned", errc::TooManySessionsReturned, "Expected only one session returned, found " + std::to_string(count) + "[ session:" + sessionId_ + " ]", ERROR_LOCATION);
   }
   else if (count == 1)
   {
      *pIsEmpty = false;
      LOG_DEBUG_MESSAGE("DB - active session: found a session with session id: " + sessionId_);
   }
   else
      LOG_DEBUG_MESSAGE("No session found with session id: " + sessionId_);

   return Success();
}

Error DBActiveSessionStorage::isValid(bool* pValue)
{
   *pValue = false;

   // First check if session exists
   bool isEmpty = true;
   Error error = this->isEmpty(&isEmpty);
   if (error)
      return error;

   if (isEmpty)
      return Success();

   // Session exists — check editor and project for R sessions
   std::string editorVal;
   error = readProperty(ActiveSession::kEditor, &editorVal);
   if (error)
      return Success(); // Can't read, treat as invalid

   bool isRSession = editorVal == kWorkbenchRStudio || editorVal.empty();
   if (!isRSession)
   {
      *pValue = true;
      return Success();
   }

   // R session: ensure project is non-empty
   std::string projectVal;
   error = readProperty(ActiveSession::kProject, &projectVal);
   if (error)
      return Success(); // Can't read, treat as invalid

   *pValue = !projectVal.empty();
   return Success();
}

} // Namespace storage
} // Namespace server
} // Namespace rstudio
