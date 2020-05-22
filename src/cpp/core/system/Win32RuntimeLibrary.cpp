/*
 * Win32RuntimeLibrary.cpp
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

namespace defaults {
int s_zero = 0;
int* _errno() { return &s_zero; }
} // namespace defaults

// helper class for RAII lifetime of library handle
class Library : boost::noncopyable
{
public:

   Library()
      : pLib_(nullptr),
        _errno(defaults::_errno)
   {
      Error error = core::system::loadLibrary("msvcrt.dll", &pLib_);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      RS_LOAD_SYMBOL(_errno);
   }

   int* (*_errno)(void);

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

private:
   void* pLib_;
};

Library& library()
{
   static Library instance;
   return instance;
}

} // end anonymous namespace


int errorNumber()
{
   return *(library()._errno());
}

} // end namespace runtime
} // end namespace core
} // end namespace rstudio
