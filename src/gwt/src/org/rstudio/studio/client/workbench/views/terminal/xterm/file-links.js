/*
 * file-links.js
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

// An xterm.js link provider that turns file paths printed in the terminal
// (git status output, compiler diagnostics, ls listings, ...) into links that
// open the file in RStudio with Ctrl+Click (Cmd+Click on macOS).
//
// Detection is deliberately loose: every whitespace-delimited token on the
// hovered line is a candidate, and the host decides which of them name an
// existing file relative to the terminal's working directory. Only those
// become links, so plain words never get underlined.
//
// The wrapped-line and string-to-cell mapping logic mirrors LinkComputer in
// xterm's web-links addon (MIT licensed, https://github.com/xtermjs/xterm.js).

(function(root) {
"use strict";

// Longest run of wrapped rows to stitch together, in characters.
var MAX_LINE_CHARS = 2048;

// Bound the work sent to the host for one line.
var MAX_CANDIDATES = 64;
var MAX_CANDIDATE_LENGTH = 512;

// Resolved lines are remembered briefly so sweeping the mouse over the same
// output does not re-query the host, and so a line whose reply xterm dropped
// (it discards replies arriving while the mouse is outside the terminal)
// still gets its links when the mouse returns.
var CACHE_TTL_MS = 5000;
var CACHE_MAX_ENTRIES = 64;

// Characters that never belong to an unquoted path token.
var TOKEN_PATTERN = /[^\s"'`<>|]+/g;

// Quoted tokens may contain spaces: git prints `rename "a b.R" -> "c.R"`.
var QUOTED_PATTERN = /"([^"\s][^"]*)"|'([^'\s][^']*)'/g;

var LEADING_JUNK = /^[(\[{<]+/;
var TRAILING_PUNCTUATION = /[.,;:!?]$/;

// Position suffixes: gcc/cli style `file:12:3`, MSVC style `file(12,3)`,
// and the `file#12` form R uses when printing srcrefs.
var POSITION_PATTERNS = [
   /^(.+?):(\d+)(?::(\d+))?$/,
   /^(.+?)\((\d+)(?:,\s*(\d+))?\)$/,
   /^(.+?)#(\d+)$/
];

var CLOSERS = { ")": "(", "]": "[", "}": "{", ">": "<" };

// Links are only decorated -- and only activate -- while the platform's
// open-link modifier is held, so hovering ordinary output does not light up
// every word that happens to name a file. xterm reads a link's decorations
// when a hover begins and then swaps in live accessors of its own, so the
// state at hover time comes from a getter and later changes are written
// through xterm's object (Linkifier._handleNewLink, read from @xterm/xterm
// 6.0.0 -- the version pinned in src/gwt/tools/build-xterm). Only one link is
// ever hovered, so one tracker serves every terminal; it records the owning
// provider too, so a terminal torn down mid-hover can let go of its link.
var modifier = {
   isMac: false,
   held: false,
   link: null,
   owner: null
};

function hasModifier(event)
{
   return modifier.isMac ? !!event.metaKey : !!event.ctrlKey;
}

function forgetHoveredLink()
{
   modifier.link = null;
   modifier.owner = null;
}

function setModifierHeld(held)
{
   if (modifier.held === held)
      return;

   modifier.held = held;

   // writing through the accessors xterm installed repaints the hovered link
   var link = modifier.link;
   if (link && link.decorations)
   {
      link.decorations.pointerCursor = held;
      link.decorations.underline = held;
   }
}

// The decorations xterm reads when a hover begins; getters, because the
// modifier may have been pressed or released since the link was built. Writes
// are ignored: xterm owns the decorations once the hover has started.
function createDecorations()
{
   var property = {
      configurable: true,
      enumerable: true,
      get: function() { return modifier.held; },
      set: function() {}
   };

   var decorations = {};
   Object.defineProperty(decorations, "pointerCursor", property);
   Object.defineProperty(decorations, "underline", property);
   return decorations;
}

// Watch the modifier, so a hovered link lights up as soon as it is pressed and
// goes dark when it is released. Key events only arrive while this document
// has focus, so the terminal's own mouse events -- which carry the modifier
// state -- are consulted as well; that also covers moving into the terminal
// with the modifier already down.
function trackModifier(element, isMac)
{
   modifier.isMac = isMac;

   if (element)
   {
      element.addEventListener("mousemove", function(event) {
         setModifierHeld(hasModifier(event));
      }, true);
   }

   // the key listeners are shared by every terminal in the document
   var ownerDocument = element && element.ownerDocument;
   if (!ownerDocument || ownerDocument.rstudioFileLinkModifier_)
      return;

   ownerDocument.rstudioFileLinkModifier_ = true;
   ownerDocument.addEventListener("keydown", function(event) {
      setModifierHeld(hasModifier(event));
   }, true);
   ownerDocument.addEventListener("keyup", function(event) {
      setModifierHeld(hasModifier(event));
   }, true);

   // a release that happens while another window has focus is never delivered
   var view = ownerDocument.defaultView;
   if (view)
      view.addEventListener("blur", function() { setModifierHeld(false); });
}

function FileLinkProvider(terminal, host)
{
   this._terminal = terminal;
   this._host = host;
   this._requestId = 0;
   this._cacheGeneration = 0;
   this._cache = {};
   this._cacheKeys = [];

   trackModifier(terminal.element, !!host.isMac);
}

FileLinkProvider.prototype.provideLinks = function(bufferLineNumber, callback)
{
   var requestId = ++this._requestId;
   var terminal = this._terminal;
   var buffer = terminal.buffer.active;
   var window = getWindowedLineStrings(terminal, bufferLineNumber - 1);
   var text = window.lines.join("");
   var matches = findCandidates(text);
   if (matches.length === 0)
   {
      callback(undefined);
      return;
   }

   var self = this;
   var cached = this._lookupCache(text);
   if (cached)
   {
      callback(self._buildLinks(window.startLine, matches, cached));
      return;
   }

   var candidates = matches.map(function(match) { return match.path; });
   var cacheGeneration = this._cacheGeneration;
   this._host.resolve(candidates, function(resolved) {
      // A cwd change invalidates pending resolutions as well as cached ones.
      if (cacheGeneration !== self._cacheGeneration)
         return;

      resolved = resolved || [];
      self._storeCache(text, resolved);

      // xterm records whatever reply arrives against the line it is
      // currently asking about, so a reply for a line the mouse has since
      // left must be dropped rather than delivered late.
      if (requestId !== self._requestId)
         return;

      // The result can be cached by text, but its offsets only apply while
      // the captured rows still contain that text in the same buffer.
      if (terminal.buffer.active !== buffer)
         return;
      var currentWindow = getWindowedLineStrings(terminal, bufferLineNumber - 1);
      if (currentWindow.startLine !== window.startLine ||
          currentWindow.lines.length !== window.lines.length ||
          currentWindow.lines.some(function(line, index) { return line !== window.lines[index]; }))
      {
         return;
      }

      callback(self._buildLinks(window.startLine, matches, resolved));
   });
};

FileLinkProvider.prototype._buildLinks = function(startLine, matches, resolved)
{
   var links = [];
   for (var i = 0; i < matches.length; i++)
   {
      var path = resolved[i];
      if (!path)
         continue;

      var range = computeRange(this._terminal, startLine, matches[i].index, matches[i].length);
      if (range)
         links.push(this._createLink(range, matches[i], path));
   }
   return links.length > 0 ? links : undefined;
};

FileLinkProvider.prototype._createLink = function(range, match, path)
{
   var self = this;
   var cacheGeneration = this._cacheGeneration;
   var host = this._host;
   var link = {
      range: range,
      text: match.text,
      decorations: createDecorations(),
      activate: function(event) {
         if (event.button !== 0 || cacheGeneration !== self._cacheGeneration)
            return;

         if (!hasModifier(event))
            return;

         host.open(path, match.line, match.column);
      },
      hover: function() {
         modifier.link = link;
         modifier.owner = self;
         host.hover(path);
      },
      leave: function() {
         if (modifier.link === link)
            forgetHoveredLink();

         // put our own decorations back, so a later hover of this same link
         // consults the modifier again rather than xterm's remembered state
         link.decorations = createDecorations();
         host.leave();
      }
   };

   return link;
};

// A terminal can be torn down with a link still hovered: xterm's linkifier
// does not call leave() when it is disposed, and an element detached from the
// document gets no mouseleave either. The hovered link closes over the
// provider and the host widget, so without this the dead terminal would be
// kept alive until some other link happened to be hovered.
FileLinkProvider.prototype.dispose = function()
{
   if (modifier.owner === this)
      forgetHoveredLink();
};

FileLinkProvider.prototype.clearCache = function()
{
   this._cacheGeneration++;
   this._cache = {};
   this._cacheKeys = [];
};

FileLinkProvider.prototype._lookupCache = function(text)
{
   var entry = this._cache[text];
   if (!entry)
      return null;

   if (Date.now() - entry.time > CACHE_TTL_MS)
   {
      delete this._cache[text];
      var stale = this._cacheKeys.indexOf(text);
      if (stale !== -1)
         this._cacheKeys.splice(stale, 1);
      return null;
   }
   return entry.resolved;
};

FileLinkProvider.prototype._storeCache = function(text, resolved)
{
   if (!this._cache[text])
   {
      this._cacheKeys.push(text);
      while (this._cacheKeys.length > CACHE_MAX_ENTRIES)
         delete this._cache[this._cacheKeys.shift()];
   }
   this._cache[text] = { time: Date.now(), resolved: resolved };
};

// Split a line into candidate tokens. Each candidate records where it sits in
// the line (so it can be mapped back to buffer cells), the path to validate,
// and any line/column suffix.
function findCandidates(text)
{
   var candidates = [];
   var masked = text;
   var match;

   QUOTED_PATTERN.lastIndex = 0;
   while ((match = QUOTED_PATTERN.exec(text)) !== null)
   {
      var quoted = match[1] !== undefined ? match[1] : match[2];
      addCandidate(candidates, match.index + 1, quoted);

      // blank out the quoted region so the unquoted pass skips it
      masked = masked.substring(0, match.index) +
               repeatSpace(match[0].length) +
               masked.substring(match.index + match[0].length);
   }

   TOKEN_PATTERN.lastIndex = 0;
   while ((match = TOKEN_PATTERN.exec(masked)) !== null)
   {
      var trimmed = trimToken(match[0]);
      if (trimmed.text.length > 0)
         addCandidate(candidates, match.index + trimmed.offset, trimmed.text);
   }

   candidates.sort(function(a, b) { return a.index - b.index; });
   return candidates.slice(0, MAX_CANDIDATES);
}

function addCandidate(candidates, index, text)
{
   if (text.length > MAX_CANDIDATE_LENGTH)
      return;

   // URLs belong to the web links addon; bare numbers and punctuation-only
   // tokens (`->`, `...`) are never paths
   if (text.indexOf("://") !== -1 || !/[A-Za-z0-9]/.test(text) || /^\d+$/.test(text))
      return;

   var position = parsePosition(text);
   candidates.push({
      index: index,
      length: text.length,
      text: text,
      path: position.path,
      line: position.line,
      column: position.column
   });
}

// Strip enclosing brackets and sentence punctuation from a token, returning
// the kept text and its offset within the original token.
function trimToken(token)
{
   var offset = 0;
   var leading = LEADING_JUNK.exec(token);
   if (leading)
   {
      offset = leading[0].length;
      token = token.substring(offset);
   }

   for (;;)
   {
      if (TRAILING_PUNCTUATION.test(token))
      {
         token = token.substring(0, token.length - 1);
         continue;
      }

      var last = token.charAt(token.length - 1);
      var opener = CLOSERS[last];
      if (opener && token.indexOf(opener) === -1)
      {
         token = token.substring(0, token.length - 1);
         continue;
      }

      break;
   }

   return { text: token, offset: offset };
}

function parsePosition(text)
{
   for (var i = 0; i < POSITION_PATTERNS.length; i++)
   {
      var match = POSITION_PATTERNS[i].exec(text);
      if (match)
      {
         return {
            path: match[1],
            line: parseInt(match[2], 10),
            column: match[3] !== undefined ? parseInt(match[3], 10) : 0
         };
      }
   }
   return { path: text, line: 0, column: 0 };
}

function repeatSpace(count)
{
   return new Array(count + 1).join(" ");
}

// Collect the run of wrapped rows the given row belongs to, so a path that
// wraps across rows is seen whole. Rows other than the last keep their
// trailing whitespace so string offsets stay one-to-one with buffer cells.
function getWindowedLineStrings(terminal, lineIndex)
{
   var buffer = terminal.buffer.active;
   var lines = [];
   var startLine = lineIndex;
   var line = buffer.getLine(lineIndex);
   if (!line)
      return { lines: lines, startLine: startLine };

   var current = line.translateToString(true);
   var chars = 0;
   var previous;
   var text;

   // Spaces may be inside a quoted path, so collect all wrapped rows within
   // the size limit instead of treating whitespace as a token boundary.
   if (line.isWrapped)
   {
      while (chars < MAX_LINE_CHARS)
      {
         previous = buffer.getLine(startLine - 1);
         if (!previous)
            break;

         startLine--;
         text = previous.translateToString(false);
         chars += text.length;
         lines.push(text);
         if (!previous.isWrapped)
            break;
      }
      lines.reverse();
   }

   var next = buffer.getLine(lineIndex + 1);
   if (next && next.isWrapped)
      current = line.translateToString(false);
   lines.push(current);

   chars = 0;
   var nextIndex = lineIndex;
   while (chars < MAX_LINE_CHARS)
   {
      next = buffer.getLine(nextIndex + 1);
      if (!next || !next.isWrapped)
         break;

      nextIndex++;
      var following = buffer.getLine(nextIndex + 1);
      text = next.translateToString(!(following && following.isWrapped));
      chars += text.length;
      lines.push(text);
   }

   return { lines: lines, startLine: startLine };
}

// Map a string offset within the stitched line back to a buffer cell,
// walking cells from (lineIndex, cellIndex) and accounting for wide
// characters. Returns [-1, -1] if the buffer changed underneath us.
function mapStringIndex(terminal, lineIndex, cellIndex, stringIndex)
{
   var buffer = terminal.buffer.active;
   var cell = buffer.getNullCell();
   var start = cellIndex;

   while (stringIndex)
   {
      var line = buffer.getLine(lineIndex);
      if (!line)
         return [-1, -1];

      for (var i = start; i < line.length; i++)
      {
         line.getCell(i, cell);
         var chars = cell.getChars();
         if (cell.getWidth())
         {
            stringIndex -= chars.length || 1;

            // a wide character that did not fit at the end of the row
            // leaves an empty cell behind and wraps to the next row
            if (i === line.length - 1 && chars === "")
            {
               var nextLine = buffer.getLine(lineIndex + 1);
               if (nextLine && nextLine.isWrapped)
               {
                  nextLine.getCell(0, cell);
                  if (cell.getWidth() === 2)
                     stringIndex += 1;
               }
            }
         }
         if (stringIndex < 0)
            return [lineIndex, i];
      }

      lineIndex++;
      start = 0;
   }

   return [lineIndex, start];
}

function computeRange(terminal, startLine, index, length)
{
   var start = mapStringIndex(terminal, startLine, 0, index);
   if (start[0] === -1)
      return null;

   var end = mapStringIndex(terminal, start[0], start[1], length);
   if (end[0] === -1)
      return null;

   // a token ending exactly at the row edge maps to column 0 of the next row
   if (end[1] === 0 && end[0] > start[0])
      end = [end[0] - 1, terminal.cols];

   return {
      start: { x: start[1] + 1, y: start[0] + 1 },
      end: { x: end[1], y: end[0] + 1 }
   };
}

root.RStudioFileLinks = {
   FileLinkProvider: FileLinkProvider,
   findCandidates: findCandidates
};

})(typeof window !== "undefined" ? window : globalThis);
