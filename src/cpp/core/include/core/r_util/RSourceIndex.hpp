/*
 * RSourceIndex.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_R_UTIL_R_SOURCE_INDEX_HPP
#define CORE_R_UTIL_R_SOURCE_INDEX_HPP

#include <string>
#include <vector>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/utility.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Algorithm.hpp>

namespace core {
namespace r_util {

class RFunctionInfo
{
public:
   RFunctionInfo(const std::string& name,
                 std::size_t line,
                 std::size_t column)
      : name_(name), line_(line), column_(column)
   {
   }

   RFunctionInfo(const std::string& context,
                 const std::string& name,
                 std::size_t line,
                 std::size_t column)
      : context_(context), name_(name), line_(line), column_(column)
   {
   }

   virtual ~RFunctionInfo() {}

   // COPYING: via compiler (copyable members)

   // accessors
   const std::string& context() const { return context_; }
   const std::string& name() const { return name_; }
   std::size_t line() const { return line_; }
   std::size_t column() const { return column_; }

   // support for RSourceIndex::findFunction

   bool nameStartsWith(const std::string& term) const
   {
      return boost::algorithm::starts_with(name_, term);
   }

   bool nameContains(const std::string& term) const
   {
      return boost::algorithm::contains(name_, term);
   }

   RFunctionInfo withContext(const std::string& context) const
   {
      return RFunctionInfo(context, name_, line_, column_);
   }

private:
   std::string context_;
   std::string name_;
   std::size_t line_;
   std::size_t column_;
};


class RSourceIndex : boost::noncopyable
{
public:
   // Index the provided R source code so that we can efficiently search
   // for functions within it
   //
   // Requirements for code:
   //   - Must be UTF-8 encoded
   //   - Must use \n only for linebreaks
   //
   RSourceIndex(const std::string& context,
                const std::string& code);

   template <typename OutputIterator>
   OutputIterator findFunction(const std::string& term,
                               bool prefixOnly,
                               OutputIterator out) const
   {
      // define the predicate
      boost::function<bool(const RFunctionInfo&)> predicate;
      if (prefixOnly)
         predicate = boost::bind(&RFunctionInfo::nameStartsWith, _1, term);
      else
         predicate = boost::bind(&RFunctionInfo::nameContains, _1, term);

      // perform the copy and transform to include context
      core::algorithm::copy_transformed_if(
                functions_.begin(),
                functions_.end(),
                out,
                predicate,
                boost::bind(&RFunctionInfo::withContext, _1, context_));

      // return the output iterator
      return out;
   }

private:
   std::string context_;
   std::vector<RFunctionInfo> functions_;
};


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_SOURCE_INDEX_HPP

