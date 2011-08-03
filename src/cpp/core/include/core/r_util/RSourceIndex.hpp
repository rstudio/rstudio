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
#include <boost/regex.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Algorithm.hpp>
#include <core/SafeConvert.hpp>
#include <core/StringUtils.hpp>

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
   int line() const { return core::safe_convert::numberTo<int>(line_,0); }
   int column() const { return core::safe_convert::numberTo<int>(column_,0); }

   // support for RSourceIndex::findFunction

   bool nameStartsWith(const std::string& term, bool caseSensitive) const
   {
      if (caseSensitive)
         return boost::algorithm::starts_with(name_, term);
      else
         return boost::algorithm::istarts_with(name_, term);
   }

   bool nameContains(const std::string& term, bool caseSensitive) const
   {
      if (caseSensitive)
         return boost::algorithm::contains(name_, term);
      else
         return boost::algorithm::icontains(name_, term);
   }

   bool nameMatches(const boost::regex& regex,
                    bool prefixOnly,
                    bool caseSensitive) const
   {
      boost::smatch match;
      boost::match_flag_type flags = boost::match_default;
      if (prefixOnly)
         flags |= boost::match_continuous;
      return regex_search(caseSensitive ? name_ : string_utils::toLower(name_),
                          match,
                          regex,
                          flags);
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

// convenience friendship for doing vector operations e.g. resize()
private:
   friend class std::vector<RFunctionInfo>;
   RFunctionInfo() : line_(0), column_(0) {}
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
                               bool caseSensitive,
                               OutputIterator out) const
   {
      // define the predicate
      boost::function<bool(const RFunctionInfo&)> predicate;

      // check for wildcard character
      if (term.find('*') != std::string::npos)
      {
         boost::regex patternRegex = patternToRegex(caseSensitive ?
                                                      term :
                                                      string_utils::toLower(term));
         predicate = boost::bind(&RFunctionInfo::nameMatches,
                                    _1,
                                    patternRegex,
                                    prefixOnly,
                                    caseSensitive);
      }
      else
      {
         if (prefixOnly)
            predicate = boost::bind(&RFunctionInfo::nameStartsWith,
                                       _1, term, caseSensitive);
         else
            predicate = boost::bind(&RFunctionInfo::nameContains,
                                       _1, term, caseSensitive);
      }

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
   static boost::regex patternToRegex(const std::string& pattern);

private:
   std::string context_;
   std::vector<RFunctionInfo> functions_;
};


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_SOURCE_INDEX_HPP

