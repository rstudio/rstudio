/*
 * HunspellSpellChecker.cpp
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

#include <core/spelling/SpellChecker.hpp>

#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>

// Including the hunspell headers caused compilation errors for Windows 64-bit
// builds. The trouble seemd to be a 'near' macro defined somewhere in the
// mingw toolchain (couldn't find where). Fix this by undefining 'near' right
// before we include hunspell.hxx.
#if defined(near)
#undef near
#endif
#include "hunspell/hunspell.hxx"

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

   Error initialize(const FilePath& affPath,
                    const FilePath& dicPath,
                    const IconvstrFunction& iconvstrFunc)
   {
      // validate that dictionaries exist
      if (!affPath.exists())
         return core::fileNotFoundError(affPath, ERROR_LOCATION);
      if (!dicPath.exists())
         return core::fileNotFoundError(dicPath, ERROR_LOCATION);

      // convert paths to system encoding before sending to external API
      std::string systemAffPath = string_utils::utf8ToSystem(affPath.absolutePath());
      std::string systemDicPath = string_utils::utf8ToSystem(dicPath.absolutePath());

      // initialize hunspell, iconvstrFunc_, and encoding_
      pHunspell_.reset(new Hunspell(systemAffPath.c_str(),
                                    systemDicPath.c_str()));
      iconvstrFunc_ = iconvstrFunc;
      encoding_ = pHunspell_->get_dic_encoding();

      // add words from dic_delta if available
      FilePath dicDeltaPath = dicPath.parent().childPath(
                                                dicPath.stem() + ".dic_delta");
      if (dicDeltaPath.exists())
      {
         Error error = mergeDicDeltaFile(dicDeltaPath);
         if (error)
            LOG_ERROR(error);
      }

      // return success
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
      bool addAffixes = boost::algorithm::starts_with(dicDeltaPath.stem(),
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
      BOOST_FOREACH(const std::string& line, lines)
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
      return Success();
   }

   Error analyzeWord(const std::string& word, std::vector<std::string>* pResult)
   {
      std::string encoded;
      Error error = iconvstrFunc_(word,"UTF-8",encoding_,false,&encoded);
      if (error)
         return error;

      char ** wlst;
      int ns = pHunspell_->analyze(&wlst,encoded.c_str());
      copyAndFreeHunspellVector(pResult,wlst,ns);
      return Success();
   }

   Error stemWord(const std::string& word, std::vector<std::string>* pResult)
   {
      std::string encoded;
      Error error = iconvstrFunc_(word,"UTF-8",encoding_,false,&encoded);
      if (error)
         return error;

      char ** wlst;
      int ns = pHunspell_->stem(&wlst,encoded.c_str());
      copyAndFreeHunspellVector(pResult,wlst,ns);
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

   Error removeWord(const std::string& word, bool *pRemoved)
   {
      std::string encoded;
      Error error = iconvstrFunc_(word,"UTF-8",encoding_,false,&encoded);
      if (error)
         return error;

      // Always returns 0?
      *pRemoved = (pHunspell_->remove(encoded.c_str()) == 0);
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
      std::string systemDicPath = string_utils::utf8ToSystem(dicPath.absolutePath());
      *pAdded = (pHunspell_->add_dic(systemDicPath.c_str(),key.c_str()) == 0);
      return Success();
   }

private:
   boost::scoped_ptr<Hunspell> pHunspell_;
   IconvstrFunction iconvstrFunc_;
   std::string encoding_;
};

} // anonymous namespace

core::Error createHunspell(const FilePath& languageDicPath,
                           boost::shared_ptr<SpellChecker>* ppHunspell,
                           const IconvstrFunction& iconvstrFunc)
{
   // create the hunspell engine
   boost::shared_ptr<HunspellSpellChecker> pNew(new HunspellSpellChecker());

   // initialize it
   FilePath dicPath = languageDicPath;
   FilePath affPath = dicPath.parent().childPath(dicPath.stem() + ".aff");
   Error error = pNew->initialize(affPath, dicPath, iconvstrFunc);
   if (error)
      return error;

   // return
   *ppHunspell = boost::shared_static_cast<SpellChecker>(pNew);
   return Success();
}


} // namespace spelling
} // namespace core 



