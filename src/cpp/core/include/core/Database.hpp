/*
 * Database.hpp
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

#ifndef CORE_DATABASE_HPP
#define CORE_DATABASE_HPP

#include <core/LogOptions.hpp>
#include <core/Thread.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include <boost/assign.hpp>
#include <boost/optional.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/variant.hpp>

#include <functional>
#include <map>
#include <sstream>
#include <vector>

#define SOCI_USE_BOOST 1
#include <soci/soci.h>

namespace rstudio {
namespace core {
namespace database {

Error getDatabaseError(const soci::soci_error& sociError, const ErrorLocation& in_location);
#define DatabaseError(sociError) getDatabaseError(sociError, ERROR_LOCATION);

struct SqliteConnectionOptions
{
   SqliteConnectionOptions(const std::string& file) : file(file), readonly(false) {}
   SqliteConnectionOptions() : readonly(false) {}
   std::string file;
   int poolSize;
   bool readonly;
   bool autoCreate;
   core::FilePath databaseDirectory;
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
   int poolSize;
   std::string secureKey; // obfuscated secure-key value
   std::string secureKeyFileUsed; // absolute path to file containing the key, for troubleshooting
   std::string secureKeyHash; // hash of secureKey (pre-obfuscation)
   bool autoCreate = false;
};

// Default options for an empty or an unread configuration
struct ProviderNotSpecifiedConnectionOptions
{
   bool enabled = false;
};

enum class Driver
{
   Sqlite,
   Postgresql,
   Unknown
};

namespace detail {
template <typename T, typename U>
typename std::enable_if<!std::is_convertible<T, U>::value, T>::type maybe_cast(U)
{
   throw std::bad_cast();
}

template <typename T, typename U>
typename std::enable_if<std::is_convertible<T, U>::value, T>::type maybe_cast(U val)
{
   return val;
}
} // namespace detail

static constexpr const char* SQLITE_DRIVER = "sqlite3";
static constexpr const char* POSTGRESQL_DRIVER = "postgresql";

typedef boost::variant<SqliteConnectionOptions, PostgresqlConnectionOptions, ProviderNotSpecifiedConnectionOptions> ConnectionOptions;
using InputParameter = soci::details::use_type_ptr;
using OutputParameter = soci::details::into_type_ptr;

class Connection;
class ConnectionPool;
class Transaction;

/**
 * Add parameter names that should be treated as secrets and not logged in full.
 * Secret values are logged as "xxx...secret-len=N" instead of the actual value.
 * By default, "secret_param" is already registered as a secret param name.
 *
 * @param paramNames List of parameter names (without the : prefix) to treat as secrets
 */
void addSecretParamNames(const std::vector<std::string>& paramNames);

/**
 * Check if a parameter name should be treated as a secret.
 */
bool isSecretParamName(const std::string& paramName);

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
      if (log::isDbTraceEnabled())
         recordDebugParam(varName, val);
      statement_.exchange(soci::use(val, varName));
      return *this;
   }

   /*
   * @brief Use when you want to conditionally pass a value or NULL based on a boolean flag
   * @param val The value to pass (will be ignored if isNull is true, but needed for type deduction)
   * @param isNull If true, NULL will be passed to the database; if false, val will be used
   * @param varName The named parameter in the SQL statement
   */
   template <typename T>
   Query& withInputOrNull(const T& val, bool isNull, const std::string& varName)
   {
      if (isNull)
      {
         if (log::isDbTraceEnabled())
            debugParams_[varName] = "NULL";
         static T dummyValueWithLifetime = T();
         static soci::indicator nullWithLifetime = soci::i_null;

         statement_.exchange(soci::use(dummyValueWithLifetime, nullWithLifetime, varName));
      }
      else
      {
         if (log::isDbTraceEnabled())
            recordDebugParam(varName, val);
         statement_.exchange(soci::use(val, varName));
      }
      return *this;
   }

   /*
   * @brief Use when the value in a database statement may be NULL
   */
   template <typename T>
   Query & withOptionalInput(const boost::optional<T>& optionalValue, const std::string& varName)
   {
      static T dummyValueWithLifetime = T();
      static soci::indicator nullWithLifetime = soci::i_null;

      if (optionalValue.has_value())
      {
         if (log::isDbTraceEnabled())
            recordDebugParam(varName, optionalValue.value());
         statement_.exchange(soci::use(optionalValue.value(), varName));
      }
      else
      {
         if (log::isDbTraceEnabled())
            debugParams_[varName] = "NULL";
         statement_.exchange(soci::use(dummyValueWithLifetime, nullWithLifetime, varName));
      }
      return *this;
   }

   template <typename T>
   Query& withOutput(T& out)
   {
      if (log::isDbTraceEnabled())
         recordOutputFormatter("", &out);
      statement_.exchange(soci::into(out));
      return *this;
   }

   template <typename T>
   Query& withOutput(T& out, const std::string& varName)
   {
      if (log::isDbTraceEnabled())
         recordOutputFormatter(varName, &out);
      statement_.exchange(soci::into(out, varName));
      return *this;
   }

   /**
    * Gets the number of rows affected by UPDATE, DELETE, or INSERT statements.
    * Does not work for SELECT statements.
    */
   long long getAffectedRows();

   /**
    * Logs the output values that were bound via withOutput() after execute() has been called.
    * Only logs if trace-db-enabled is set in logging.conf. Call this after execute() to see the output values.
    */
   void logOutputValues() const;

   // Returns the SQL statement used in this query
   std::string sqlStatement() const { return sqlStatement_; }

   /**
    * Returns a compact query identifier like "select:jobs:1234" based on operation type,
    * table name, and thread ID. Useful for correlating log entries.
    */
   std::string queryId() const;

   // Returns a debug string with named parameters substituted with their values,
   // prefixed by the query ID for easy correlation
   std::string debugString() const;

private:
   friend class Connection;
   friend class Rowset;

   // Helper to record a debug parameter value as a string
   template <typename T>
   void recordDebugParam(const std::string& name, const T& val)
   {
      std::ostringstream oss;
      oss << val;
      debugParams_[name] = oss.str();
   }

   // Type-erased output formatter - stores name and a function to format the value
   struct OutputFormatter
   {
      std::string name;
      std::function<std::string()> formatter;
   };

   // Helper to record an output formatter that can be invoked after execute()
   template <typename T>
   void recordOutputFormatter(const std::string& name, T* ptr)
   {
      OutputFormatter fmt;
      fmt.name = name;
      fmt.formatter = [ptr]() -> std::string {
         std::ostringstream oss;
         oss << *ptr;
         return oss.str();
      };
      debugOutputFormatters_.push_back(std::move(fmt));
   }

   // Specialization for vector types to format as array
   template <typename T>
   void recordOutputFormatter(const std::string& name, std::vector<T>* ptr)
   {
      OutputFormatter fmt;
      fmt.name = name;
      fmt.formatter = [ptr]() -> std::string {
         std::ostringstream oss;
         oss << "[";
         for (size_t i = 0; i < ptr->size(); ++i)
         {
            if (i > 0) oss << ", ";
            if (i >= 10 && ptr->size() > 12)
            {
               oss << "... (" << (ptr->size() - 10) << " more)";
               break;
            }
            oss << (*ptr)[i];
         }
         oss << "]";
         return oss.str();
      };
      debugOutputFormatters_.push_back(std::move(fmt));
   }

   soci::statement statement_;
   boost::optional<soci::soci_error> prepareError_;
   std::string sqlStatement_;
   std::map<std::string, std::string> debugParams_;  // Only populated when debug enabled
   std::vector<OutputFormatter> debugOutputFormatters_;  // Only populated when debug enabled
};

using Row = soci::row;
using RowsetIterator = soci::rowset_iterator<Row>;

// Helper function to format a soci::row for debug logging
// Returns a string like: {col1: "value1", col2: 123, col3: NULL}
std::string debugRowString(const Row& row);

class Rowset
{
public:
   Rowset() = default;
   ~Rowset();

   // Disable copy and move - Row (soci::row) is non-copyable and non-movable
   Rowset(const Rowset&) = delete;
   Rowset& operator=(const Rowset&) = delete;
   Rowset(Rowset&&) = delete;
   Rowset& operator=(Rowset&&) = delete;

   /*
   * Warning - SOCI rowset iterators, which these are typing, are single-pass input iterators.
   *    You cannot safely copy and advance multiple iterators independently.
   *    You cannot reliably count rows by advancing iterators multiple times.
   *    You cannot use std::distance(begin, end) or end - begin, to count rows.
   */
   RowsetIterator begin();
   RowsetIterator end();

   /**
    * Increments the internal row counter and returns the provided iterator.
    * Use this when iterating to enable row counting for debug logging.
    * Example: for (auto it = rows.begin(); it != rows.end(); rows.next(++it)) { ... }
    * Or simply iterate normally - the destructor will log row count if debug is enabled.
    */
   RowsetIterator& next(RowsetIterator& it);

   /**
    * Gets the number of rows that have been iterated over so far.
    * This is only accurate if next() was called during iteration,
    * or after iteration is complete.
    */
   size_t rowCount() const { return rowCount_; }

   size_t columnCount() const;

   /*
   * @brief Get a NULLable value from a row as a boost::optional<T>
   * @details Converts soci::error exceptions and std::exceptions into core::Error.
   * The wrapped SOCI library supports the following C++ types:
   * Commonly Supported Types
   *    int
   *    long
   *    long long
   *    double
   *    std::string
   *    std::tm (for date/time)
   *    char
   *    short
   *    unsigned long (sometimes, but not always safe)
   *    std::vector<T> (for bulk operations, not for get<T> on a single row)
   * Not reliably supported, may cause SOCI to throw or cause undefined behavior
   *    Unsigned types (unsigned int, unsigned short, etc.)
   * Also, be aware that the DB column type must match up with the requested type T.
   * If the column is an integer type in the DB, but T is a long long, this can cause a bad_cast
   * exception in the underlying library. See Utility functions for conversions.
   *
   * @param row - iterator to the row from which to extract the value.
   *              IMPORTANT - It is left to the caller to ensure the iterator is valid!
   * @param columnName - Name of the column from which to retrieve the value
   * @param result - If the value was NULL boost::optional containing boost::none.
   *                 Otherwise, the optional contains value obtained from the row
   * @return core::Error indicating error from the underlying library. This can occur if the
   *    column did not exist or if an unsupported type was used. Otherwise, core::Success()
   */
   template <typename T>
   static core::Error getOptionalValue(RowsetIterator & row, const std::string& columnName, boost::optional<T> * result)
   {
      *result = boost::none;

      soci::data_type dt;
      try
      {
         // SOCI library has find_column private, so we'll check if it exists by whether or not the following call throws
         dt = row->get_properties(columnName).get_data_type();
      }
      catch(soci::soci_error & e)
      {
         // Column did not exist
         return DatabaseError(e);
      }

      if (row->get_indicator(columnName) != soci::i_null)
      {
         try
         {
            if (dt == soci::dt_double)
               *result = detail::maybe_cast<T>(row->get<double>(columnName));
            else if (dt == soci::dt_integer)
               *result = detail::maybe_cast<T>(row->get<int>(columnName));
            else if (dt == soci::dt_long_long)
               *result = detail::maybe_cast<T>(row->get<long long>(columnName));
            else if (dt == soci::dt_unsigned_long_long)
               *result = detail::maybe_cast<T>(row->get<unsigned long long>(columnName));
            else
               *result = row->get<T>(columnName);
         }
         catch (soci::soci_error & e)
         {
            return core::Error(boost::system::errc::invalid_argument,
               "soci::exception from the underlying library when getting value for column: " + columnName + " . " + e.what(), ERROR_LOCATION);
         }
         catch(std::exception & e)
         {
            // This is likely from an unsupported type T
            return core::Error(boost::system::errc::invalid_argument,
               "std::exception from the underlying library when getting value for column: " + columnName + " . " + e.what(), ERROR_LOCATION);
         }
      }

      return core::Success();
   }

   /*
   * @brief Get a value from a row that should not be NULL
   * @details Converts soci::error exceptions and std::exceptions into core::Error.
   * The wrapped SOCI library supports the following C++ types:
   *    int
   *    long
   *    long long
   *    double
   *    std::string
   *    std::tm (for date/time)
   *    char
   *    short
   *    unsigned long (sometimes, but not always safe)
   *    std::vector<T> (for bulk operations, not for get<T> on a single row)
   *  Not reliably supported, may cause SOCI to throw or cause undefined behavior
   *    Unsigned types (unsigned int, unsigned short, etc.)
   * Also, be aware that the DB column type must match up with the requested type T.
   * If the column is an integer type in the DB, but T is a long long, this can cause a bad_cast
   * exception in the underlying library. See Utility functions for conversions.
   *
   * @param row - iterator to the row from which to extract the value.
   *              IMPORTANT - It is left to the caller to ensure the iterator is valid!
   * @param columnName - Name of the column from which to retrieve the value
   * @param result - If the value was NULL boost::optional containing boost::none.
   *                 Otherwise, the optional contains value obtained from the row
   * @return core::Error indicating error from the underlying library. This can occur if the
   *    column did not exist or if an unsupported type was used. Otherwise, core::Success()
   */
   template <typename T>
   static core::Error getValue(RowsetIterator & row, const std::string& columnName, T * result)
   {
      boost::optional<T> optionalValue;
      core::Error error = getOptionalValue<T>(row, columnName, &optionalValue);
      if (error)
      {
         return error;
      }

      if( !optionalValue.has_value() )
      {
         return core::Error(boost::system::errc::invalid_argument, "Column " + columnName + " is NULL, but was expected to be NOT NULL", ERROR_LOCATION);
      }

      *result = optionalValue.value();
      return core::Success();
   }

   // The following are utility functions for obtaining value types that are not directly supported by the underlying SOCI library
   // The boost::optional overloads are for when a DB value may be NULL
   static core::Error getBoolStrValue(RowsetIterator & row, const std::string& columnName, bool * result);
   static core::Error getBoolStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<bool> * result);
   static core::Error getUIntIntValue(RowsetIterator & row, const std::string& columnName, unsigned int * result);
   static core::Error getUIntIntValue(RowsetIterator & row, const std::string& columnName, boost::optional<unsigned int> * result);
   static core::Error getMillisecondSinceEpochStrValue(RowsetIterator & row, const std::string& columnName, boost::posix_time::ptime * result);
   static core::Error getMillisecondSinceEpochStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<boost::posix_time::ptime> * result);
   static core::Error getISO8601StrValue(RowsetIterator & row, const std::string& columnName, boost::posix_time::ptime * result);
   static core::Error getISO8601StrValue(RowsetIterator & row, const std::string& columnName, boost::optional<boost::posix_time::ptime> * result);
   static core::Error getFilepathStrValue(RowsetIterator & row, const std::string& columnName, core::FilePath * result);
   static core::Error getFilepathStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<core::FilePath> * result);
   static core::Error getJSONStrValue(RowsetIterator & row, const std::string& columnName, core::json::Object * result);
   static core::Error getJSONStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<core::json::Object> * result);
   static core::Error getJSONStrValue(RowsetIterator & row, const std::string& columnName, core::json::Array * result);
   static core::Error getJSONStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<core::json::Array> * result);

private:
   friend class Connection;

   Row row_;
   boost::optional<Query&> query_;
   size_t rowCount_ = 0;           // Count of rows iterated (for trace logging)
   std::string queryId_;           // Stored for trace logging in destructor (compact query ID)
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

typedef boost::shared_ptr<IConnection> DatabaseConnection;
typedef std::function<DatabaseConnection()> DatabaseConnectionFactory;

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
   ConnectionPool(const ConnectionOptions& options);

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
   bool testAndReconnect(boost::shared_ptr<Connection>& connection);

   thread::ThreadsafeQueue<boost::shared_ptr<Connection> > connections_;
   ConnectionOptions connectionOptions_;
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

struct SchemaVersion {
   public:
      SchemaVersion() = default;
      SchemaVersion(std::string date, std::string flower);
      SchemaVersion(const SchemaVersion& other);
      SchemaVersion(SchemaVersion&& other);

      std::string Date;
      std::string Flower;

      std::string toString() const;

      bool isEmpty() const;

      SchemaVersion& operator=(const SchemaVersion& other);
      SchemaVersion& operator=(SchemaVersion&& other);

      bool operator<(const SchemaVersion& other) const;
      bool operator<=(const SchemaVersion& other) const;
      bool operator>(const SchemaVersion& other) const;
      bool operator>=(const SchemaVersion& other) const;
      bool operator==(const SchemaVersion& other) const;
      bool operator!=(const SchemaVersion& other) const;

   private:
      static const std::map<std::string, int>& versionMap();
};

// Stream operator for SchemaVersion
std::ostream& operator<<(std::ostream& os, const rstudio::core::database::SchemaVersion& version);

class SchemaUpdater
{
public:
   SchemaUpdater(const boost::shared_ptr<IConnection>& connection,
                 const FilePath& migrationsPath);

   // updates the database schema to the highest version
   Error update();

   // returns whether or not the schema is up-to-date with the latest schema version
   Error isUpToDate(bool* pUpToDate);

   // gets the current database schema version
   Error databaseSchemaVersion(SchemaVersion* pVersion);

private:
   static constexpr const char* SCHEMA_TABLE = "schema_version";
   static constexpr const char* SQL_EXTENSION = ".sql";
   static constexpr const char* SQLITE_EXTENSION = ".sqlite";
   static constexpr const char* POSTGRESQL_EXTENSION = ".postgresql";
   static constexpr const char* CREATE_TABLES_STEM = "CreateTables";
   static constexpr const char* ALTER_TABLES_STEM = "AlterTables";

   // returns whether or not a schema version is present in the database
   Error isSchemaVersionPresent(bool* pIsPresent);

   Error getSchemaTableColumnCount(std::size_t* pColumnCount);

   // returns the highest version that can be migrated to with the migrations
   // specified when constructing this SchemaUpdater
   Error highestMigrationVersion(SchemaVersion* pVersion);

   // gets the actual migration files from the migration path
   Error migrationFiles(std::vector<std::pair<SchemaVersion, FilePath> >* pMigrationFiles);

   Error createSchema();

   // updates the database schema to the specified version if it is contained
   // within the migration schemas, or no higher than that version if it is not present
   Error updateToVersion(const SchemaVersion& maxVersion);

   bool parseVersionOfFile(const FilePath& file, SchemaVersion* pVersion);

   boost::shared_ptr<IConnection> connection_;
   FilePath migrationsPath_;
};

// Gets the password and postgresql connection string from the specified
// connection options
Error parsePostgresqlConnectionOptions(const PostgresqlConnectionOptions& options,
                                       std::string* pConnectionString,
                                       std::string* pPassword);

// validates connection options - used for test purposes only
Error validateOptions(const ConnectionOptions& options,
                      std::string* pConnectionStr,
                      std::string* pPassword = nullptr);

// connect to the database with the specified connection options
Error connect(const ConnectionOptions& options,
              boost::shared_ptr<IConnection>* pPtrConnection);

// create a pool of database connections with the specified connection options
// the pool will create/establish multiple connections with the database and
// will only be returned if every connection was successful
Error createConnectionPool(size_t poolSize,
                           const ConnectionOptions& options,
                           boost::shared_ptr<ConnectionPool>* pPool);

// execute a provided query and pass each row to the rowHandler
Error execAndProcessQuery(boost::shared_ptr<database::IConnection> pConnection,
                          const std::string& sql,
                          const boost::function<void(const database::Row&)>& rowHandler =
                             boost::function<void(const database::Row&)>());

// uses soci::indicator to safely parse a string value from a row
std::string getRowStringValue(const Row& row, const std::string& column);

} // namespace database
} // namespace core
} // namespace rstudio


#endif // CORE_DATABASE_HPP

