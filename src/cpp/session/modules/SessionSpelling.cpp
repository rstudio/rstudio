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
#include <core/spelling/HunspellDictionaryManager.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace spelling {

namespace {

class SpellingEngine : boost::noncopyable
{
private:
   friend SpellingEngine& spellingEngine();
   SpellingEngine()
      : dictManager_(
            session::options().hunspellDictionariesPath(),
            module_context::userScratchPath().complete("dictionaries"))
   {
   }

public:
   core::spelling::hunspell::DictionaryManager& dictionaryManager()
   {
      return dictManager_;
   }

   core::spelling::SpellChecker& spellChecker(const std::string& langId)
   {
      if (!pSpellChecker_ || (langId != currentLangId_))
      {
         core::spelling::hunspell::Dictionary dict =
                           dictManager_.dictionaryForLanguageId(langId);
         if (!dict.empty())
         {
            Error error = core::spelling::createHunspell(dict.affPath(),
                                                         dict.dicPath(),
                                                         &pSpellChecker_,
                                                         &r::util::iconvstr);
            if (!error)
               currentLangId_ = langId;
            else
               pSpellChecker_.reset(new core::spelling::NoSpellChecker());
         }
         else
         {
            pSpellChecker_.reset(new core::spelling::NoSpellChecker());
         }
      }
      return *pSpellChecker_;
   }


private:
   core::spelling::hunspell::DictionaryManager dictManager_;
   boost::shared_ptr<core::spelling::SpellChecker> pSpellChecker_;
   std::string currentLangId_;
};

SpellingEngine& spellingEngine()
{
   static SpellingEngine instance;
   return instance;
}

// R function for testing & debugging
SEXP rs_checkSpelling(SEXP wordSEXP)
{
   bool isCorrect;
   std::string word = r::sexp::asString(wordSEXP);

   Error error = spellingEngine().spellChecker("en_US").checkSpelling(
                                                                   word,
                                                                   &isCorrect);

   // We'll return true here so as not to tie up the front end.
   if (error)
   {
      LOG_ERROR(error);
      isCorrect = true;
   }

   r::sexp::Protect rProtect;
   return r::sexp::create(isCorrect, &rProtect);
}

Error checkSpelling(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string langId, word;
   Error error = json::readParams(request.params, &langId, &word);
   if (error)
      return error;

   bool isCorrect;
   error = spellingEngine().spellChecker(langId).checkSpelling(word,&isCorrect);
   if (error)
      return error;

   pResponse->setResult(isCorrect);

   return Success();
}

Error suggestionList(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string langId, word;
   Error error = json::readParams(request.params, &langId, &word);
   if (error)
      return error;

   std::vector<std::string> sugs;
   error = spellingEngine().spellChecker(langId).suggestionList(word,&sugs);
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
   std::string langId, word;
   Error error = json::readParams(request.params, &langId, &word);
   if (error)
      return error;

   bool added;
   error = spellingEngine().spellChecker(langId).addWord(word,&added);
   if (error)
      return error;

   pResponse->setResult(added);
   return Success();
}


json::Object dictionaryAsJson(const core::spelling::hunspell::Dictionary& dict)
{
   json::Object dictJson;
   dictJson["id"] = dict.id();
   dictJson["name"] = dict.name();
   return dictJson;
}

} // anonymous namespace


core::json::Array availableLanguagesAsJson()
{
   using namespace core::spelling::hunspell;

   FilePath userDictionariesDir =
                  module_context::userScratchPath().complete("dictionaries");
   DictionaryManager dictManager(options().hunspellDictionariesPath(),
                                 userDictionariesDir);

   std::vector<Dictionary> dictionaries;
   Error error = dictManager.availableLanguages(&dictionaries);
   if (error)
   {
      LOG_ERROR(error);
      return core::json::Array();
   }

   core::json::Array dictionariesJson;
   std::transform(dictionaries.begin(),
                  dictionaries.end(),
                  std::back_inserter(dictionariesJson),
                  dictionaryAsJson);

   return dictionariesJson;
}

Error initialize()
{
   // register rs_ensureFileHidden with R
   R_CallMethodDef methodDef;

   methodDef.name = "rs_checkSpelling" ;
   methodDef.fun = (DL_FUNC) rs_checkSpelling ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

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

