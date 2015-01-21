/*
 * Main.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <iostream>
#include <fstream>

#include <boost/test/minimal.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>

#include <core/libclang/LibClang.hpp>

using namespace rstudio;
using namespace rstudio::core;

int test_main(int argc, char * argv[])
{
   try
   { 
      // setup log
      initializeStderrLog("coredev", core::system::kLogLevelWarning);

      // ignore sigpipe
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // write a C++ file
      std::string cpp =
        "#include <string>\n"
        "class X { public:\n"
        "   void test(int y, int x = 10);\n"
        "}\n"
        "void X::test(int y, int x) {}\n"
        "void foobar() {\n"
        "   X x;\n"
        "   x."
        "}";
      std::ofstream ostr("foo.cpp");
      ostr << cpp;
      ostr.close();

      // load libclang
      using namespace libclang;
      std::string diagnostics;
      clang().load(EmbeddedLibrary(), LibraryVersion(3,4,0), &diagnostics);
      if (!clang().isLoaded())
      {
         std::cerr << "Failed to load libclang: " << diagnostics << std::endl;
         return EXIT_FAILURE;
      }

      // create a source index and get a translation unit for it
      SourceIndex sourceIndex;
      TranslationUnit tu = sourceIndex.getTranslationUnit("foo.cpp");
      if (tu.empty())
      {
         std::cerr << "No translation unit foo.cpp" << std::endl;
         return EXIT_FAILURE;
      }

      // code complete
      CodeCompleteResults results = tu.codeCompleteAt("foo.cpp", 8, 6);
      for (unsigned i = 0; i<results.getNumResults(); i++) {
        std::cout << results.getResult(i).getTypedText() << std::endl;
        std::cout << "   " << results.getResult(i).getText() << std::endl;
      }

      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

