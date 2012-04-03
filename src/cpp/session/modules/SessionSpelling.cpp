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

#include <core/spelling/HunspellSpellingEngine.hpp>
#ifdef __APPLE__
#include <core/spelling/MacSpellingEngine.hpp>
#endif

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>
#include <r/RExec.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace spelling {

namespace {

// underlying spelling engine (may be hunspell or osx native)
boost::scoped_ptr<core::spelling::SpellingEngine> s_pSpellingEngine;

// R function for testing & debugging
SEXP rs_checkSpelling(SEXP wordSEXP)
{
   bool isCorrect;
   std::string word = r::sexp::asString(wordSEXP);

   Error error = s_pSpellingEngine->checkSpelling(word, &isCorrect);

   // We'll return true here so as not to tie up the front end.
   if (error)
   {
      LOG_ERROR(error);
      isCorrect = true;
   }

   r::sexp::Protect rProtect;
   return r::sexp::create(isCorrect, &rProtect);
}

SEXP rs_learnWord(SEXP wordSEXP)
{
   std::string word = r::sexp::asString(wordSEXP);
   Error error = s_pSpellingEngine->learnWord(word);
   if (error)
      LOG_ERROR(error);

   return R_NilValue;
}

json::Object dictionaryAsJson(const core::spelling::HunspellDictionary& dict)
{
   json::Object dictJson;
   dictJson["id"] = dict.id();
   dictJson["name"] = dict.name();
   return dictJson;
}

FilePath userDictionariesDir()
{
   return module_context::userScratchPath().childPath("dictionaries");
}

FilePath allLanguagesDir()
{
   return module_context::userScratchPath().childPath(
                                          "dictionaries/languages-system");
}


Error checkSpelling(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   json::Array words;
   Error error = json::readParams(request.params, &words);
   if (error)
      return error;

   json::Array misspelledIndexes;
   for (std::size_t i=0; i<words.size(); i++)
   {
      if (!json::isType<std::string>(words[i]))
      {
         BOOST_ASSERT(false);
         continue;
      }

      std::string word = words[i].get_str();
      bool isCorrect = true;
      error = s_pSpellingEngine->checkSpelling(word, &isCorrect);
      if (error)
         return error;

      if (!isCorrect)
         misspelledIndexes.push_back(static_cast<int>(i));
   }

   pResponse->setResult(misspelledIndexes);

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
   error = s_pSpellingEngine->suggestionList(word, &sugs);
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

Error learnWord(const json::JsonRpcRequest& request,
                json::JsonRpcResponse*)
{
   std::string word;
   Error error = json::readParams(request.params, &word);
   if (error)
      return error;

   return s_pSpellingEngine->learnWord(word);
}

Error installAllDictionaries(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   // form system path to all languages dir
   std::string targetDir = string_utils::utf8ToSystem(
                                    allLanguagesDir().absolutePath());

   // perform the download
   r::exec::RFunction dlFunc(".rs.downloadAllDictionaries", targetDir);
   Error error = dlFunc.call();
   if (error)
   {
      std::string userMessage = r::endUserErrorMessage(error);
      pResponse->setError(error, json::Value(userMessage));
      return Success();
   }
   else
   {
      pResponse->setResult(spelling::spellingPrefsContextAsJson());
      return Success();
   }
}


// reset dictionary on user settings changed
void onUserSettingsChanged()
{
   s_pSpellingEngine->useDictionary(userSettings().spellingLanguage());
}

} // anonymous namespace


core::json::Object spellingPrefsContextAsJson()
{
   using namespace core::spelling;

   core::json::Object contextJson;

   HunspellDictionaryManager dictManager(options().hunspellDictionariesPath(),
                                         userDictionariesDir());

   std::vector<HunspellDictionary> dictionaries;
   Error error = dictManager.availableLanguages(&dictionaries);
   if (error)
   {
      LOG_ERROR(error);
      return core::json::Object();
   }

   core::json::Array dictionariesJson;
   std::transform(dictionaries.begin(),
                  dictionaries.end(),
                  std::back_inserter(dictionariesJson),
                  dictionaryAsJson);


   // return json
   contextJson["all_languages_installed"] = dictManager.allLanguagesInstalled();
   contextJson["available_languages"] = dictionariesJson;
   return contextJson;
}

Error initialize()
{
   // register rs_ensureFileHidden with R
   R_CallMethodDef methodDef;

   methodDef.name = "rs_checkSpelling" ;
   methodDef.fun = (DL_FUNC) rs_checkSpelling ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   methodDef.name = "rs_learnWord" ;
   methodDef.fun = (DL_FUNC) rs_learnWord ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   // initialize spelling engine
   using namespace core::spelling;
#ifdef __APPLE__
   s_pSpellingEngine.reset(new MacSpellingEngine());
#else
   HunspellDictionaryManager dictManager(
                           session::options().hunspellDictionariesPath(),
                           userDictionariesDir());
   HunspellSpellingEngine* pHunspell = new HunspellSpellingEngine(
                                                      dictManager,
                                                      &r::util::iconvstr);
   s_pSpellingEngine.reset(pHunspell);
#endif

   // set dictionary
   s_pSpellingEngine->useDictionary(userSettings().spellingLanguage());

   // connect to user settings changed
   userSettings().onChanged.connect(onUserSettingsChanged);

   // register rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "check_spelling", checkSpelling))
      (bind(registerRpcMethod, "suggestion_list", suggestionList))
      (bind(registerRpcMethod, "learn_word", learnWord))
      (bind(registerRpcMethod, "install_all_dictionaries", installAllDictionaries))
      (bind(sourceModuleRFile, "SessionSpelling.R"));
   return initBlock.execute();
}


} // namespace spelling
} // namespace modules
} // namesapce session

