/*
 * AceBackgroundHighlighter.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.JsVectorInteger;
import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Timer;

public class AceBackgroundHighlighter
      implements EditorModeChangedEvent.Handler,
                 DocumentChangedEvent.Handler,
                 AttachEvent.Handler
{
   private static class HighlightPattern
   {
      public HighlightPattern(String begin, String end)
      {
         this.begin = Pattern.create(begin, "");
         this.end = Pattern.create(end, "");
      }
      
      public Pattern begin;
      public Pattern end;
   }
   
   private class UpdateTimer extends Timer
   {
      @Override
      public void run()
      {
         int n = editor_.getRowCount();
         int startRow = row_;
         int endRow = Math.min(startRow + 200, editor_.getRowCount());
         
         for (int row = startRow; row < endRow; row++)
         {
            int state = rowStates_.get(row);
            
            // clear a pre-existing marker if necessary
            int marker = markerIds_.get(row);
            if (marker != 0)
            {
               session_.removeMarker(marker);
               markerIds_.set(row, 0);
            }
            
            // don't show background highlighting if this
            // chunk lies within a fold
            AceFold fold = session_.getFoldAt(row, 0);
            if (fold != null)
               continue;
            
            // if this is a non-text state, then draw a marker
            if (state != STATE_TEXT)
            {
               String className = (state == STATE_CHUNK_START)
                     ? MARKER_CLASS + " rstudio_chunk_start"
                     : MARKER_CLASS;
               
               int markerId = session_.addMarker(
                     Range.create(row, 0, row, Integer.MAX_VALUE),
                     className,
                     MARKER_TYPE,
                     false);
               
               markerIds_.set(row, markerId);
            }
         }
         
         // update worker state and reschedule if there's
         // more work to be done
         row_ = endRow;
         if (endRow != n)
            schedule(DELAY_MS);
      }
      
      public void start(int row)
      {
         row_ = row;
         run();
      }
      
      private int row_;
      private static final int DELAY_MS = 20;
   }
   
   public AceBackgroundHighlighter(AceEditor editor)
   {
      editor_ = editor;
      session_ = editor.getSession();
      
      highlightPatterns_ = new ArrayList<HighlightPattern>();
      handlers_ = new HandlerRegistrations(
            editor.addEditorModeChangedHandler(this),
            editor.addDocumentChangedHandler(this),
            editor.addAttachHandler(this));
      
      int n = editor.getRowCount();
      rowStates_ = JsVectorInteger.createArray(n).cast();
      markerIds_ = JsVectorInteger.createArray(n).cast();
      
      updateTimer_ = new UpdateTimer();
      
      refreshHighlighters();
   }
   
   // Handlers ----
   @Override
   public void onEditorModeChanged(EditorModeChangedEvent event)
   {
      clearMarkers();
      refreshHighlighters();
      synchronizeFrom(0);
   }
   
   @Override
   public void onDocumentChanged(DocumentChangedEvent event)
   {
      AceDocumentChangeEventNative nativeEvent = event.getEvent();
      
      String action = nativeEvent.getAction();
      String text = nativeEvent.getText();
      
      Range range = nativeEvent.getRange();
      int startRow = range.getStart().getRow();
      int endRow = range.getEnd().getRow();
      
      // NOTE: this will need to change with the next version of Ace,
      // as the layout of document changed events will have changed there
      rowStates_.set(startRow, STATE_UNKNOWN);
      if (action.startsWith("insert"))
      {
         int newlineCount = endRow - startRow;
         for (int i = 0; i < newlineCount; i++)
            rowStates_.insert(startRow, STATE_UNKNOWN);
      }
      else if (action.startsWith("remove"))
      {
         int newlineCount = endRow - startRow;
         if (newlineCount > 0)
            rowStates_.remove(startRow, newlineCount);
      }
      
      synchronizeFrom(startRow);
   }
   
   @Override
   public void onAttachOrDetach(AttachEvent event)
   {
      if (!event.isAttached())
      {
         handlers_.removeHandler();
      }
   }
   
   // Private Methods ----
   HighlightPattern selectBeginPattern(String line)
   {
      for (HighlightPattern pattern : highlightPatterns_)
      {
         Pattern begin = pattern.begin;
         Match match = begin.match(line, 0);
         if (match != null)
            return pattern;
      }
      
      return null;
   }
   
   private HighlightPattern findActiveHighlightPattern(int startRow)
   {
      for (int row = startRow; row >= 0; row--)
      {
         int state = rowStates_.get(row);
         switch (state)
         {
         case STATE_TEXT:
            break;
            
         case STATE_CHUNK_BODY:
         case STATE_CHUNK_END:
         case STATE_UNKNOWN:
            continue;
            
         case STATE_CHUNK_START:
            String line = editor_.getLine(row);
            return selectBeginPattern(line);
         }
      }
      
      return null;
   }
   
   private int computeState(int row)
   {
      String line = editor_.getLine(row);
      int state = (row > 0)
            ? rowStates_.get(row - 1)
            : STATE_TEXT;
            
      switch (state)
      {
      case STATE_UNKNOWN:
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
         
         Match match = activeHighlightPattern_.end.match(line, 0);
         if (match != null)
         {
            activeHighlightPattern_ = null;
            return STATE_CHUNK_END;
         }
         
         return STATE_CHUNK_BODY;
      }
      default:
         return STATE_UNKNOWN;
      }
   }
   
   private void synchronizeFrom(int startRow)
   {
      // if this row has no state, then we need to look
      // back until we find a row with cached state
      while (startRow > 0 && rowStates_.get(startRow - 1) == STATE_UNKNOWN)
         startRow--;
      
      // figure out what highlighter is active for this particular row
      activeHighlightPattern_ = findActiveHighlightPattern(startRow);
      
      // iterate over rows in the document and update highlight state
      int endRow = editor_.getRowCount();
      for (int row = startRow; row < endRow; row++)
      {
         // determine what state this row is in
         int state = computeState(row);
         
         // if there is no change in state, then exit early
         if (state == rowStates_.get(row))
            break;
         
         // update state for this row
         rowStates_.set(row, state);
      }
      
      // start the worker that will update ace
      updateTimer_.start(startRow);
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
                  "^\\s*[/][*}{3,}\\s*[Rr]\\s*$",
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
                  "<<(.*?)>>",
                  "^\\s*@\\s*$")
            );
   }
   
   private static final List<HighlightPattern> rMarkdownHighlightPatterns()
   {
      return ListUtil.create(
            
            // code chunks
            new HighlightPattern(
                  "^(?:[ ]{4})?`{3,}\\s*\\{(?:.*)\\}\\s*$",
                  "^(?:[ ]{4})?`{3,}\\s*$"),
            
            // latex blocks
            new HighlightPattern(
                  "^[$][$]\\s*$",
                  "^[$][$]\\s*$")
            
            );
            
   }
   
   private final AceEditor editor_;
   private final EditSession session_;
   private HighlightPattern activeHighlightPattern_;
   private final List<HighlightPattern> highlightPatterns_;
   private final HandlerRegistrations handlers_;
   
   private final JsVectorInteger rowStates_;
   private final JsVectorInteger markerIds_;
   private final UpdateTimer updateTimer_;
   
   private static final String MARKER_CLASS = "ace_foreign_line background_highlight";
   private static final String MARKER_TYPE = "fullLine";
   private static final Map<String, List<HighlightPattern>> HIGHLIGHT_PATTERN_REGISTRY;
   
   static {
      HIGHLIGHT_PATTERN_REGISTRY = new HashMap<String, List<HighlightPattern>>();
      HIGHLIGHT_PATTERN_REGISTRY.put("mode/rmarkdown", rMarkdownHighlightPatterns());
      HIGHLIGHT_PATTERN_REGISTRY.put("mode/c_cpp", cStyleHighlightPatterns());
      HIGHLIGHT_PATTERN_REGISTRY.put("mode/sweave", sweaveHighlightPatterns());
      HIGHLIGHT_PATTERN_REGISTRY.put("mode/rhtml", htmlStyleHighlightPatterns());
   }
   
   private static final int STATE_UNKNOWN      = 0;
   private static final int STATE_TEXT         = 1;
   private static final int STATE_CHUNK_START  = 2;
   private static final int STATE_CHUNK_BODY   = 3;
   private static final int STATE_CHUNK_END    = 4;
}
