/*
 * ServerSetupDb.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#ifndef SERVER_SETUP_DB_HPP
#define SERVER_SETUP_DB_HPP

#include <istream>
#include <ostream>
#include <string>

#include <boost/shared_ptr.hpp>

#include <core/Database.hpp>
#include <server/ServerOptions.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace server {

// Validates a database/role identifier against ^[A-Za-z_][A-Za-z0-9_-]*$.
// Writes a [FAIL] line to `out` and sets *pPassed = false if invalid.
core::Error validateIdentifier(const std::string& identifier,
                                std::ostream& out,
                                bool* pPassed);

// Generates a password from an OpenSSL CSPRNG (core::system::crypto::random),
// mapped via unbiased rejection sampling onto a SQL-literal-safe charset
// (excludes ' " \ ;), exactly 32 characters. Returns a non-Success Error
// only if the underlying CSPRNG call fails (e.g. the system entropy source
// is unavailable).
core::Error generateServiceUserPassword(std::string* pPassword);

// Returns the charset used by generateServiceUserPassword(), so tests can
// assert every generated character is a member of it.
const std::string& serviceUserPasswordCharset();

// Resolves the master password in order of precedence: masterPasswordFile (its
// first line), then the RSERVER_SETUP_DB_MASTER_PASSWORD environment variable,
// then a masked interactive prompt on `in`/`out`. Returns a non-Success Error
// only if masterPasswordFile was specified but could not be read.
core::Error resolveMasterPassword(const std::string& masterPasswordFile,
                                   std::istream& in,
                                   std::ostream& out,
                                   std::string* pPassword);

// Options controlling --setup-db behavior, parsed from argv by the caller
// (see program_options::detectShowPassword/detectPrintOnly/extractMasterPasswordFile/
// extractHost/extractPort/extractMasterUsername/extractDatabaseName/extractDatabaseUser).
struct SetupDbFlags
{
   bool showPassword = false;
   bool printOnly = false;
   std::string masterPasswordFile;
   std::string host;
   std::string port;
   std::string masterUsername;
   std::string databaseName;
   std::string databaseUser;
};

// Prompts for the master DB host, port, and username (reading from `in`,
// writing prompts to `out`) -- skipping each prompt whose value was already
// supplied via flags.host/flags.port/flags.masterUsername -- resolves the
// master password per resolveMasterPassword's precedence, and connects to
// the "postgres" maintenance database as that user. The resulting connection
// and its options are shared by both the base setupDb() call (main database)
// and the Pro overlay's setupDb() call (audit database), so the master
// password is only prompted for once per `rserver --setup-db` invocation.
// Returns a non-Success Error only if the command itself could not run (e.g.
// the master password file could not be read); a connection failure is
// reported as a [FAIL] line on `out` with *pPassed set to false.
core::Error connectAsMaster(const SetupDbFlags& flags,
                             std::istream& in,
                             std::ostream& out,
                             boost::shared_ptr<core::database::IConnection>* pConnection,
                             core::database::PostgresqlConnectionOptions* pMasterOptions,
                             bool* pPassed);

// Creates `dbName`/`dbUser` (with `password`) and grants PostgreSQL 15+-safe
// privileges on the server `pMasterConnection` is already connected to,
// idempotently. Reconnects once -- using `masterConnectionOptions` with
// database=dbName -- to grant schema privileges. Shared by the base
// setupDb() (main database) and the Pro overlay's setupDb() (audit
// database), both of which already hold an open master connection via
// connectAsMaster(). Sets *pUserCreated to true only if CREATE USER actually
// ran (i.e. `dbUser` did not already exist); callers that generate a fresh
// password for `dbUser` should only trust/persist it when *pUserCreated is
// true, since `password` is otherwise never applied to the (pre-existing)
// role.
core::Error createDatabaseAndUser(boost::shared_ptr<core::database::IConnection> pMasterConnection,
                                   const core::database::PostgresqlConnectionOptions& masterConnectionOptions,
                                   const std::string& dbName,
                                   const std::string& dbUser,
                                   const std::string& password,
                                   std::ostream& out,
                                   bool* pPassed,
                                   bool* pUserCreated);

// Writes the given key/value pairs as a fresh, 0600 config file at `path`,
// overwriting anything already there. Used only for the standalone
// --print-only credentials file: it has no other keys worth preserving, and
// may be stale from a previous run, so a fresh write is the right behavior
// there (unlike database.conf -- see mergeWriteDatabaseConfigFile below).
// Exposed here (rather than kept file-local) so it can be unit tested
// directly.
core::Error writeCredentialsFileFresh(const core::FilePath& path,
                                       const std::string& host,
                                       const std::string& port,
                                       const std::string& dbName,
                                       const std::string& dbUser,
                                       const std::string& password);

// Loads any existing database.conf at `path` (a missing file is not an
// error -- Settings::initialize() starts from an empty map), sets the six
// connection keys, and writes it back -- preserving any other keys the
// admin had already configured there rather than clobbering the whole file.
// Verifies the write by re-reading the file afterward, since
// Settings::endUpdate() only LOG_ERRORs on a failed write rather than
// propagating it. Exposed here (rather than kept file-local) so it can be
// unit tested directly.
core::Error mergeWriteDatabaseConfigFile(const core::FilePath& path,
                                          const std::string& host,
                                          const std::string& port,
                                          const std::string& dbName,
                                          const std::string& dbUser,
                                          const std::string& password);

// Prompts for the main database/user name (reading from `in`, writing
// prompts and [PASS]/[FAIL]/[INFO] lines to `out`) -- skipping each prompt
// whose value was already supplied via flags.databaseName/flags.databaseUser
// -- creates it via
// createDatabaseAndUser using the already-open master connection from
// connectAsMaster(), then writes (or, if flags.printOnly, prints the path to
// a standalone credentials file for) the resulting connection settings.
// Prints the generated password to `out` when flags.showPassword is set.
// A service-user password is only generated -- and the config/credentials
// file only written -- when `dbUser` did not already exist; an idempotent
// re-run against an already-provisioned database leaves any existing
// database.conf/credentials file untouched rather than overwriting it with a
// password that was never applied to the real (pre-existing) role. Returns a
// non-Success Error only if the command itself could not run; SQL/connection
// failures are reported as [FAIL] lines on `out` with *pPassed set to false.
core::Error setupDb(const Options& options,
                     const SetupDbFlags& flags,
                     boost::shared_ptr<core::database::IConnection> pMasterConnection,
                     const core::database::PostgresqlConnectionOptions& masterConnectionOptions,
                     std::istream& in,
                     std::ostream& out,
                     bool* pPassed);

} // namespace server
} // namespace rstudio

#endif // SERVER_SETUP_DB_HPP
