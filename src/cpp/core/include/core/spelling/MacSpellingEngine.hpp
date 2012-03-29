/*
 * MacSpellingEngine.hpp
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

#ifndef CORE_SPELLING_MAC_SPELLING_ENGINE_HPP
#define CORE_SPELLING_MAC_SPELLING_ENGINE_HPP

#include <boost/scoped_ptr.hpp>

#include <core/spelling/SpellingEngine.hpp>

namespace core {
namespace spelling {

class MacSpellingEngine : public SpellingEngine
{
public:
   MacSpellingEngine();

public:
   Error initialize();

   Error checkSpelling(const std::string& langId,
                       const std::string& word,
                       bool *pCorrect);

   Error suggestionList(const std::string& langId,
                        const std::string& word,
                        std::vector<std::string>* pSugs);

   Error addWord(const std::string& langId,
                 const std::string& word,
                 bool *pAdded);

   Error removeWord(const std::string& langId,
                    const std::string& word,
                    bool *pRemoved);

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

} // namespace spelling
} // namespace core 


#endif // CORE_SPELLING_MAC_SPELLING_ENGINE_HPP

