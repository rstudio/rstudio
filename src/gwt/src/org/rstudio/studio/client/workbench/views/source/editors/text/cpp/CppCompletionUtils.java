/*
 * CppCompletionUtils.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import com.google.gwt.dom.client.NativeEvent;

public class CppCompletionUtils
{
   public static boolean isCppIdentifierKey(NativeEvent event)
   {
      if (event.getAltKey() || event.getCtrlKey() || event.getMetaKey())
         return false;
      
      int keyCode = event.getKeyCode();
      if (keyCode >= 'a' && keyCode <= 'z')
         return true;
      if (keyCode >= 'A' && keyCode <= 'Z')
         return true;
      if (KeyboardHelper.isUnderscore(event))
         return true;
     
      if (event.getShiftKey())
         return false;
      
      if (keyCode >= '0' && keyCode <= '9')
         return true;
      
      return false;
   }
   
   public static boolean isCppIdentifierChar(char c)
   {
      return ((c >= 'a' && c <= 'z') || 
              (c >= 'A' && c <= 'Z') || 
              (c >= '0' && c <= '9') ||
               c == '_');
   }
   
   public static CompletionPosition getCompletionPosition(DocDisplay docDisplay,
                                                          boolean explicit,
                                                          boolean alwaysComplete,
                                                          int autoChars)
   {      
      // get the current line of code
      String line = docDisplay.getCurrentLine();
      
      // is this an '#include' line?
      Pattern reInclude = Pattern.create("^\\s*#+\\s*include", "");
      boolean isInclude = reInclude.test(line);
      
      // get the cursor position
      Position position = docDisplay.getCursorPosition();
      
      // is there already a C++ identifier character at this position? 
      // if so then bail
      if ((position.getColumn() < line.length()) &&
          CppCompletionUtils.isCppIdentifierChar(StringUtil.charAt(line, position.getColumn())))
      {
         return null;
      }
      
      
      // determine the column right before this one
      int inputCol = position.getColumn() - 1;
               
      // walk backwards across C++ identifier symbols
      int col = inputCol;
      if (isInclude)
      {
         while (col >= 0 && !StringUtil.isOneOf(StringUtil.charAt(line, col), '/', '<', '"'))
         {
            col--;
         }
      }
      else
      {
         while ((col >= 0) && CppCompletionUtils.isCppIdentifierChar(StringUtil.charAt(line, col)))
         {
            col--;
         }
      }
     
      // record source position
      Position startPos = Position.create(position.getRow(), col + 1);
      
      // check for a completion triggering sequence
      char ch = StringUtil.charAt(line, col);
      char prefixCh = StringUtil.charAt(line, col - 1);
      
      // member
      if (ch == '.' || (prefixCh == '-' && ch == '>'))
      {
         return new CompletionPosition(startPos, 
                                       "", // no user text (get all completions)
                                       CompletionPosition.Scope.Member);
      }
      
      // scope
      else if (prefixCh == ':' && ch == ':') 
      {
         return new CompletionPosition(startPos,
                                       "", // no user text (get all completions)
                                       CompletionPosition.Scope.Namespace);
      }
      
      // directory
      else if (isInclude && ch == '/')
      {
         return new CompletionPosition(startPos,
                                       "", // no user test (get all completions)
                                       CompletionPosition.Scope.File);
      }
      
      // minimum character threshold
      else if ((alwaysComplete || explicit) &&                     // either always completing or explicit
               ((inputCol - col) >= (explicit ? 1 : autoChars)) && // meets the character threshold
               (isInclude || ch != '"'))                           // not a quote character
      {
         // calculate user text (up to two characters of additional
         // server side filter)
         String userText = line.substring(
               col + 1, Math.min(col + 3, position.getColumn()));
         
         CompletionPosition.Scope scope = isInclude
               ? CompletionPosition.Scope.File
               : CompletionPosition.Scope.Global;
               
         return new CompletionPosition(startPos,
                                       userText,
                                       scope);
      }
      else
      {
         return null;
      }
   }
}
