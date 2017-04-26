/*
 * TerminalLocalEcho.java
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
package org.rstudio.studio.client.workbench.views.terminal;

import java.util.LinkedList;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.StringSink;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

public class TerminalLocalEcho
{
   public TerminalLocalEcho(StringSink writer)
   {
      writer_ = writer;
      
   }
   
   public void echo(String input)
   {
      if (paused())
         return;
      
      // input longer than one character is likely a control sequence, or
      // pasted text; only local-echo and sync with single-character input
      if (input.length() == 1) 
      {
         int ch = input.charAt(0);
         if (ch >= 32 /*space*/ && ch <= 126 /*tilde*/)
         {
            localEcho_.add(input);
            writer_.write(input);
         }
      }
   }
   
   public boolean isEmpty()
   {
      return localEcho_.isEmpty();
   }
   
   public void write(String output)
   {
      // Rapid typing with intermixed backspaces can cause shell
      // to insert ^H and ESC[K into the already-local-echoed output.
      // Also, typing and backspacing at the start of a line can cause
      // shell to return a BEL (^G) mixed with previously echoed output.
      // 
      // Thus remove ANSI control sequences from output when matching or we 
      // can easily get out of sync and orphan deleted characters in the 
      // local buffer.
      //
      // This manifests as characters you can't backspace over, but aren't
      // seen by the shell process when you press enter.
      int chunkStart = 0;
      int chunkEnd = output.length();
      Match match = ANSI_CTRL_PATTERN.match(output,  0);
      while (match != null)
      {
         chunkEnd = match.getIndex();

         // try to match local-echoed text up to this ignored sequence
         String outputToMatch = output.substring(chunkStart, chunkEnd);
         if (outputToMatch.length() > 0)
         {
            int matchLen = outputNonEchoed(outputToMatch);
            if (matchLen == 0)
            {
               // didn't match previously echoed text at all; write 
               // everything after that chunk
               writer_.write(output.substring(chunkEnd));
               return;
            }
            // Otherwise completely or partially matched; at this point
            // we've echoed everything necessary up to the end of currently
            // chunk and can move onto next one.
         }

         writer_.write(match.getValue()); // write special sequence
         
         chunkStart = chunkEnd + match.getValue().length();
         chunkEnd = output.length();
         match = match.nextMatch();
      }

      outputNonEchoed(output.substring(chunkStart, chunkEnd));
   }
   
   /**
    * Skip any previously local-echoed output, write out any trailing text
    * that wasn't previously echoed. Only exact-match from beginning of string.
    * @param outputToMatch text to match against previously echoed text
    * @return length of matched sequence
    */
   private int outputNonEchoed(String outputToMatch)
   {
      String lastOutput = "";
      while (!localEcho_.isEmpty() && lastOutput.length() < outputToMatch.length())
      {
         lastOutput += localEcho_.poll();
      }

      if (lastOutput.equals(outputToMatch))
      {
         // all matched, nothing to output
         return outputToMatch.length();
      }

      else if (outputToMatch.startsWith(lastOutput))
      {
         // output is superset of what was local-echoed; write out the
         // unmatched part
         writer_.write(outputToMatch.substring(lastOutput.length()));
         return lastOutput.length();
      }
      else
      {
         // didn't match previously echoed text; delete local-input
         // queue so we don't get too far out of sync and write text as-is
         
         // diagnostics to help isolate cases where local-echo is 
         // not matching as expected 
         diagnostic("Received: '" + AnsiCode.prettyPrint(outputToMatch) + 
               "' Had: '" + AnsiCode.prettyPrint(lastOutput) + "'");
         
         localEcho_.clear();
         writer_.write(outputToMatch);
         return 0;
      }
   }
   
   public void clear()
   {
      localEcho_.clear();
   }
   
   public void pause(int pauseMillis)
   {
      stopEchoPause_ = System.currentTimeMillis() + pauseMillis;
      clear();
   }
   
   public boolean paused()
   {
      if (stopEchoPause_ == 0)
         return false;
      
      if (stopEchoPause_ > 0 && System.currentTimeMillis() < stopEchoPause_)
      {
         return true;
      }
      else
      {
         stopEchoPause_ = 0;
         return false;
      }
   }
   
   private void diagnostic(String msg)
   {
      if (diagnostic_ == null)
         diagnostic_ = new StringBuilder();
     
      diagnostic_.append(StringUtil.getTimestamp());
      diagnostic_.append(": ");
      diagnostic_.append(msg);
      diagnostic_.append("\n");
   }

   public String getDiagnostics()
   {
      if (diagnostic_ == null || diagnostic_.length() == 0)
         return("<none>\n");
      else
         return diagnostic_.toString();
   }

   public void resetDiagnostics()
   {
      diagnostic_ = null;
   }
   
  // Matches ANSI control sequences or BS, CR, LF, DEL, BEL
   private static final Pattern ANSI_CTRL_PATTERN =
         Pattern.create("(?:" + AnsiCode.ANSI_REGEX + ")|(?:" + "[\b\n\r\177\7]" + ")");

   // Pause local-echo until this time
   private long stopEchoPause_;
   private StringBuilder diagnostic_;
   
   private final StringSink writer_;
   private LinkedList<String> localEcho_ = new LinkedList<String>();
}
