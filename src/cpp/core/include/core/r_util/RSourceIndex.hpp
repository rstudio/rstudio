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

class RSourceItem
{
public:
   enum Type
   {
      None = 0,
      Function = 1,
      Method = 2,
      Class = 3
   };

public:
   RSourceItem(int type,
               const std::string& name,
               std::size_t line,
               std::size_t column)
      : type_(type), name_(name), line_(line), column_(column)
   {
   }

   RSourceItem(const std::string& context,
               int type,
               const std::string& name,
               std::size_t line,
               std::size_t column)
      : context_(context), type_(type), name_(name), line_(line), column_(column)
   {
   }

   virtual ~RSourceItem() {}

   // COPYING: via compiler (copyable members)

   // accessors
   int type() const { return type_; }
   const std::string& context() const { return context_; }
   const std::string& name() const { return name_; }
   int line() const { return core::safe_convert::numberTo<int>(line_,0); }
   int column() const { return core::safe_convert::numberTo<int>(column_,0); }

   // support for RSourceIndex::search

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

   RSourceItem withContext(const std::string& context) const
   {
      return RSourceItem(context, type_, name_, line_, column_);
   }

private:
   std::string context_;
   int type_;
   std::string name_;
   std::size_t line_;
   std::size_t column_;

// convenience friendship for doing vector operations e.g. resize()
private:
   friend class std::vector<RSourceItem>;
   RSourceItem() : line_(0), column_(0) {}
};


class RSourceIndex : boost::noncopyable
{
public:
   // Create empty source index (searches will always return no results)
   RSourceIndex()
   {
   }

   // Index the provided R source code so that we can efficiently search
   // for functions within it
   //
   // Requirements for code:
   //   - Must be UTF-8 encoded
   //   - Must use \n only for linebreaks
   //
   RSourceIndex(const std::string& context,
                const std::string& code)
   {
      update(context, code);
   }

   const std::string& context() const { return context_; }

   // update the source index
   void update(const std::string& context, const std::string& code);

   // query for whether the source index is empty
   bool empty() const { return context_.empty(); }

   void clear()
   {
      context_.clear();
      items_.clear();
   }

   template <typename OutputIterator>
   OutputIterator search(const std::string& term,
                         const std::string& newContext,
                         bool prefixOnly,
                         bool caseSensitive,
                         OutputIterator out) const
   {
      // define the predicate
      boost::function<bool(const RSourceItem&)> predicate;

      // check for wildcard character
      if (term.find('*') != std::string::npos)
      {
         boost::regex patternRegex = patternToRegex(caseSensitive ?
                                                      term :
                                                      string_utils::toLower(term));
         predicate = boost::bind(&RSourceItem::nameMatches,
                                    _1,
                                    patternRegex,
                                    prefixOnly,
                                    caseSensitive);
      }
      else
      {
         if (prefixOnly)
            predicate = boost::bind(&RSourceItem::nameStartsWith,
                                       _1, term, caseSensitive);
         else
            predicate = boost::bind(&RSourceItem::nameContains,
                                       _1, term, caseSensitive);
      }

      // perform the copy and transform to include context
      core::algorithm::copy_transformed_if(
                items_.begin(),
                items_.end(),
                out,
                predicate,
                boost::bind(&RSourceItem::withContext, _1, newContext));

      // return the output iterator
      return out;
   }

   template <typename OutputIterator>
   OutputIterator search(const std::string& term,
                         bool prefixOnly,
                         bool caseSensitive,
                         OutputIterator out) const
   {
      return search(term, context_, prefixOnly, caseSensitive, out);
   }

private:
   static boost::regex patternToRegex(const std::string& pattern);

private:
   std::string context_;
   std::vector<RSourceItem> items_;
};


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_SOURCE_INDEX_HPP

