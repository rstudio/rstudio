/*
 * FindReplace.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.regex.Pattern.ReplaceOperation;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Search;

// TODO: For regex mode, stop using Ace's search code and do our own, in order
//    to avoid bugs with context directives (lookahead/lookbehind, ^, $)

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
                      GlobalDisplay globalDisplay,
                      boolean showingReplace)
   {
      editor_ = editor;
      display_ = display;
      globalDisplay_ = globalDisplay;
      errorCaption_ = showingReplace ? "Find/Replace" : "Find";
      
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
                                    true,
                                    false,
                                    regex);

      Range range = search.find(editor_.getSession());

      if (range == null)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                                    errorCaption_,
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

      Pattern pattern = createPattern();
      String line = editor_.getCurrentLine();
      Match m = pattern.match(line,
                              editor_.getSelectionStart().getColumn());
      if (m != null
          && m.getIndex() == editor_.getSelectionStart().getColumn()
          && m.getValue().length() == editor_.getSelectionValue().length())
      {
         String replacement = display_.getReplaceValue().getValue();
         editor_.replaceSelection(display_.getRegex().getValue()
                                  ? substitute(m, replacement, line)
                                  : replacement);
      }

      find(FindType.Forward);
   }

   private Pattern createPattern()
   {
      boolean caseSensitive = display_.getCaseSensitive().getValue();
      boolean regex = display_.getRegex().getValue();
      String find = display_.getFindValue().getValue();

      String flags = caseSensitive ? "gm" : "igm";
      String query = regex ? find : Pattern.escape(find);
      return Pattern.create(query, flags);
   }

   private void replaceAll()
   {
      String code = editor_.getCode();
      boolean regex = display_.getRegex().getValue();
      String find = display_.getFindValue().getValue();
      String repl = display_.getReplaceValue().getValue();

      int occurrences = 0;
      if (find.length() > 0)
      {
         Pattern pattern = createPattern();
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
            if (regex)
               result.append(substitute(m, repl, code));
            else
               result.append(repl);

            // Point to the end of this match
            pos = index + m.getValue().length();
         }
         result.append(code, pos, code.length());

         editor_.replaceCode(result.toString());
      }
      globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                                 errorCaption_,
                                 occurrences + " occurrences replaced.");
   }

   private String substitute(final Match match,
                             String replacement,
                             final String data)
   {
      Pattern pattern = Pattern.create("[$\\\\]([1-9][0-9]?|.)");
      return pattern.replaceAll(replacement, new ReplaceOperation()
      {
         public String replace(Match m)
         {
            char p = m.getValue().charAt(0);
            char c = m.getValue().charAt(1);
            switch (p)
            {
               case '\\':
                  switch (c)
                  {
                     case '\\':
                        return "\\";
                     case 'n':
                        return "\n";
                     case 'r':
                        return "\r";
                     case 't':
                        return "\t";
                  }
                  break;
               case '$':
                  switch (c)
                  {
                     case '$':
                        return "$";
                     case '&':
                        return match.getValue();
                     case '`':
                        String prefix = data.substring(0, match.getIndex());
                        int lastLF = prefix.lastIndexOf("\n");
                        if (lastLF > 0)
                           prefix = prefix.substring(lastLF + 1);
                        return prefix;
                     case '\'':
                        String suffix = data.substring(match.getIndex() + match.getValue().length());
                        int firstBreak = suffix.indexOf("\r");
                        if (firstBreak < 0)
                           firstBreak = suffix.indexOf("\n");
                        if (firstBreak >= 0)
                           suffix = suffix.substring(0, firstBreak);
                        return suffix;
                  }
                  break;
            }

            switch (c)
            {
               case '1':
               case '2':
               case '3':
               case '4':
               case '5':
               case '6':
               case '7':
               case '8':
               case '9':
                  int index = Integer.parseInt(m.getGroup(1));
                  return StringUtil.notNull(match.getGroup(index));
            }
            return m.getValue();
         }
      });
   }

   private final AceEditor editor_;
   private final Display display_;
   private final GlobalDisplay globalDisplay_;
   private final String errorCaption_;
   
   private static boolean defaultCaseSensitive_ = false;
   private static boolean defaultRegex_ = false;
}
