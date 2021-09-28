/*
 * Database.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include <core/FileSerializer.hpp>
#include <core/http/Util.hpp>
#include <core/RegexUtils.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/Crypto.hpp>

#include <soci/row-exchange.h>
#include <soci/postgresql/soci-postgresql.h>
#include <soci/sqlite3/soci-sqlite3.h>

#include "config.h"

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

namespace system {
namespace crypto {
   // stubs for pro-only code
   Error decryptPassword(const std::string& secureKey, const std::string& keyHash, std::string& password)
   {
      return Success();
   }

   bool passwordContainsKeyHash(const std::string& password)
   {
      return false;
   }
} // namespace crypto
} // namespace system

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
   ConnectVisitor(bool validateOnly,
                  boost::shared_ptr<IConnection>* pPtrConnection,
                  std::string* pConnectionStr = nullptr,
                  std::string* pPassword = nullptr) :
      validateOnly_(validateOnly),
      pPtrConnection_(pPtrConnection),
      pConnectionStr_(pConnectionStr),
      pPassword_(pPassword)
   {
   }

   Error operator()(const SqliteConnectionOptions& options) const
   {
      std::string readonly = options.readonly ? " readonly=true" : "";
      std::string connectionStr = "shared_cache=true" + readonly + " dbname=\"" + options.file + "\"";
      if (pConnectionStr_)
         *pConnectionStr_ = connectionStr;

      // no validation for sqlite as it is not configurable
      if (validateOnly_)
         return Success();

      try
      {
         boost::shared_ptr<IConnection> pConnection(new Connection(soci::sqlite3, connectionStr));

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
         std::string connectionStr;

         // prefer connection-uri
         std::string password;
         if (!options.connectionUri.empty())
         {
            Error error = parseConnectionUri(options.connectionUri, password, &connectionStr);
            if (error)
               return error;
         }
         else
         {
            boost::format fmt("host='%1%' port='%2%' dbname='%3%' user='%4%' connect_timeout='%5%'");
            connectionStr =
                  boost::str(fmt %
                             options.host %
                             options.port %
                             options.database %
                             options.username %
                             safe_convert::numberToString(options.connectionTimeoutSeconds, "0"));
         }

         Error error = getPassword(options, password);
         if (error)
            return error;

         // Make the password part of the connection string
         // unless requested to be returned as-is separately
         if (!pPassword_)
         {
            password = pgEncode(password, false);
            connectionStr += " password='" + password + "'";
         }
         else
            *pPassword_ = password;

         if (pConnectionStr_)
            *pConnectionStr_ = connectionStr;

         if (validateOnly_)
            return Success();

         boost::shared_ptr<IConnection> pConnection(new Connection(soci::postgresql, connectionStr));
         *pPtrConnection_ = pConnection;
         return Success();
      }
      catch (soci::soci_error& error)
      {
         return DatabaseError(error);
      }
   }

   Error parseConnectionUri(const std::string& uri,
                            std::string& password,
                            std::string* pConnectionStr) const
   {
      boost::regex re("(postgres|postgresql)://([^/#?]+)(.*)", boost::regex::icase);
      boost::cmatch matches;

      std::string host, path;
      if (regex_utils::match(uri.c_str(), matches, re))
      {
         host = matches[2];
         path = matches[3];
      }
      else
      {
         return systemError(boost::system::errc::invalid_argument,
                            "connection-uri specified is not a valid PostgreSQL connection URI",
                            ERROR_LOCATION);
      }

      // extract user and password information
      std::string user;
      std::vector<std::string> hostParts;
      boost::split(hostParts, host, boost::is_any_of("@"));

      if (hostParts.size() == 2)
      {
         // user information included
         std::vector<std::string> userParts;
         boost::split(userParts, hostParts.at(0), boost::is_any_of(":"));

         if (userParts.size() == 2)
         {
            user = userParts.at(0);
            password = userParts.at(1);
         }
         else if (userParts.size() == 1)
         {
            user = userParts.at(0);
         }
         else
         {
            return systemError(boost::system::errc::invalid_argument,
                               "connection-uri specified is not a valid PostgreSQL connection URI - "
                                  "too many user : password specifications",
                               ERROR_LOCATION);
         }

         host = hostParts.at(1);
      }
      else if (hostParts.size() > 2)
      {
         return systemError(boost::system::errc::invalid_argument,
                            "connection-uri specified is not a valid PostgreSQL connection URI - "
                               "too many user @ host specifications",
                            ERROR_LOCATION);
      }

      // extract host and port information
      std::string port;
      hostParts.clear();

      size_t squareBegin = host.find('[');
      if (squareBegin != std::string::npos)
      {
         size_t squareEnd = host.find(']');
         if (squareEnd == std::string::npos)
         {
            return systemError(boost::system::errc::invalid_argument,
                               "connection-uri specified is not a valid PostgreSQL connection URI - "
                                  "specified IPv6 address has no matching end bracket ']'",
                               ERROR_LOCATION);
         }

         std::string ip6Host = host.substr(0, squareEnd + 1);
         size_t colonPos = host.find(':', squareEnd + 1);
         if (colonPos != std::string::npos)
         {
            port = host.substr(colonPos + 1);
         }
         host = ip6Host;
      }
      else
      {
         boost::split(hostParts, host, boost::is_any_of(":"));

         if (hostParts.size() == 2)
         {
            host = hostParts.at(0);
            port = hostParts.at(1);
         }
         else if (hostParts.size() > 2)
         {
            return systemError(boost::system::errc::invalid_argument,
                               "connection-uri specified is not a valid PostgreSQL connection URI - "
                                  "too many host : port specifications",
                               ERROR_LOCATION);
         }
      }

      // extract database name and params
      std::string database;
      std::vector<std::string> parameters;
      size_t paramStart = path.find("?");
      if (paramStart != std::string::npos)
      {
         std::string params = path.substr(paramStart + 1);
         std::vector<std::string> paramParts;
         boost::split(paramParts, params, boost::is_any_of("&"));

         for (const std::string& param : paramParts)
         {
            parameters.push_back(param);
         }

         // skip over / in the path
         database = path.substr(1, paramStart - 1);
      }
      else
      {
         // skip over / in the path
         database = path.empty() ? path : path.substr(1);
      }

      // write out connection string
      *pConnectionStr += "host='" + pgEncode(host) + "'";
      if (!port.empty())
         *pConnectionStr += " port='" + pgEncode(port) + "'";
      if (!user.empty())
         *pConnectionStr += " user='" + pgEncode(user) + "'";
      if (!database.empty())
         *pConnectionStr += " dbname='" + pgEncode(database) + "'";

      for (const std::string& param : parameters)
      {
         size_t equalPos = param.find('=');
         if (equalPos != std::string::npos)
         {
            std::string paramName = param.substr(0, equalPos);
            std::string paramValue = param.substr(equalPos + 1);
            *pConnectionStr += " " +  paramName + "='" + pgEncode(paramValue) + "'";
         }
         else
         {
            return systemError(boost::system::errc::invalid_argument,
                               "connection-uri specified is not a valid PostgreSQL connection URI - "
                                  "no parameter value specified for parameter " + param,
                               ERROR_LOCATION);
         }
      }

      return Success();
   }

   Error getPassword(const PostgresqlConnectionOptions& options, std::string& password) const
   {
      // override password from the input with the one from options if any
      if (!options.password.empty())
         password = options.password;

      // Somewhat convoluted due to need to handle several cases (Pro-only):
      //
      // (1) password without embedded encryption key; this could be a plain-text
      //     password or an encrypted password generated before we added such embedding, but
      //     we can't be sure without trying to decrypt and treating as plain text if that fails
      // (2) an encrypted password with embedded key hash; if it won't decrypt, this is an error
      //     and we don't want to treat as plain text
      //
      // In a future release we could simplify by assuming a password without embedded key must
      // be plain text. Tracked in https://github.com/rstudio/rstudio-pro/issues/2446
      // 

      bool assumeEncrypted = core::system::crypto::passwordContainsKeyHash(password);

      Error error = core::system::crypto::decryptPassword(options.secureKey, options.secureKeyHash, password);
      if (error)
      {
         static bool warnOnce = false;

         if (assumeEncrypted)
            return error;

         // decrypt failed, we'll just use the password as-is
         if (!warnOnce)
         {
            warnOnce = true;
            LOG_DEBUG_MESSAGE(error.asString());
            LOG_WARNING_MESSAGE("A plain text value is potentially being used for the PostgreSQL password, or an encrypted password could not be decrypted. The RStudio Server documentation for PostgreSQL shows how to encrypt this value.");
         }
      }
      return Success();
   }

   std::string pgEncode(const std::string& str,
                        bool isUrl = true) const
   {
      // ensure we first decode from URL string format
      std::string val = isUrl ? http::util::urlDecode(str) : str;

      // escape postgres special characters
      boost::replace_all(val, "\\", "\\\\");
      boost::replace_all(val, "'", "\\'");

      return val;
   }

private:
   bool validateOnly_;
   boost::shared_ptr<IConnection>* pPtrConnection_;
   std::string* pConnectionStr_;
   std::string* pPassword_;
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

size_t Rowset::columnCount() const
{
   return row_.size();
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

ConnectionPool::ConnectionPool(const ConnectionOptions& options) :
   connectionOptions_(options)
{
}

void ConnectionPool::testAndReconnect(boost::shared_ptr<Connection>& connection)
{
   // do not test Sqlite connections - there is no backend system to connect to in this case
   // so any errors on the file handle itself we do not want to gracefully recover from, as they would
   // indicate a very serious programming error
   if (connection->driver() == Driver::Sqlite)
      return;

   // it is possible for connections to go stale (such as if the upstream connection is closed)
   // which will prevent it from being usable - we test for this by running a very efficient query
   // and checking to make sure that no error has occurred
   Error error = connection->executeStr("SELECT 1");
   if (!error)
      return;

   error.addProperty("description", "Connection check query failed when getting connection from the pool");
   LOG_ERROR(error);

   // a connection error has occurred - attempt to reopen the connection by throwing this one away
   // and replacing it with a new one
   boost::shared_ptr<IConnection> newConnection;
   error = connect(connectionOptions_, &newConnection);
   if (error)
   {
      // could not re-establish connection - simply log an error
      // future attempts to use this connection will be responsible for further attempts
      error.addProperty("description", "Could not re-establish database connection");
      LOG_ERROR(error);
      return;
   }

   connection = boost::static_pointer_cast<Connection>(newConnection);
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
         // test connection to ensure it is still alive
         testAndReconnect(connection);

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

   // test connection to ensure it is still alive
   testAndReconnect(connection);

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

SchemaVersion::SchemaVersion(std::string date, std::string flower) :
   Date(std::move(date)),
   Flower(std::move(flower))
{
}

SchemaVersion::SchemaVersion(SchemaVersion&& other) :
   Date(std::move(other.Date)),
   Flower(std::move(other.Flower))
{
}

SchemaVersion::SchemaVersion(const SchemaVersion& other) :
   Date(other.Date),
   Flower(other.Flower)
{
}

bool SchemaVersion::isEmpty() const
{
   return Date.empty() && Flower.empty();
}

std::string SchemaVersion::toString() const
{
   return Date + "_" + Flower;
}

SchemaVersion& SchemaVersion::operator=(const SchemaVersion& other)
{
   if (this != &other)
   {
      Date = other.Date;
      Flower = other.Flower;
   }
   return *this;
}

SchemaVersion& SchemaVersion::operator=(SchemaVersion&& other)
{
   if (this != &other)
   {
      Date = std::move(other.Date);
      Flower = std::move(other.Flower);
   }
   return *this;
}

bool SchemaVersion::operator<(const SchemaVersion& other) const
{
   if (*this == other)
      return false;

   if (isEmpty() && !other.isEmpty())
      return true;

   if (other.isEmpty())
      return false;

   const auto& versions = versionMap();
   int thisFlowerIndex = (versions.find(Flower) != versions.end()) ? versions.at(Flower) : -1;
   int otherFlowerIndex = (versions.find(other.Flower) != versions.end()) ? versions.at(other.Flower) : -1;

   if (thisFlowerIndex < otherFlowerIndex)
      return true;
   else if (otherFlowerIndex < thisFlowerIndex)
      return false;

   // If the date is empty, we should treat this like "the latest version at this flower"
   if (Date.empty() && !other.Date.empty())
      return false;

   if (other.Date.empty())
      return true;

   if (Date < other.Date)
      return true;

   return false;
}

bool SchemaVersion::operator<=(const SchemaVersion& other) const
{
   return (*this == other) || (*this < other);
}

bool SchemaVersion::operator>(const SchemaVersion& other) const
{
   return (other < *this);
}

bool SchemaVersion::operator>=(const SchemaVersion& other) const
{
   return !(*this < other);
}

bool SchemaVersion::operator==(const SchemaVersion& other) const
{
   return (this == &other) || ((Date == other.Date) && (Flower == other.Flower));
}

const std::map<std::string, int>& SchemaVersion::versionMap()
{
   static boost::mutex m;
   static std::map<std::string, int> versions;

   // Check if the map is empty before locking the mutex to avoid the cost of
   // locking on every access But if it _is_ empty, lock and then double check
   // that it's still empty before modifying it.
   if (versions.empty())
   {
      LOCK_MUTEX(m)
      {
         if (versions.empty())
         {
            versions[""] = 0;
            versions["Ghost Orchid"] = 1;
         }
      }
      END_LOCK_MUTEX
   }

   return versions;
}

SchemaUpdater::SchemaUpdater(const boost::shared_ptr<IConnection>& connection,
                             const FilePath& migrationsPath) :
   connection_(connection),
   migrationsPath_(migrationsPath)
{
}

Error SchemaUpdater::migrationFiles(std::vector<std::pair<SchemaVersion, FilePath> >* pMigrationFiles)
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
         SchemaVersion version;
         if (parseVersionOfFile(file, &version))
         {
            pMigrationFiles->emplace_back(version, file);
         }
      }
   }

   // sort descending - highest version filename wins
   auto comparator = [](const std::pair<SchemaVersion, FilePath>& a,
                        const std::pair<SchemaVersion, FilePath>& b)
   { return a.first > b.first; };
   std::sort(pMigrationFiles->begin(), pMigrationFiles->end(), comparator);

   return Success();
}

Error SchemaUpdater::highestMigrationVersion(SchemaVersion* pVersion)
{
   std::vector<std::pair<SchemaVersion, FilePath> > files;
   Error error = migrationFiles(&files);
   if (error)
      return error;

   if (files.empty())
   {
      // no migration files - we do not consider this an error, but instead
      // simply consider that this database cannot be migrated past version 0
      return Success();
   }

   *pVersion = files.at(0).first;
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
   Query query = connection_->query(queryStr).withOutput(count);
   Error error = connection_->execute(query);
   if (error)
      return error;

   *pIsPresent = count > 0;
   return Success();
}

Error SchemaUpdater::getSchemaTableColumnCount(int* pColumnCount)
{
   int columnCount = 0;
   Error error;
   if (connection_->driverName() == SQLITE_DRIVER)
   {
      // This query is explicity a SELECT * because we use the # of columns to determine if 
      // we're pre- or post- GhostOrchid
      Query query = connection_->query(std::string("SELECT * FROM ") + SCHEMA_TABLE);
      Rowset rows;
      error = connection_->execute(query, rows);
      columnCount = rows.columnCount();
   }
   else
   {
      Query query = connection_->query(std::string("SELECT COUNT(1) FROM information_schema.columns WHERE table_name='") + SCHEMA_TABLE +
                 "' AND table_schema = current_schema")
                 .withOutput(columnCount);
      error = connection_->execute(query);
   }

   if (error)
   {
      error.addProperty("query", connection_->session().get_last_query());
      return error;   
   }

   *pColumnCount = columnCount;
   return Success();
}

bool SchemaUpdater::parseVersionOfFile(const FilePath& file, SchemaVersion* pVersion)
{
   std::string fileStem = file.getStem();
   if (fileStem == CREATE_TABLES_STEM)
      return false;

   std::vector<std::string> split;
   boost::split(split, fileStem, boost::is_any_of("_"));
   if (split.size() != 3)
   {
     if (split.size() == 2)
         LOG_DEBUG_MESSAGE("Not applying sql schema file from previous release: " + file.getAbsolutePath());
     else
         LOG_DEBUG_MESSAGE("Not applying unrecognized sql schema file: " + file.getAbsolutePath());
      return false;
   }

   *pVersion = SchemaVersion(split[0], boost::replace_all_copy(split[1], "-", " "));

   return true;
}

Error SchemaUpdater::databaseSchemaVersion(SchemaVersion* pVersion)
{
   SchemaVersion version;
   int schemaColumnCount = 0;
   Error error = getSchemaTableColumnCount(&schemaColumnCount);
   if (error)
      return error;

   static const std::string currentVersionCol = "current_version";
   static const std::string releaseNameCol = "release_name";
   std::string stmt;
   if (schemaColumnCount == 2)
      stmt = std::string("SELECT " + currentVersionCol + ", " + releaseNameCol + " FROM \"") + SCHEMA_TABLE + "\"";
   else
      stmt = std::string("SELECT " + currentVersionCol + " FROM \"") + SCHEMA_TABLE + "\"";

   Query query = connection_->query(stmt).withOutput(version.Date);
   if (schemaColumnCount == 2)
      query.withOutput(version.Flower);

   error = connection_->execute(query);
   if (error)
      return error;

   // Previously the table name was included in the schema version - parse it out.
   if (schemaColumnCount == 1)
   {
      std::vector<std::string> split;
      boost::split(split, version.Date, boost::is_any_of("_"));
      if (split.size() >= 1)
         version.Date = split[0];
   }

   *pVersion = version;
   return Success();
}

Error SchemaUpdater::isUpToDate(bool* pUpToDate)
{
   SchemaVersion version;
   Error error = databaseSchemaVersion(&version);
   if (error)
      return error;

   SchemaVersion migrationVersion;
   error = highestMigrationVersion(&migrationVersion);
   if (error)
      return error;

   *pUpToDate = version >= migrationVersion;
   return Success();
}

Error SchemaUpdater::update()
{
   bool schemaPresent = false;
   Error error = isSchemaVersionPresent(&schemaPresent);
   if (error)
      return error;

   if (schemaPresent)
   {
      SchemaVersion migrationVersion;
      error = highestMigrationVersion(&migrationVersion);
      if (error)
         return error;

      SchemaVersion currentVersion;
      error = databaseSchemaVersion(&currentVersion);
      if (currentVersion < migrationVersion)
      {
         LOG_INFO_MESSAGE(
            "Updating database schema version from version " +
            currentVersion.toString() +
            " to version " +
            migrationVersion.toString());
         return updateToVersion(migrationVersion);
      }
      else
      {
         LOG_INFO_MESSAGE("Database schema version is up to date.");
         return Success();
      }
   }
   else
   {
      LOG_INFO_MESSAGE("Database schema has not been created yet. Creating database schema...");
      return createSchema();
   }
}

Error SchemaUpdater::createSchema()
{
   Transaction transaction(connection_);

   FilePath createTablesFile;
   Error error = migrationsPath_.completeChildPath(std::string(CREATE_TABLES_STEM) + std::string(SQL_EXTENSION), createTablesFile);

   if (error || !createTablesFile.exists())
   {
      if (connection_->driverName() == POSTGRESQL_DRIVER)
      {
         error = migrationsPath_.completeChildPath(std::string(CREATE_TABLES_STEM) + std::string(POSTGRESQL_EXTENSION), createTablesFile);
         if (error)
            return error;
      }
      else
      {
         error = migrationsPath_.completeChildPath(std::string(CREATE_TABLES_STEM) + std::string(SQLITE_EXTENSION), createTablesFile);
         if (error)
            return error;
      }
   }

   std::string fileContents;
   error = readStringFromFile(createTablesFile, &fileContents);
   if (error)
      return error;

   error = connection_->executeStr(fileContents);
   if (error)
      return error;

   transaction.commit();
   return Success();
}

Error SchemaUpdater::updateToVersion(const SchemaVersion& maxVersion)
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

   SchemaVersion currentVersion;
   Error error = databaseSchemaVersion(&currentVersion);
   if (error)
      return error;

   if (currentVersion >= maxVersion)
      return Success();

   std::vector<std::pair<SchemaVersion, FilePath> > files;
   error = migrationFiles(&files);
   if (error)
      return error;

   for (const std::pair<SchemaVersion, FilePath>& migrationFile : files)
   {

      bool applyMigration = false;

      if (migrationFile.second.getExtensionLowerCase() == SQL_EXTENSION)
      {
         // plain sql - apply the migration
         applyMigration = true;
      }
      else if (migrationFile.second.getExtensionLowerCase() == SQLITE_EXTENSION)
      {
         // sqlite file - only apply migration if we are connected to a SQLite database
         applyMigration = connection_->driverName() == SQLITE_DRIVER;
      }
      else if (migrationFile.second.getExtensionLowerCase() == POSTGRESQL_EXTENSION)
      {
         // postgresql file - only apply migration if we are connected to a PostgreSQL database
         applyMigration = connection_->driverName() == POSTGRESQL_DRIVER;
      }

      if (!applyMigration)
         continue;

      // we are clear to apply the migration
      // load the file and execute its SQL contents
      std::string fileContents;
      error = readStringFromFile(migrationFile.second, &fileContents);
      if (error)
         return error;

      error = connection_->executeStr(fileContents);
      if (error)
         return error;
   }

   transaction.commit();
   return Success();
}

Error validateOptions(const ConnectionOptions& options,
                      std::string* pConnectionStr,
                      std::string* pPassword /*= nullptr*/)
{
   return boost::apply_visitor(ConnectVisitor(true, nullptr, pConnectionStr, pPassword), options);
}

Error connect(const ConnectionOptions& options,
              boost::shared_ptr<IConnection>* pPtrConnection)
{
   return boost::apply_visitor(ConnectVisitor(false, pPtrConnection), options);
}

Error createConnectionPool(size_t poolSize,
                           const ConnectionOptions& options,
                           boost::shared_ptr<ConnectionPool>* pPool)
{
   pPool->reset(new ConnectionPool(options));

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
