/*
 * Database.cpp
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

#include <core/Database.hpp>

#include <set>

#include <boost/algorithm/string.hpp>
#include <boost/format.hpp>
#include <boost/thread.hpp>

#include <core/DateTime.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/Util.hpp>
#include <core/Log.hpp>
#include <core/RegexUtils.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/Crypto.hpp>

#include <soci/row-exchange.h>
#include <soci/sqlite3/soci-sqlite3.h>

#ifdef RSTUDIO_HAS_SOCI_POSTGRESQL
# include <soci/postgresql/soci-postgresql.h>
#endif


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

Error getDatabaseError(const soci::soci_error& sociError, const ErrorLocation& in_location)
{
   // The value() for the connection_error enum is 0 which looks like Success if used in Error
   if (sociError.get_error_category() == soci::soci_error::connection_error)
      return Error(boost::system::errc::not_connected, sociError.get_error_message(), in_location);
   return Error(sociError.get_error_category(), sociError.get_error_message(), in_location);
}

static constexpr size_t kMaxTraceParamValueLength = 128;

// Number of characters to show at start and end of truncated values
static constexpr size_t kTruncatePreviewLength = 16;

// Set of parameter names that should be treated as secrets (not logged in full)
static std::set<std::string> s_secretParamNames = {"secret_param", "key"};

void addSecretParamNames(const std::vector<std::string>& paramNames)
{
   for (const auto& name : paramNames)
   {
      s_secretParamNames.insert(name);
   }
}

bool isSecretParamName(const std::string& paramName)
{
   return s_secretParamNames.find(paramName) != s_secretParamNames.end();
}

// Threshold in milliseconds for logging slow queries when debug logging is enabled
static constexpr double kSlowQueryThresholdMs = 100.0;

// Maximum number of retries for transient SQLite locking errors
static constexpr int kMaxSqliteRetries = 3;

// Delay between retries in milliseconds (will be multiplied by retry attempt)
static constexpr int kSqliteRetryDelayMs = 100;

/**
 * @brief Checks if a SOCI error is a transient SQLite locking error that can be retried.
 *
 * SQLite can return "database is locked" or "database table is locked" errors when there
 * is contention between readers and writers. These are often transient and can be resolved
 * by waiting and retrying.
 *
 * @param error The SOCI error to check
 * @return true if the error is a transient locking error that should be retried
 */
bool isSqliteTransientLockError(const soci::soci_error& error)
{
   std::string msg = error.get_error_message();
   // Check for various SQLite locking error messages
   // "database is locked" - general database lock
   // "database table is locked" - table-level lock
   // These typically indicate SQLITE_BUSY or SQLITE_LOCKED conditions
   return (msg.find("is locked") != std::string::npos ||
           msg.find("SQLITE_BUSY") != std::string::npos ||
           msg.find("SQLITE_LOCKED") != std::string::npos);
}

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
      // suppress unused value warning
      (void) pPassword_;
   }

   Error operator()(const SqliteConnectionOptions& options) const
   {
      std::string readonly = options.readonly ? "readonly=true " : "";
      // Note: shared_cache is intentionally NOT used here. SQLite's shared-cache mode introduces
      // table-level locks that undermine WAL mode's concurrency benefits (readers blocking writers
      // and vice versa within the same process). With WAL mode, each connection having its own cache
      // provides better read/write concurrency.
      // Note: busy_timeout is set via PRAGMA below rather than the SOCI timeout parameter to keep
      // all SQLite configuration in one place and avoid conflicting timeout values.
      std::string connectionStr = readonly + "dbname=\"" + options.file + "\"";
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

         // enable WAL mode to improve read/write concurrency
         // WAL allows readers and writers to proceed concurrently
         if (!options.readonly)
         {
            error = pConnection->executeStr("PRAGMA journal_mode = WAL;");
            if (error)
               return error;

            // Set synchronous to NORMAL for better write performance while still being safe with WAL
            // NORMAL is safe for WAL mode - data is still protected against corruption
            error = pConnection->executeStr("PRAGMA synchronous = NORMAL;");
            if (error)
               return error;
         }

         // Set busy timeout to handle transient locks - wait up to 30 seconds
         // This is important for high-concurrency scenarios where multiple connections
         // may be accessing the database simultaneously
         error = pConnection->executeStr("PRAGMA busy_timeout = 30000;");
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
#ifdef RSTUDIO_HAS_SOCI_POSTGRESQL
      std::string connectionStr;
      try
      {
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

         if (!password.empty())
         {
            // Make the password part of the connection string
            // unless requested to be returned as-is separately
            if (!pPassword_)
            {
               // unencrypted password
               password = pgEncode(password, false);
               connectionStr += " password='" + password + "'";
            }
            else
               *pPassword_ = password;
         }
         else
         {
            // When a password isn't specified, we authenticate using SSL certificates.
            // This requires that the connection string contain sslcert and sslkey parameters and that sslmode=verify-ca.
            if (!boost::algorithm::contains(connectionStr, "sslcert") || 
                !boost::algorithm::contains(connectionStr, "sslkey") ||
                !boost::algorithm::contains(connectionStr, "sslrootcert"))
            {
               // Return invalid configuration error
               return systemError(boost::system::errc::invalid_argument,
                                  "Because a password has not been specified in database.conf,"
                                  " the Postgres connection must be configured to use SSL authentication."
                                  " This requires including the path to sslcert, sslkey, and sslrootcert in the connection URI."
                                  " Update database.conf to add these values to the connection URI or add a password."
                                  " For more information about PostgreSQL SSL Authentication see https://www.postgresql.org/docs/current/auth-cert.html",
                                  ERROR_LOCATION);
            }
            else if (!boost::algorithm::contains(connectionStr, "sslmode=verify-ca")) 
            {
               connectionStr += " sslmode=verify-ca";
            }
         }

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
#else
      return Error(boost::system::errc::operation_not_supported, ERROR_LOCATION);
#endif
   }

   Error operator()(const ProviderNotSpecifiedConnectionOptions&) const
   {
      // No provider specified, so we cannot connect
      return systemError(boost::system::errc::invalid_argument,
                         "No database connection options specified",
                         ERROR_LOCATION);
   }

   Error parseConnectionUri(const std::string& uri,
                            std::string& password,
                            std::string* pConnectionStr) const
   {
#ifdef RSTUDIO_HAS_SOCI_POSTGRESQL
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
#else
      return Error(boost::system::errc::operation_not_supported, ERROR_LOCATION);
#endif
   }

   Error getPassword(const PostgresqlConnectionOptions& options, std::string& password) const
   {
#ifdef RSTUDIO_HAS_SOCI_POSTGRESQL
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
         {
            error.addProperty("key-files", "/var/lib/rstudio-server/secure-cookie-key (managed), /etc/rstudio/secure-cookie-key (manual using XDG_CONFIG_DIRS)");
            return error;
         }

         // decrypt failed, we'll just use the password as-is
         if (!warnOnce)
         {
            warnOnce = true;
            LOG_DEBUG_MESSAGE(error.asString());
            LOG_WARNING_MESSAGE("A plain text value is potentially being used for the PostgreSQL password, or an encrypted password could not be decrypted. The RStudio Server documentation for PostgreSQL shows how to encrypt this value.");
         }
      }
      return Success();
#else
      return Error(boost::system::errc::operation_not_supported, ERROR_LOCATION);
#endif
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
   statement_(session),
   sqlStatement_(sqlStatement)
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

std::string Query::queryId() const
{
   // Extract operation type from SQL
   std::string sql = sqlStatement_;
   boost::to_lower(sql);
   boost::trim(sql);

   std::string op;
   if (boost::starts_with(sql, "select")) op = "q";
   else if (boost::starts_with(sql, "insert")) op = "i";
   else if (boost::starts_with(sql, "update")) op = "u";
   else if (boost::starts_with(sql, "delete")) op = "d";
   else if (boost::starts_with(sql, "create")) op = "c";
   else if (boost::starts_with(sql, "alter")) op = "alt";
   else if (boost::starts_with(sql, "drop")) op = "drp";
   else op = "sql";

   // Get thread ID - use lower bits for brevity
   std::ostringstream tidStream;
   tidStream << boost::this_thread::get_id();
   std::string tidStr = tidStream.str();
   // Take last 4 hex chars for brevity
   if (tidStr.length() > 4)
      tidStr = tidStr.substr(tidStr.length() - 4);

   return op + "-" + tidStr;
}

std::string Query::debugString() const
{
   std::string prefix = "[" + queryId() + "] ";

   if (debugParams_.empty())
      return prefix + sqlStatement_;

   std::string result = sqlStatement_;
   for (const auto& param : debugParams_)
   {
      // Replace :paramName with the value
      // We search for :name followed by a non-identifier character or end of string
      // SQL identifiers can contain alphanumeric characters and underscores
      std::string placeholder = ":" + param.first;
      size_t pos = 0;
      while ((pos = result.find(placeholder, pos)) != std::string::npos)
      {
         // Check that this is a complete parameter name (not a prefix of another)
         size_t endPos = pos + placeholder.length();
         bool isCompleteMatch = (endPos >= result.length()) ||
            (!std::isalnum(static_cast<unsigned char>(result[endPos])) && result[endPos] != '_');

         if (isCompleteMatch)
         {
            std::string value = param.second;

            // Check if this is a secret parameter - mask it
            if (isSecretParamName(param.first) && value.length() > 4)
            {
               value = value.substr(0, 3) + "...(size=" + std::to_string(value.length()) + ")";
            }
            // Truncate long values (over 128 chars) to avoid log bloat
            else if (value.length() > kMaxTraceParamValueLength)
            {
               value = value.substr(0, kTruncatePreviewLength) + "...(size=" +
                       std::to_string(value.length()) + ")..." +
                       value.substr(value.length() - kTruncatePreviewLength);
            }

            // Format the value - quote strings, leave numbers as-is
            if (value != "NULL" && !boost::starts_with(value, "xxx...secret"))
            {
               // Simple heuristic: if it's not a number, quote it
               bool isNumeric = !value.empty() &&
                  std::all_of(value.begin(), value.end(), [](char c) {
                     return std::isdigit(static_cast<unsigned char>(c)) || c == '.' || c == '-';
                  });
               if (!isNumeric)
                  value = "'" + value + "'";
            }
            result.replace(pos, placeholder.length(), value);
            pos += value.length();
         }
         else
         {
            pos += placeholder.length();
         }
      }
   }
   return prefix + result;
}

long long Query::getAffectedRows()
{
   long long affectedRows = statement_.get_affected_rows();
   if (log::isDbTraceEnabled())
   {
      std::string qid = "[" + queryId() + "]";
      if (affectedRows == 0)
         LOG_TRACE_MESSAGE("db" + qid + " => 0 rows affected");
      else if (affectedRows == 1)
         LOG_TRACE_MESSAGE("db" + qid + " => 1 row affected");
      else
         LOG_TRACE_MESSAGE("db" + qid + " => " + std::to_string(affectedRows) + " rows affected");
   }
   return affectedRows;
}

void Query::logOutputValues() const
{
   if (!log::isDbTraceEnabled() || debugOutputFormatters_.empty())
      return;

   std::string qid = "[" + queryId() + "]";
   std::ostringstream oss;
   oss << "db" << qid << " => {";
   bool first = true;
   for (const auto& fmt : debugOutputFormatters_)
   {
      if (!first) oss << ", ";
      first = false;
      if (!fmt.name.empty())
         oss << fmt.name << ": ";
      oss << fmt.formatter();
   }
   oss << "}";
   LOG_TRACE_MESSAGE(oss.str());
}

std::string debugRowString(const Row& row)
{
   std::ostringstream oss;
   oss << "{";
   for (size_t i = 0; i < row.size(); ++i)
   {
      if (i > 0) oss << ", ";

      const soci::column_properties& props = row.get_properties(i);
      const std::string& name = props.get_name();
      oss << name << ": ";

      if (row.get_indicator(i) == soci::i_null)
      {
         oss << "NULL";
      }
      else
      {
         try
         {
            // Try to get as string first (most common case)
            switch (props.get_data_type())
            {
               case soci::dt_string:
                  {
                     std::string value = row.get<std::string>(i);

                     if (isSecretParamName(name) && value.length() > 4)
                     {
                        value = value.substr(0, 3) + "...(size=" + std::to_string(value.length()) + ")";
                     }
                     else if (value.length() > kMaxTraceParamValueLength)
                     {
                        value = value.substr(0, kTruncatePreviewLength) + "...(size=" +
                                std::to_string(value.length()) + ")..." +
                                value.substr(value.length() - kTruncatePreviewLength);
                     }
                     oss << "\"" << value << "\"";
                  }
                  break;
               case soci::dt_integer:
                  oss << row.get<int>(i);
                  break;
               case soci::dt_long_long:
                  oss << row.get<long long>(i);
                  break;
               case soci::dt_double:
                  oss << row.get<double>(i);
                  break;
               default:
                  // For other types, try string representation
                  oss << "\"" << row.get<std::string>(i) << "\"";
                  break;
            }
         }
         catch (std::exception& e)
         {
            oss << "<error>: " << e.what();
         }
      }
   }
   oss << "}";
   return oss.str();
}

Rowset::~Rowset()
{
   if (log::isDbTraceEnabled() && query_.has_value())
   {
      LOG_TRACE_MESSAGE("db[" + queryId_ + "] => " + std::to_string(rowCount_) + " rows read");
   }
}

RowsetIterator& Rowset::next(RowsetIterator& it)
{
   if (it != end())
   {
      ++rowCount_;

      if (log::isDbTraceEnabled() && query_.has_value() &&
          query_.get().debugOutputFormatters_.empty())
      {
         LOG_TRACE_MESSAGE("db[" + queryId_ + "] row " + std::to_string(rowCount_) + ": " +
                           debugRowString(row_));
      }
   }
   return it;
}

RowsetIterator Rowset::begin()
{
   if (query_)
   {
      RowsetIterator it(query_.get().statement_, row_);

      if (it != end())
      {
         rowCount_ = 1;
         if (log::isDbTraceEnabled() &&
             query_.get().debugOutputFormatters_.empty())
         {
            LOG_TRACE_MESSAGE("db[" + queryId_ + "] row 1: " + debugRowString(row_));
         }
      }

      return it;
   }

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

core::Error Rowset::getBoolStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<bool> * result)
{
   *result = boost::none;

   boost::optional<std::string> valueAsOptionaString;
   core::Error error = getOptionalValue<std::string>(row, columnName, &valueAsOptionaString);
   if (error)
   {
      return error;
   }

   if (!valueAsOptionaString.has_value())
   {
      return core::Success();
   }

   std::transform(valueAsOptionaString.value().begin(), valueAsOptionaString.value().end(),
      valueAsOptionaString.value().begin(), ::tolower);
   if (valueAsOptionaString.value() == "true" || valueAsOptionaString.value() == "t" || valueAsOptionaString.value() == "1" ||
       valueAsOptionaString.value() == "yes" || valueAsOptionaString.value() == "y")
   {
      *result = true;
      return core::Success();
   }

   if (valueAsOptionaString.value() == "false" || valueAsOptionaString.value() == "f" || valueAsOptionaString.value() == "0" ||
       valueAsOptionaString.value() == "no" || valueAsOptionaString.value() == "n")
   {
      *result = false;
      return core::Success();
   }

   // Conversion failed
   return core::Error(boost::system::errc::invalid_argument, 
      "Could not convert field " + columnName + " from string to bool", ERROR_LOCATION);
}

core::Error Rowset::getBoolStrValue(RowsetIterator & row, const std::string& columnName, bool * result)
{
   boost::optional<bool> optionalValue;
   core::Error error = getBoolStrValue(row, columnName, &optionalValue);
   if (error)
   {
      return error;
   }

   if( !optionalValue.has_value() )
   {
      return core::Error(boost::system::errc::invalid_argument, "Column " + columnName + " is NULL", ERROR_LOCATION);
   }

   *result = optionalValue.value();
   return core::Success();
}

core::Error Rowset::getUIntIntValue(RowsetIterator & row, const std::string& columnName, boost::optional<unsigned int> * result)
{
   *result = boost::none;

   boost::optional<int> valueAsOptionaInt;
   core::Error error = getOptionalValue<int>(row, columnName, &valueAsOptionaInt);
   if (error)
   {
      return error;
   }

   if (!valueAsOptionaInt.has_value())
   {
      return core::Success();
   }

   *result = core::safe_convert::numberTo<int, unsigned int>(valueAsOptionaInt.value());
   if( !result->has_value() )
   {
      // Conversion failed
      return core::Error(boost::system::errc::invalid_argument, 
         "Could not convert field " + columnName + " from integral to unsigned", ERROR_LOCATION);
   }

   return core::Success();
}

core::Error Rowset::getUIntIntValue(RowsetIterator & row, const std::string& columnName, unsigned int * result)
{
   boost::optional<unsigned int> optionalValue;
   core::Error error = getUIntIntValue(row, columnName, &optionalValue);
   if (error)
   {
      return error;
   }

   if( !optionalValue.has_value() )
   {
      return core::Error(boost::system::errc::invalid_argument, "Column " + columnName + " is NULL", ERROR_LOCATION);
   }

   *result = optionalValue.value();
   return core::Success();
}

core::Error Rowset::getMillisecondSinceEpochStrValue(RowsetIterator &row, const std::string& columnName, boost::optional<boost::posix_time::ptime> * result)
{
   *result = boost::none;

   boost::optional<std::string> timestampAsOptionalString;
   core::Error error = getOptionalValue<std::string>(row, columnName, &timestampAsOptionalString);
   if (error)
   {
      return error;
   }

   if (!timestampAsOptionalString.has_value())
   {
      return core::Success();
   }

   auto timestampAsOptionalDouble = core::safe_convert::stringTo<double>(timestampAsOptionalString.value());
   if( timestampAsOptionalDouble.has_value() )
   {
      *result = core::date_time::timeFromMillisecondsSinceEpoch(timestampAsOptionalDouble.value());
      return core::Success();
   }

   // Could not convert the DB value to a valid timestamp
   return core::Error(boost::system::errc::invalid_argument, 
      "Could not convert field " + columnName + " from milliseconds since epoc string to ptime - value is: " + timestampAsOptionalString.value(), ERROR_LOCATION);
}

core::Error Rowset::getMillisecondSinceEpochStrValue(RowsetIterator & row, const std::string& columnName, boost::posix_time::ptime * result)
{
   boost::optional<boost::posix_time::ptime> optionalPtime;
   core::Error error = getMillisecondSinceEpochStrValue(row, columnName, &optionalPtime);
   if (error)
   {
      return error;
   }

   if( !optionalPtime.has_value() )
   {
      return core::Error(boost::system::errc::invalid_argument, "Column " + columnName + " is NULL", ERROR_LOCATION);
   }

   *result = optionalPtime.value();
   return core::Success();
}

core::Error Rowset::getISO8601StrValue(RowsetIterator & row, const std::string& columnName, boost::optional<boost::posix_time::ptime> * result)
{
   *result = boost::none;

   boost::optional<std::string> timestampAsOptionalString;
   core::Error error = getOptionalValue<std::string>(row, columnName, &timestampAsOptionalString);
   if (error)
   {
      return error;
   }

   if (!timestampAsOptionalString.has_value())
   {
      return core::Success();
   }

   std::string timestampStr = timestampAsOptionalString.value();
   if (timestampStr.empty())
      return core::Success();

   try
   {
      *result = boost::posix_time::from_iso_string(timestampStr);
   }
   catch(const std::exception & e)
   {
      return core::Error(boost::system::errc::invalid_argument,
         "Could not convert field " + columnName + " from ISO 8601 string to ptime - value: " + timestampStr, ERROR_LOCATION);
   }

   if( result->value().is_not_a_date_time() )
   {
      return core::Error(boost::system::errc::invalid_argument,
         "Could not convert field " + columnName + " from ISO 8601 string to ptime - invalid result", ERROR_LOCATION);
   }

   return core::Success();
}

core::Error Rowset::getISO8601StrValue(RowsetIterator & row, const std::string& columnName, boost::posix_time::ptime * result)
{
   boost::optional<boost::posix_time::ptime> optionalPtime;
   core::Error error = getISO8601StrValue(row, columnName, &optionalPtime);
   if (error)
   {
      return error;
   }

   if( !optionalPtime.has_value() )
   {
      *result = boost::posix_time::not_a_date_time;
      return core::Success();
   }

   *result = optionalPtime.value();
   return core::Success();
}

core::Error Rowset::getFilepathStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<core::FilePath> * result)
{
   *result = boost::none;

   boost::optional<std::string> filepathAsOptionalString;
   core::Error error = getOptionalValue<std::string>(row, columnName, &filepathAsOptionalString);
   if (error)
   {
      return error;
   }

   if (!filepathAsOptionalString.has_value())
   {
      return core::Success();
   }

   // There is currently no validation that the string is a valid filepath cause it may only exist on the session node
   core::FilePath filepath(filepathAsOptionalString.value());
   *result = filepath;
   return core::Success();
}

core::Error Rowset::getFilepathStrValue(RowsetIterator & row, const std::string& columnName, core::FilePath * result)
{
   boost::optional<core::FilePath> optionalFilepath;
   core::Error error = getFilepathStrValue(row, columnName, &optionalFilepath);
   if (error)
   {
      return error;
   }

   if( !optionalFilepath.has_value() )
   {
      return core::Error(boost::system::errc::invalid_argument, "Column " + columnName + " is NULL", ERROR_LOCATION);
   }

   *result = optionalFilepath.value();
   return core::Success();
}

core::Error Rowset::getJSONStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<core::json::Object> * result)
{
   *result = boost::none;

   boost::optional<std::string> jsonAsOptionalString;
   core::Error error = getOptionalValue<std::string>(row, columnName, &jsonAsOptionalString);
   if (error)
   {
      return error;
   }

   if (!jsonAsOptionalString.has_value())
   {
      return core::Success();
   }

   core::json::Object jsonObject;
   error = jsonObject.parse(jsonAsOptionalString.value());
   if( error )
   {
      return core::Error(boost::system::errc::invalid_argument,
         "Could not convert field " + columnName + " from JSON string to JSON object: " + error.asString(), ERROR_LOCATION);
   }
   *result = jsonObject;

   return core::Success();
}

core::Error Rowset::getJSONStrValue(RowsetIterator & row, const std::string& columnName, core::json::Object * result)
{
   boost::optional<core::json::Object> optionalJson;
   core::Error error = getJSONStrValue(row, columnName, &optionalJson);
   if (error)
   {
      return error;
   }

   if( !optionalJson.has_value() )
   {
      return core::Error(boost::system::errc::invalid_argument, "Column " + columnName + " is NULL", ERROR_LOCATION);
   }

   *result = optionalJson.value();
   return core::Success();
}

core::Error Rowset::getJSONStrValue(RowsetIterator & row, const std::string& columnName, boost::optional<core::json::Array> * result)
{
   *result = boost::none;

   boost::optional<std::string> jsonAsOptionalString;
   core::Error error = getOptionalValue<std::string>(row, columnName, &jsonAsOptionalString);
   if (error)
   {
      return error;
   }

   if (!jsonAsOptionalString.has_value())
   {
      return core::Success();
   }

   core::json::Array jsonArray;
   error = jsonArray.parse(jsonAsOptionalString.value());
   if( error )
   {
      return core::Error(boost::system::errc::invalid_argument,
         "Could not convert field " + columnName + " from JSON string to JSON Array: " + error.asString(), ERROR_LOCATION);
   }
   *result = jsonArray;

   return core::Success();
}

core::Error Rowset::getJSONStrValue(RowsetIterator & row, const std::string& columnName, core::json::Array * result)
{
   boost::optional<core::json::Array> optionalJson;
   core::Error error = getJSONStrValue(row, columnName, &optionalJson);
   if (error)
   {
      return error;
   }

   if( !optionalJson.has_value() )
   {
      return core::Error(boost::system::errc::invalid_argument, "Column " + columnName + " is NULL", ERROR_LOCATION);
   }

   *result = optionalJson.value();
   return core::Success();
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

   // Retry loop for transient SQLite locking errors
   for (int attempt = 0; attempt <= kMaxSqliteRetries; ++attempt)
   {
      try
      {
         // Track timing for slow query detection when debug logging is enabled
         bool trackTiming = log::isLogLevel(log::LogLevel::DEBUG_LEVEL);
         double startTime = trackTiming ? date_time::millisecondsSinceEpoch() : 0;

         if (log::isDbTraceEnabled())
            LOG_TRACE_MESSAGE("db" + query.debugString());

         query.statement_.define_and_bind();
         bool result = query.statement_.execute(true);

         if (pDataReturned)
            *pDataReturned = result;

         query.statement_.bind_clean_up();

         // Log output values if trace-db is enabled
         if (log::isDbTraceEnabled())
            query.logOutputValues();

         // Log slow queries (only the SQL statement, not parameter values, unless trace-db is enabled)
         if (trackTiming)
         {
            double elapsedMs = date_time::millisecondsSinceEpoch() - startTime;
            if (elapsedMs >= kSlowQueryThresholdMs)
            {
               std::string logMsg = log::isDbTraceEnabled() ? query.debugString() : query.sqlStatement();
               LOG_DEBUG_MESSAGE("Slow DB statement: " + std::to_string(static_cast<int>(elapsedMs)) + "ms: " + logMsg);
            }
         }

         return Success();
      }
      catch (soci::soci_error& error)
      {
         // Check if this is a transient SQLite locking error that we should retry
         if (attempt < kMaxSqliteRetries && isSqliteTransientLockError(error))
         {
            int delayMs = kSqliteRetryDelayMs * (attempt + 1);
            LOG_DEBUG_MESSAGE("SQLite lock contention detected, retrying in " +
                              std::to_string(delayMs) + "ms (attempt " +
                              std::to_string(attempt + 1) + " of " +
                              std::to_string(kMaxSqliteRetries) + "): " +
                              query.sqlStatement());
            boost::this_thread::sleep(boost::posix_time::milliseconds(delayMs));
            continue;
         }
         return DatabaseError(error);
      }
   }

   // Should not reach here, but just in case
   return Error(boost::system::errc::resource_unavailable_try_again,
                "Failed to execute query after retries",
                ERROR_LOCATION);
}

Error Connection::execute(Query& query,
                          Rowset& rowset)
{
   if (query.prepareError_)
      return DatabaseError(query.prepareError_.get());

   // Retry loop for transient SQLite locking errors
   for (int attempt = 0; attempt <= kMaxSqliteRetries; ++attempt)
   {
      try
      {
         // Track timing for slow query detection when debug logging is enabled
         bool trackTiming = log::isLogLevel(log::LogLevel::DEBUG_LEVEL);
         double startTime = trackTiming ? date_time::millisecondsSinceEpoch() : 0;

         if (log::isDbTraceEnabled())
            LOG_TRACE_MESSAGE("db" + query.debugString());

         query.statement_.define_and_bind();
         query.statement_.exchange_for_rowset(soci::into(rowset.row_));
         query.statement_.execute(false);

         rowset.query_ = query;

         // Store query ID for compact trace logging in Rowset destructor
         if (log::isDbTraceEnabled())
            rowset.queryId_ = query.queryId();

         // Log slow queries (only the SQL statement, not parameter values, unless trace-db is enabled)
         if (trackTiming)
         {
            double elapsedMs = date_time::millisecondsSinceEpoch() - startTime;
            if (elapsedMs >= kSlowQueryThresholdMs)
            {
               std::string logMsg = log::isDbTraceEnabled() ? query.debugString() : query.sqlStatement();
               LOG_DEBUG_MESSAGE("Slow DB query: " + std::to_string(static_cast<int>(elapsedMs)) + "ms: " + logMsg);
            }
         }

         return Success();
      }
      catch (soci::soci_error& error)
      {
         // Check if this is a transient SQLite locking error that we should retry
         if (attempt < kMaxSqliteRetries && isSqliteTransientLockError(error))
         {
            int delayMs = kSqliteRetryDelayMs * (attempt + 1);
            LOG_DEBUG_MESSAGE("SQLite lock contention detected, retrying in " +
                              std::to_string(delayMs) + "ms (attempt " +
                              std::to_string(attempt + 1) + " of " +
                              std::to_string(kMaxSqliteRetries) + "): " +
                              query.sqlStatement());
            boost::this_thread::sleep(boost::posix_time::milliseconds(delayMs));
            continue;
         }
         return DatabaseError(error);
      }
   }

   // Should not reach here, but just in case
   return Error(boost::system::errc::resource_unavailable_try_again,
                "Failed to execute query after retries",
                ERROR_LOCATION);
}

Error Connection::executeStr(const std::string& queryStr)
{
   // Retry loop for transient SQLite locking errors
   for (int attempt = 0; attempt <= kMaxSqliteRetries; ++attempt)
   {
      try
      {
         // Track timing for slow query detection when debug logging is enabled
         bool trackTiming = log::isLogLevel(log::LogLevel::DEBUG_LEVEL);
         double startTime = trackTiming ? date_time::millisecondsSinceEpoch() : 0;

         if (log::isDbTraceEnabled())
            LOG_TRACE_MESSAGE("db: " + queryStr);

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

         // Log slow queries
         if (trackTiming)
         {
            double elapsedMs = date_time::millisecondsSinceEpoch() - startTime;
            if (elapsedMs >= kSlowQueryThresholdMs)
            {
               LOG_DEBUG_MESSAGE("Slow DB statement: " + std::to_string(static_cast<int>(elapsedMs)) + "ms: " + queryStr);
            }
         }

         return Success();
      }
      catch (soci::soci_error& error)
      {
         // Check if this is a transient SQLite locking error that we should retry
         if (attempt < kMaxSqliteRetries && isSqliteTransientLockError(error))
         {
            int delayMs = kSqliteRetryDelayMs * (attempt + 1);
            LOG_DEBUG_MESSAGE("SQLite lock contention detected, retrying in " +
                              std::to_string(delayMs) + "ms (attempt " +
                              std::to_string(attempt + 1) + " of " +
                              std::to_string(kMaxSqliteRetries) + "): " +
                              queryStr);
            boost::this_thread::sleep(boost::posix_time::milliseconds(delayMs));
            continue;
         }
         Error res = DatabaseError(error);
         return res;
      }
   }

   // Should not reach here, but just in case
   return Error(boost::system::errc::resource_unavailable_try_again,
                "Failed to execute query after retries",
                ERROR_LOCATION);
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

bool ConnectionPool::testAndReconnect(boost::shared_ptr<Connection>& connection)
{
   // do not test Sqlite connections - there is no backend system to connect to in this case
   // so any errors on the file handle itself we do not want to gracefully recover from, as they would
   // indicate a very serious programming error
   if (connection->driver() == Driver::Sqlite)
      return true;

   // it is possible for connections to go stale (such as if the upstream connection is closed)
   // which will prevent it from being usable - we test for this by running a very efficient query
   // and checking to make sure that no error has occurred
   Error error = connection->executeStr("SELECT 1");
   if (!error)
      return true;

   LOG_DEBUG_MESSAGE("Replacing stale db connection in pool - check query returned: " + error.asString() + ")");

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
      return false;
   }

   connection = boost::static_pointer_cast<Connection>(newConnection);

   return true;
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
         if (!testAndReconnect(connection))
            LOG_WARNING_MESSAGE("DB get connection - returning invalid connection");

         // create wrapper PooledConnection around retrieved Connection
         return boost::shared_ptr<IConnection>(new PooledConnection(shared_from_this(), connection));
      }
      else
      {
         LOG_ERROR_MESSAGE("Potential hang detected: could not get database connection from pool "
                           "after 30 seconds. If issue persists, please notify Posit Support");
      }
   }
}

bool ConnectionPool::getConnection(const boost::posix_time::time_duration& maxWait,
                                   boost::shared_ptr<IConnection>* pConnection)
{
   boost::shared_ptr<Connection> connection;
   if (!connections_.deque(&connection, maxWait))
   {
      LOG_DEBUG_MESSAGE("In DB getConnection - timed out in trying to find a connection");
      return false;
   }

   bool validConnection;
   // test connection to ensure it is still alive
   if (testAndReconnect(connection))
      validConnection = true;
   else
   {
      LOG_WARNING_MESSAGE("Unable to get valid DB connection for operation");
      validConnection = false;
   }
   pConnection->reset(new PooledConnection(shared_from_this(), connection));
   return validConnection;
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
   auto thisFlowerIndex = (versions.find(Flower) != versions.end()) ? versions.at(Flower) : versions.size();
   auto otherFlowerIndex = (versions.find(other.Flower) != versions.end()) ? versions.at(other.Flower) : versions.size();

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
   return (*this == other) || (!(*this < other));
}

bool SchemaVersion::operator==(const SchemaVersion& other) const
{
   if (this == &other || (Date == other.Date && Flower == other.Flower))
      return true;
   if (Date == other.Date)
   {
      // check version map to see if the flowers different but equivalent strings
      // this allows us to properly handle typos :) 
      const auto& versions = versionMap();
      int thisFlowerIndex = (versions.find(Flower) != versions.end()) ? versions.at(Flower) : versions.size();
      int otherFlowerIndex = (versions.find(other.Flower) != versions.end()) ? versions.at(other.Flower) : versions.size();
      return thisFlowerIndex == otherFlowerIndex;
   }
   return false;
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
            versions["Prairie Trillium"] = 2;
            versions["Spotted Wakerobin"] = 3;
            versions["Elsbeth Geranium"] = 4;
            versions["Cherry Blossom"] = 5;
            versions["Mountain Hydrangea"] = 6;
            versions["Desert Sunflower"] = 7;
            versions["Ocean Storm"] = 8;
            versions["Chocolate Cosmos"] = 9;
            versions["Cranberry Hisbiscus"] = 10;
            versions["Cranberry Hibiscus"] = 10;
            versions["Kousa Dogwood"] = 11;
            versions["Mariposa Orchid"] = 12;
            versions["Cucumberleaf Sunflower"] = 13;
            versions["Apple Blossom"] = 14;
            versions["Globemaster Allium"] = 15;
         }
      }
      END_LOCK_MUTEX
   }

   return versions;
}

// Add stream operator for SchemaVersion
std::ostream& operator<<(std::ostream& os, const rstudio::core::database::SchemaVersion& version)
{
   os << version.toString();
   return os;
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

Error SchemaUpdater::getSchemaTableColumnCount(std::size_t* pColumnCount)
{
   std::size_t columnCount = 0;
   Error error;
   if (connection_->driverName() == SQLITE_DRIVER)
   {
      // This query is explicitly a SELECT * because we use the # of columns to determine if 
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
   std::size_t schemaColumnCount = 0;
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
   LOG_INFO_MESSAGE("Updating database schema version using migration path: " + migrationsPath_.getAbsolutePath());
   
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

      LOG_DEBUG_MESSAGE("Current Database Schema Version:\t\t" + currentVersion.Date + " : " + currentVersion.Flower);
      LOG_DEBUG_MESSAGE("Highest Available Database Schema Version:\t" + migrationVersion.Date + " : " + migrationVersion.Flower);

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

   std::reverse(files.begin(), files.end());

   for (const std::pair<SchemaVersion, FilePath>& migrationFile : files)
   {
      // Is the migration file from an update that predates the current version?
      // If so, skip it
      if(migrationFile.first <= currentVersion)
         continue;

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

      LOG_DEBUG_MESSAGE("Applying database schema alter file " + migrationFile.second.getAbsolutePath());

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
         // Logging the error before resetting the pool because a customer saw a SEGV when handling this error.
         LOG_ERROR_MESSAGE("Error allocating database connection: " + std::to_string(i+1) + " with pool-size: " + std::to_string(poolSize) + ": " + error.asString());

         // destroy the pool, which will free each previously created connections
         pPool->reset();
         return error;
      }

      // add connection to the pool
      (*pPool)->returnConnection(boost::static_pointer_cast<Connection>(connection));
   }

   return Success();
}

Error execAndProcessQuery(boost::shared_ptr<database::IConnection> pConnection,
                          const std::string& sql,
                          const boost::function<void(const database::Row&)>& rowHandler)
{
   Rowset rows;
   Query query = pConnection->query(sql);
   Error error = pConnection->execute(query, rows);
   if (error)
      return error;

   if (!rowHandler.empty())
   {
      for (RowsetIterator it = rows.begin(); it != rows.end(); ++it)
      {
         const Row& row = *it;
         rowHandler(row);
      }
   }

   return Success();
}

std::string getRowStringValue(const Row& row, const std::string& column)
{
   soci::indicator indicator = row.get_indicator(column);
   if (indicator == soci::i_ok)
   {
      return row.get<std::string>(column);
   }
   LOG_WARNING_MESSAGE("Could not retrieve " + column + " value from database row.");
   return std::string();
}

} // namespace database
} // namespace core
} // namespace rstudio
