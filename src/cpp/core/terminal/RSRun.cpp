/*
 * RSRun.cpp
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

#include <core/terminal/RSRun.hpp>

namespace rstudio {
namespace core {
namespace terminal {

namespace {

} // anonymous namespace

// Start of an RSRun ESC sequence. Uses ANSI OSC – Operating System Command plus
// what should be a unique prefix in any reasonable universe.
const char* const kRSRunPrefix = "\033]RSRUN;";

// End of an RSRun ESC sequence via ESC\ (ANSI ST - String Terminator).
const char* const kRSRunSuffix = "\033\\";

RSRun::RSRun()
{
}

std::string RSRun::processESC(const std::string& input)
{
   // parse and strip the ESC sequence
   std::string output = stripESC(input);

   //if (console_input::executing())

   return output;
}

std::string RSRun::stripESC(const std::string& strInput)
{
   switch (state_)
   {
      case ParseState::normal:
         break;

      case ParseState::partial:
         // Potential partial sequence previously seen, tack it back on
         // and see if there's still a partial or full match
         break;

      case ParseState::running:
         // An R command is executing from a prior ESC sequence
         break;
   }

   return strInput; 
}

} // namespace terminal
} // namespace core
} // namespace rstudio
