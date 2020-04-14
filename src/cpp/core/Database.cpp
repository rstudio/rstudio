/*
 * Database.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/Database.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/format.hpp>
#include <boost/regex.hpp>

#include <core/FileSerializer.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>

#include <soci/row-exchange.h>
#include <soci/postgresql/soci-postgresql.h>
#include <soci/sqlite3/soci-sqlite3.h>

// Database Boost Errors
// Declare soci errors as boost errors.
// =================================================================================================================
namespace RSTUDIO_BOOST_NAMESPACE {
namespace system {

template <>
struct is_error_code_enum<soci::soci_error::error_category>
{
   static const bool value = true;
};

} // namespace system
} // namespace boost

namespace rstudio {
namespace core {
namespace database {
   const boost::system::error_category& databaseErrorCategory();
}
}
}

namespace soci {

inline boost::system::error_code make_error_code(soci::soci_error::error_category e)
{
   return { e, rstudio::core::database::databaseErrorCategory() };
}

inline boost::system::error_condition make_error_condition(soci::soci_error::error_category e)
{
   return { e, rstudio::core::database::databaseErrorCategory() };
}

}

namespace rstudio {
namespace core {
namespace database {

class DatabaseErrorCategory : public boost::system::error_category
{
public:
   const char* name() const BOOST_NOEXCEPT override;

   std::string message(int ev) const override;
};

const boost::system::error_category& databaseErrorCategory()
{
   static DatabaseErrorCategory databaseErrorCategoryConst;
   return databaseErrorCategoryConst;
}

const char* DatabaseErrorCategory::name() const BOOST_NOEXCEPT
{
   return "database";
}

std::string DatabaseErrorCategory::message(int ev) const
{
   switch (ev)
   {
      case soci::soci_error::error_category::connection_error:
         return "Connection Error";
      case soci::soci_error::error_category::invalid_statement:
         return "Invalid Statement";
      case soci::soci_error::error_category::no_privilege:
         return "No Privilege";
      case soci::soci_error::error_category::no_data:
         return "No Data";
      case soci::soci_error::error_category::constraint_violation:
         return "Constraint Violation";
      case soci::soci_error::error_category::unknown_transaction_state:
         return "Unknown Transaction State";
      case soci::soci_error::error_category::system_error:
         return "System Error";
      case soci::soci_error::error_category::unknown:
      default:
         return "Unknown Error";
   }
}

#define DatabaseError(sociError) Error(sociError.get_error_category(), sociError.get_error_message(), ERROR_LOCATION);

// Database errors =================================================================================================

class ConnectVisitor : public boost::static_visitor<Error>
{
public:
   ConnectVisitor(boost::shared_ptr<IConnection>* pPtrConnection) :
      pPtrConnection_(pPtrConnection)
   {
   }

   Error operator()(const SqliteConnectionOptions& options) const
   {
      try
      {
         boost::shared_ptr<IConnection> pConnection(new Connection(soci::sqlite3, "dbname=\"" + options.file + "\""));

         // foreign keys must explicitly be enabled for sqlite
         Error error = pConnection->executeStr("PRAGMA foreign_keys = ON;");
         if (error)
            return error;

         *pPtrConnection_ = pConnection;
         return Success();
      }
      catch (soci::soci_error& error)
      {
         return DatabaseError(error);
      }
   }

   Error operator()(const PostgresqlConnectionOptions& options) const
   {
      try
      {
         boost::format fmt("host='%1%' port='%2%' dbname='%3%' user='%4%' password='%5%' connect_timeout='%6%'");
         std::string connectionStr =
               boost::str(fmt %
                          options.host %
                          options.port %
                          options.database %
                          options.user %
                          options.password %
                          safe_convert::numberToString(options.connectionTimeoutSeconds, "0"));

         boost::shared_ptr<IConnection> pConnection(new Connection(soci::postgresql, connectionStr));
         *pPtrConnection_ = pConnection;
         return Success();
      }
      catch (soci::soci_error& error)
      {
         return DatabaseError(error);
      }
   }

private:
   boost::shared_ptr<IConnection>* pPtrConnection_;
};

Query::Query(const std::string& sqlStatement,
             soci::session& session) :
   statement_(session)
{
   // it's possible that prepare can throw a database exception, but we
   // do not want to surface errors until execute() is called
   try
   {
      statement_.alloc();
      statement_.prepare(sqlStatement);
   }
   catch (soci::soci_error& error)
   {
      prepareError_ = error;
   }
}

RowsetIterator Rowset::begin()
{
   if (query_)
      return RowsetIterator(query_.get().statement_, row_);

   return end();
}

RowsetIterator Rowset::end()
{
   return RowsetIterator();
}

Connection::Connection(const soci::backend_factory& factory,
                       const std::string& connectionStr) :
   session_(factory, connectionStr)
{
}

Query Connection::query(const std::string& sqlStatement)
{
   return Query(sqlStatement, session_);
}

Error Connection::execute(Query& query,
                          bool* pDataReturned)
{
   if (query.prepareError_)
      return DatabaseError(query.prepareError_.get());

   try
   {
      query.statement_.define_and_bind();
      bool result = query.statement_.execute(true);

      if (pDataReturned)
         *pDataReturned = result;

      query.statement_.bind_clean_up();

      return Success();
   }
   catch (soci::soci_error& error)
   {
      return DatabaseError(error);
   }
}

Error Connection::execute(Query& query,
                          Rowset& rowset)
{
   if (query.prepareError_)
      return DatabaseError(query.prepareError_.get());

   try
   {
      query.statement_.define_and_bind();
      query.statement_.exchange_for_rowset(soci::into(rowset.row_));
      query.statement_.execute(false);

      rowset.query_ = query;

      return Success();
   }
   catch (soci::soci_error& error)
   {
      return DatabaseError(error);
   }
}

Error Connection::executeStr(const std::string& queryStr)
{
   try
   {
      // SOCI backends do not necessarily support running multiple statements
      // in one invocation - to work around this, we split any passed in SQL
      // into one invocation per SQL statement (delimited by ;)
      std::vector<std::string> queries;
      boost::regex regex(";[ \\t\\r\\f\\v]*\\n");
      std::string queryStrCopy = queryStr;
      boost::regex_split(std::back_inserter(queries), queryStrCopy, regex);
      for (std::string& query : queries)
      {
         query = string_utils::trimWhitespace(query);
         if (!query.empty())
            session_ << query;
      }

      return Success();
   }
   catch (soci::soci_error& error)
   {
      return DatabaseError(error);
   }
}

std::string Connection::driverName() const
{
   return session_.get_backend_name();
}

PooledConnection::PooledConnection(const boost::shared_ptr<ConnectionPool>& pool,
                                   const boost::shared_ptr<Connection>& connection) :
   pool_(pool),
   connection_(connection)
{
}

PooledConnection::~PooledConnection()
{
   pool_->returnConnection(connection_);
}

Query PooledConnection::query(const std::string& sqlStatement)
{
   return connection_->query(sqlStatement);
}

Error PooledConnection::execute(Query& query,
                                Rowset& rowset)
{
   return connection_->execute(query, rowset);
}

Error PooledConnection::execute(Query& query,
                                bool* pDataReturned)
{
   return connection_->execute(query, pDataReturned);
}

Error PooledConnection::executeStr(const std::string& queryStr)
{
   return connection_->executeStr(queryStr);
}

std::string PooledConnection::driverName() const
{
   return connection_->driverName();
}

boost::shared_ptr<IConnection> ConnectionPool::getConnection()
{
   // block until a connection is available, but log an error
   // if this takes a long time, because we want to ensure that if we are in a hang
   // condition (i.e. threads are not properly returning connections to the pool) we
   // let the users/developers know that something is fishy
   boost::shared_ptr<Connection> connection;
   while (true)
   {
      if (connections_.deque(&connection, boost::posix_time::seconds(30)))
      {
         // create wrapper PooledConnection around retrieved Connection
         return boost::shared_ptr<IConnection>(new PooledConnection(shared_from_this(), connection));
      }
      else
      {
         LOG_ERROR_MESSAGE("Potential hang detected: could not get database connection from pool "
                           "after 30 seconds. If issue persists, please notify RStudio Support");
      }
   }
}

bool ConnectionPool::getConnection(const boost::posix_time::time_duration& maxWait,
                                   boost::shared_ptr<IConnection>* pConnection)
{
   boost::shared_ptr<Connection> connection;
   if (!connections_.deque(&connection, maxWait))
      return false;

   pConnection->reset(new PooledConnection(shared_from_this(), connection));
   return true;
}

void ConnectionPool::returnConnection(const boost::shared_ptr<Connection>& connection)
{
   connections_.enque(connection);
}

Transaction::Transaction(const boost::shared_ptr<IConnection>& connection) :
   connection_(connection),
   transaction_(connection->session())
{
}

void Transaction::commit()
{
   transaction_.commit();
}

void Transaction::rollback()
{
   transaction_.rollback();
}

SchemaUpdater::SchemaUpdater(const boost::shared_ptr<IConnection>& connection,
                             const FilePath& migrationsPath) :
   connection_(connection),
   migrationsPath_(migrationsPath)
{
}

Error SchemaUpdater::migrationFiles(std::vector<FilePath>* pMigrationFiles)
{
   std::vector<FilePath> children;
   Error error = migrationsPath_.getChildren(children);
   if (error)
      return error;

   for (const FilePath& file : children)
   {
      std::string extension = file.getExtensionLowerCase();
      if (extension == SQL_EXTENSION ||
          extension == SQLITE_EXTENSION ||
          extension == POSTGRESQL_EXTENSION)
      {
         pMigrationFiles->push_back(file);
      }
   }

   return Success();
}

Error SchemaUpdater::highestMigrationVersion(std::string* pVersion)
{
   std::vector<FilePath> files;
   Error error = migrationFiles(&files);
   if (error)
      return error;

   if (files.empty())
   {
      // no migration files - we do not consider this an error, but instead
      // simply consider that this database cannot be migrated past version 0
      *pVersion = "0";
      return Success();
   }

   // sort descending - highest version filename wins
   auto comparator = [](const FilePath& a, const FilePath& b)
   {
      return a.getStem() > b.getStem();
   };
   std::sort(files.begin(), files.end(), comparator);

   *pVersion = files.at(0).getStem();
   return Success();
}

Error SchemaUpdater::isSchemaVersionPresent(bool* pIsPresent)
{
   std::string queryStr;
   if (connection_->driverName() == SQLITE_DRIVER)
   {
      queryStr = std::string("SELECT COUNT(1) FROM sqlite_master WHERE type='table' AND name='") + SCHEMA_TABLE + "'";
   }
   else if (connection_->driverName() == POSTGRESQL_DRIVER)
   {
      queryStr = std::string("SELECT COUNT(1) FROM information_schema.tables WHERE table_name='") + SCHEMA_TABLE +
                 "' AND table_schema = current_schema";
   }
   else
   {
      return DatabaseError(soci::soci_error("Unsupported database driver"));
   }

   int count = 0;
   Query query = connection_->query(queryStr)
         .withOutput(count);
   Error error = connection_->execute(query);
   if (error)
      return error;

   *pIsPresent = count > 0;
   return Success();
}

Error SchemaUpdater::databaseSchemaVersion(std::string* pVersion)
{
   bool versionPresent = false;
   Error error = isSchemaVersionPresent(&versionPresent);
   if (error)
      return error;

   std::string currentSchemaVersion = "0";
   if (!versionPresent)
   {
      // no schema version present - add the table to the database so it is available
      // for updating whenever migrations occur
      error = connection_->executeStr(std::string("CREATE TABLE \"") + SCHEMA_TABLE + "\" (current_version text)");
      if (error)
         return error;

      Query query = connection_->query(std::string("INSERT INTO \"") + SCHEMA_TABLE + "\" VALUES (:val)")
            .withInput(currentSchemaVersion);
      error = connection_->execute(query);
      if (error)
         return error;

      *pVersion = currentSchemaVersion;
      return Success();
   }

   Query query = connection_->query(std::string("SELECT current_version FROM \"") + SCHEMA_TABLE + "\"")
         .withOutput(currentSchemaVersion);

   error = connection_->execute(query);
   if (error)
      return error;

   *pVersion = currentSchemaVersion;
   return Success();
}

Error SchemaUpdater::isUpToDate(bool* pUpToDate)
{
   std::string version;
   Error error = databaseSchemaVersion(&version);
   if (error)
      return error;

   std::string migrationVersion;
   error = highestMigrationVersion(&migrationVersion);
   if (error)
      return error;

   *pUpToDate = version >= migrationVersion;
   return Success();
}

Error SchemaUpdater::update()
{
   std::string migrationVersion;
   Error error = highestMigrationVersion(&migrationVersion);
   if (error)
      return error;

   std::string currentVersion;
   error = databaseSchemaVersion(&currentVersion);
   if (currentVersion < migrationVersion)
      return updateToVersion(migrationVersion);
   else
      return Success();
}

Error SchemaUpdater::updateToVersion(const std::string& maxVersion)
{
   // create a transaction to perform the following steps:
   // 1. Check the current database schema version
   // 2. Check if we need to update
   // 3. Update (if necessary)
   // 4. Save new database schema version
   // performing this in a transaction ensures that we rollback if anything
   // fails, and also ensures that other nodes cannot update concurrently
   Transaction transaction(connection_);

   // for postgresql, specifically lock the version table in exclusive mode
   // to ensure that no other connection can use the version table AT ALL
   // during this schema update
   if (connection_->driverName() == POSTGRESQL_DRIVER)
   {
      Query query = connection_->query(std::string("LOCK \"") + SCHEMA_TABLE + "\" IN ACCESS EXCLUSIVE MODE");
      Error error = connection_->execute(query);
      if (error)
         return error;
   }

   std::string currentVersion;
   Error error = databaseSchemaVersion(&currentVersion);
   if (error)
      return error;

   if (currentVersion >= maxVersion)
      return Success();

   std::vector<FilePath> files;
   error = migrationFiles(&files);
   if (error)
      return error;

   // sort ascending
   auto comparator = [](const FilePath& a, const FilePath& b)
   {
      return a.getStem() < b.getStem();
   };
   std::sort(files.begin(), files.end(), comparator);

   for (const FilePath& migrationFile : files)
   {
      // if the version has already been applied (database version is newer or same)
      // then skip this particular migration
      if (migrationFile.getStem() <= currentVersion)
         continue;

      // if the migration file version is higher than the max specified version, we're done
      if (migrationFile.getStem() > maxVersion)
         break;

      bool applyMigration = false;

      if (migrationFile.getExtensionLowerCase() == SQL_EXTENSION)
      {
         // plain sql - apply the migration
         applyMigration = true;
      }
      else if (migrationFile.getExtensionLowerCase() == SQLITE_EXTENSION)
      {
         // sqlite file - only apply migration if we are connected to a SQLite database
         applyMigration = connection_->driverName() == SQLITE_DRIVER;
      }
      else if (migrationFile.getExtensionLowerCase() == POSTGRESQL_EXTENSION)
      {
         // postgresql file - only apply migration if we are connected to a PostgreSQL database
         applyMigration = connection_->driverName() == POSTGRESQL_DRIVER;
      }

      if (!applyMigration)
         continue;

      // we are clear to apply the migration
      // load the file and execute its SQL contents
      std::string fileContents;
      error = readStringFromFile(migrationFile, &fileContents);
      if (error)
         return error;

      error = connection_->executeStr(fileContents);
      if (error)
         return error;

      // record the new version in the version table
      std::string version = migrationFile.getStem();
      Query updateVersionQuery = connection_->query(std::string("UPDATE \"") + SCHEMA_TABLE + "\" SET current_version = (:ver)")
            .withInput(version);
      error = connection_->execute(updateVersionQuery);
      if (error)
         return error;
   }

   transaction.commit();
   return Success();
}

Error connect(const ConnectionOptions& options,
              boost::shared_ptr<IConnection>* pPtrConnection)
{
   return boost::apply_visitor(ConnectVisitor(pPtrConnection), options);
}

Error createConnectionPool(size_t poolSize,
                           const ConnectionOptions& options,
                           boost::shared_ptr<ConnectionPool>* pPool)
{
   pPool->reset(new ConnectionPool());

   for (size_t i = 0; i < poolSize; ++i)
   {
      boost::shared_ptr<IConnection> connection;
      Error error = connect(options, &connection);
      if (error)
      {
         // destroy the pool, which will free each previously created connections
         pPool->reset();
         return error;
      }

      // add connection to the pool
      (*pPool)->returnConnection(boost::static_pointer_cast<Connection>(connection));
   }

   return Success();
}

} // namespace database
} // namespace core
} // namespace rstudio
