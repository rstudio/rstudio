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
#include <core/Result.hpp>
#include <core/QueryBuilder.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <shared_core/SafeConvert.hpp>
#include <server_core/ServerDatabase.hpp>

#include <numeric>

using namespace rstudio::core;
using namespace rstudio::core::database;
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
const SqlIdentifier kTableName = "active_session_metadata";
const SqlIdentifier kSessionIdColumnName = "session_id";
const SqlIdentifier kProjectColumnName = "project";

static std::map<std::string, SqlIdentifier> kASMColumns;
static std::map<std::string, std::string> kASMProperties;

void populateASMMaps()
{
   static bool ready = false;
   if (ready)
      return;

   kASMColumns[ActiveSession::kEditor] = "workbench";
   kASMProperties["workbench"] = ActiveSession::kEditor;

   kASMColumns[ActiveSession::kProjectId] = kProjectColumnName;
   kASMProperties[kProjectColumnName] = ActiveSession::kProjectId;

   std::string keys[] = {
      ActiveSession::kCreated,
      ActiveSession::kExecuting,
      ActiveSession::kInitial,
      ActiveSession::kLastUsed,
      ActiveSession::kLabel,
      ActiveSession::kProject,
      ActiveSession::kSavePromptRequired,
      ActiveSession::kRunning,
      ActiveSession::kRVersion,
      ActiveSession::kRVersionHome,
      ActiveSession::kRVersionLabel,
      ActiveSession::kWorkingDir,
      ActiveSession::kActivityState,
      ActiveSession::kLastStateUpdated,
      ActiveSession::kLastResumed,
      ActiveSession::kSuspendTimestamp,
      ActiveSession::kBlockingSuspend,
      ActiveSession::kLaunchParameters,
#ifdef RSTUDIO_PRO_BUILD
      ActiveSession::kSuspendSize,
#endif
#ifdef RSTUDIO_UNIT_TESTS_ENABLED
      // only used in tests
      "user_id",
      "session_id",
#endif
   };

   for (const std::string& key : keys) {
      // Since these are compile-time constants, we know they're already validated.
      // Skip the validation step by using the const char* constructor.
      kASMColumns[key] = key.c_str();
      kASMProperties[key] = key;
   }

   ready = true;
};

inline Result<SqlIdentifier> columnName(const std::string& propertyName)
{
   populateASMMaps();
   auto iter = kASMColumns.find(propertyName);
   if (iter == kASMColumns.end())
   {
      return Unexpected(Error("Unknown property " + propertyName, boost::system::errc::invalid_argument, ERROR_LOCATION));
   }

   return iter->second;
}

inline Result<std::string> propertyName(const std::string& columnName)
{
   populateASMMaps();

   auto iter = kASMProperties.find(columnName);
   if (iter == kASMProperties.end())
   {
      return Unexpected(Error("Unknown column " + columnName, boost::system::errc::invalid_argument, ERROR_LOCATION));
   }

   return iter->second;
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

   if (*pName == ActiveSession::kProject)
   {
      std::string projectId = ProjectId(kProjectNoneId, user.getUserId()).asString();

      *pName = ActiveSession::kProjectId;
      *pValue = projectId;
   }
   if (*pName == ActiveSession::kSuspendSize)
   {
      if ((*pValue).empty() || !safe_convert::stringTo<int>(*pValue).has_value())
         *pValue = "0";
   }
}

bool isProjectNoneId(const std::string& projectId)
{
   return projectId == kProjectNoneId || ProjectId(projectId).id() == kProjectNoneId;
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
         {
            // int columns
            pTargetMap->emplace(key, std::to_string(iter->get<int>(key)));
         }
         else if (key == kProjectColumnName)
         {
            // Store the projectId temporarily - it will be resolved to a path after the connection is released
            std::string projectId = iter->get<std::string>(key, "");
            if (projectId.size() == 8)
               projectId = ProjectId(projectId, user.getUserId()).asString();
            // Store raw projectId for later resolution
            pTargetMap->emplace(kTempProjectId, projectId);
         }
         else
         {
            // Unknown columns in the database result should be preserved as-is
            auto propResult = propertyName(key);
            std::string propName = propResult ? *propResult : key;
            pTargetMap->emplace(propName, iter->get<std::string>(key, ""));
         }
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

template <typename CONTAINER>
Error sessionPropError(const std::string& prefix, const std::string& sessionId, const CONTAINER& properties, const Error& cause, const ErrorLocation& loc)
{
   std::ostringstream message;
   message << prefix << " [ session:" << sessionId;
   if (!properties.empty())
      message << " properties:" << algorithm::join(properties, ",",
         [](const std::string& propName) {
            auto colName = columnName(propName);
            return colName ? *colName : ("UNKNOWN:" + propName);
         }
      );
   message << " ]";
   if (cause)
      return Error("DatabaseException", errc::DBError, message.str(), cause, loc);
   return Error("DatabaseException", errc::DBError, message.str(), loc);
}

Error sessionPropError(const std::string& prefix, const std::string& sessionId, const std::map<std::string, std::string>& properties, const Error& cause, const ErrorLocation& loc)
{
   std::vector<std::string> propNames;
   for (const auto& [propName, value] : properties)
      propNames.push_back(propName);
   return sessionPropError(prefix, sessionId, propNames, cause, loc);
}

Error sessionPropError(const std::string& prefix, const std::string& sessionId, const std::string& propName, const Error& cause, const ErrorLocation& loc)
{
   return sessionPropError(prefix, sessionId, std::vector<std::string>({ propName }), cause, loc);
}

Error sessionPropError(const std::string& prefix, const std::string& sessionId, const Error& cause, const ErrorLocation& loc)
{
   return sessionPropError(prefix, sessionId, std::vector<std::string>(), cause, loc);
}

Error getSessionCount(boost::shared_ptr<database::IConnection> connection, std::string sessionId, int* pCount)
{
   database::Query query = connection->query("SELECT COUNT(*) FROM " + kTableName + " WHERE " + kSessionIdColumnName + " = :id")
      .withInput(sessionId, "id")
      .withOutput(*pCount);

   Error error = connection->execute(query);

   if (error)
      return sessionPropError("Error while retrieving session count for", sessionId, error, ERROR_LOCATION);

   return Success();
}

} // anonymous namespace

Error getConn(boost::shared_ptr<database::IConnection>* connection)
{
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

   auto columnStr = columnName(name);
   if (!columnStr)
      return columnStr.error();

   SelectBuilder builder(connection, kTableName);
   builder.add(*columnStr);
   builder.where(kSessionIdColumnName, sessionId_);
   database::Query query = builder.build();

   database::Rowset rowset;
   error = connection->execute(query, rowset);

   if (error)
      return sessionPropError("Database exception during property read", sessionId_, name, error, ERROR_LOCATION);

   auto iter = rowset.begin();

   if (iter == rowset.end())
      return Error("Session does not exist", errc::SessionNotFound, ERROR_LOCATION);

   if (name == ActiveSession::kProject)
   {
      projectId = iter->get<std::string>(0, "");

      if (projectId.size() == 8)
         projectId = ProjectId(projectId, user_.getUserId()).asString();
      if (isProjectNoneId(projectId) || projectId.empty())
         *pValue = kProjectNone;
   }
   else
   {
      if (name != kUserId)
         *pValue = iter->get<std::string>(0, "");
      else
         *pValue = std::to_string(iter->get<int>(0));
   }

   // Sanity check number of returned rows, by using the pk in the where clause we should only get 1 row
   int count = 1;
   for (++iter; iter != rowset.end(); ++iter)
      ++count;

   if (count > 1)
      return Error("Too many sessions returned", errc::TooManySessionsReturned, "Expected only one session returned, found " + std::to_string(count) + "[ session:" + sessionId_ + " ]", ERROR_LOCATION);

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

      SelectBuilder builder(connection, kTableName);
      for (const std::string& propName : names) {
         auto colName = columnName(propName);
         if (!colName)
            return colName.error();
         builder.add(*colName);
      }

      if (names.find(ActiveSession::kProject) != names.end())
         builder.add("project");

      builder.where(kSessionIdColumnName, sessionId_);
      database::Query query = builder.build();

      database::Rowset rowset;
      error = connection->execute(query, rowset);

      if (error)
         return sessionPropError("Database exception during properties read", sessionId_, std::vector<std::string>(names.begin(), names.end()), error, ERROR_LOCATION);

      database::RowsetIterator iter = rowset.begin();
      if (iter == rowset.end())
         return Error("Session does not exist", errc::SessionNotFound, ERROR_LOCATION);

      populateMapWithRow(iter, pValues, user_);

      // Sanity check number of returned rows, by using the pk in the where clause we should only get 1 row
      int count = 1;
      for (++iter; iter != rowset.end(); ++iter)
         ++count;

      if (count > 1)
         return Error("Too many sessions returned", errc::TooManySessionsReturned, "Expected only one session returned, found " + std::to_string(count) + "[ session:" + sessionId_ + " ]", ERROR_LOCATION);
   }

   return Success();
}

Error DBActiveSessionStorage::readProperties(std::map<std::string, std::string>* pValues)
{
   std::set<std::string> all;
   for (const auto& [propName, colName] : kASMColumns) {
      all.insert(propName);
   }
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
   convertProperty(&name, &value, user_, connection);
   auto colName = columnName(name);
   if (!colName)
      return colName.error();

   UpdateBuilder builder(connection, kTableName);
   builder.add(*colName, value);
   builder.where(kSessionIdColumnName, sessionId_);
   Query query = builder.build();
   error = connection->execute(query);

   if (error)
      return sessionPropError("Database exception while updating session metadata", sessionId_, name, error, ERROR_LOCATION);

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

   std::vector<std::pair<SqlIdentifier, std::string>> sqlProps;

   Transaction transaction(connection);

   UpdateBuilder update(connection, kTableName);
   for (const auto& prop : properties)
   {
      // Populate propNames and propValues from the input properties, applying conversions
      std::string name = prop.first;
      std::string value = prop.second;
      convertProperty(&name, &value, user_, connection);
      auto colName = columnName(name);
      if (!colName)
         return colName.error();
      sqlProps.emplace_back(*colName, value);
      update.add(*colName, value);
   }
   update.where(kSessionIdColumnName, sessionId_);

   database::Query updateQuery = update.build();
   error = connection->execute(updateQuery);
   if (error)
      return sessionPropError("Error while updating properties", sessionId_, properties, error, ERROR_LOCATION);

   if (updateQuery.getAffectedRows() == 0)
   {
      SelectBuilder select(connection, "licensed_users");
      select
         .add("id")
         .where("user_name", user_.getUsername())
         .where("user_id", user_.getUserId());

      Query selectQuery = select.build();
      Rowset rows;
      error = connection->execute(selectQuery, rows);
      if (error)
         return sessionPropError("Error while getting user key", sessionId_, properties, error, ERROR_LOCATION);

      auto iter = rows.begin();

      if (iter == rows.end())
         return sessionPropError("Could not find user", sessionId_, properties, Error(), ERROR_LOCATION);

      int licensedUserId = iter->get<int>("id");

      ++iter;
      if (iter != rows.end())
         return sessionPropError("Found duplicate user", sessionId_, properties, Error(), ERROR_LOCATION);

      InsertBuilder insert(connection, kTableName);
      insert.add(kSessionIdColumnName, sessionId_);
      insert.add("user_id", licensedUserId);

      for (const auto& [colName, propValue] : sqlProps)
      {
         insert.add(colName, propValue);
      }

      Query insertQuery = insert.build();
      error = connection->execute(insertQuery);

      if (error)
         return sessionPropError("Error while inserting new session", sessionId_, properties, error, ERROR_LOCATION);
   }

   transaction.commit();
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
      return sessionPropError("Error while deleting session metadata", sessionId_, error, ERROR_LOCATION);

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
