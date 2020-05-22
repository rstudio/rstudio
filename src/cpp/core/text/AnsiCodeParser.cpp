/*
 * AnsiCodeParser.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/text/AnsiCodeParser.hpp>

#include <boost/regex.hpp>

namespace rstudio {
namespace core {
namespace text {

namespace {

// Match ANSI escape codes copied from https://github.com/chalk/ansi-regex
const char* kAnsiMatch = "[\\x1b\\x9b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-PRZcf-nqry=><@]";

// Match xterm title sequence (followed by text, ended by BEL)
const char* kXTermTitleMatch = "\\x1b]0;.*?\\x07";

} // anonymous namespace

void stripAnsiCodes(std::string* pStr)
{
   if (!pStr)
      return;

   std::string replacement;
   *pStr = boost::regex_replace(*pStr, boost::regex(kAnsiMatch), replacement);
   *pStr = boost::regex_replace(*pStr, boost::regex(kXTermTitleMatch), replacement);
}

} // namespace text
} // namespace core
} // namespace rstudio
