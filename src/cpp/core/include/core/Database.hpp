/*
 * Database.hpp
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

#ifndef CORE_DATABASE_HPP
#define CORE_DATABASE_HPP

#include <core/Thread.hpp>
#include <shared_core/FilePath.hpp>

#include <boost/optional.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/variant.hpp>

#define SOCI_USE_BOOST 1
#include <soci/soci.h>

namespace rstudio {
namespace core {
namespace database {

struct SqliteConnectionOptions
{
   SqliteConnectionOptions(const std::string& file) : file(file), readonly(false) {}
   SqliteConnectionOptions() : readonly(false) {}
   std::string file;
   bool readonly;
};

struct PostgresqlConnectionOptions
{
   std::string database;
   std::string host;
   std::string port;
   std::string username;
   std::string password;
   std::string connectionUri;
   int connectionTimeoutSeconds;
   std::string secureKey;
};

enum class Driver
{
   Sqlite,
   Postgresql,
   Unknown
};

static constexpr const char* SQLITE_DRIVER = "sqlite3";
static constexpr const char* POSTGRESQL_DRIVER = "postgresql";

typedef boost::variant<SqliteConnectionOptions, PostgresqlConnectionOptions> ConnectionOptions;
using InputParameter = soci::details::use_type_ptr;
using OutputParameter = soci::details::into_type_ptr;

class Connection;
class ConnectionPool;
class Transaction;

class Query
{
public:
   Query(const std::string& sqlStatement,
         soci::session& session);

   template <typename T>
   Query& withInput(const T& val)
   {
      statement_.exchange(soci::use(val));
      return *this;
   }

   template <typename T>
   Query& withInput(const T& val, const std::string& varName)
   {
      statement_.exchange(soci::use(val, varName));
      return *this;
   }

   template <typename T>
   Query& withOutput(T& out)
   {
      statement_.exchange(soci::into(out));
      return *this;
   }

   template <typename T>
   Query& withOutput(T& out, const std::string& varName)
   {
      statement_.exchange(soci::into(out, varName));
      return *this;
   }

private:
   friend class Connection;
   friend class Rowset;

   soci::statement statement_;
   boost::optional<soci::soci_error> prepareError_;
};

using Row = soci::row;
using RowsetIterator = soci::rowset_iterator<Row>;

class Rowset
{
public:
   RowsetIterator begin();
   RowsetIterator end();

private:
   friend class Connection;

   Row row_;
   boost::optional<Query&> query_;
};

class IConnection
{
public:
   virtual Query query(const std::string& sqlStatement) = 0;

   virtual Error execute(Query& query,
                         Rowset& rowset) = 0;

   virtual Error execute(Query& query,
                         bool* pDataReturned = nullptr) = 0;

   virtual Error executeStr(const std::string& queryStr) = 0;

   Driver driver() const
   {
      std::string driverStr = driverName();
      if (driverStr == SQLITE_DRIVER)
         return Driver::Sqlite;
      else if (driverStr == POSTGRESQL_DRIVER)
         return Driver::Postgresql;
      else
         return Driver::Unknown;
   }

   virtual std::string driverName() const = 0;

   virtual soci::session& session() = 0;
};

class Connection : public IConnection
{
public:
   virtual ~Connection() {}

   Query query(const std::string& sqlStatement) override;

   Error execute(Query& query,
                 Rowset& rowset) override;

   Error execute(Query& query,
                 bool* pDataReturned = nullptr) override;

   Error executeStr(const std::string& queryStr) override;

   std::string driverName() const override;

   soci::session& session() override { return session_; }

private:
   friend class ConnectVisitor;
   friend class Transaction;

   // private constructor - use global connect function
   Connection(const soci::backend_factory& factory,
              const std::string& connectionStr);

   soci::session session_;
};

class PooledConnection : public IConnection
{
public:
   virtual ~PooledConnection();

   Query query(const std::string& sqlStatement) override;

   Error execute(Query& query,
                 Rowset& rowset) override;

   Error execute(Query& query,
                 bool* pDataReturned = nullptr) override;

   Error executeStr(const std::string& queryStr) override;

   std::string driverName() const override;

   soci::session& session() override { return connection_->session(); }

private:
   friend class ConnectionPool;

   // private constructor - get PooledConnection from ConnectionPool
   PooledConnection(const boost::shared_ptr<ConnectionPool>& pool,
                    const boost::shared_ptr<Connection>& connection);

   boost::shared_ptr<ConnectionPool> pool_;
   boost::shared_ptr<Connection> connection_;
};

class ConnectionPool : public boost::enable_shared_from_this<ConnectionPool>
{
public:
   // get a connection from the connection pool, blocking until one becomes available
   boost::shared_ptr<IConnection> getConnection();

   // get a connection from the connection pool, waiting for at most maxWait for one
   // to become available. if no connection becomes available, false is returned and
   // the connection is empty, otherwise the connection is set and true is returned
   bool getConnection(const boost::posix_time::time_duration& maxWait,
                      boost::shared_ptr<IConnection>* pConnection);

private:
   friend class PooledConnection;
   friend Error createConnectionPool(size_t poolSize,
                                     const ConnectionOptions& options,
                                     boost::shared_ptr<ConnectionPool>* pPool);

   void returnConnection(const boost::shared_ptr<Connection>& connection);

   thread::ThreadsafeQueue<boost::shared_ptr<Connection>> connections_;
};

class Transaction
{
public:
   Transaction(const boost::shared_ptr<IConnection>& connection);

   void commit();
   void rollback();

   // when this class goes out of scope, the transaction
   // is automatically aborted if not previously committed

private:
   boost::shared_ptr<IConnection> connection_;
   soci::transaction transaction_;
};

class SchemaUpdater
{
public:
   SchemaUpdater(const boost::shared_ptr<IConnection>& connection,
                 const FilePath& migrationsPath);

   // updates the database schema to the highest version
   Error update();

   // updates the database schema to the specified version if it is contained
   // within the migration schemas, or no higher than that version if it is not present
   Error updateToVersion(const std::string& maxVersion);

   // returns whether or not the schema is up-to-date with the latest schema version
   Error isUpToDate(bool* pUpToDate);

   // gets the current database schema version
   Error databaseSchemaVersion(std::string* pVersion);

private:
   static constexpr const char* SCHEMA_TABLE = "schema_version";
   static constexpr const char* SQL_EXTENSION = ".sql";
   static constexpr const char* SQLITE_EXTENSION = ".sqlite";
   static constexpr const char* POSTGRESQL_EXTENSION = ".postgresql";

   // returns whether or not a schema version is present in the database
   Error isSchemaVersionPresent(bool* pIsPresent);

   // returns the highest version that can be migrated to with the migrations
   // specified when constructing this SchemaUpdater
   Error highestMigrationVersion(std::string* pVersion);

   // gets the actual migration files from the migration path
   Error migrationFiles(std::vector<FilePath>* pMigrationFiles);

   boost::shared_ptr<IConnection> connection_;
   FilePath migrationsPath_;
};

// validates connection options - used for test purposes only
Error validateOptions(const ConnectionOptions& options,
                      std::string* pConnectionStr);

// connect to the database with the specified connection options
Error connect(const ConnectionOptions& options,
              boost::shared_ptr<IConnection>* pPtrConnection);

// create a pool of database connections with the specified connection options
// the pool will create/establish multiple connections with the database and
// will only be returned if every connection was successful
Error createConnectionPool(size_t poolSize,
                           const ConnectionOptions& options,
                           boost::shared_ptr<ConnectionPool>* pPool);

} // namespace database
} // namespace core
} // namespace rstudio


#endif // CORE_DATABASE_HPP

