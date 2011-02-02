/*
 * FindReplace.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.findreplace;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasValue;
import org.rstudio.codemirror.client.CodeMirror;
import org.rstudio.codemirror.client.CodeMirror.CursorPosition;
import org.rstudio.codemirror.client.CodeMirrorEditor;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.GlobalDisplay;

public class FindReplace
{
   public interface Display
   {
      HasValue<String> getFindValue();
      HasValue<String> getReplaceValue();
      HasValue<Boolean> getCaseSensitive();
      HasValue<Boolean> getFindBackwards();
      HasClickHandlers getFindButton();
      HasClickHandlers getFindNextButton();
      HasClickHandlers getFindPrevButton();
      HasClickHandlers getReplace();
      HasClickHandlers getReplaceAll();
   }

   public FindReplace(CodeMirrorEditor editor,
                      Display display,
                      GlobalDisplay globalDisplay)
   {
      editor_ = editor;
      display_ = display;
      globalDisplay_ = globalDisplay;

      /*
      display.getFindValue().addValueChangeHandler(new ValueChangeHandler<String>()
      {
         public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent)
         {
            find(FindType.Incremental);
         }
      });
      */

      addClickHandler(display.getFindButton(), new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            find(display_.getFindBackwards().getValue() ? FindType.Reverse
                                                        : FindType.Forward);
         }
      });

      addClickHandler(display.getFindNextButton(), new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            find(FindType.Forward);
         }
      });

      addClickHandler(display.getFindPrevButton(), new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            find(FindType.Reverse);
         }
      });

      addClickHandler(display.getReplace(), new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            replace();
         }
      });

      addClickHandler(display.getReplaceAll(), new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            replaceAll();
         }
      });
   }

   private void addClickHandler(HasClickHandlers hasClickHandlers,
                                ClickHandler clickHandler)
   {
      if (hasClickHandlers != null)
         hasClickHandlers.addClickHandler(clickHandler);
   }

   private enum FindType { Forward, Reverse, Incremental
   }

   private boolean find(FindType findType)
   {
      boolean reverse = findType == FindType.Reverse;

      String searchString = display_.getFindValue().getValue();
      boolean ignoreCase = !display_.getCaseSensitive().getValue();

      CodeMirror codeMirror = editor_.getRawEditor();
      boolean success = editor_.find(
            codeMirror.cursorPosition(findType != FindType.Forward),
            searchString,
            ignoreCase,
            reverse);

      if (!success)
      {
         CursorPosition pos;
         if (reverse)
         {
            pos = CursorPosition.create(
                  codeMirror.lastLine(),
                  codeMirror.lineContent(codeMirror.lastLine()).length());
         }
         else
         {
            pos = CursorPosition.create(codeMirror.firstLine(), 0);
         }

         success = editor_.find(pos,
                                searchString,
                                ignoreCase,
                                reverse);

         if (!success && findType != FindType.Incremental)
         {
            globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                                       "Find/Replace",
                                       "No more occurrences.");
         }
      }

      return success;
   }

   private void replace()
   {
      boolean ignoreCase = !display_.getCaseSensitive().getValue();
      String searchString = display_.getFindValue().getValue();
      String replacement = display_.getReplaceValue().getValue();
      String selected = editor_.getSelectionValue();
      if (ignoreCase ? searchString.equalsIgnoreCase(selected)
                     : searchString.equals(selected))
      {
         editor_.replaceSelection(replacement);
      }

      find(FindType.Forward);
   }

   private void replaceAll()
   {
      String code = editor_.getCode();
      boolean caseSensitive = display_.getCaseSensitive().getValue();
      String find = display_.getFindValue().getValue();
      String repl = display_.getReplaceValue().getValue();

      int occurrences = 0;
      if (find.length() > 0)
      {
         String flags = caseSensitive ? "g" : "ig";
         Pattern pattern = Pattern.create(Pattern.escape(find), flags);
         StringBuilder result = new StringBuilder();

         int pos = 0; // pointer into original string
         for (Match m = pattern.match(code, 0);
              m != null;
              m = m.nextMatch())
         {
            occurrences++;

            // Add everything between the end of the last match, and this one
            int index = m.getIndex();
            result.append(code, pos, index);

            // Add the replacement value
            result.append(repl);

            // Point to the end of this match
            pos = index + m.getValue().length();
         }
         result.append(code, pos, code.length());

         CodeMirror codeMirror = editor_.getRawEditor();
         int line = codeMirror.lineNumber(codeMirror.cursorPosition(true).getLine());
         editor_.markScrollPosition();
         editor_.selectAll();
         editor_.replaceSelection(result.toString());
         codeMirror.selectLine(codeMirror.nthLine(line), 0);
         editor_.restoreScrollPosition();
      }
      globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                                 "Find/Replace",
                                 occurrences + " occurrences replaced.");
   }

   private final CodeMirrorEditor editor_;
   private final Display display_;
   private final GlobalDisplay globalDisplay_;
}
