/*
 * SessionFind.hpp
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

#ifndef SESSION_FIND_HPP
#define SESSION_FIND_HPP

#include <core/StringUtils.hpp>

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

boost::regex getGrepOutputRegex(bool isGitGrep);

boost::regex getColorEncodingRegex(bool isGitGrep);

// helper class used to process file replacements
class Replacer : public boost::noncopyable
{
public:
   explicit Replacer(bool ignoreCase,
                     std::string encoding = "") :
      encoding_(encoding),
      ignoreCase_(ignoreCase)
   {
   }

   core::Error replacePreview(const size_t dMatchOn, const size_t dMatchOff,
                              size_t eMatchOn, size_t eMatchOff,
                              std::string* pEncodedLine, std::string* pDecodedLine,
                              size_t* pReplaceMatchOff) const;

   void replaceLiteral(size_t matchOn, size_t matchOff,
                       const std::string& replaceLiteral, std::string* pLine,
                       size_t* pReplaceMatchOff) const
   {
      *pLine = pLine->replace(matchOn, (matchOff - matchOn), replaceLiteral);
      *pReplaceMatchOff = matchOn + replaceLiteral.size();

      std::string matchOffString = pLine->substr(0, *pReplaceMatchOff);
      core::string_utils::utf8Distance(matchOffString.begin(),
                                       matchOffString.end(),
                                       pReplaceMatchOff);
   }

   core::Error replaceRegex(size_t matchOn, size_t matchOff,
                            const std::string& findRegex, const std::string& replaceRegex,
                            std::string* pLine, size_t* pReplaceMatchOff) const
   {
      core::Error error;
      if (ignoreCase_)
         error = replaceRegexIgnoreCase(matchOn, matchOff, findRegex, replaceRegex, pLine,
                                        pReplaceMatchOff);
      else
         error = replaceRegexWithCase(matchOn, matchOff, findRegex, replaceRegex, pLine,
                                      pReplaceMatchOff);
      return error;
   }
   std::string decode(const std::string& encoded) const;

   static std::string decode(const std::string& encoded, const std::string& encoding,
                             bool& firstDecodeError);


private:
   std::string encoding_;
   bool ignoreCase_;
   core::Error completeReplace(const boost::regex& searchRegex, const std::string& replaceRegex,
                               size_t matchOn, size_t matchOff, std::string* pLine,
                               size_t* pReplaceMatchOff) const;

   core::Error replaceRegexIgnoreCase(size_t matchOn, size_t matchOff,
                                      const std::string& findRegex, const std::string& replaceRegex,
                                      std::string* pLine, size_t* pReplaceMatchOff) const;

   core::Error replaceRegexWithCase(size_t matchOn, size_t matchOff,
                                    const std::string& findRegex, const std::string& replaceRegex,
                                    std::string* pLine, size_t* pReplaceMatchOff) const;
};

} // namespace find
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_FIND_HPP
