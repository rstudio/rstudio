/*
 * UrlOpenerMain.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <windows.h>
#include <shlwapi.h>

#include <iostream>

#include <core/Log.hpp>

#include <core/system/System.hpp>

int main(int argc, char** argv)
{
   try
   {
      // initialize log
      initializeSystemLog("urlopener", core::system::kLogLevelWarning);

      // check arguments
      if (argc < 2)
      {
         std::cerr << "Error: Not enough arguments" << std::endl;
         return EXIT_FAILURE;
      }

      // shell execute
      DWORD ret = (DWORD) ::ShellExecute(NULL,
                                         "open",
                                         argv[1],
                                         NULL,
                                         NULL,
                                         SW_SHOW);

      // check for error
      if(ret <= 32)
      {
        if(ret == ERROR_FILE_NOT_FOUND ||
           ret == ERROR_PATH_NOT_FOUND ||
           ret == SE_ERR_FNF ||
           ret == SE_ERR_PNF)
        {
           std::cerr << argv[1] << " not found" << std::endl;
        }
        else if(ret == SE_ERR_ASSOCINCOMPLETE || ret == SE_ERR_NOASSOC)
        {
           std::cerr << "file association for " << argv[1] <<
                        " not available or invalid" << std::endl;
        }
        else if(ret == SE_ERR_ACCESSDENIED || ret == SE_ERR_SHARE)
        {
           std::cerr << "access to " << argv[1] << " denied" << std::endl;
        }
        else
        {
           std::cerr << "problem in displaying " << argv[1] << std::endl;
        }

        return ret;
      }
      else
      {
         return EXIT_SUCCESS;
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}
