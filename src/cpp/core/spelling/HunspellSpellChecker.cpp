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

#include <boost/scoped_ptr.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

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

      // initialize hunspell, iconvstrFunc_, encoding_, and return success
      pHunspell_.reset(new Hunspell(affPath.absolutePath().c_str(),
                                    dicPath.absolutePath().c_str()));
      iconvstrFunc_ = iconvstrFunc;
      encoding_ = pHunspell_->get_dic_encoding();
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


private:
   boost::scoped_ptr<Hunspell> pHunspell_;
   IconvstrFunction iconvstrFunc_;
   std::string encoding_;
};

} // anonymous namespace

core::Error createHunspell(const FilePath& affPath,
                           const FilePath& dicPath,
                           boost::shared_ptr<SpellChecker>* ppHunspell,
                           const IconvstrFunction& iconvstrFunc)
{
   // create the hunspell engine
   boost::shared_ptr<HunspellSpellChecker> pNew(new HunspellSpellChecker());

   // initialize it
   Error error = pNew->initialize(affPath, dicPath, iconvstrFunc);
   if (error)
      return error;

   // return
   *ppHunspell = boost::shared_static_cast<SpellChecker>(pNew);
   return Success();
}


} // namespace spelling
} // namespace core 



