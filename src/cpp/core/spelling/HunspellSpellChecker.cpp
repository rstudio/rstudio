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

   Error initialize(const std::string& affPath, const std::string& dicPath)
   {
      // validate that dictionaries exist
      if (!FilePath(affPath).exists())
         return core::fileNotFoundError(affPath, ERROR_LOCATION);
      if (!FilePath(dicPath).exists())
         return core::fileNotFoundError(dicPath, ERROR_LOCATION);

      // initialize hunspell and return success
      pHunspell_.reset(new Hunspell(affPath.c_str(), dicPath.c_str()));
      return Success();
   }

public:
   bool checkSpelling(const std::string& word)
   {
      return pHunspell_->spell(word.c_str());
   }

private:
   boost::scoped_ptr<Hunspell> pHunspell_;
};

} // anonymous namespace


core::Error createHunspell(const std::string& affPath,
                           const std::string& dicPath,
                           boost::shared_ptr<SpellChecker>* pHunspell)
{
   // create the hunspell engine
   boost::shared_ptr<HunspellSpellChecker> pNew(new HunspellSpellChecker());

   // initialize it
   Error error = pNew->initialize(affPath, dicPath);
   if (error)
      return error;

   // return
   *pHunspell = boost::shared_static_cast<SpellChecker>(pNew);
   return Success();
}


} // namespace spelling
} // namespace core 



