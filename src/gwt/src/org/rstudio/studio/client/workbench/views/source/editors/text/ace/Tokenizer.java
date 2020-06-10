/*
 * Tokenizer.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;

public class Tokenizer extends JavaScriptObject
{
   protected Tokenizer()
   {
   }
   
   public static final native Tokenizer createRTokenizer() /*-{
      var Tokenizer = $wnd.require("ace/tokenizer").Tokenizer;
      var RHighlightRules = $wnd.require("mode/r_highlight_rules").RHighlightRules;
      return new Tokenizer(new RHighlightRules().getRules());
   }-*/;
   
   // This is a wrapper function that takes a line (that may have
   // new lines) and properly tokenizes it as a flat token array, which
   // makes it very easy to iterate and munge. This will work for any
   // generic tokenizer (not just R)
   private final native Token[] doTokenize(String line) /*-{
      
      var currentToken;
      var tokens = [];
      var state = "start";
      var splat = line.split("\n");
      
      // Add the first line.
      var tokenizedLine = this.getLineTokens(splat[0], state);
      tokens = tokenizedLine.tokens;
      state = tokenizedLine.state;
      
      // If there were no tokens on this line, add a newline token.
      if (tokens.length === 0)
      {
         tokens.push({
            type: "text",
            value: "\n"
         });
      }
      
      // Cache the final token. We may need to munge it.
      var lastToken = tokens[tokens.length - 1];
      
      // Iterate through the rest of the lines.
      for (var i = 1; i < splat.length; i++) {
         
         var tokenizedLine = this.getLineTokens(splat[i], state);
         var lineTokens = tokenizedLine.tokens;
         var n = lineTokens.length;
         
         state = tokenizedLine.state;
         
         // We may skip the first token if it is text.
         var start = 0;
         
         // If there are no tokens on this line...
         if (n === 0)
         {
            // ... and the last token of the previous line
            // was whitespace, then add a newline to it.
            if (lastToken.type === "text")
            {
               lastToken.value += "\n";
               continue;
            }
            
            // ... otherwise, add a newline token and set
            // it as the 'lastToken', implicitly adding a
            // newline to the previous line.
            else
            {
               tokens.push({
                  type: "text",
                  value: "\n"
               });
               lastToken = tokens[tokens.length - 1];
               continue;
            }
         }
         
         // If the last token on the previous line was 'text'...
         if (lastToken.type === "text")
         {
            // ... and the first token on this line is text too,
            // then merge them
            if (lineTokens[0].type === "text")
            {
               start++;
               lastToken.value += "\n";
               lastToken.value += lineTokens[0].value;
            }
            
            // ... otherwise, just append a newline to the last token. 
            else
            {
               lastToken.value += "\n";
            }
         }
         
         // ... otherwise, if the last token on the previous line was not 'text'...
         else
         {
            // ... and the first token on this line was text,
            // prepend a newline to it -- effectively merging a
            // single 'newline' whitespace token into that token.
            if (lineTokens[0].type === "text")
            {
               lineTokens[0].value = "\n" + lineTokens[0].value;
            }
            
            // ... otherwise, insert a newline text token before
            // appending the tokens from this line -- this adds
            // a single whitespace token, separating two non-whitespace
            // tokens.
            else
            {
               tokens.push({
                  type: "text",
                  value: "\n"
               });
               lastToken = tokens[tokens.length - 1];
            }
         }
         
         // Push back the rest of the tokens.
         var n = lineTokens.length;
         for (var j = start; j < n; j++)
            tokens.push(lineTokens[j]);
         
         // Update the last token.
         lastToken = tokens[tokens.length - 1];
         
      }
      
      return tokens;
      
   }-*/;
   
   public final List<Token> tokenize(String line)
   {
      return new ArrayList<Token>(Arrays.asList(doTokenize(line)));
   }
}
