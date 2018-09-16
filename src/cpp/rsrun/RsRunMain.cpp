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
#include <core/system/System.hpp>

#include "config.h"

using namespace rstudio;

const std::string ESC = "\033";
const std::string BEL = "\a";
const std::string RSRUN_PREFIX = "0FCD24A8";

int main(int argc, char** argv)
{
   // Create a named pipe
   std::string pipeIdentifier = core::system::generateShortenedUuid();

   std::string sampleOutput = ESC + "]" + RSRUN_PREFIX + ";" + pipeIdentifier + ";" + "getwd()" + BEL;

   std::cout << sampleOutput;

   return EXIT_SUCCESS;
}

