/*
 * HunspellSpellingEngine.hpp
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

#ifndef CORE_SPELLING_HUNSPELL_SPELLING_ENGINE_HPP
#define CORE_SPELLING_HUNSPELL_SPELLING_ENGINE_HPP

#include <boost/scoped_ptr.hpp>
#include <boost/function.hpp>

#include <core/spelling/SpellingEngine.hpp>

#include <core/spelling/HunspellDictionaryManager.hpp>

namespace rstudio {
namespace core {

class FilePath;

namespace spelling {

typedef boost::function<core::Error(const std::string&,
                                    const std::string&,
                                    const std::string&,
                                    bool,
                                    std::string*)> IconvstrFunction;

class HunspellSpellingEngine : public SpellingEngine
{
public:
   HunspellSpellingEngine(const std::string& langId,
                          const HunspellDictionaryManager& dictionaryManager,
                          const IconvstrFunction& iconvstrFunction);

public:

   void useDictionary(const std::string& langId);

   Error checkSpelling(const std::string& word,
                       bool *pCorrect);

   Error suggestionList(const std::string& word,
                        std::vector<std::string>* pSugs);

   Error wordChars(std::wstring* pChars);

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

} // namespace spelling
} // namespace core 
} // namespace rstudio


#endif // CORE_SPELLING_HUNSPELL_SPELLING_ENGINE_HPP

