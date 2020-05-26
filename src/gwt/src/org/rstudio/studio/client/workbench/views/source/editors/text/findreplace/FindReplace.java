/*
 * FindReplace.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.findreplace;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.regex.Pattern.ReplaceOperation;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Search;

// TODO: For regex mode, stop using Ace's search code and do our own, in order
//    to avoid bugs with context directives (lookahead/lookbehind, ^, $)

public class FindReplace
{
   public interface Display
   {
      HasValue<String> getFindValue();
      void addFindKeyUpHandler(KeyUpHandler keyUpHandler);
      HasValue<String> getReplaceValue();
      HasValue<Boolean> getInSelection();
      HasValue<Boolean> getCaseSensitive();
      HasValue<Boolean> getWrapSearch();
      HasValue<Boolean> getWholeWord();
      HasValue<Boolean> getRegex();
      HasClickHandlers getReplaceAll();
      
      void activate(String searchText, 
                    boolean defaultForward, 
                    boolean inSelection);
      
      void focusFindField(boolean selectAll);
      Widget getUnderlyingWidget();
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
      
      HasValue<Boolean> wholeWord = display_.getWholeWord();
      wholeWord.setValue(defaultWholeWord_);
      wholeWord.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            defaultWholeWord_ = event.getValue();
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
      
      HasValue<Boolean> wrapSearch = display_.getWrapSearch();
      wrapSearch.setValue(defaultWrapSearch_);
      wrapSearch.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            defaultWrapSearch_ = event.getValue();
         }
      });
      
      HasValue<Boolean> inSelection = display_.getInSelection();
      inSelection.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {     
            if (event.getValue())
            {
               resetTargetSelection();
               display_.focusFindField(true);
            }
            else
               clearTargetSelection();
         }
      });
      
      
      addClickHandler(display.getReplaceAll(), new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            replaceAll();
         }
      });
      
      display_.addFindKeyUpHandler(new KeyUpHandler() {

         @Override
         public void onKeyUp(KeyUpEvent event)
         {
            int keycode = event.getNativeKeyCode();
            if (KeyboardHelper.isNavigationalKeycode(keycode) || 
                KeyboardHelper.isControlKeycode(keycode)) 
            {
               return;
            } 
            // perform incremental search
            find(defaultForward_ ? FindType.Forward : FindType.Reverse, true);
         }
         
      });
   }
   
   public void activate(String searchText, 
                        boolean defaultForward,
                        boolean inSelection)
   {
      defaultForward_ = defaultForward;
      incrementalSearchPosition_ = null;
      display_.activate(searchText, defaultForward, inSelection);
   }
   
   public void findNext()
   {
      find(FindType.Forward);
   }
   
   public void findPrevious()
   {
      find(FindType.Reverse);
   }
   
   public void replaceAndFind()
   {
      replace();
   }
   
   public void notifyEditorFocused()
   {
      display_.getInSelection().setValue(false, true);
   }
   
   public void notifyClosing()
   {
      clearTargetSelection();
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
      return find(findType, false);
   }
   
   public void selectAll()
   {
      // NOTE: 'null' range here implies whole document
      Range range = null;
      if (targetSelection_ != null)
         range = targetSelection_.getRange();
      
      boolean wholeWord = display_.getWholeWord().getValue();
      boolean caseSensitive = display_.getCaseSensitive().getValue();
      
      String searchString = display_.getFindValue().getValue();
      if (searchString.length() != 0)
      {
         editor_.selectAll(searchString, range, wholeWord, caseSensitive);
         editor_.focus();
      }
   }
   
   private boolean find(FindType findType, boolean incremental)
   {
      String searchString = display_.getFindValue().getValue();
      if (searchString.length() == 0)
      {
         // if this was an incremental search and the user has cleared
         // out the searching string then return to the original position
         if (incremental && (incrementalSearchPosition_ != null))
            editor_.setSelectionRange(Range.fromPoints(
                  incrementalSearchPosition_, incrementalSearchPosition_));
         
         return false;
      }
      
      boolean ignoreCase = !display_.getCaseSensitive().getValue();
      boolean regex = display_.getRegex().getValue();
      boolean wholeWord = display_.getWholeWord().getValue();
      boolean wrap = display_.getWrapSearch().getValue();
     
      // if we are searching in a selection then create a custom position
      // (based on the current selection) and range (based on the originally
      // saved selection range)
      Position position = null;
      Range range = null;
      if (display_.getInSelection().getValue() && (targetSelection_ != null))
      {       
         range = targetSelection_.getRange();
         
         if (findType == FindType.Forward)
         {
            Position selectionEnd = editor_.getSelectionEnd();
            if (selectionEnd.isBefore(range.getEnd()))
               position = selectionEnd;
         }
         else
         {
            Position selectionStart = editor_.getSelectionStart();
            if (selectionStart.isAfter(range.getStart()))
               position = selectionStart;
         }
      }
      
      // if this is an incremental search and we don't have a previous
      // incremental start position then set it, otherwise clear it
      if (incremental)
      {
         if (incrementalSearchPosition_ == null)
         {
            if (position != null)
               incrementalSearchPosition_ = position;
            else
               incrementalSearchPosition_ = defaultForward_ ? 
                                          editor_.getSelectionStart() :
                                          editor_.getSelectionEnd();
         }
        
         // incremental searches always continue searching from the
         // original search position
         position = incrementalSearchPosition_;
      }
      else
      {
         incrementalSearchPosition_ = null;
      }
      
      // do the search
      Search search = Search.create(searchString,
                                    findType != FindType.Forward,
                                    wrap,
                                    !ignoreCase,
                                    wholeWord,
                                    position,
                                    range,
                                    regex);
   
      try
      {
         Range resultRange = search.find(editor_.getSession());
         if (resultRange == null)
         {
            if (!incremental)
            {
               globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                                          errorCaption_,
                                          "No more occurrences.");
            }
            else
            {
               editor_.collapseSelection(true);
            }
            
            return false;
         }
         else
         {
            editor_.revealRange(resultRange, false);
            return true;
         }
      }
      catch(Throwable e)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_ERROR,
               errorCaption_,
               "Invalid search term.");
         
         return false;
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
         
         if (targetSelection_ != null)
            targetSelection_.syncMarker();
      }

      find(defaultForward_ ? FindType.Forward : FindType.Reverse);
   }

   private Pattern createPattern()
   {
      boolean caseSensitive = display_.getCaseSensitive().getValue();
      boolean regex = display_.getRegex().getValue();
      String find = display_.getFindValue().getValue();
      boolean wholeWord = display_.getWholeWord().getValue();

      String flags = caseSensitive ? "gm" : "igm";
      String query = regex ? find : Pattern.escape(find);
      if (wholeWord)
         query = "\\b" + query + "\\b";
      
      return Pattern.create(query, flags);
   }

   private void replaceAll()
   {
      String code = null; 
      if (targetSelection_ != null)
      {
         Range range = targetSelection_.getRange();
         code = editor_.getCode(range.getStart(), range.getEnd());
      }
      else
      {
         code = editor_.getCode();
      }

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
            
            // If the data matched is an empty string (which can happen for
            // regexps that don't consume characters such as ^ or $), then we
            // didn't advance the state of the underlying RegExp object, and
            // we'll loop forever (see case 4191). Bail out.
            if (m.getValue().length() == 0)
            {
               break;
            }
         }
         result.append(code, pos, code.length());

         String newCode = result.toString();
         
         // either replace all or replace just the target range
         if (targetSelection_ != null)
         {
            // restore and then replace the selection
            editor_.setSelectionRange(targetSelection_.getRange());
            editor_.replaceSelection(newCode, false);
            
            // reset the target selection
            resetTargetSelection();
         }
         else
         {
            editor_.replaceCode(newCode);
         }      
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
   private boolean defaultForward_ = true;
   private Position incrementalSearchPosition_ = null;
   
   private class TargetSelectionTracker
   {
      public TargetSelectionTracker()
      { 
         // expand the selection to include lines (ace will effectively do
         // this for a range based search)
         editor_.fitSelectionToLines(true);
         Position start = editor_.getSelectionStart();
         Position end = editor_.getSelectionEnd();
         anchoredSelection_ = editor_.createAnchoredSelection(
               display_.getUnderlyingWidget(),
               start,
               end);
         // collapse the cursor to the beginning or end
         editor_.collapseSelection(defaultForward_);
         
         // sync marker
         syncMarker();
      }
      
      public Range getRange()
      {
         return anchoredSelection_.getRange();
      }
      
      public void syncMarker()
      {
         clear();
         
         markerId_ = editor_.getSession().addMarker(getRange(),
                                                   "ace_find_line",
                                                   "background",
                                                   false);
      }
      
      public void clear()
      {
         if (anchoredSelection_ != null)
            anchoredSelection_.detach();
         
         if (markerId_ != null)
            editor_.getSession().removeMarker(markerId_);
      }
      
      private Integer markerId_ = null;
      private AnchoredSelection anchoredSelection_ = null;
      
   }
   private TargetSelectionTracker targetSelection_ = null;
   
   private void clearTargetSelection()
   {
      if (targetSelection_ != null)
         targetSelection_.clear();
      targetSelection_ = null;
   }
   
   private void resetTargetSelection()
   {
      clearTargetSelection();
      targetSelection_ = new TargetSelectionTracker();
   }
   
   private static boolean defaultCaseSensitive_ = false;
   private static boolean defaultWrapSearch_ = true;
   private static boolean defaultRegex_ = false;
   private static boolean defaultWholeWord_ = false;
}
