/*
 * Win32RuntimeLibrary.cpp
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

#include <core/system/Win32RuntimeLibrary.hpp>

#include <boost/noncopyable.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>

#include <core/system/LibraryLoader.hpp>

#define RS_LOAD_SYMBOL(__NAME__)                                              \
   do                                                                         \
   {                                                                          \
      Error error = system::loadSymbol(pLib_, #__NAME__, (void**) &__NAME__); \
      if (error)                                                              \
         LOG_ERROR(error);                                                    \
   } while (0)

namespace rstudio {
namespace core {
namespace runtime {

namespace {

// forward-declare
class Library;

// a handle to the runtime library now loaded
Library* s_pRuntimeLibrary = nullptr;

namespace defaults {
int s_zero = 0;
int* _errno() { return &s_zero; }
} // namespace defaults

// helper class for RAII lifetime of library handle
class Library : boost::noncopyable
{
public:

   Library(const char* libraryName)
      : pLib_(nullptr),
        _errno(defaults::_errno)
   {
      Error error = core::system::loadLibrary(libraryName, &pLib_);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      RS_LOAD_SYMBOL(_errno);
   }

   ~Library()
   {
      try
      {
         if (pLib_ != nullptr)
         {
            Error error = core::system::closeLibrary(pLib_);
            if (error)
               LOG_ERROR(error);
         }
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

public:
   void* pLib_;
   int* (*_errno)(void);
};

Library& library()
{
   return *s_pRuntimeLibrary;
}

} // end anonymous namespace

core::Error initialize(bool isUcrt)
{
   s_pRuntimeLibrary = new Library(isUcrt ? "ucrtbase.dll" : "msvcrt.dll");
   return Success();
}

int errorNumber()
{
   return *(library()._errno());
}

} // end namespace runtime
} // end namespace core
} // end namespace rstudio
