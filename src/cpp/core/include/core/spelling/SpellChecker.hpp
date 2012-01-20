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

#include <string>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>


namespace core {

class Error;

namespace spelling {

class SpellChecker : boost::noncopyable
{
public:
   virtual ~SpellChecker() {}
   virtual bool checkSpelling(const std::string& word) = 0;
};

core::Error createHunspell(const std::string& libPath,
                           const std::string& affPath,
                           const std::string& dicPath,
                           boost::shared_ptr<SpellChecker>* pHunspell);


} // namespace spelling
} // namespace core 


#endif // CORE_SPELLING_SPELL_CHECKER_HPP

