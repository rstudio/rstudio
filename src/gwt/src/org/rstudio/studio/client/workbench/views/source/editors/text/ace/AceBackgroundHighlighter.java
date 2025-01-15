/*
 * AceBackgroundHighlighter.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.JsVector;
import org.rstudio.core.client.JsVectorInteger;
import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class AceBackgroundHighlighter
      implements EditorModeChangedEvent.Handler,
                 DocumentChangedEvent.Handler
{
   private static class HighlightPattern
   {
      public HighlightPattern(String begin, String end)
      {
         this(
               Pattern.create(begin, ""),
               Pattern.create(end, ""),
               -1);
      }
      
      public HighlightPattern(Pattern begin,
                              Pattern end,
                              int matchLength)
      {
         begin_ = begin;
         end_ = end;
         matchLength_ = matchLength;
      }
      
      public HighlightPattern applyBeginPattern(String line)
      {
         Match match = begin_.match(line, 0);
         if (match == null)
            return null;
         
         int matchLength = match.getGroupOrDefault(1, "").length();
         return new HighlightPattern(begin_, end_, matchLength);
      }
      
      public boolean applyEndPattern(String line)
      {
         Match match = end_.match(line, 0);
         if (match == null)
            return false;
         
         int matchLength = match.getGroupOrDefault(1, "").length();
         return matchLength_ == matchLength;
      }
      
      private Pattern begin_;
      private Pattern end_;
      private int matchLength_;
   }
  
   private class Worker
   {
      public Worker()
      {
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               work();
            }
         };
      }
      
      private void work()
      {
         if (!enabled_)
            return;
         
         // determine range to update
         int n = editor_.getRowCount();
         int startRow = row_;
         int endRow = Math.min(startRow + CHUNK_SIZE, editor_.getRowCount());
         
         activeHighlightPattern_ = findActiveHighlightPattern(startRow);
         
         // first, update local background state for each row
         for (int row = startRow; row < endRow; row++)
         {
            // determine what state this row is in
            int state = computeState(row);
            
            // if there's been no change, bail
            boolean isConsistentState =
                  rowStates_.isSet(row) &&
                  rowPatterns_.isSet(row) &&
                  (rowStates_.get(row) == state) &&
                  (rowPatterns_.get(row) == activeHighlightPattern_);
            
            if (isConsistentState)
               break;
            
            // update state for this row
            rowStates_.set(row, state);
            rowPatterns_.set(row, activeHighlightPattern_);
         }
         
         // then, notify Ace and perform actual rendering of markers
         for (int row = startRow; row < endRow; row++)
         {
            int state = rowStates_.get(row);
            int marker = markerIds_.get(row, 0);
            
            // bail early if no action is necessary
            boolean isConsistentState =
                  (state == STATE_TEXT && marker == 0) ||
                  (state != STATE_TEXT && marker != 0);
            
            if (isConsistentState)
               continue;
            
            // clear a pre-existing marker if necessary
            if (marker != 0)
            {
               session_.removeMarker(marker);
               markerIds_.set(row, 0);
            }
            
            // if this is a non-text state, then draw a marker
            if (state != STATE_TEXT)
            {
               int markerId = session_.addMarker(
                     Range.create(row, 0, row, Integer.MAX_VALUE),
                     MARKER_CLASS,
                     MARKER_TYPE,
                     false);
               
               markerIds_.set(row, markerId);
            }
         }
         
         // update worker state and reschedule if there's
         // more work to be done
         row_ = endRow;
         if (endRow != n)
            timer_.schedule(DELAY_MS);
      }
      
      public void start(int row)
      {
         row_ = Math.min(row, row_);
         timer_.schedule(0);
      }
      
      private final Timer timer_;
      private int row_;
      
      private static final int DELAY_MS = 5;
      private static final int CHUNK_SIZE = 200;
   }
   
   public AceBackgroundHighlighter(AceEditor editor)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      editor_ = editor;
      session_ = editor.getSession();
      
      highlightPatterns_ = new ArrayList<>();
      editor.addEditorModeChangedHandler(this);
      
      int n = editor.getRowCount();
      rowStates_ = JavaScriptObject.createArray(n).cast();
      rowPatterns_ = JavaScriptObject.createArray(n).cast();
      markerIds_ = JavaScriptObject.createArray(n).cast();
      worker_ = new Worker();
      
      enabled_ = prefs_.highlightCodeChunks().getGlobalValue();
      prefs_.highlightCodeChunks().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            boolean value = event.getValue();
            if (value)
            {
               enabled_ = true;
               synchronizeFrom(0);
            }
            else
            {
               enabled_ = false;
               clearMarkers();
            }
         }
      });
      
      activeModeId_ = editor.getSession().getMode().getId();
      refreshHighlighters();
   }
   
   @Inject
   private void initialize(UserPrefs prefs)
   {
      prefs_ = prefs;
   }
   
   // Handlers ----
   @Override
   public void onEditorModeChanged(EditorModeChangedEvent event)
   {
      // nothing to do if mode did not change
      if (event.getMode() == activeModeId_)
         return;
      
      activeModeId_ = event.getMode();
      clearMarkers();
      clearRowState();
      refreshHighlighters();
      
      if (documentChangedHandler_ != null)
      {
         documentChangedHandler_.removeHandler();
         documentChangedHandler_ = null;
      }
      
      if (!highlightPatterns_.isEmpty())
      {
         documentChangedHandler_ = editor_.addDocumentChangedHandler(this);
         synchronizeFrom(0);
      }
   }
   
   @Override
   public void onDocumentChanged(DocumentChangedEvent event)
   {
      AceDocumentChangeEventNative nativeEvent = event.getEvent();
      
      String action = nativeEvent.getAction();
      
      Range range = nativeEvent.getRange();
      int startRow = range.getStart().getRow();
      int endRow = range.getEnd().getRow();
      
      // NOTE: this will need to change with the next version of Ace,
      // as the layout of document changed events will have changed there
      rowStates_.unset(startRow);
      rowPatterns_.unset(startRow);
      if (action.startsWith("insert"))
      {
         int newlineCount = endRow - startRow;
         rowStates_.insert(startRow, JsVectorInteger.ofLength(newlineCount));
         rowPatterns_.insert(startRow, JsVector.<HighlightPattern>ofLength(newlineCount));
      }
      else if (action.startsWith("remove"))
      {
         int newlineCount = endRow - startRow;
         if (newlineCount > 0)
         {
            rowStates_.remove(startRow, newlineCount);
            rowPatterns_.remove(startRow,newlineCount);
         }
      }
      
      synchronizeFrom(startRow);
   }
   
   // Private Methods ----
   HighlightPattern selectBeginPattern(String line)
   {
      for (HighlightPattern pattern : highlightPatterns_)
      {
         HighlightPattern matchedPattern = pattern.applyBeginPattern(line);
         if (matchedPattern != null)
         {
            return matchedPattern;
         }
      }
      
      return null;
   }
   
   private HighlightPattern findActiveHighlightPattern(int startRow)
   {
      for (int row = startRow; row >= 0; row--)
      {
         // check for a cached highlight pattern
         HighlightPattern pattern = rowPatterns_.get(row);
         if (pattern != null)
            return pattern;
         
         // no pattern available; re-compute based on current state
         int state = rowStates_.get(row);
         switch (state)
         {
         case STATE_TEXT:
            break;
         case STATE_CHUNK_START:
            String line = editor_.getLine(row);
            return selectBeginPattern(line);
         case STATE_CHUNK_BODY:
         case STATE_CHUNK_END:
         default:
            continue;
         }
      }
      
      return null;
   }
   
   private int computeState(int row)
   {
      String line = editor_.getLine(row);
      int state = (row > 0)
            ? rowStates_.get(row - 1, STATE_TEXT)
            : STATE_TEXT;
            
      switch (state)
      {
      case STATE_TEXT:
      case STATE_CHUNK_END:
      {
         HighlightPattern pattern = selectBeginPattern(line);
         if (pattern != null)
         {
            activeHighlightPattern_ = pattern;
            return STATE_CHUNK_START;
         }
         
         return STATE_TEXT;
      }
      case STATE_CHUNK_START:
      case STATE_CHUNK_BODY:
      {
         assert activeHighlightPattern_ != null
               : "Unexpected null highlight pattern";
         
         if (activeHighlightPattern_.applyEndPattern(line))
         {
            activeHighlightPattern_ = null;
            return STATE_CHUNK_END;
         }
         
         return STATE_CHUNK_BODY;
      }
      }
      
      // shouldn't be reached
      return STATE_TEXT;
   }
   
   private void synchronizeFrom(int startRow)
   {
      if (!enabled_)
         return;
      
      // if this row has no state, then we need to look
      // back until we find a row with cached state
      while (startRow > 0 && !rowStates_.isSet(startRow - 1))
         startRow--;
      
      // start the worker that will update ace
      worker_.start(startRow);
   }
   
   private void refreshHighlighters()
   {
      highlightPatterns_.clear();
      
      String modeId = editor_.getModeId();
      if (StringUtil.isNullOrEmpty(modeId))
         return;
      
      if (HIGHLIGHT_PATTERN_REGISTRY.containsKey(modeId))
      {
         highlightPatterns_.addAll(HIGHLIGHT_PATTERN_REGISTRY.get(modeId));
      }
   }
   
   private void clearRowState()
   {
      rowStates_.fill(0);
      rowPatterns_.fill((HighlightPattern) null);
   }
   
   private void clearMarkers()
   {
      for (int i = 0, n = markerIds_.length(); i < n; i++)
      {
         int markerId = markerIds_.get(i);
         if (markerId != 0)
            session_.removeMarker(markerId);
      }
      
      markerIds_.fill(0);
   }
   
   private static List<HighlightPattern> cStyleHighlightPatterns()
   {
      return ListUtil.create(
            new HighlightPattern(
                  "^\\s*[/][*]{3,}\\s*[Rr]\\s*$",
                  "^\\s*[*]+[/]")
      );
   }
   
   private static List<HighlightPattern> htmlStyleHighlightPatterns()
   {
      return ListUtil.create(
            new HighlightPattern(
                  "^<!--\\s*begin[.]rcode\\s*(?:.*)",
                  "^\\s*end[.]rcode\\s*-->")
      );
   }
   
   private static List<HighlightPattern> sweaveHighlightPatterns()
   {
      return ListUtil.create(
            new HighlightPattern(
                  "^\\s*<<.*>>=\\s*$",
                  "^\\s*@\\s*$")
      );
   }
   
   private static final List<HighlightPattern> rMarkdownHighlightPatterns()
   {
      return ListUtil.create(
            
            // code chunks
            new HighlightPattern(
                  "^(?:[ ]{4})?(`{3,})\\s*\\{.*\\}\\s*$",
                  "^(?:[ ]{4})?(`{3,})\\s*$"),
            
            // latex blocks
            new HighlightPattern(
                  "^[$][$]\\s*$",
                  "^[$][$].*$")
            
      );
            
   }
   
   private final AceEditor editor_;
   private final EditSession session_;
   private final List<HighlightPattern> highlightPatterns_;
   
   private HighlightPattern activeHighlightPattern_;
   private String activeModeId_;
   private HandlerRegistration documentChangedHandler_;
   private boolean enabled_;
   
   private final JsVectorInteger rowStates_;
   private final JsVectorInteger markerIds_;
   private final JsVector<HighlightPattern> rowPatterns_;
   
   private final Worker worker_;
   
   private static final String MARKER_CLASS = "ace_foreign_line background_highlight";
   private static final String MARKER_TYPE = "fullLine";
   private static final Map<String, List<HighlightPattern>> HIGHLIGHT_PATTERN_REGISTRY;
   
   // Injected ----
   private UserPrefs prefs_;
   
   // Static Members ----
   
   static {
      HIGHLIGHT_PATTERN_REGISTRY = new HashMap<>();
      HIGHLIGHT_PATTERN_REGISTRY.put("mode/rmarkdown", rMarkdownHighlightPatterns());
      HIGHLIGHT_PATTERN_REGISTRY.put("mode/c_cpp", cStyleHighlightPatterns());
      HIGHLIGHT_PATTERN_REGISTRY.put("mode/sweave", sweaveHighlightPatterns());
      HIGHLIGHT_PATTERN_REGISTRY.put("mode/rhtml", htmlStyleHighlightPatterns());
   }
   
   private static final int STATE_TEXT         = 1;
   private static final int STATE_CHUNK_START  = 2;
   private static final int STATE_CHUNK_BODY   = 3;
   private static final int STATE_CHUNK_END    = 4;
}
