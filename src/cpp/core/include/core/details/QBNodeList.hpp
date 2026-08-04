/*
 * QBNodeList.hpp
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

#ifndef CORE_DETAILS_QBNODELIST_HPP
#define CORE_DETAILS_QBNODELIST_HPP

#include "QBNode.hpp"

#include <memory>
#include <stdexcept>

namespace rstudio {
namespace core {
namespace database {
namespace detail {

class QBNodeList : private std::vector<std::unique_ptr<QBBaseNode>>
{
   using base = std::vector<std::unique_ptr<QBBaseNode>>;

   template <typename NODE, typename BASE_ITER>
   class iterator_wrap {
   public:
      iterator_wrap() = default;
      iterator_wrap(const iterator_wrap&) = default;
      iterator_wrap(iterator_wrap&&) = default;
      iterator_wrap& operator=(const iterator_wrap&) = default;
      iterator_wrap& operator=(iterator_wrap&&) = default;
      iterator_wrap(const BASE_ITER& iter) : iter(iter) {}
      iterator_wrap(BASE_ITER&& iter) : iter(iter) {}

      NODE operator*() const { return iter->get(); }
      NODE operator->() const { return iter->get(); }

      iterator_wrap& operator++() { ++iter; return *this; }
      iterator_wrap operator++(int) { return iterator_wrap(iter++); }

      bool operator==(const iterator_wrap& other) const { return iter == other.iter; }
      bool operator!=(const iterator_wrap& other) const { return iter != other.iter; }
      bool operator<(const iterator_wrap& other) const { return iter < other.iter; }
      bool operator>(const iterator_wrap& other) const { return iter > other.iter; }
      bool operator<=(const iterator_wrap& other) const { return iter <= other.iter; }
      bool operator>=(const iterator_wrap& other) const { return iter >= other.iter; }

   private:
      BASE_ITER iter;
   };

public:
   using value_type = QBBaseNode*;
   using iterator = iterator_wrap<QBBaseNode*, base::iterator>;
   using const_iterator = iterator_wrap<const QBBaseNode*, base::const_iterator>;

   // takes ownership of node
   void add(QBBaseNode* node)
   {
      int dedupe = 1;
      for (const QBBaseNode* existing : *this)
      {
         if (existing->column == node->column)
            dedupe++;
      }
      if (dedupe > 1 || node->column.toString().empty())
         node->refName += std::to_string(dedupe);
      emplace_back(node);
   }

   iterator begin() { return iterator(base::begin()); }
   iterator end() { return iterator(base::end()); }
   const_iterator begin() const { return const_iterator(base::begin()); }
   const_iterator end() const { return const_iterator(base::end()); }

   using base::empty;
   using base::size;
};

struct QBCompoundNode : public QBBaseNode
{
   QBCompoundNode(const std::string& op) : QBBaseNode(SqlIdentifier()), op(op) {}

   virtual void apply(Query& query, const std::string& prefix = std::string())
   {
      std::string extPrefix = prefix + refName + "_";
      for (QBBaseNode* node : nodes)
      {
         node->apply(query, extPrefix);
      }
   }

   virtual std::string toClause(const std::string& prefix = std::string()) const
   {
      // WHERE () is invalid SQL
      if (nodes.empty())
      {
         // AND implies that no records should be matched if no conditions match
         if (op == "AND")
            return "(1 = 0)";
         // OR implies that all records should be matched if no conditions fail
         return "(1 = 1)";
      }
      std::string extPrefix = prefix + refName + "_";
      std::ostringstream ss;
      bool first = true;
      ss << "(";
      for (const QBBaseNode* node : nodes)
      {
         if (first)
            first = false;
         else
            ss << " " << op << " ";
         ss << node->toClause(extPrefix);
      }
      ss << ")";
      return ss.str();
   }

   virtual std::string toInsertValue() const
   {
      throw std::logic_error("Cannot insert a compound node");
   }

   std::string op;
   QBNodeList nodes;
};

} // namespace detail
} // namespace database
} // namespace core
} // namespace rstudio

#endif // CORE_DETAILS_QBNODELIST_HPP
