/*
 * QBNode.hpp
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

#ifndef CORE_DETAILS_QBNODE_HPP
#define CORE_DETAILS_QBNODE_HPP

#include <core/Database.hpp>

#include <core/SqlIdentifier.hpp>

#include <iostream>
#include <sstream>
#include <vector>

namespace rstudio {
namespace core {
namespace database {

class SelectBuilder;

namespace detail {

enum BindType {
   Select,
   Insert,
   Equals,
   WhereIn,
};

// Polymorphic base type for bound values
struct QBBaseNode
{
   explicit QBBaseNode(const SqlIdentifier& column) : column(column), refName(column) {}
   virtual ~QBBaseNode() {}

   virtual void apply(Query& query, const std::string& prefix = std::string()) = 0;
   virtual std::string toClause(const std::string& prefix = std::string()) const = 0;

   virtual std::string toInsertValue() const
   {
      return ":" + refName;
   }

   inline static std::string getColumnName(const QBBaseNode* n)
   {
      return n->column;
   }

   inline static std::string getInsertValue(const QBBaseNode* n)
   {
      return n->toInsertValue();
   }

   SqlIdentifier column;
   std::string refName;
};

template <typename T>
struct QBSupportChecker
{
private:
   using t_ = std::int8_t;
   using f_ = std::int16_t;
   template <typename U> static t_ check_support(decltype(soci::details::exchange_traits<U>::x_type)*);
   template <typename U> static f_ check_support(...);

public:
   using type = T;
   static constexpr bool value = sizeof(check_support<T>(0)) == sizeof(t_);
};

template <> struct QBSupportChecker<json::Array> : std::true_type {};
template <> struct QBSupportChecker<json::Object> : std::true_type {};

// Typesafe container for bound values
template <typename T>
struct QBNode : public QBBaseNode
{
   QBNode(const SqlIdentifier& column, T value) : QBBaseNode(column), value(value), isNull(false) {}
   QBNode(const SqlIdentifier& column, std::nullptr_t) : QBBaseNode(column), value(), isNull(true) {}

   virtual ~QBNode() {}

   T value;
   bool isNull;
   std::string op = "=";

   void apply(Query& query, const std::string& prefix = std::string())
   {
      query.withInputOrNull(value, isNull, prefix + refName);
   }

   virtual std::string toClause(const std::string& prefix = std::string()) const
   {
      return column + " " + op + " :" + prefix + refName;
   }
};

struct QBIsNullNode : public QBBaseNode
{
   QBIsNullNode(const SqlIdentifier& column) : QBBaseNode(column) {}

   void apply(Query&, const std::string& = std::string())
   {
      // no-op, expands to an expression with no bound parameters
   }

   virtual std::string toClause(const std::string& = std::string()) const
   {
      return column + " IS NULL";
   }
};

// Typesafe container for arrays of bound values
template <typename CONTAINER>
struct QBListNode : public QBBaseNode
{
   QBListNode(const SqlIdentifier& column, const typename CONTAINER::const_iterator& begin, const typename CONTAINER::const_iterator& end)
   : QBBaseNode(column), values(begin, end) {}

   virtual ~QBListNode() {}

   std::vector<typename CONTAINER::value_type> values;

   void apply(Query& query, const std::string& prefix = std::string())
   {
      for (std::size_t i = 0; i < values.size(); i++) {
         query.withInput(values[i], prefix + refName + "_" + std::to_string(i));
      }
   }

   virtual std::string toClause(const std::string& prefix = std::string()) const
   {
      // WHERE x IN () is invalid SQL, but the intent is that no records should be matched
      if (values.empty())
         return "(1 = 0)";

      std::ostringstream ss;
      ss << column << " IN (";
      for (std::size_t i = 0; i < values.size(); i++) {
         if (i > 0)
            ss << ", ";
         ss << ":" << prefix << refName << "_" << i;
      }
      ss << ")";
      return ss.str();
   }
};

} // namespace detail
} // namespace database
} // namespace core
} // namespace rstudio

#endif // CORE_DETAILS_QBNODE_HPP
