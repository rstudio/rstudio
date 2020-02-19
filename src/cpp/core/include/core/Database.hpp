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

#include <boost/optional.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/variant.hpp>

#include <soci/session.h>
#include <soci/soci-platform.h>

namespace rstudio {
namespace core {

class Error;

namespace database {

struct SqliteConnectionOptions
{
   std::string file;
};

struct PostgresqlConnectionOptions
{
   std::string database;
   std::string host;
   std::string port;
   std::string user;
   std::string password;
   int connectionTimeoutSeconds;
};

typedef boost::variant<SqliteConnectionOptions, PostgresqlConnectionOptions> ConnectionOptions;
using InputParameter = soci::details::use_type_ptr;
using OutputParameter = soci::details::into_type_ptr;

class Connection;

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
   Query& withOutput(T& out)
   {
      statement_.exchange(soci::into(out));
      return *this;
   }

private:
   friend class Connection;
   soci::statement statement_;
   boost::optional<soci::soci_error> prepareError_;
};

class Connection
{
public:
   Query query(const std::string& sqlStatement);
   Error execute(Query& query);

private:
   friend class ConnectVisitor;

   // private constructor - use global connect function
   Connection(const soci::backend_factory& factory,
              const std::string& connectionStr);

   soci::session session_;
};

Error connect(const ConnectionOptions& options,
              boost::shared_ptr<Connection>* pPtrConnection);



} // namespace database
} // namespace core
} // namespace rstudio


#endif // CORE_DATABASE_HPP

