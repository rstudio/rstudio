/*
 * RsRunMain.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/terminal/RSRunCmd.hpp>

#include "config.h"

using namespace rstudio;

int main(int argc, char** argv)
{
   if (core::system::getenv("RSTUDIO_TERM").empty())
   {
      std::cerr << "rsrun: unsupported terminal: must invoke with RStudio terminal\n";
      return EXIT_FAILURE;
   }
   
   // Create a named pipe
   std::string pipeIdentifier = core::system::generateShortenedUuid();

   std::string sampleOutput  = core::terminal::kRSRunPrefix;
   sampleOutput += ";";
   sampleOutput += pipeIdentifier;
   sampleOutput += ";";
   sampleOutput +="getwd()";
   sampleOutput += core::terminal::kRSRunSuffix;
   std::cout << sampleOutput;

   return EXIT_SUCCESS;
}

