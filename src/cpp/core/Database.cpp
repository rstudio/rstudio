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

#include <shared_core/Error.hpp>

#include <soci/postgresql/soci-postgresql.h>
#include <soci/sqlite3/soci-sqlite3.h>

namespace rstudio {
namespace core {
namespace database {

class ConnectVisitor : public boost::static_visitor<Error>
{
public:
   ConnectVisitor(boost::shared_ptr<Connection>* pPtrConnection) :
      pPtrConnection_(pPtrConnection)
   {
   }

   Error operator()(const SqliteConnectionOptions& options) const
   {
      try
      {
         boost::shared_ptr<Connection> pConnection(new Connection(soci::sqlite3, "dbname=" + options.file));
         *pPtrConnection_ = pConnection;
         return Success();
      }
      catch(soci::soci_error& error)
      {
         return Error(boost::system::errc::protocol_error, error.get_error_message(), ERROR_LOCATION);
      }
   }

   Error operator()(const PostgresqlConnectionOptions& options) const
   {
      return Success();
   }

private:
   boost::shared_ptr<Connection>* pPtrConnection_;
};

Error connect(const ConnectionOptions& options,
              boost::shared_ptr<Connection>* pPtrConnection)
{
   return boost::apply_visitor(ConnectVisitor(pPtrConnection), options);
}

Query::Query(const std::string& sqlStatement,
             soci::session& session) :
   statement_(session)
{
   // it's possible that prepare can throw a database exception, but we
   // do not want to surface errors until execute() is called
   try
   {
      statement_.prepare(sqlStatement);
   }
   catch (soci::soci_error& error)
   {
      prepareError_ = error;
   }
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

Error Connection::execute(Query& query)
{
   if (query.prepareError_)
      return Error(boost::system::errc::protocol_error, ERROR_LOCATION);

   try
   {
      query.statement_.define_and_bind();
      query.statement_.execute(true);
      query.statement_.bind_clean_up();
   }
   catch (soci::soci_error&)
   {
      return Error(boost::system::errc::protocol_error, ERROR_LOCATION);
   }

   return Success();
}

} // namespace database
} // namespace core
} // namespace rstudio
