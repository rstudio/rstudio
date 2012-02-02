/*
 * SpellChecker.hpp
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

#ifndef CORE_SPELLING_SPELL_CHECKER_HPP
#define CORE_SPELLING_SPELL_CHECKER_HPP

#include <vector>
#include <string>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>


namespace core {

class Error;
class FilePath;

namespace spelling {

class SpellChecker : boost::noncopyable
{
public:
   virtual ~SpellChecker() {}
   virtual bool checkSpelling(const std::string& word) = 0;
   virtual void suggestionList(const std::string& word, std::vector<std::string>* pSugs) = 0;
};

core::Error createHunspell(const core::FilePath& affPath,
                           const core::FilePath& dicPath,
                           boost::shared_ptr<SpellChecker>* pHunspell,
                           const boost::function<core::Error(const std::string& value,
                                                    const std::string& from,
                                                    const std::string& to,
                                                    bool allowSubstitution,
                                                    std::string* pResult)>& pIconvStr);


} // namespace spelling
} // namespace core 


#endif // CORE_SPELLING_SPELL_CHECKER_HPP

