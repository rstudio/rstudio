/*
 * rsession-launcher.cpp
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

#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>

#include <shared_core/Error.hpp>

#include <core/system/System.hpp>

#ifdef __APPLE__
# define kSessionLibraryName "librsession.dylib"
#else
# define kSessionLibraryName "rsession.so"
#endif

using namespace rstudio;
using namespace rstudio::core;

int main(int argc, char** argv)
{
   // 1. Load the R library.
   const char* rPath = getenv("R_LIBRARY_PATH");
   if (rPath && dlopen(rPath, RTLD_NOW | RTLD_GLOBAL) == NULL)
   {
      fprintf(stderr, "Failed to load R: %s\n", dlerror());
      return 1;
   }

   // 2. Load the rsession support library.
   FilePath executablePath;
   Error error = core::system::executablePath(nullptr, &executablePath);
   if (error)
      LOG_ERROR(error);

   std::string rsessionLibraryPath = executablePath.getParent().completeChildPath(kSessionLibraryName).getAbsolutePath();
   void* rsession = dlopen(rsessionLibraryPath.c_str(), RTLD_NOW | RTLD_LOCAL);
   if (!rsession)
   {
      fprintf(stderr, "Failed to load rsession: %s\n", dlerror());
      return 1;
   }

   // 3. Call the rsession's main function.
   typedef int (*main_func_t)(int, char**);
   main_func_t rsession_main = (main_func_t) dlsym(rsession, "rsession_main");
   if (!rsession_main)
   {
      fprintf(stderr, "Failed to find rsession_main\n");
      return 1;
   }

   return rsession_main(argc, argv);
}
