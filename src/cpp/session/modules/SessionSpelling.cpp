/*
 * SessionSpelling.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionSpelling.hpp"

#include <gsl/gsl-lite.hpp>

#include <boost/shared_ptr.hpp>

#include <core/Exec.hpp>
#include <core/system/Xdg.hpp>

#include <core/Algorithm.hpp>
#include <core/spelling/HunspellSpellingEngine.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>
#include <r/RExec.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

#include <shared_core/Error.hpp>

#define kDictionariesPath "dictionaries"
#define kSystemLanguages kDictionariesPath "/languages-system"
#define kCustomDictionaries kDictionariesPath "/custom"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace spelling {

namespace {

// underlying spelling engine
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

json::Object dictionaryAsJson(const core::spelling::HunspellDictionary& dict)
{
   json::Object dictJson;
   dictJson["id"] = dict.id();
   dictJson["name"] = dict.name();
   return dictJson;
}

void syncSpellingEngineDictionaries()
{
   s_pSpellingEngine->useDictionary(prefs::userPrefs().spellingDictionaryLanguage());
}


core::spelling::HunspellDictionaryManager hunspellDictionaryManager()
{
   core::spelling::HunspellDictionaryManager dictManager(
                                         options().hunspellDictionariesPath(),
                                         userDictionariesDir());
   return dictManager;
}

} // end anonymous namespace

FilePath userDictionariesDir()
{
   return module_context::userScratchPath().completeChildPath("dictionaries");
}

/*
 * \deprecated
 * For getting all languages from pre-1.3 RStudio
 * */
FilePath legacyAllLanguagesDir()
{
   return module_context::userScratchPath().completeChildPath(kSystemLanguages);
}

/*
 * \deprecated
 * For getting custom languages from pre-1.3 RStudio
 * */
FilePath legacyCustomDictionariesDir()
{
   return module_context::userScratchPath().completeChildPath(kCustomDictionaries);
}

FilePath allDictionariesDir()
{
   return core::system::xdg::userConfigDir().completeChildPath(kDictionariesPath);
}

FilePath allLanguagesDir()
{
   return core::system::xdg::userConfigDir().completeChildPath(kSystemLanguages);
}

FilePath customDictionariesDir()
{
   return core::system::xdg::userConfigDir().completeChildPath(kCustomDictionaries);
}

namespace {

// This responds to the request path of /dictionaries/<dict>/<dict>.dic
// and returns the file at <userScratchDir>/dictionaries/<dict>.dic
// Typo.js expects a hardcoded path in this form and we want to avoid forking it
void handleDictionaryRequest(const http::Request& request, http::Response* pResponse)
{
   std::string prefix = "/dictionaries/";
   std::string fileName = http::util::pathAfterPrefix(request, prefix);
   std::vector<std::string> splat = core::algorithm::split(fileName, "/");

   if (splat.size() != 2)
   {
      pResponse->setNotFoundError(request);
      return;
   }

   // preference order: custom -> system -> pre-installed
   if (customDictionariesDir().completePath(splat[1]).exists())
   {
      pResponse->setCacheableFile(customDictionariesDir().completePath(splat[1]), request);
   }
   else if (allLanguagesDir().completePath(splat[1]).exists())
   {
      pResponse->setCacheableFile(allLanguagesDir().completePath(splat[1]), request);
   }
   else if (core::system::xdg::systemConfigDir()
               .completePath(kCustomDictionaries).completePath(splat[1]).exists())
   {
      pResponse->setCacheableFile(core::system::xdg::systemConfigFile(
               kCustomDictionaries).completePath(splat[1]), request);
   }
   else if (core::system::xdg::systemConfigDir()
               .completePath(kSystemLanguages).completePath(splat[1]).exists())
   {
      pResponse->setCacheableFile(core::system::xdg::systemConfigFile(
               kSystemLanguages).completePath(splat[1]), request);
   }
   /*
    * \deprecated
    * Calls to old deprecated dictionary locations for RStudio 1.2 and earlier
    */
   else if (legacyCustomDictionariesDir().completePath(splat[1]).exists())
   {
      pResponse->setCacheableFile(legacyCustomDictionariesDir().completePath(splat[1]), request);
   }
   else if (legacyAllLanguagesDir().completePath(splat[1]).exists())
   {
      pResponse->setCacheableFile(legacyAllLanguagesDir().completePath(splat[1]), request);
   }
   else if (options().hunspellDictionariesPath().completePath(splat[1]).exists())
   {
      pResponse->setCacheableFile(options().hunspellDictionariesPath().completePath(splat[1]), request);
   }
   else if (boost::algorithm::ends_with(splat[1], "aff"))
   {
      // the aff file is optional, especially for custom dictionaries
      pResponse->setCacheableBody("", request);
   }
   else
   {
      pResponse->setNotFoundError(request);
   }
}

Error checkSpelling(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   json::Array words;
   Error error = json::readParams(request.params, &words);
   if (error)
      return error;

   json::Array misspelledIndexes;
   for (std::size_t i=0; i<words.getSize(); i++)
   {
      if (!json::isType<std::string>(words[i]))
      {
         BOOST_ASSERT(false);
         continue;
      }

      std::string word = words[i].getString();
      bool isCorrect = true;
      error = s_pSpellingEngine->checkSpelling(word, &isCorrect);
      if (error)
      {
         // if we can't check a word, ignore it; some combinations of platform, non-ASCII
         // characters, and locale are known to fail in iconv, and we don't want to put those
         // failures in front of the user (we just won't be able to spell check those words)
         LOG_ERROR(error);
      }
      else if (!isCorrect) 
      {
         misspelledIndexes.push_back(gsl::narrow_cast<int>(i));
      }
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
                  json::toJsonValue<std::string>);
   pResponse->setResult(sugsJson);

   return Success();
}

Error getWordChars(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   std::wstring wordChars;
   Error error = s_pSpellingEngine->wordChars(&wordChars);
   if (error)
      return error;

   pResponse->setResult(string_utils::wideToUtf8(wordChars));

   return Success();
}



Error addCustomDictionary(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // get the argument
   std::string dict;
   Error error = json::readParams(request.params, &dict);
   if (error)
      return error;
   FilePath dictPath = module_context::resolveAliasedPath(dict);

   // verify .dic extension
   if (!dictPath.hasExtensionLowerCase(".dic"))
   {
      std::string msg = "Dictionary files must have a .dic extension";
      Error error(json::errc::ParamInvalid, ERROR_LOCATION);
      pResponse->setError(error, json::Value(msg));
      return Success();
   }

   // perform the add
   using namespace rstudio::core::spelling;
   HunspellDictionaryManager dictManager = hunspellDictionaryManager();
   error = dictManager.custom().add(dictPath);
   if (error)
      return error;

   // sync spelling engine
   syncSpellingEngineDictionaries();

   // return
   pResponse->setResult(json::toJsonArray(dictManager.custom().dictionaries()));
   return Success();
}

Error removeCustomDictionary(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   // get the argument
   std::string name;
   Error error = json::readParams(request.params, &name);
   if (error)
      return error;

   // perform the remove
   using namespace rstudio::core::spelling;
   HunspellDictionaryManager dictManager = hunspellDictionaryManager();
   error = dictManager.custom().remove(name);
   if (error)
      return error;

   // sync spelling engine
   syncSpellingEngineDictionaries();

   // return
   pResponse->setResult(json::toJsonArray(dictManager.custom().dictionaries()));
   return Success();
}

Error installAllDictionaries(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   // form system path to all languages dir
   std::string targetDir = string_utils::utf8ToSystem(
      allLanguagesDir().getAbsolutePath());

   // perform the download
   r::exec::RFunction dlFunc(".rs.downloadAllDictionaries",
                                targetDir,
                                module_context::haveSecureDownloadFileMethod());
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
void onUserSettingsChanged(const std::string& layer, const std::string& pref)
{
   if (pref != kSpellingDictionaryLanguage)
      return;

   syncSpellingEngineDictionaries();
}

SEXP rs_dictionariesPath(SEXP typeSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   
   r::sexp::Protect protect;
   
   if (type == "bundled")
   {
      return r::sexp::createUtf8(
               options().hunspellDictionariesPath().getAbsolutePath(),
               &protect);
   }
   else if (type == "extra")
   {
      return r::sexp::createUtf8(
               allDictionariesDir().getAbsolutePath(),
               &protect);
   }
   else if (type == "user")
   {
      return r::sexp::createUtf8(
               userDictionariesDir().getAbsolutePath(),
               &protect);
   }
   else
   {
      r::exec::warning("unknown dictionary type '" + type + "'");
      return R_NilValue;
   }
}

SEXP rs_userDictionariesPath()
{
   r::sexp::Protect protect;
   return r::sexp::create(
      userDictionariesDir().getAbsolutePath(),
            &protect);
}

} // anonymous namespace


core::json::Object spellingPrefsContextAsJson()
{
   using namespace rstudio::core::spelling;

   core::json::Object contextJson;

   HunspellDictionaryManager dictManager = hunspellDictionaryManager();
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


   std::vector<std::string> customDicts = dictManager.custom().dictionaries();
   core::json::Array customDictsJson = json::toJsonArray(customDicts);

   // return json
   contextJson["all_languages_installed"] = dictManager.allLanguagesInstalled();
   contextJson["available_languages"] = dictionariesJson;
   contextJson["custom_dictionaries"] = customDictsJson;
   return contextJson;
}

Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_checkSpelling);
   RS_REGISTER_CALL_METHOD(rs_dictionariesPath);
   RS_REGISTER_CALL_METHOD(rs_userDictionariesPath);

   // initialize spelling engine
   using namespace rstudio::core::spelling;
   HunspellSpellingEngine* pHunspell = new HunspellSpellingEngine(
      prefs::userPrefs().spellingDictionaryLanguage(),
      hunspellDictionaryManager(),
      &r::util::iconvstr);
   s_pSpellingEngine.reset(pHunspell);

   // connect to user settings changed
   prefs::userPrefs().onChanged.connect(onUserSettingsChanged);

   // register rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "check_spelling", checkSpelling))
      (bind(registerRpcMethod, "suggestion_list", suggestionList))
      (bind(registerRpcMethod, "get_word_chars", getWordChars))
      (bind(registerRpcMethod, "add_custom_dictionary", addCustomDictionary))
      (bind(registerRpcMethod, "remove_custom_dictionary", removeCustomDictionary))
      (bind(registerRpcMethod, "install_all_dictionaries", installAllDictionaries))
      (bind(registerUriHandler, "/dictionaries", handleDictionaryRequest))
      (bind(sourceModuleRFile, "SessionSpelling.R"));
   return initBlock.execute();
}


} // namespace spelling
} // namespace modules
} // namespace session
} // namespace rstudio

