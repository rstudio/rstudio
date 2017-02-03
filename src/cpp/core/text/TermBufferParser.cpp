/*
 * TermBufferParser.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <core/text/TermBufferParser.hpp>

namespace rstudio {
namespace core {
namespace text {

namespace {

enum ParseState {
   NORMAL,			// parsing regular text
   HAVE_ESC,      // found an ESC
   HAVE_BRACKET,  // found a [ after an ESC
   HAVE_QM,       // found a ? after ESC[
   HAVE_INT,      // finding one or more digits after ESC[?
   HAVE_TERM      // found 'h' or 'l' after ESC[?nnnn
};

struct ParseMetadata
{
   ParseMetadata(bool altActive /*initial buffer state*/)
      :
        state(NORMAL),
        haveSupportedSeq(false),
        currentCh('\0'),
        startCount(0)
   {
      if (altActive) // in alt-buffer mode from the start
         startCount = 1;
   }

   void setNewState(ParseState newState, size_t index, char ch)
   {
      if (newState == state)
         return;

      state = newState;
      currentCh = ch;

      switch(newState)
      {
      case NORMAL:
         haveSupportedSeq = false;
         break;

      case HAVE_ESC:
         escSequence.push_back(ch);
         break;

      case HAVE_BRACKET:
      case HAVE_QM:
         escSequence.push_back(ch);
         break;

      case HAVE_INT:
         // start accumulating ints until we get a non-int
         numberStr.clear();
         numberStr.push_back(ch);
         escSequence.push_back(ch);
         break;

      case HAVE_TERM:
         escSequence.push_back(ch);
         if (ch == 'h' || ch == 'l')
         {
            if (!numberStr.compare("1049") ||
                 !numberStr.compare("1047") ||
                 !numberStr.compare("47"))
             {
               // found a valid alt-buffer sequence
               haveSupportedSeq = true;
             }
         }
         break;

      default:
         break;
      }
   }

   void continueState(size_t index, char ch)
   {
      currentCh = ch;

      switch (state)
      {
      case HAVE_INT:
         numberStr.push_back(ch);
         escSequence.push_back(ch);
         break;

      default:
         break;
      }
   }

   void processState(bool lastCall)
   {
      switch (state)
      {
      case HAVE_TERM:
         if (!haveSupportedSeq)
         {
            // write out unknown sequence if we're not already inside
            // alternate buffer
            if (startCount == 0)
               output.append(escSequence); // pass through ESC sequence
         }
         else
         {
            if (currentCh == 'h') // start alt-buffer
               startCount++;
            else
               startCount = 0;
         }

         escSequence.clear();
         state = NORMAL;
         break;

      case NORMAL:
         if (startCount == 0 && !lastCall)
         {
            if (!escSequence.empty())
            {
               output.append(escSequence);
               escSequence.clear();
            }
            output.push_back(currentCh);
         }
         break;

      default:
         break;
      }

      if (lastCall)
      {
         if (startCount == 0 && !escSequence.empty())
         {
            output.append(escSequence);
         }
      }
   }

   ParseState state;

   // output buffer
   std::string output;

   // escape sequence buffer
   std::string escSequence;

   // was a supported escape sequence found?
   bool haveSupportedSeq;

   // number string as it is extraced from sequence
   std::string numberStr;

   // current character
   char currentCh;

   // number of start-alt-buffer sequences encountered with an end sequence
   int startCount;
};


} // anonymous namespace

std::string stripSecondaryBuffer(const std::string& strInput, bool* pAltBufferActive)
{
   // XTerm.js supported alt-buffer start sequences:
   //
   //   ESC[?1049h, ESC[?1047h, ESC[?47h
   //
   // End sequences:
   //   ESC[?1049l, ESC[?1047l, ESC[?47l
   //
   // Assume there could be multiple (unclosed) start sequences, but the
   // first end sequence closes them all (no nesting, as the terminal only
   // supports a single alt-buffer).
   //
   // Implemented as a 1-pass parser. If we see any of the start sequences
   // (or are already "inside" an unclosed start sequence from a previous call)
   // discard the text up to and including the first end-sequence (or the end
   // of the string).
   //
   // At completion, the passed-in bool is updated to reflect which buffer
   // we were left in.

   // initial state
   bool altActive = pAltBufferActive ? *pAltBufferActive : false;
   size_t origLength = strInput.length();

   ParseMetadata parse(altActive);

   // Single pass through the original string. Look for the escape sequences,
   // and when we have a confirmed run of text that isn't between them,
   // append it to the output.
   for (size_t i = 0; i < origLength; i++)
   {
      char ch = strInput[i];
      switch (parse.state)
      {
      case NORMAL:
         if (ch == '\033')
            parse.setNewState(HAVE_ESC, i, ch);
         else
            parse.continueState(i, ch);
         break;

      case HAVE_ESC:
         if (ch == '[')
            parse.setNewState(HAVE_BRACKET, i, ch);
         else
            parse.setNewState(NORMAL, i, ch);
         break;

      case HAVE_BRACKET:
         if (ch == '?')
            parse.setNewState(HAVE_QM, i, ch);
         else
            parse.setNewState(NORMAL, i, ch);
         break;

      case HAVE_QM:
         if (::isdigit(ch))
            parse.setNewState(HAVE_INT, i, ch);
         else
            parse.setNewState(NORMAL, i, ch);
         break;

      case HAVE_INT:
         if (::isdigit(ch))
            parse.continueState(i, ch);
         else
            parse.setNewState(HAVE_TERM, i, ch);
         break;

      default:
         break;
      }

      parse.processState(false);
   }

   parse.processState(true);

   // set output mode
   if (pAltBufferActive)
      *pAltBufferActive = parse.startCount > 0;;

   return parse.output;
}

} // namespace text
} // namespace core
} // namespace rstudio
