/*
 * AnsiCodeParser.hpp
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

#ifndef ANSI_CODE_PARSER_HPP
#define ANSI_CODE_PARSER_HPP

#include <string>

namespace rstudio {
namespace core {
namespace text {

enum AnsiCodeMode {
   AnsiColorOff = 0,  // don't do any processing of ANSI escape codes
   AnsiColorOn = 1,   // convert ANSI color escape codes into css styles
   AnsiColorStrip = 2 // strip out ANSI escape sequences but don't apply styles
};

// Strip Ansi codes from a string
void stripAnsiCodes(std::string* pStr);

} // namespace text
} // namespace core
} // namespace rstudio

#endif // ANSI_CODE_PARSER_HPP
