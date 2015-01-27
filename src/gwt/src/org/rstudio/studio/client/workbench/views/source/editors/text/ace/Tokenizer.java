package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.gwt.core.client.JavaScriptObject;

public class Tokenizer extends JavaScriptObject
{
   protected Tokenizer()
   {
   }
   
   // This is a wrapper function that takes a line (that may have
   // new lines) and properly tokenizes it as a flat token array.
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
         state = tokenizedLine.state;
         
         // We may skip the first token if it is text.
         var start = 0;
         
         // If there are no tokens on this line...
         if (lineTokens.length === 0)
         {
            // ... and the last token of the previous line
            // was whitespace, then add a newline to it.
            if (lastToken.type === "text")
            {
               lastToken.value += "\n";
               continue;
            }
            
            // Otherwise, add a newline token and set
            // it as current.
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
            // then merge them.
            if (lineTokens[0].type === "text")
            {
               start++;
               lastToken.value += "\n";
               lastToken.value += lineTokens[0].value;
            }
            
            // Otherwise, just append a newline to the last token. 
            else
            {
               lastToken.value += "\n";
            }
         }
         
         // If the last token on the previous line was not 'text'...
         else
         {
            // ... and the first token on this line was text,
            // prepend a newline.
            if (lineTokens[0].type === "text")
            {
               lineTokens[0].value = "\n" + lineTokens[0].value;
            }
            
            // Otherwise, insert a newline text token before
            // appending the tokens from this line.
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
         for (var j = start; j < lineTokens.length; j++)
            tokens.push(lineTokens[j]);
         
         // Update the last token.
         lastToken = tokens[tokens.length - 1];
         
      }
      
      return tokens;
      
   }-*/;
   
   public final ArrayList<Token> tokenize(String line)
   {
      ArrayList<Token> tokens =
            new ArrayList<Token>(Arrays.asList(doTokenize(line)));
      
      return tokens;
   }
}
