/*
 * QueryBuilder.hpp
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

#ifndef CORE_QUERYBUILDER_HPP
#define CORE_QUERYBUILDER_HPP

#include <core/Database.hpp>

#include <shared_core/json/Json.hpp>
#include <core/SqlIdentifier.hpp>
#include <core/details/QBMixins.hpp>

#include <functional>
#include <memory>
#include <sstream>

namespace rstudio {
namespace core {
namespace database {

/**
 * Object to construct a `SELECT` query.
 *
 * Example usage:
 *    SelectBuilder builder(dbConnection, "table_name");
 *    builder.add("column_a", value)
 *           .add("column_b", value)
 *           .where("column_c", value);
 *    Query query = builder.build();
 *
 * NOTE: Don't write `Query query = SelectBuilder(...)....build()` because
 * the bound values will be deleted before SOCI can execute the query.
 */
class SelectBuilder :
   public detail::QBWhereMixin<SelectBuilder>
{
public:
   enum { addBindType = detail::BindType::Select };

   SelectBuilder(DatabaseConnection db, const SqlIdentifier& table);

   // Bind a known value to a named column
   SelectBuilder& add(const SqlIdentifier& column);

   // Returns true if no columns have been added
   bool empty() const;

   // Constructs the SQL used to perform the update
   std::string toSQL() const;

   // Constructs a Query object to perform the select and binds the provided values to it
   Query build();

protected:
   DatabaseConnection db;
   std::vector<SqlIdentifier> columns;
};

/**
 * Object to construct an `INSERT INTO` query.
 *
 * Example usage:
 *    InsertBuilder builder(dbConnection, "table_name");
 *    Query query = builder
 *       .add("column_a", value)
 *       .add("column_b", value)
 *       .build();
 *
 * NOTE: Don't write `Query query = InsertBuilder(...)....build()` because
 * the bound values will be deleted before SOCI can execute the query.
 */
class InsertBuilder :
   public detail::QBAddMixin<InsertBuilder>
{
public:
   enum { addBindType = detail::BindType::Insert };

   InsertBuilder(DatabaseConnection db, const SqlIdentifier& table);

   // Constructs the SQL used to perform the insert
   std::string toSQL() const;

   // Constructs a Query object to perform the insert and binds the provided values to it
   Query build();

   // Transforms the insert query into an upsert operation.
   //
   // The ON CONFLICT (...) DO UPDATE clause will contain all columns added to the builder
   // except for columns named in `keys` and `noUpdate`.
   //
   // Parameters:
   //    keys: The set of columns used in the table's UNIQUE constraint
   //    noUpdate: Columns that should not be updated if the row already exists
   InsertBuilder& onConflictUpdate(const std::vector<SqlIdentifier>& keys, const std::vector<SqlIdentifier>& noUpdate = {});

protected:
   DatabaseConnection db;
   std::vector<SqlIdentifier> conflictKeys;
   std::vector<SqlIdentifier> conflictNoUpdate;
};

/**
 * Object to construct an `UPDATE` query.
 *
 * Example usage:
 *    UpdateBuilder builder(dbConnection, "table_name");
 *    builder.add("column_a", value)
 *           .add("column_b", value)
 *           .where("column_c", value);
 *    Query query = builder.build();
 *
 * NOTE: Don't write `Query query = UpdateBuilder(...)....build()` because
 * the bound values will be deleted before SOCI can execute the query.
 */
class UpdateBuilder :
   public detail::QBAddMixin<UpdateBuilder>,
   public detail::QBWhereMixin<UpdateBuilder>
{
public:
   enum { addBindType = detail::BindType::Equals };

   UpdateBuilder(DatabaseConnection db, const SqlIdentifier& table);

   // Constructs the SQL used to perform the update
   std::string toSQL() const;

   // Constructs a Query object to perform the update and binds the provided values to it
   Query build();

protected:
   DatabaseConnection db;
};

/**
 * Object to construct a `DELETE` query.
 *
 * Example usage:
 *    DeleteBuilder builder(dbConnection, "table_name");
 *    builder.where("column_a", value);
 *    Query query = builder.build();
 *
 * NOTE: Don't write `Query query = DeleteBuilder(...)....build()` because
 * the bound values will be deleted before SOCI can execute the query.
 */
class DeleteBuilder :
   public detail::QBWhereMixin<DeleteBuilder>
{
public:
   DeleteBuilder(DatabaseConnection db, const SqlIdentifier& table);

   // Constructs the SQL used to perform the delete
   std::string toSQL() const;

   // Constructs a Query object to perform the delete and binds the provided values to it
   Query build();

protected:
   DatabaseConnection db;
};

/**
 * Object to construct an arbitrary SQL query.
 *
 * Example usage:
 *    RawQueryBuilder builder(dbConnection);
 *    builder << "SELECT " << kColumnA << ", " << kColumnB
 *       << " FROM " << kTableName
 *       << " WHERE id = " << id;
 *    Query query = builder.build();
 *
 * The column names and table names are pre-validated, but the variables are
 * substituted with bound parameters.
 */
class RawQueryBuilder
   : private detail::QBAddMixin<RawQueryBuilder>
{
public:
   // Wrapper to tell RawQueryBuilder to mark a bound parameter as secret
   template <typename T>
   struct Secret
   {
      Secret(const T& value) : value(value) {}
      T value;
   };

   RawQueryBuilder(DatabaseConnection db);

   // Add literal SQL text
   RawQueryBuilder& operator<<(const char sql[]);

   // Add a pre-validated identifier
   RawQueryBuilder& operator<<(const SqlIdentifier& identifier);

   // Add a bound parameter and bind a value
   template <typename T>
   RawQueryBuilder& operator<<(Secret<T>&& value)
   {
      return add(addBinding(true), value.value);
   }

   // Add a bound parameter and bind a value
   template <typename T>
   RawQueryBuilder& operator<<(const T& value)
   {
      return add(addBinding(false), value);
   }

   // Construct the SQL used to perform the query
   std::string toSQL() const;

   // Constructs a Query object to perform the query and binds the provided values to it
   Query build();

protected:
   SqlIdentifier addBinding(bool secret);

   DatabaseConnection db;
   std::ostringstream queryText;
};

template <typename T>
RawQueryBuilder::Secret<T> SECRET(const T& value)
{
   return RawQueryBuilder::Secret<T>(value);
}

/**
 * Object to construct a (... OR ...) expression for use in a WHERE clause.
 *
 * Example usage:
 *    SelectBuilder builder(dbConnection, "table_name");
 *    builder.add("id");
 *    builder.where(
 *       QBWhereOr().where("column_a", value).where("column_b", value)
 *    );
 *    Query query = builder.build();
 *
 * This produces a query similar to:
 *    SELECT id FROM table_name WHERE (column_a = ? OR column_b = ?)
 */
class QBWhereOr :
   public detail::QBCompoundWhere,
   public detail::QBWhereMixin<QBWhereOr>
{
   friend class QBWhereMixin<QBWhereOr>;
public:
   QBWhereOr() : QueryBuilder("_"), QBWhereMixin(this) {}

   // After calling takeNode(), the QBWhereOr is no longer valid.
   virtual detail::QBCompoundNode* takeNode()
   {
      detail::QBCompoundNode* node = new detail::QBCompoundNode("OR");
      node->nodes = std::move(whereClauses);
      return node;
   }
};

/**
 * Object to construct a (... AND ...) expression for use in a WHERE clause.
 *
 * Example usage:
 *    SelectBuilder builder(dbConnection, "table_name");
 *    builder.add("id");
 *    builder.where(
 *       QBWhereAnd().where("column_a", value).where("column_b", value)
 *    );
 *    Query query = builder.build();
 *
 * This produces a query similar to:
 *    SELECT id FROM table_name WHERE (column_a = ? AND column_b = ?)
 *
 * This is only practically useful within a QBWhereOr() expression, as the
 * default behavior of .where() is already to use AND.
 */
class QBWhereAnd :
   public detail::QBCompoundWhere,
   public detail::QBWhereMixin<QBWhereAnd>
{
   friend class QBWhereMixin<QBWhereAnd>;
public:
   QBWhereAnd() : QueryBuilder("_"), QBWhereMixin(this) {}

   // After calling takeNode(), the QBWhereAnd is no longer valid.
   virtual detail::QBCompoundNode* takeNode()
   {
      detail::QBCompoundNode* node = new detail::QBCompoundNode("AND");
      node->nodes = std::move(whereClauses);
      return node;
   }
};

} // namespace database
} // namespace core
} // namespace rstudio

#endif // CORE_QUERYBUILDER_HPP
