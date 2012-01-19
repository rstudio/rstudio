/*
 * SessionHunspell.cpp
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
#include "SessionHunspell.hpp"

#ifndef _WIN32
  #include <dlfcn.h>
#else


#endif

#include <boost/utility.hpp>

#include <core/Error.hpp>

using namespace core;

namespace session {
namespace modules {
namespace spelling {
namespace hunspell {

namespace {

typedef struct Hunhandle Hunhandle;

typedef Hunhandle* (*PtrHunspellCreate)(const char*, const char*);
typedef void (*PtrHunspellDestroy)(Hunhandle*);
typedef int (*PtrHunspellSpell)(Hunhandle*,const char*);
typedef int (*PtrHunspellSuggest)(Hunhandle*, char***, const char*);

struct LibHunspell
{
   LibHunspell()
      : create(NULL),
        destroy(NULL),
        spell(NULL),
        suggest(NULL)
   {
   }

   PtrHunspellCreate create;
   PtrHunspellDestroy destroy;
   PtrHunspellSpell spell;
   PtrHunspellSuggest suggest;
};

} // anonymous namespace

Error initialize()
{
   LibHunspell hs;
#if defined(_WIN32)


#else

#if defined(__APPLE__)
   void* pHunspell = ::dlopen("libhunspell-1.2.dylib", RTLD_LAZY | RTLD_LOCAL);
#else
   void* pHunspell = ::dlopen("libhunspell-1.2.so.0", RTLD_LAZY | RTLD_LOCAL);
#endif

   if (pHunspell)
   {
      hs.create = (PtrHunspellCreate)::dlsym(pHunspell,"Hunspell_create");
      hs.destroy = (PtrHunspellDestroy)::dlsym(pHunspell, "Hunspell_destroy");
      hs.spell = (PtrHunspellSpell)::dlsym(pHunspell, "Hunspell_spell");
      hs.suggest = (PtrHunspellSuggest)::dlsym(pHunspell, "Hunspell_suggest");
   }

#endif


   return Success();
}

} // namespace hunspell
} // namespace spelling
} // namespace modules
} // namespace session
