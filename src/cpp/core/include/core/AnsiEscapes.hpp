/*
 * ANSIEscapes.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#ifndef CORE_ANSI_ESCAPES_HPP
#define CORE_ANSI_ESCAPES_HPP

// Helper definitions. Unfortunately, because C / C++ expand macro definitions
// at time of use, rather than time of definition, we need these names to
// persist, while avoiding potential collisions with other symbols.
#define __BEL__ "\\x{07}"
#define __ESC__ "\\x{1b}"
#define __CSI__ "\\x{1b}\\x{5b}"
#define __ST__  "\\x{1b}\\x{5c}"
#define __OSC__ "\\x{1b}\\x{5d}"


// Custom escapes, used by RStudio for grouping output.
#define kAnsiEscapeGroupStartError    "\033G1;"
#define kAnsiEscapeGroupStartWarning  "\033G2;"
#define kAnsiEscapeGroupStartMessage  "\033G3;"
#define kAnsiEscapeGroupEnd           "\033g"


// Custom escapes, used by RStudio for highlighting output.
#define kAnsiEscapeHighlightStartError    "\033H1;"
#define kAnsiEscapeHighlightStartWarning  "\033H2;"
#define kAnsiEscapeHighlightStartMessage  "\033H3;"
#define kAnsiEscapeHighlightEnd           "\033h"


// A catch-all for ANSI colors.
#define kAnsiEscapeColorRegex "(?:" __CSI__ "([\\d;]*)" "m" ")*"


// For parsing encoded ANSI URLs / hyperlinks.
#define kAnsiUrlRegex "(?:" __OSC__ "8;" "(.*?)" "(?:" __BEL__ "|" __ST__ ")" ")*"


// For constructing ANSI hyperlinks.
#define ANSI_HYPERLINK(__TYPE__, __LINK__, __TEXT__) \
   "\033[38;5;252m" "\033]8;;" __TYPE__ ":" __LINK__ __BEL__ __TEXT__ "\033]8;;" __BEL__ "\033[39m"



#endif /* CORE_ANSI_ESCAPES_HPP */
