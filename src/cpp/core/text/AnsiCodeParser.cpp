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

// Match every escape sequence with a single ordered alternation, so the
// whole strip happens in one left-to-right pass. Sequential passes over the
// rewritten string can delete visible text: removing one sequence may place
// a previously-inert byte next to a leftover ESC, which a later pass then
// consumes (e.g. tmux passthrough doubles ESC bytes, and a second pass
// would eat the 7 in ESC ESC [31m 7). regex_replace scans the original
// string only, so with one pass a deletion can never create a new match.
//
// The alternatives, tried in order:
//
// 1. String-type sequences: OSC (ESC ]), DCS (ESC P), SOS (ESC X), the
//    GNU screen / tmux window title (ESC k), PM (ESC ^), APC (ESC _).
//    These carry a free-form payload terminated by BEL or ST (ESC \) --
//    e.g. window titles, hyperlinks, or the OSC 3008 context metadata
//    (machine id, user, cwd, ...) that systemd >= 258 emits around every
//    shell prompt -- so the payload is stripped along with the sequence.
//    The payload excludes newlines so an unterminated sequence cannot
//    swallow subsequent lines (at the cost of consuming the remainder of
//    its own line), and the terminator is optional so a sequence truncated
//    at end of line is still removed. NOTE: '^' in the introducer class is
//    literal only because it is not first; reordering the class would
//    silently turn it into a negation.
//
// 2. CSI sequences, following the ECMA-48 grammar: parameter bytes
//    (0x30-0x3f, which admits the colon-separated SGR form, e.g.
//    ESC[38:5:196m), then intermediate bytes (0x20-0x2f, e.g. the space in
//    DECSCUSR ESC[1 q), then one final byte (0x40-0x7e). The 8-bit CSI
//    introducer 0x9b is deliberately not matched: this operates on UTF-8
//    bytes, where 0x9b is a valid continuation byte, so matching it would
//    corrupt multibyte text.
//
// 3. RStudio's private output grouping / highlighting markers (ESC G<n>;
//    and ESC H<n>;, per AnsiEscapes.hpp), the only sequences whose numeric
//    parameters trail the final byte. Limiting the parameter tail to G and
//    H keeps digits after standard two-byte escapes visible (the 2024 in
//    ESC E 2024 is text, not a parameter).
//
// 4. nF sequences: intermediate bytes (0x20-0x2f) then a final byte, e.g.
//    the charset designation ESC ( B, or ESC # 8.
//
// 5. Any remaining two-byte sequence, final byte 0x30-0x7e: Fp, Fs, and Fe
//    forms such as ESC 7, ESC =, ESC c, a bare ST (ESC \), or a truncated
//    ESC [. The final byte is optional so that an orphan ESC -- one
//    followed by a newline, a control byte, or end of input -- is still
//    removed rather than reaching the editor.
const char* kEscapeSequenceMatch =
   "\\x1b"
   "(?:"
   "[\\]PXk^_][^\\x07\\x1b\\r\\n]*(?:\\x07|\\x1b\\\\)?"
   "|\\[[0-?]*[ -/]*[@-~]"
   "|[GH][0-9;]*"
   "|[ -/]+[0-~]"
   "|[0-~]?"
   ")";

} // anonymous namespace

void stripAnsiCodes(std::string* pStr)
{
   if (!pStr)
      return;

   static const boost::regex escapeRegex(kEscapeSequenceMatch);
   *pStr = boost::regex_replace(*pStr, escapeRegex, std::string());
}

} // namespace text
} // namespace core
} // namespace rstudio
