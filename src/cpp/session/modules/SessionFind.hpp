/*
 * SessionFind.hpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#ifndef SESSION_FIND_HPP
#define SESSION_FIND_HPP

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <boost/utility.hpp>
#include <boost/regex.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace find {

core::json::Object findInFilesStateAsJson();

core::Error initialize();

// helper class used to process file replacements
class Replacer : public boost::noncopyable
{
public:
   explicit Replacer(bool ignoreCase) :
      ignoreCase_(ignoreCase)
   {
   }

   void replaceLiteralWithLiteral(int matchOn, int matchOff, std::string* line,
                                  const std::string* replaceLiteral,
                                  int* pReplaceMatchOff)
   {
      *line = line->replace(matchOn, (matchOff - matchOn), *replaceLiteral);
      *pReplaceMatchOff = matchOn + replaceLiteral->size();
   }

   core::Error replaceLiteralWithRegex(int matchOn, int matchOff, std::string* line,
                                const std::string* replaceRegex,
                                int* pReplaceMatchOff)
   {
      std::string find(line->substr(matchOn, matchOff));
      return (replaceRegexWithRegex(matchOn, matchOff, line, &find,
                                    replaceRegex, pReplaceMatchOff));
   }

   core::Error replaceRegexWithLiteral(int matchOn, int matchOff, std::string* line,
                                 const std::string* findRegex, const std::string* replaceLiteral,
                                 int* pReplaceMatchOff)
   {
      return (replaceRegexWithRegex(matchOn, matchOff, line, findRegex, replaceLiteral,
                                    pReplaceMatchOff));
   }

   core::Error replaceRegexWithRegex(int matchOn, int matchOff, std::string* line,
                              const std::string* findRegex, const std::string* replaceRegex,
                              int* pReplaceMatchOff)
  {
     core::Error error;
     if (ignoreCase_)
        error = replaceRegexWithCaseInsensitiveRegex(matchOn, matchOff, line, findRegex, replaceRegex,
                                                     pReplaceMatchOff);
     else
        error = replaceRegexWithCaseSensitiveRegex(matchOn, matchOff, line, findRegex, replaceRegex,
                                                   pReplaceMatchOff);
     return error;
  }

private:
   core::Error replaceRegexWithRegex(const boost::regex* searchRegex, const boost::regex* replaceRegex,
                               int matchOn, int matchOff, std::string* line,
                               int* pReplaceMatchOff)
   {
      std::string temp;
      try
      {
         temp = boost::regex_replace(*line, *searchRegex, *replaceRegex);
      }
      catch (const std::runtime_error& e)
      {
         return core::Error(-1, e.what(), ERROR_LOCATION);
      }

      std::string endOfString = line->substr(matchOff).c_str();
      size_t replaceMatchOff;
      if (endOfString.empty())
         replaceMatchOff = temp.length();
      else
         replaceMatchOff = temp.find(endOfString);
      *line = temp;
      std::string replaceString = temp.substr(matchOn, (replaceMatchOff - matchOn));
      *pReplaceMatchOff = matchOn  + replaceString.size();
      return core::Success();
   }

   core::Error replaceRegexWithCaseInsensitiveRegex(int matchOn, int matchOff, std::string* line,
                                              const std::string* findRegex, const std::string* replaceRegex,
                                              int* pReplaceMatchOff)
   {
      try
      {
         boost::regex find(findRegex->c_str(), boost::regex::grep | boost::regex::icase);
         boost::regex replace(replaceRegex->c_str(), boost::regex::grep | boost::regex::icase);

         core::Error error = replaceRegexWithRegex(&find, &replace, matchOn, matchOff, line,
                                             pReplaceMatchOff);
         return error;
      }
      catch (const std::runtime_error& e)
      {
         return core::Error(-1, e.what(), ERROR_LOCATION);
      }
   }

   core::Error replaceRegexWithCaseSensitiveRegex(int matchOn, int matchOff, std::string* line,
                                            const std::string* findRegex, const std::string* replaceRegex,
                                            int* pReplaceMatchOff)
   {
      try
      {
         boost::regex find(findRegex->c_str(), boost::regex::grep);
         boost::regex replace(replaceRegex->c_str(), boost::regex::grep);

         core::Error error = replaceRegexWithRegex(&find, &replace, matchOn, matchOff, line,
                                             pReplaceMatchOff);
         return error;
      }
      catch (const std::runtime_error& e)
      {
         return core::Error(-1, e.what(), ERROR_LOCATION);
      }
   }

private:
   bool ignoreCase_;
};

} // namespace find
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_FIND_HPP
