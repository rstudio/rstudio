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

#include <boost/optional.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/variant.hpp>

#include <soci/session.h>
#include <soci/soci-platform.h>
#include <soci/transaction.h>

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
   soci::statement statement_;
   boost::optional<soci::soci_error> prepareError_;
};

class IConnection
{
public:
   virtual Query query(const std::string& sqlStatement) = 0;
   virtual Error execute(Query& query,
                         bool* pDataReturned = nullptr) = 0;
};

class Connection : public IConnection
{
public:
   Query query(const std::string& sqlStatement) override;
   Error execute(Query& query,
                 bool* pDataReturned = nullptr) override;
   virtual ~Connection() {}

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
                 bool* pDataReturned = nullptr) override;

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
   boost::shared_ptr<PooledConnection> getConnection();

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
   Transaction(const boost::shared_ptr<Connection>& connection);

   void commit();
   void rollback();

   // when this class goes out of scope, the transaction
   // is automatically aborted if not previously committed

private:
   boost::shared_ptr<Connection> connection_;
   soci::transaction transaction_;
};

Error connect(const ConnectionOptions& options,
              boost::shared_ptr<Connection>* pPtrConnection);

Error createConnectionPool(size_t poolSize,
                           const ConnectionOptions& options,
                           boost::shared_ptr<ConnectionPool>* pPool);

} // namespace database
} // namespace core
} // namespace rstudio


#endif // CORE_DATABASE_HPP

