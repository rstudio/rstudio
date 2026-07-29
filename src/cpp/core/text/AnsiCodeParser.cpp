/*
 * AnsiCodeParser.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <core/text/AnsiCodeParser.hpp>

#include <boost/regex.hpp>

namespace rstudio {
namespace core {
namespace text {

namespace {

// Match ANSI escape codes copied from https://github.com/chalk/ansi-regex
const char* kAnsiMatch = "[\\x1b\\x9b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-PRZcf-nqry=><@]";

// Match string-type sequences: OSC (ESC ]), DCS (ESC P), SOS (ESC X),
// PM (ESC ^), APC (ESC _). These carry a free-form payload terminated by
// BEL or ST (ESC \) -- e.g. window titles, hyperlinks, or the OSC 3008
// context metadata (machine id, user, cwd, ...) that systemd >= 258 emits
// around every shell prompt -- so the payload must be stripped along with
// the sequence itself. The payload excludes newlines so an unterminated
// sequence cannot swallow subsequent lines, and the terminator is optional
// so a sequence truncated at end of line is still removed.
const char* kStringSequenceMatch = "\\x1b[\\]PX^_][^\\x07\\x1b\\r\\n]*(?:\\x07|\\x1b\\\\)?";

// Match simple ESC + letter sequences with optional numeric parameters
// e.g., ESC G, ESC H1;, ESC g (used by RStudio for clickable error links)
// Raw ESC byte (0x1B) followed by letter and optional params
const char* kSimpleEscapeMatch = "\\x1b[A-Za-z][0-9;]*";

} // anonymous namespace

void stripAnsiCodes(std::string* pStr)
{
   if (!pStr)
      return;

   std::string replacement;

   // Strip string-type sequences first: kSimpleEscapeMatch and kAnsiMatch
   // would otherwise consume just their introducers (e.g. ESC P), leaving
   // the payload behind as visible text
   *pStr = boost::regex_replace(*pStr, boost::regex(kStringSequenceMatch), replacement);

   // Strip simple ESC sequences next (they include params like "1;")
   // before kAnsiMatch which would only match "ESC G" without the params
   *pStr = boost::regex_replace(*pStr, boost::regex(kSimpleEscapeMatch), replacement);
   *pStr = boost::regex_replace(*pStr, boost::regex(kAnsiMatch), replacement);
}

} // namespace text
} // namespace core
} // namespace rstudio
