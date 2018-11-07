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
#include <core/RegexUtils.hpp>
#include <boost/regex.hpp>

namespace rstudio {
namespace core {
namespace terminal {

namespace {

// Start of an RSRun ESC sequence. Uses ANSI OSC – Operating System Command plus
// what should be a unique prefix in any reasonable universe.
const char* const kRSRunPrefix = "\033]RSRUN;";

// End of an RSRun ESC sequence via ESC\ (ANSI ST - String Terminator).
const char* const kRSRunSuffix = "\033\\";

// Regex to match RSRun ESC sequence with group 1 as pipeId and group 2
// as the embedded R code.
const char* const kESCRegex = R"(\033]RSRUN;([A-Fa-f0-9]{8});([\S\s]+)\033\\)";

} // anonymous namespace

RSRunCmd::RSRunCmd()
{
}

bool RSRunCmd::findESC(const std::string& input)
{
   reset();

   boost::regex re(kESCRegex);
   boost::smatch match;
   if (!regex_utils::search(input, match, re))
      return false;

   pipe_ = match[1];
   payload_ = match[2];
   return true;
}

void RSRunCmd::reset()
{
   payload_.clear();
   pipe_.clear();
}

std::string RSRunCmd::createESC(const std::string& pipeId, const std::string& payload)
{
   return std::string(kRSRunPrefix) + pipeId + ";" + payload + kRSRunSuffix;
}

} // namespace terminal
} // namespace core
} // namespace rstudio
