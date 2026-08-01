/*
 * QBMixins.hpp
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

#ifndef CORE_DETAILS_QBMIXINS_HPP
#define CORE_DETAILS_QBMIXINS_HPP

#include "QBNodeList.hpp"

#include <core/Algorithm.hpp>

namespace rstudio {
namespace core {
namespace database {

class SelectBuilder;

namespace detail {

using algorithm::optional_cast;

std::string buildWhereClause(const QBNodeList& whereClauses);

/**
 * Base class that all QueryBuilder classes virtually inherit from.
 */
class QueryBuilder
{
protected:
   QueryBuilder() = delete;
   QueryBuilder(const QueryBuilder&) = delete;
   explicit QueryBuilder(const SqlIdentifier& table);
   virtual ~QueryBuilder() = default;

   SqlIdentifier table;
   QBNodeList nodes;

   std::string buildInsertQuery() const;
   std::string buildSetClause(const std::vector<SqlIdentifier>& exclude = {}) const;
};

/**
 * Mixin class for query builders that have add() functions.
 *
 *   BUILDER: The final type of the most derived subclass.
 *         Subclasses should specify themselves as the template parameter.
 *         This permits the chainable functions to return the derived type.
 */
template <typename BUILDER>
class QBAddMixin : virtual public QueryBuilder
{
private:
   BUILDER* self;

protected:
   // QueryBuilder("_") will never be invoked due to virtual inheritance, but it must
   // be present to satisfy ISO C++'s requirements.
   QBAddMixin(BUILDER* self) : QueryBuilder("_"), self(self) {}

public:
   // Bind a known value to a named column
   template <typename T>
   BUILDER& add(const SqlIdentifier& column, const T& value)
   {
      static_assert(!QBSupportChecker<T>::value || true, "unsupported type");
      nodes.add(new QBNode<T>(column, value));
      return *self;
   }

   // Bind a known value to a named column
   // This overload treats nullptr as SQL NULL
   BUILDER& add(const SqlIdentifier& column, std::nullptr_t)
   {
      nodes.add(new QBNode<std::string>(column, nullptr));
      return *self;
   }

   // Bind a known value to a named column
   // This overload promotes string literals to std::string
   BUILDER& add(const SqlIdentifier& column, const char* value)
   {
      nodes.add(new QBNode<std::string>(column, value));
      return *self;
   }

   // Bind an optional value to a named column; store NULL if not set
   template <typename T>
   BUILDER& add(const SqlIdentifier& column, const boost::optional<T>& value)
   {
      static_assert(!QBSupportChecker<T>::value || true, "unsupported type");
      if (value.has_value())
         nodes.add(new QBNode<T>(column, *value));
      else
         nodes.add(new QBNode<T>(column, nullptr));
      return *self;
   }

   // Convert JSON to string to store in a named column
   BUILDER& add(const SqlIdentifier& column, const json::Value& value)
   {
      nodes.add(new QBNode<std::string>(column, value.write()));
      return *self;
   }

   // Convert optional JSON to string to store in a named column; store NULL if not set
   BUILDER& add(const SqlIdentifier& column, const boost::optional<json::Value>& value)
   {
      if (!value.has_value())
         nodes.add(new QBNode<std::string>(column, nullptr));
      else
         nodes.add(new QBNode<std::string>(column, value->write()));
      return *self;
   }

   // Convert optional JSON to string to store in a named column; store NULL if not set
   BUILDER& add(const SqlIdentifier& column, const boost::optional<json::Array>& value)
   {
      return add(column, optional_cast<json::Value>(value));
   }

   // Convert optional JSON to string to store in a named column; store NULL if not set
   BUILDER& add(const SqlIdentifier& column, const boost::optional<json::Object>& value)
   {
      return add(column, optional_cast<json::Value>(value));
   }

   // Bind an optional value at a named column, but only if it's set (does not convert to NULL)
   template <typename T>
   BUILDER& addIfSet(const SqlIdentifier& column, const boost::optional<T>& value)
   {
      static_assert(!QBSupportChecker<T>::value || true, "unsupported type");
      if (value.has_value())
         return add(column, value);
      return *self;
   }

   // Returns true if no columns have been bound
   bool empty() const
   {
      return nodes.empty();
   }
};

/**
 * Base class for QBWhereOr and QBWhereAnd.
 *
 * This is defined separately, with virtual inheritance, to simplify
 * the otherwise-circular dependency between QBWhereMixin and the
 * compound clause containers, while preventing passing e.g. a
 * SelectBuilder to a where() call.
 */
class QBCompoundWhere : virtual public QueryBuilder
{
public:
   // After calling takeNode(), the QBCompoundWhere is no longer valid.
   virtual QBCompoundNode* takeNode() = 0;
};

/**
 * Mixin class for query builders that have where() functions.
 *
 *   BUILDER: The final type of the most derived subclass.
 *         Subclasses should specify themselves as the template parameter.
 *         This permits the chainable functions to return the derived type.
 */
template <typename BUILDER>
class QBWhereMixin : virtual public QueryBuilder
{
private:
   BUILDER* self;

protected:
   // QueryBuilder("_") will never be invoked due to virtual inheritance, but it must
   // be present to satisfy ISO C++'s requirements.
   QBWhereMixin(BUILDER* self) : QueryBuilder("_"), self(self) {}

public:
   // Restricts the query to only target rows where the specified column contains
   // the specified value. Multiple conditions are combined with `AND`.
   template <typename T>
   BUILDER&& where(const SqlIdentifier& column, T value)
   {
      static_assert(!QBSupportChecker<T>::value || true, "unsupported type");
      auto node = new QBNode<T>(column, value);
      whereClauses.add(node);
      return std::move(*self);
   }

   // As where(), but this overload treats nullptr as SQL NULL using the IS operator
   BUILDER&& where(const SqlIdentifier& column, std::nullptr_t)
   {
      auto node = new QBIsNullNode(column);
      whereClauses.add(node);
      return std::move(*self);
   }

   // As where(), but checks for NULL if the optional object does not contain a value
   template <typename T>
   BUILDER&& where(const SqlIdentifier& column, const boost::optional<T>& value)
   {
      if (value.has_value())
         return where(column, *value);
      else
         return where(column, nullptr);
   }

   // As where(), but this overload promotes string literals to std::string
   BUILDER&& where(const SqlIdentifier& column, const char* value)
   {
      return where<std::string>(column, value);
   }

   // Restricts the query to only target rows where the specified column contains
   // one of the specified values. Multiple conditions are combined with `AND`.
   template <typename CONTAINER>
   BUILDER&& whereIn(const SqlIdentifier& column, const CONTAINER& values)
   {
      static_assert(!QBSupportChecker<typename CONTAINER::value_type>::value || true, "unsupported type");
      auto node = new QBListNode<CONTAINER>(column, values.begin(), values.end());
      whereClauses.add(node);
      return std::move(*self);
   }

   // As whereIn(), but accepts an initializer list
   template <typename T>
   BUILDER&& whereIn(const SqlIdentifier& column, std::initializer_list<T> values)
   {
      auto node = new QBListNode<std::initializer_list<T>>(column, values.begin(), values.end());
      whereClauses.add(node);
      return std::move(*self);
   }

   // As where(), but no condition is added if the provided value is not set.
   template <typename T>
   BUILDER&& whereIfSet(const SqlIdentifier& column, boost::optional<T> value)
   {
      if (value.has_value())
         return where(column, *value);
      return std::move(*self);
   }

   // As where(), but compares using the LIKE operator
   BUILDER&& whereLike(const SqlIdentifier& column, const std::string& pattern)
   {
      auto node = new QBNode<std::string>(column, pattern);
      node->op = "LIKE";
      whereClauses.add(node);
      return std::move(*self);
   }

   // As where(), but accepts a compound clause
   BUILDER&& where(QBCompoundWhere&& clause)
   {
      whereClauses.add(clause.takeNode());
      return std::move(*self);
   }

protected:
   std::string buildWhereClause() const
   {
      return detail::buildWhereClause(whereClauses);
   }

   QBNodeList whereClauses;
};

} // namespace detail
} // namespace database
} // namespace core
} // namespace rstudio

#endif // CORE_DETAILS_QBMIXINS_HPP
