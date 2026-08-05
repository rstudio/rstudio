/*
 * QueryBuilder.cpp
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

#include <core/QueryBuilder.hpp>
#include <core/details/QBNodeList.hpp>

namespace rstudio {
namespace core {
namespace database {

namespace detail {

static void bindAllNodes(Query& query, QBNodeList& nodes, const std::string& prefix = std::string())
{
   for (QBBaseNode* node : nodes)
   {
      node->apply(query, prefix);
   }
}

std::string buildWhereClause(const QBNodeList& whereClauses)
{
   std::ostringstream ss;
   if (!whereClauses.empty()) {
      ss << " WHERE ";
      bool first = true;
      for (const QBBaseNode* node : whereClauses)
      {
         if (first)
            first = false;
         else
            ss << " AND ";
         ss << node->toClause("where_");
      }
   }
   return ss.str();
}

QueryBuilder::QueryBuilder(const SqlIdentifier& table)
: table(table)
{
   // initializers only
}

std::string QueryBuilder::buildInsertQuery() const
{
   std::ostringstream ss;
   ss << "INSERT INTO " << table << " (" << algorithm::join(nodes, ", ", QBBaseNode::getColumnName)
      << ") VALUES (" << algorithm::join(nodes, ", ", QBBaseNode::getInsertValue) << ")";
   return ss.str();
}

std::string QueryBuilder::buildSetClause(const std::vector<SqlIdentifier>& exclude) const
{
   std::ostringstream ss;
   ss << " SET ";
   bool first = true;
   for (const QBBaseNode* node : nodes)
   {
      if (std::find(exclude.begin(), exclude.end(), node->column) != exclude.end())
         continue;
      if (first)
         first = false;
      else
         ss << ", ";
      ss << node->toClause();
   }
   return ss.str();
}

} // namespace detail

SelectBuilder::SelectBuilder(DatabaseConnection db, const SqlIdentifier& table)
: QueryBuilder(table), QBWhereMixin(this), db(db)
{
   // initializers only
}

SelectBuilder& SelectBuilder::add(const SqlIdentifier& column)
{
   columns.push_back(column);
   return *this;
}

bool SelectBuilder::empty() const
{
   return columns.empty();
}

std::string SelectBuilder::toSQL() const
{
   std::ostringstream ss;
   ss << "SELECT " << algorithm::join(columns.begin(), columns.end(), ", ")
      << " FROM " << table << buildWhereClause();
   return ss.str();
}

Query SelectBuilder::build()
{
   Query query(toSQL(), db->session());
   bindAllNodes(query, nodes);
   bindAllNodes(query, whereClauses, "where_");
   return query;
}

InsertBuilder::InsertBuilder(DatabaseConnection db, const SqlIdentifier& table)
: QueryBuilder(table), QBAddMixin(this), db(db)
{
   // initializers only
}

std::string InsertBuilder::toSQL() const
{
   if (empty())
      return "";

   std::ostringstream ss;
   ss << buildInsertQuery();

   if (!conflictKeys.empty())
   {
      using namespace detail;
      ss << " ON CONFLICT (" << algorithm::join(conflictKeys.begin(), conflictKeys.end(), ", ") << ") DO ";
      if (conflictNoUpdate.size() < nodes.size())
         ss << "UPDATE " << buildSetClause(conflictNoUpdate);
      else
         ss << "NOTHING";
   }

   return ss.str();
}

Query InsertBuilder::build()
{
   Query query(toSQL(), db->session());
   bindAllNodes(query, nodes);
   return query;
}

InsertBuilder& InsertBuilder::onConflictUpdate(const std::vector<SqlIdentifier>& keys, const std::vector<SqlIdentifier>& noUpdate)
{
   conflictKeys = keys;
   conflictNoUpdate = keys;
   conflictNoUpdate.insert(conflictNoUpdate.end(), noUpdate.begin(), noUpdate.end());
   return *this;
}

UpdateBuilder::UpdateBuilder(DatabaseConnection db, const SqlIdentifier& table)
: QueryBuilder(table), QBAddMixin(this), QBWhereMixin(this), db(db)
{
   // initializers only
}

std::string UpdateBuilder::toSQL() const
{
   return "UPDATE " + table + buildSetClause() + buildWhereClause();
}

Query UpdateBuilder::build()
{
   Query query(toSQL(), db->session());
   bindAllNodes(query, nodes);
   bindAllNodes(query, whereClauses, "where_");
   return query;
}

DeleteBuilder::DeleteBuilder(DatabaseConnection db, const SqlIdentifier& table)
: QueryBuilder(table), QBWhereMixin(this), db(db)
{
   // initializers only
}

std::string DeleteBuilder::toSQL() const
{
   return "DELETE FROM " + table + buildWhereClause();
}

Query DeleteBuilder::build()
{
   Query query(toSQL(), db->session());
   bindAllNodes(query, whereClauses, "where_");
   return query;
}

RawQueryBuilder::RawQueryBuilder(DatabaseConnection db)
: QueryBuilder("_"), QBAddMixin(this), db(db)
{
   // initializers only
}

SqlIdentifier RawQueryBuilder::addBinding(bool secret)
{
   std::string name = (secret ? "secret_param" : "bind") + std::to_string(nodes.size());
   if (secret)
      addSecretParamNames({ name });
   queryText << ":" << name;
   // Use .c_str() because this is already known to be safe
   return SqlIdentifier(name.c_str());
}

RawQueryBuilder& RawQueryBuilder::operator<<(const char sql[])
{
   queryText << sql;
   return *this;
}

RawQueryBuilder& RawQueryBuilder::operator<<(const SqlIdentifier& identifier)
{
   queryText << identifier;
   return *this;
}

std::string RawQueryBuilder::toSQL() const
{
   return queryText.str();
}

Query RawQueryBuilder::build()
{
   Query query(toSQL(), db->session());
   bindAllNodes(query, nodes);
   return query;
}

} // namespace database
} // namespace core
} // namespace rstudio
