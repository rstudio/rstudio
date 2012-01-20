/*
 * Main.cpp
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

#include <iostream>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>

#include <core/system/System.hpp>

#include <core/spelling/SpellChecker.hpp>

using namespace core ;

int main(int argc, char * const argv[]) 
{
   try
   { 
      // initialize log
      initializeSystemLog("coredev", core::system::kLogLevelWarning);

#if defined(__APPLE__)
      std::string hunspellPath = "libhunspell-1.2.dylib";
#else
      std::string hunspellPath = "libhunspell-1.2.so.0";
#endif
     
      boost::shared_ptr<core::spelling::SpellChecker> pSpellChecker;
      Error error =  core::spelling::createHunspell(hunspellPath,
                                                    &pSpellChecker);
      if (error)
         LOG_ERROR(error);
   
     
      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

