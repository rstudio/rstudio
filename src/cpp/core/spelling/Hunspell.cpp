/*
 * Hunspell.cpp
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

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>

#include <core/system/LibraryLoader.hpp>

namespace core {
namespace spelling {

namespace {

typedef struct Hunhandle Hunhandle;

typedef Hunhandle* (*PtrHunspellCreate)(const char*, const char*);
typedef void (*PtrHunspellDestroy)(Hunhandle*);
typedef int (*PtrHunspellSpell)(Hunhandle*,const char*);

struct HunspellSymbols
{
   HunspellSymbols()
      : create(NULL),
        destroy(NULL),
        spell(NULL)
   {
   }

   PtrHunspellCreate create;
   PtrHunspellDestroy destroy;
   PtrHunspellSpell spell;
};


class Hunspell : public SpellChecker
{
public:
   Hunspell()
      : pLib_(NULL), pHandle_(NULL)
   {
   }

   virtual ~Hunspell()
   {
      try
      {
         destroy();
      }
      catch(...)
      {
      }
   }

   Error initialize(const std::string& libPath)
   {
      // load the library
      Error error = core::system::loadLibrary(libPath, &pLib_);
      if (error)
         return error;

      // set the symbols
      using boost::bind;
      ExecBlock setSymbols;
      setSymbols.addFunctions()
        (bind(&Hunspell::setSymbol, this, "Hunspell_create", (void**)&sym_.create))
        (bind(&Hunspell::setSymbol, this, "Hunspell_destroy", (void**)&sym_.destroy))
        (bind(&Hunspell::setSymbol, this, "Hunspell_spell", (void**)&sym_.spell));
      return setSymbols.execute();
   }

   void destroy()
   {
      if (pHandle_ != NULL)
      {
         sym_.destroy(pHandle_);
         pHandle_ = NULL;
      }

      if (pLib_ != NULL)
      {
         Error error = core::system::closeLibrary(pLib_);
         if (error)
            LOG_ERROR(error);
         pLib_ = NULL;
      }

      sym_ = HunspellSymbols();
   }

public:
   bool checkSpelling(const std::string& word)
   {
      return sym_.spell(pHandle_, word.c_str()) != 0;
   }

private:

   Error setSymbol(const std::string& name, void** pSymbol)
   {
      return core::system::loadSymbol(pLib_, name, pSymbol);
   }

private:
   void* pLib_;
   Hunhandle* pHandle_;
   HunspellSymbols sym_;
};

} // anonymous namespace


core::Error createHunspell(const std::string& libPath,
                           boost::shared_ptr<SpellChecker>* pHunspell)
{
   // create the hunspell engine
   boost::shared_ptr<Hunspell> pNew(new Hunspell());

   // initialize it
   Error error = pNew->initialize(libPath);
   if (error)
      return error;

   // return
   *pHunspell = boost::shared_static_cast<SpellChecker>(pNew);
   return Success();
}


} // namespace spelling
} // namespace core 



