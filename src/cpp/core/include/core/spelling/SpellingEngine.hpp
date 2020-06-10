/*
 * SpellingEngine.hpp
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

#ifndef CORE_SPELLING_SPELLING_ENGINE_HPP
#define CORE_SPELLING_SPELLING_ENGINE_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>

namespace rstudio {
namespace core {

class Error;

namespace spelling {

class SpellingEngine : boost::noncopyable
{
public:
   virtual ~SpellingEngine() {}

   virtual void useDictionary(const std::string& langId) = 0;

   virtual Error checkSpelling(const std::string& word,
                               bool *pCorrect) = 0;

   virtual Error suggestionList(const std::string& word,
                                std::vector<std::string>* pSugs) = 0;

   virtual Error wordChars(std::wstring* pChars) = 0;
};

} // namespace spelling
} // namespace core 
} // namespace rstudio


#endif // CORE_SPELLING_SPELLING_ENGINE_HPP

