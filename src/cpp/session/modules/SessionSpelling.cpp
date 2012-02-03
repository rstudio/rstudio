/*
 * SessionSpelling.cpp
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

#include "SessionSpelling.hpp"

#include <boost/shared_ptr.hpp>

#include <core/Error.hpp>

#include <core/spelling/SpellChecker.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace spelling {

namespace {

// spell checking engine
boost::shared_ptr<core::spelling::SpellChecker> s_pSpellChecker;

// R function for testing & debugging
SEXP rs_checkSpelling(SEXP wordSEXP)
{
   bool isCorrect;
   std::string word = r::sexp::asString(wordSEXP);

   Error error = s_pSpellChecker->checkSpelling(word,&isCorrect);

   r::sexp::Protect rProtect;
   return r::sexp::create(isCorrect, &rProtect);
}

SEXP rs_suggestionList(SEXP wordSEXP)
{
    std::string word = r::sexp::asString(wordSEXP);
    std::vector<std::string> sugs;

    s_pSpellChecker->suggestionList(word,&sugs);

    r::sexp::Protect rProtect;
    return r::sexp::create(sugs,&rProtect);
}

SEXP rs_analyzeWord(SEXP wordSEXP)
{
   std::string word = r::sexp::asString(wordSEXP);
   std::vector<std::string> res;

   s_pSpellChecker->suggestionList(word,&res);

   r::sexp::Protect rProtect;
   return r::sexp::create(res,&rProtect);
}


} // anonymous namespace


Error initialize()
{
   // register rs_ensureFileHidden with R
   R_CallMethodDef checkSpellingMethodDef;

   checkSpellingMethodDef.name = "rs_checkSpelling" ;
   checkSpellingMethodDef.fun = (DL_FUNC) rs_checkSpelling ;
   checkSpellingMethodDef.numArgs = 1;
   r::routines::addCallMethod(checkSpellingMethodDef);

   checkSpellingMethodDef.name = "rs_suggestionList" ;
   checkSpellingMethodDef.fun = (DL_FUNC) rs_suggestionList ;
   checkSpellingMethodDef.numArgs = 1;
   r::routines::addCallMethod(checkSpellingMethodDef);

   checkSpellingMethodDef.name = "rs_analyzeWord" ;
   checkSpellingMethodDef.fun = (DL_FUNC) rs_analyzeWord ;
   checkSpellingMethodDef.numArgs = 1;
   r::routines::addCallMethod(checkSpellingMethodDef);

   // initialize the spell checker
   using namespace core::spelling;
   session::Options& options = session::options();
   FilePath enUSPath = options.hunspellDictionariesPath().childPath("en_US");
   return createHunspell(enUSPath.childPath("en_US.aff"),
                         enUSPath.childPath("en_US.dic"),
                         &s_pSpellChecker,
                         r::util::iconvstr);
}


} // namespace spelling
} // namespace modules
} // namesapce session

