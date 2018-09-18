/*
 * RSRunCmd.cpp
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

#include <core/terminal/RSRunCmd.hpp>

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

RSRunCmd::RSRunCmd()
{
}

std::string RSRunCmd::processESC(const std::string& input)
{
   std::string output;
   switch (state_)
   {
      case ParseState::normal:
         output = RSRunCmd::stripESC(input);
         break;

      case ParseState::running:
         // An R command is executing from a prior ESC sequence
         break;
   }
   
   //if (console_input::executing())

   return output;
}

void RSRunCmd::reset()
{
   payload_.clear();
   pipe_.clear();
   state_ = ParseState::normal;
}

std::string RSRunCmd::stripESC(const std::string& strInput)
{
   auto pos = strInput.find(kRSRunPrefix);
   if (pos != std::string::npos)
   {
   }
   return strInput; 
}

std::string RSRunCmd::createESC(const std::string& pipeId, const std::string& payload)
{
   return std::string(kRSRunPrefix) + pipeId + ";" + payload + kRSRunSuffix;
}

} // namespace terminal
} // namespace core
} // namespace rstudio
