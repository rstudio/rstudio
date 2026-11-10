/*
 * MarkdownRender.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

// Test-only driver for comparing the complete RStudio Markdown pipeline.
#include <iostream>
#include <iterator>

#include <shared_core/Error.hpp>
#include <core/markdown/Markdown.hpp>

int main(int argc, char* argv[])
{
   using namespace rstudio::core;
   markdown::HTMLOptions options;
   if (argc == 2 && std::string(argv[1]) == "--safe")
      options.escape = options.safelink = options.skipImages = true;
   else if (argc != 1)
      return 2;

   std::string input(std::istreambuf_iterator<char>(std::cin), {});
   std::string output;
   Error error = markdown::markdownToHTML(input, markdown::Extensions(), options, &output);
   if (error)
   {
      std::cerr << error << std::endl;
      return 1;
   }
   std::cout << output;
   return 0;
}
