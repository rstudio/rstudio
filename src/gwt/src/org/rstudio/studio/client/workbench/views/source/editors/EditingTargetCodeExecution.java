/*
 * EditingTargetCodeExecution.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.source.editors;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleExecutePendingInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.inject.Inject;

public class EditingTargetCodeExecution
{
   public interface CodeExtractor
   {
      String extractCode(DocDisplay docDisplay, Range range);
   }
   
   public EditingTargetCodeExecution(DocDisplay docDisplay)
   {
      this(docDisplay, new CodeExtractor() {
         @Override
         public String extractCode(DocDisplay docDisplay, Range range)
         {
            return docDisplay.getCode(range.getStart(), range.getEnd());
         }
      });
   }
   
   public EditingTargetCodeExecution(DocDisplay docDisplay,
                                     CodeExtractor codeExtractor)
   {
      docDisplay_ = docDisplay;
      codeExtractor_ = codeExtractor;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(EventBus events, UIPrefs prefs)
   {
      events_ = events;
      prefs_ = prefs;
   }
   
   public void executeSelection(boolean consoleExecuteWhenNotFocused)
   {  
      // allow console a chance to execute code if we aren't focused
      if (consoleExecuteWhenNotFocused && !docDisplay_.isFocused())
      {
         events_.fireEvent(new ConsoleExecutePendingInputEvent());
         return;
      }
      
      
      Range selectionRange = docDisplay_.getSelectionRange();
      boolean noSelection = selectionRange.isEmpty();
      if (noSelection)
      {
         int row = docDisplay_.getSelectionStart().getRow();
         selectionRange = Range.fromPoints(
               Position.create(row, 0),
               Position.create(row, docDisplay_.getLength(row)));
      }

      executeRange(selectionRange);
      
      // advance if there is no current selection
      if (noSelection)
      {
         if (!docDisplay_.moveSelectionToNextLine(true))
            docDisplay_.moveSelectionToBlankLine();
      }
   }
   
   public void executeRange(Range range)
   {
      String code = codeExtractor_.extractCode(docDisplay_, range);
     
      setLastExecuted(range.getStart(), range.getEnd());
      
      // trim intelligently
      code = code.trim();
      if (code.length() == 0)
         code = "\n";
      
      // strip roxygen off the beginning of lines
      if (isRoxygenExampleRange(range))
      {
         code = code.replaceFirst("^\\s*#'\\s?", "");
         code = code.replaceAll("\n\\s*#'\\s?", "\n");
      }
      
      // send to console
      events_.fireEvent(new SendToConsoleEvent(
                                  code, 
                                  true, 
                                  prefs_.focusConsoleAfterExec().getValue()));
   }
   
   public void executeLastCode()
   {
      if (lastExecutedCode_ != null)
      {
         String code = lastExecutedCode_.getValue();
         if (code != null && code.trim().length() > 0)
         {
            events_.fireEvent(new SendToConsoleEvent(code, true));
         }
      }
   }
   
   public void setLastExecuted(Position start, Position end)
   {
      detachLastExecuted();
      lastExecutedCode_ = docDisplay_.createAnchoredSelection(start, end);
   }

   public void detachLastExecuted()
   {
      if (lastExecutedCode_ != null)
      {
         lastExecutedCode_.detach();
         lastExecutedCode_ = null;
      }
   }
   
   private boolean isRoxygenExampleRange(Range range)
   {
      // ensure all of the lines in the selection are within roxygen
      int selStartRow = range.getStart().getRow();
      int selEndRow = range.getEnd().getRow();
      
      // ignore the last row if it's column 0
      if (range.getEnd().getColumn() == 0)
         selEndRow = Math.max(selEndRow-1, selStartRow);
      
      for (int i=selStartRow; i<=selEndRow; i++)
      {
         if (!isRoxygenLine(docDisplay_.getLine(i)))
            return false;
      }
      
      // scan backwards and look for @example
      int row = selStartRow;
      while (--row >= 0)
      {
         String line = docDisplay_.getLine(row);
         
         // must still be within roxygen
         if (!isRoxygenLine(line))
            return false;
         
         // if we are in an example block return true
         if (line.matches("^\\s*#'\\s*@example.*$"))
            return true;
      }
      
      // didn't find the example block
      return false;
   }
   
   private boolean isRoxygenLine(String line)
   {
      String trimmedLine = line.trim();
      return (trimmedLine.length() == 0) || trimmedLine.startsWith("#'");
   }
   
   private EventBus events_;
   private UIPrefs prefs_;
   private final DocDisplay docDisplay_;
   private final CodeExtractor codeExtractor_;
   private AnchoredSelection lastExecutedCode_;
}

