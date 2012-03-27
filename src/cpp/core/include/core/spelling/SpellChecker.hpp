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

#include <core/Error.hpp>

namespace core {

class FilePath;

namespace spelling {

class SpellChecker : boost::noncopyable
{
public:
   virtual ~SpellChecker() {}
   virtual Error checkSpelling(const std::string& word, bool *pCorrect) = 0;
   virtual Error suggestionList(const std::string& word,
                                std::vector<std::string>* pSugs) = 0;
   virtual Error analyzeWord(const std::string& word,
                             std::vector<std::string>* pResult) = 0;
   virtual Error stemWord(const std::string& word,
                          std::vector<std::string>* pResult) = 0;
   virtual Error addWord(const std::string& word, bool *pAdded) = 0;
   virtual Error removeWord(const std::string& word, bool *pRemoved) = 0;
   virtual Error addDictionary(const FilePath& dicPath,
                               const std::string& key,
                               bool *pAdded) = 0;
};

class NoSpellChecker : public SpellChecker
{
public:
   virtual Error checkSpelling(const std::string& word, bool *pCorrect)
   {
      *pCorrect = true;
      return Success();
   }

   virtual Error suggestionList(const std::string& word,
                                std::vector<std::string>* pSugs)
   {
      return Success();
   }

   virtual Error analyzeWord(const std::string&,
                             std::vector<std::string>*)
   {
      return Success();
   }

   virtual Error stemWord(const std::string& word,
                          std::vector<std::string>* pResult)
   {
      return Success();
   }

   virtual Error addWord(const std::string& word, bool *pAdded)
   {
      return Success();
   }

   virtual Error removeWord(const std::string& word, bool *pRemoved)
   {
      return Success();
   }

   virtual Error addDictionary(const FilePath& dicPath,
                               const std::string& key,
                               bool *pAdded)
   {
      return Success();
   }
};


typedef boost::function<core::Error(const std::string&,
                                    const std::string&,
                                    const std::string&,
                                    bool,
                                    std::string*)> IconvstrFunction;

core::Error createHunspell(const core::FilePath& languageDicPath,
                           boost::shared_ptr<SpellChecker>* pHunspell,
                           const IconvstrFunction& iconvstrFunc);

#ifdef __APPLE__
core::Error createMac(boost::shared_ptr<SpellChecker>* pMac);
#endif

} // namespace spelling
} // namespace core 


#endif // CORE_SPELLING_SPELL_CHECKER_HPP

