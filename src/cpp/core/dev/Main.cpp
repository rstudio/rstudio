/*
 * Main.cpp
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

#include <iostream>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>

#include <core/system/System.hpp>

#include <core/markdown/Markdown.hpp>

using namespace core ;

int main(int argc, char * const argv[]) 
{
   try
   { 
      // initialize log
      initializeSystemLog("coredev", core::system::kLogLevelWarning);

      // markdown to process
      std::string input =
         "Test markdown input\n"

         "$latex x + 1$\n"

         "```r\n"
         "cat(\"$latex y + 1$\n\")\n"
         "```\n"

         "$latex y + 1$\n"

         "\\\\( 1^2 \\\\)\n"

         "\\\\[ 1^2 \\\\]\n"

         "```\n"
         "## $latex y + 1$\n"
         "```\n"



         "`$latex z + 1$`\n"
      ;

      std::string output;
      Error error = markdown::markdownToHTML(input,
                                             markdown::Extensions(),
                                             markdown::HTMLOptions(),
                                             &output);
      if (error)
         LOG_ERROR(error);

      std::cerr << output << std::endl;
     
      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

