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
#include <core/Exec.hpp>

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

   // We'll return true here so as not to tie up the front end.
   if (error)
   {
      LOG_ERROR(error);
      isCorrect = true;
   }

   r::sexp::Protect rProtect;
   return r::sexp::create(isCorrect, &rProtect);
}

SEXP rs_suggestionList(SEXP wordSEXP)
{
   std::string word = r::sexp::asString(wordSEXP);
   std::vector<std::string> sugs;

   Error error = s_pSpellChecker->suggestionList(word,&sugs);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect rProtect;
   return r::sexp::create(sugs,&rProtect);
}

SEXP rs_analyzeWord(SEXP wordSEXP)
{
   std::string word = r::sexp::asString(wordSEXP);
   std::vector<std::string> res;

   Error error = s_pSpellChecker->analyzeWord(word,&res);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect rProtect;
   return r::sexp::create(res,&rProtect);
}

SEXP rs_stemWord(SEXP wordSEXP)
{
   std::string word = r::sexp::asString(wordSEXP);
   std::vector<std::string> res;

   Error error = s_pSpellChecker->stemWord(word,&res);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect rProtect;
   return r::sexp::create(res,&rProtect);
}

SEXP rs_addWord(SEXP wordSEXP)
{
   std::string word = r::sexp::asString(wordSEXP);
   bool added;

   Error error = s_pSpellChecker->addWord(word,&added);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect rProtect;
   return r::sexp::create(added,&rProtect);
}

SEXP rs_removeWord(SEXP wordSEXP)
{
   std::string word = r::sexp::asString(wordSEXP);
   bool removed;

   Error error = s_pSpellChecker->removeWord(word,&removed);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect rProtect;
   return r::sexp::create(removed,&rProtect);
}

SEXP rs_addDictionary(SEXP dicSEXP, SEXP keySEXP)
{
   FilePath dicPath = FilePath(r::sexp::asString(dicSEXP));
   std::string key = r::sexp::asString(keySEXP);
   bool added;

   Error error = s_pSpellChecker->addDictionary(dicPath,key,&added);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect rProtect;
   return r::sexp::create(added,&rProtect);
}

Error checkSpelling(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string word;
   Error error = json::readParams(request.params, &word);
   if (error)
      return error;

   bool isCorrect;
   error = s_pSpellChecker->checkSpelling(word,&isCorrect);
   if (error)
      return error;

   pResponse->setResult(isCorrect);

   return Success();
}

Error suggestionList(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string word;
   Error error = json::readParams(request.params, &word);
   if (error)
      return error;

   std::vector<std::string> sugs;
   error = s_pSpellChecker->suggestionList(word,&sugs);
   if (error)
      return error;

   json::Array sugsJson;
   std::transform(sugs.begin(),
                  sugs.end(),
                  std::back_inserter(sugsJson),
                  json::toJsonString);
   pResponse->setResult(sugsJson);

   return Success();
}

Error addToDictionary(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string word;
   Error error = json::readParams(request.params, &word);
   if (error)
      return error;

   bool added;
   error = s_pSpellChecker->addWord(word,&added);
   if (error)
      return error;

   pResponse->setResult(added);
   return Success();
}


} // anonymous namespace


Error initialize()
{
   // register rs_ensureFileHidden with R
   R_CallMethodDef methodDef;

   methodDef.name = "rs_checkSpelling" ;
   methodDef.fun = (DL_FUNC) rs_checkSpelling ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   methodDef.name = "rs_suggestionList" ;
   methodDef.fun = (DL_FUNC) rs_suggestionList ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   methodDef.name = "rs_analyzeWord" ;
   methodDef.fun = (DL_FUNC) rs_analyzeWord ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   methodDef.name = "rs_stemWord" ;
   methodDef.fun = (DL_FUNC) rs_stemWord ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   methodDef.name = "rs_addWord" ;
   methodDef.fun = (DL_FUNC) rs_addWord ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   methodDef.name = "rs_removeWord" ;
   methodDef.fun = (DL_FUNC) rs_removeWord ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   methodDef.name = "rs_addDictionary" ;
   methodDef.fun = (DL_FUNC) rs_addDictionary ;
   methodDef.numArgs = 2;
   r::routines::addCallMethod(methodDef);

   // initialize the spell checker
   using namespace core::spelling;
   session::Options& options = session::options();
   FilePath enUSPath = options.hunspellDictionariesPath().childPath("en_US");
   Error error = createHunspell(enUSPath.childPath("en_US.aff"),
                                enUSPath.childPath("en_US.dic"),
                                &s_pSpellChecker,
                                &r::util::iconvstr);
   if (error)
      return error;

   // register rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "check_spelling", checkSpelling))
      (bind(registerRpcMethod, "suggestion_list", suggestionList))
      (bind(registerRpcMethod, "add_to_dictionary", addToDictionary));
   return initBlock.execute();
}


} // namespace spelling
} // namespace modules
} // namesapce session

