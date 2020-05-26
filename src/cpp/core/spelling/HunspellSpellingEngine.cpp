/*
 * HunspellSpellingEngine.cpp
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

#include <core/spelling/HunspellSpellingEngine.hpp>

#include <boost/algorithm/string.hpp>

#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>

#include <core/spelling/HunspellDictionaryManager.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

// Including the hunspell headers caused compilation errors for Windows 64-bit
// builds. The trouble seemd to be a 'near' macro defined somewhere in the
// mingw toolchain (couldn't find where). Fix this by undefining 'near' right
// before we include hunspell.hxx.
#if defined(near)
#undef near
#endif
#include "hunspell/hunspell.hxx"

namespace rstudio {
namespace core {
namespace spelling {

namespace {

// remove morphological description from text
void removeMorphologicalDescription(std::string* pText)
{
   std::size_t tabPos = pText->find('\t');
   if (tabPos != std::string::npos)
      *pText = pText->substr(0, tabPos);
}

// extract the word from the dic_delta line -- remove the
// optional affix and then replace escaped / chracters
bool parseDicDeltaLine(std::string line,
                       std::string* pWord,
                       std::string* pAffix)
{
   // skip empty lines
   boost::algorithm::trim(line);
   if (line.empty())
      return false;

   // find delimiter
   std::size_t wordEndPos = line.size();
   for (std::size_t i = 0; i < line.size(); i++)
   {
      if (line[i] == '/' && i > 0 && line[i - 1] != '\\')
      {
         wordEndPos = i;
         break;
      }
   }

   // extract word and escape forward slashes
   std::string word = line.substr(0, wordEndPos);
   *pWord = boost::algorithm::replace_all_copy(word, "\\/", "/");

   // extract affix (if any)
   if (wordEndPos < line.size() - 1)
   {
      *pAffix = line.substr(wordEndPos + 1);
      removeMorphologicalDescription(pAffix);
   }
   else
   {
      pAffix->clear();
      removeMorphologicalDescription(pWord);
   }

   return true;
}

// The hunspell api allows you to add words with affixes by providing an
// example word already in the dictionary that has the same affix. The google
// english .dic_delta files use the hard-coded integer values 6 and 7 to
// (respecitvely) indicate possesive (M) and possesive/plural (MS) affixes.
// Therefore, this function needs to return words that are marked as
// M or MS consistently in the main dictionaries of the 4 english variations.
// If we want to extend affix support to other languages we'll need to
// do a simillar mapping
std::string exampleWordForEnglishAffix(const std::string& affix)
{
   if (affix == "6") // possesive (M)
      return "Arcadia";
   else if (affix == "7") // possessive or plural (MS)
      return "beverage";
   else
      return std::string();
}

class SpellChecker : boost::noncopyable
{
public:
   virtual ~SpellChecker() {}
   virtual Error checkSpelling(const std::string& word, bool *pCorrect) = 0;
   virtual Error suggestionList(const std::string& word,
                                std::vector<std::string>* pSugs) = 0;
   virtual Error wordChars(std::wstring* pWordChars) = 0;
};

class NoSpellChecker : public SpellChecker
{
public:
   Error checkSpelling(const std::string& word, bool *pCorrect)
   {
      *pCorrect = true;
      return Success();
   }

   Error suggestionList(const std::string& word,
                        std::vector<std::string>* pSugs)
   {
      return Success();
   }

   Error wordChars(std::wstring *pWordChars)
   {
      return Success();
   }
};

class HunspellSpellChecker : public SpellChecker
{
public:
   HunspellSpellChecker()
   {
   }

   virtual ~HunspellSpellChecker()
   {
      try
      {
         pHunspell_.reset();
      }
      catch(...)
      {
      }
   }

   Error initialize(const HunspellDictionary& dictionary,
                    const IconvstrFunction& iconvstrFunc)
   {
      // validate that dictionaries exist
      if (!dictionary.affPath().exists())
         return core::fileNotFoundError(dictionary.affPath(), ERROR_LOCATION);
      if (!dictionary.dicPath().exists())
         return core::fileNotFoundError(dictionary.dicPath(), ERROR_LOCATION);

      // convert paths to system encoding before sending to external API
      std::string systemAffPath = string_utils::utf8ToSystem(
         dictionary.affPath().getAbsolutePath());
      std::string systemDicPath = string_utils::utf8ToSystem(
         dictionary.dicPath().getAbsolutePath());

      // initialize hunspell, iconvstrFunc_, and encoding_
      pHunspell_.reset(new Hunspell(systemAffPath.c_str(),
                                    systemDicPath.c_str()));
      iconvstrFunc_ = iconvstrFunc;
      encoding_ = pHunspell_->get_dic_encoding();

      // add words from dic_delta if available
      FilePath dicPath = dictionary.dicPath();
      FilePath dicDeltaPath = dicPath.getParent().completeChildPath(
         dicPath.getStem() + ".dic_delta");
      if (dicDeltaPath.exists())
      {
         Error error = mergeDicDeltaFile(dicDeltaPath);
         if (error)
            LOG_ERROR(error);
      }

      // return success
      return Success();
   }

   Error wordChars(std::wstring *pWordChars)
   {
      int len;
      unsigned short *pChars = pHunspell_->get_wordchars_utf16(&len);

      for (int i = 0; i < len; i++)
         pWordChars->push_back(pChars[i]);

      return Success();
   }

private:

   // helpers
   void copyAndFreeHunspellVector(std::vector<std::string>* pVec,
                                    char **wlst,
                                    int len)
   {
      for (int i=0; i < len; i++)
      {
         pVec->push_back(wlst[i]);
      }
      pHunspell_->free_list(&wlst, len);
   }

   Error mergeDicDeltaFile(const FilePath& dicDeltaPath)
   {
      // determine whether we are going to support affixes -- we do this for
      // english only right now because we can correctly (by inspection) map
      // the chromium numeric affix indicators (6 and 7) to the right
      // hunspell example words. it's worth investigating whether we can do
      // this for other languages as well
      bool addAffixes = boost::algorithm::starts_with(dicDeltaPath.getStem(),
                                                      "en_");

      // read the file and strip the BOM
      std::string contents;
      Error error = core::readStringFromFile(dicDeltaPath, &contents);
      if (error)
         return error;
      core::stripBOM(&contents);

      // split into lines
      std::vector<std::string> lines;
      boost::algorithm::split(lines,
                              contents,
                              boost::algorithm::is_any_of("\n"));

      // parse lines for words
      bool added;
      std::string word, affix, example;
      for (const std::string& line : lines)
      {
         if (parseDicDeltaLine(line, &word, &affix))
         {
            example = exampleWordForEnglishAffix(affix);
            if (!example.empty() && addAffixes)
            {
               Error error = addWordWithAffix(word, example, &added);
               if (error)
                  LOG_ERROR(error);
            }
            else
            {
               Error error = addWord(word, &added);
               if (error)
                  LOG_ERROR(error);
            }
         }
      }

      return Success();
   }


public:
   Error checkSpelling(const std::string& word, bool *pCorrect)
   {
      std::string encoded;
      Error error = iconvstrFunc_(word,"UTF-8",encoding_,false,&encoded);
      if (error)
         return error;

      *pCorrect = pHunspell_->spell(encoded.c_str());
      return Success();
   }

   Error suggestionList(const std::string& word, std::vector<std::string>* pSug)
   {
      std::string encoded;
      Error error = iconvstrFunc_(word,"UTF-8",encoding_,false,&encoded);
      if (error)
         return error;

      char ** wlst;
      int ns = pHunspell_->suggest(&wlst,encoded.c_str());
      copyAndFreeHunspellVector(pSug,wlst,ns);

      for (std::string& sug : *pSug)
      {
         error = iconvstrFunc_(sug, encoding_, "UTF-8", true, &sug);
         if (error)
            return error;
      }

      return Success();
   }

   Error addWord(const std::string& word, bool *pAdded)
   {
      std::string encoded;
      Error error = iconvstrFunc_(word,"UTF-8",encoding_,false,&encoded);
      if (error)
         return error;

      // Following the Hunspell::add method through it's various code paths
      // it seems the return value is always 0, meaning there's really no
      // error ever thrown if the method fails.
      *pAdded = (pHunspell_->add(encoded.c_str()) == 0);
      return Success();
   }

   Error addWordWithAffix(const std::string& word,
                          const std::string& example,
                          bool *pAdded)
   {
      std::string wordEncoded;
      Error error = iconvstrFunc_(word,
                                  "UTF-8",
                                  encoding_,
                                  false,
                                  &wordEncoded);
      if (error)
         return error;

      std::string exampleEncoded;
      error = iconvstrFunc_(example,
                            "UTF-8",
                            encoding_,
                            false,
                            &exampleEncoded);
      if (error)
         return error;

      *pAdded = (pHunspell_->add_with_affix(wordEncoded.c_str(),
                                            exampleEncoded.c_str()) == 0);
      return Success();
   }

   // Hunspell dictionary files are simple: the first line is an integer
   // indicating the number of entries (one per line), and each line contains
   // a word followed by '/' plus modifier flags. Example user.dic:
   // ----------
   // 3
   // lol/S
   // rofl/S
   // tl;dr/S
   // ----------
   // The '/S' modifier treats 'ROFL','rofl', and 'Rofl' as correct spellings.
   Error addDictionary(const FilePath& dicPath,
                       const std::string& key,
                       bool *pAdded)
   {
      if (!dicPath.exists())
         return core::fileNotFoundError(dicPath, ERROR_LOCATION);

      // Convert path to system encoding before sending to external api
      std::string systemDicPath = string_utils::utf8ToSystem(dicPath.getAbsolutePath());
      *pAdded = (pHunspell_->add_dic(systemDicPath.c_str(),key.c_str()) == 0);
      return Success();
   }

private:
   boost::scoped_ptr<Hunspell> pHunspell_;
   IconvstrFunction iconvstrFunc_;
   std::string encoding_;
};

} // anonymous namespace

struct HunspellSpellingEngine::Impl
{
   Impl(const std::string& langId,
        const HunspellDictionaryManager& dictionaryManager,
        const IconvstrFunction& iconvstrFunction)
      : currentLangId_(langId),
        dictManager_(dictionaryManager),
        iconvstrFunction_(iconvstrFunction)
   {
   }

   void useDictionary(const std::string& langId)
   {
      if (dictionaryContextChanged(langId))
         resetDictionaries(langId);
   }

   SpellChecker& spellChecker()
   {
      if (!pSpellChecker_)
         resetDictionaries(currentLangId_);

      return *pSpellChecker_;
   }

private:
   bool dictionaryContextChanged(const std::string& langId)
   {
      return(langId != currentLangId_ ||
             dictManager_.custom().dictionaries() != currentCustomDicts_);
   }

   void resetDictionaries(const std::string& langId)
   {
      HunspellDictionary dict = dictManager_.dictionaryForLanguageId(langId);
      if (!dict.empty())
      {
         HunspellSpellChecker* pHunspell = new HunspellSpellChecker();
         pSpellChecker_.reset(pHunspell);

         Error error = pHunspell->initialize(dict, iconvstrFunction_);
         if (!error)
         {
            currentLangId_ = langId;
            currentCustomDicts_ = dictManager_.custom().dictionaries();
            for (const std::string& dict : currentCustomDicts_)
            {
               bool added;
               FilePath dicPath = dictManager_.custom().dictionaryPath(dict);
               Error error = pHunspell->addDictionary(dicPath,
                                                      dicPath.getStem(),
                                                      &added);
               if (error)
                  LOG_ERROR(error);
            }
         }
         else
         {
            LOG_ERROR(error);

            pSpellChecker_.reset(new NoSpellChecker());
         }
      }
      else
      {
         pSpellChecker_.reset(new NoSpellChecker());
      }
   }



private:
   std::string currentLangId_;
   std::vector<std::string> currentCustomDicts_;
   HunspellDictionaryManager dictManager_;
   IconvstrFunction iconvstrFunction_;
   boost::shared_ptr<SpellChecker> pSpellChecker_;
};


HunspellSpellingEngine::HunspellSpellingEngine(
                           const std::string& langId,
                           const HunspellDictionaryManager& dictionaryManager,
                           const IconvstrFunction& iconvstrFunction)
   : pImpl_(new Impl(langId, dictionaryManager, iconvstrFunction))
{
}


void HunspellSpellingEngine::useDictionary(const std::string& langId)
{
   pImpl_->useDictionary(langId);
}

Error HunspellSpellingEngine::checkSpelling(const std::string& word,
                                            bool *pCorrect)
{
   return pImpl_->spellChecker().checkSpelling(word, pCorrect);
}

Error HunspellSpellingEngine::suggestionList(const std::string& word,
                                             std::vector<std::string>* pSugs)
{
   return pImpl_->spellChecker().suggestionList(word, pSugs);
}

Error HunspellSpellingEngine::wordChars(std::wstring *pChars)
{
   return pImpl_->spellChecker().wordChars(pChars);
}

} // namespace spelling
} // namespace core 
} // namespace rstudio



