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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HasValue;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Search;

public class FindReplace
{
   public interface Display
   {
      HasValue<String> getFindValue();
      HasValue<String> getReplaceValue();
      HasValue<Boolean> getCaseSensitive();
      HasValue<Boolean> getRegex();
      HasValue<Boolean> getFindBackwards();
      HasClickHandlers getFindButton();
      HasClickHandlers getFindNextButton();
      HasClickHandlers getFindPrevButton();
      HasClickHandlers getReplace();
      HasClickHandlers getReplaceAll();
   }

   public FindReplace(AceEditor editor,
                      Display display,
                      GlobalDisplay globalDisplay)
   {
      editor_ = editor;
      display_ = display;
      globalDisplay_ = globalDisplay;
      
      HasValue<Boolean> caseSensitive = display_.getCaseSensitive();
      caseSensitive.setValue(defaultCaseSensitive_);
      caseSensitive.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            defaultCaseSensitive_ = event.getValue();
         }
      });
      HasValue<Boolean> regex = display_.getRegex();
      regex.setValue(defaultRegex_);
      regex.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            defaultRegex_ = event.getValue();
         }
      });
      
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

   private enum FindType { Forward, Reverse }

   private boolean find(FindType findType)
   {
      String searchString = display_.getFindValue().getValue();
      if (searchString.length() == 0)
         return false;
      
      boolean ignoreCase = !display_.getCaseSensitive().getValue();
      boolean regex = display_.getRegex().getValue();

      Search search = Search.create(searchString,
                                    findType != FindType.Forward,
                                    true,
                                    !ignoreCase,
                                    false,
                                    false,
                                    regex);

      Range range = search.find(editor_.getSession());

      if (range == null)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                                    "Find/Replace",
                                    "No more occurrences.");
         return false;
      }
      else
      {
         editor_.getSession().getSelection().setSelectionRange(range);
         return true;
      }
   }

   private void replace()
   {
      String searchString = display_.getFindValue().getValue();
      if (searchString.length() == 0)
         return;
      boolean ignoreCase = !display_.getCaseSensitive().getValue();
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
      boolean regex = display_.getRegex().getValue();
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

         editor_.replaceCode(result.toString());
      }
      globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                                 "Find/Replace",
                                 occurrences + " occurrences replaced.");
   }

   private final AceEditor editor_;
   private final Display display_;
   private final GlobalDisplay globalDisplay_;
   
   private static boolean defaultCaseSensitive_ = false;
   private static boolean defaultRegex_ = false;
}
