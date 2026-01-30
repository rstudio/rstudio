/*
 * TextEditingTargetAssistantHelper.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.MathUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.diff.JsDiff;
import org.rstudio.core.client.diff.JsDiff.Delta;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.EventProperty;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.assistant.Assistant;
import org.rstudio.studio.client.workbench.assistant.model.AssistantConstants;
import org.rstudio.studio.client.workbench.assistant.model.AssistantEvent;
import org.rstudio.studio.client.workbench.assistant.model.AssistantEvent.AssistantEventType;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantGenerateCompletionsResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantNextEditSuggestionsResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantNextEditSuggestionsResultEntry;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantCompletion;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantError;
import org.rstudio.studio.client.workbench.assistant.server.AssistantServerOperations;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.InsertionBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceDocumentChangeEventNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

import jsinterop.base.Any;
import jsinterop.base.Js;
import jsinterop.base.JsArrayLike;

public class TextEditingTargetAssistantHelper
{
   interface AssistantCommandBinder extends CommandBinder<Commands, TextEditingTargetAssistantHelper>
   {
   }

   // A wrapper class for Assistant Completions, which is used to track partially-accepted completions.
   private static class Completion
   {
      public Completion(AssistantCompletion originalCompletion)
      {
         // Copilot includes trailing '```' for some reason in some cases,
         // remove those if we're inserting in an R document.
         this.insertText = postProcessCompletion(originalCompletion.insertText);
         this.displayText = this.insertText;

         this.startLine = originalCompletion.range.start.line;
         this.startCharacter = originalCompletion.range.start.character;
         this.endLine = originalCompletion.range.end.line;
         this.endCharacter = originalCompletion.range.end.character;

         this.originalCompletion = originalCompletion;
         this.partialAcceptedLength = 0;
      }

      public String insertText;
      public String displayText;
      public int startLine;
      public int startCharacter;
      public int endLine;
      public int endCharacter;
      public AssistantCompletion originalCompletion;
      public int partialAcceptedLength;
   }

   /**
    * Enum representing the type of edit operation.
    */
   private enum EditType
   {
      ADDITION,
      DELETION
   }

   /**
    * Enum representing the type of next edit suggestion display.
    */
   private enum SuggestionType
   {
      NONE,
      GHOST_TEXT,
      DELETION,
      INSERTION,
      REPLACEMENT,
      MIXED,
      DIFF
   }

   /**
    * Represents a single edit operation with its type, range, and text.
    */
   private static class EditDelta
   {
      public EditDelta(EditType type, Range range, String text)
      {
         this.type = type;
         this.range = range;
         this.text = text;
      }

      public final EditType type;
      public final Range range;
      public final String text;  // The text being added/deleted; null for deletions
   }

   /**
    * Helper class for computing and storing edit deltas between original and replacement text.
    * Computes the actual ranges of text that would be added or deleted.
    */
   private static class EditDeltas
   {
      public EditDeltas(String originalText, String replacementText)
      {
         JsArrayLike<Delta> deltas = JsDiff.diffChars(originalText, replacementText);

         // Track position in the original text (for deletions)
         StringBuilder originalPos = new StringBuilder();

         for (int i = 0, n = deltas.getLength(); i < n; i++)
         {
            Delta delta = deltas.getAt(i);

            if (delta.added)
            {
               hasAdditions_ = true;

               // Compute the range in the original text where this addition occurs
               // (it's inserted at the current position, so start == end)
               String prefix = originalPos.toString();
               int line = StringUtil.countMatches(prefix, '\n');
               int character = prefix.length() - prefix.lastIndexOf('\n') - 1;
               Range range = Range.create(line, character, line, character);

               deltas_.add(new EditDelta(EditType.ADDITION, range, delta.value));
               // Additions don't consume original text, so don't advance originalPos
            }
            else if (delta.removed)
            {
               hasDeletions_ = true;

               // Compute the range in the original text for this deletion
               String prefix = originalPos.toString();
               String postfix = prefix + delta.value;

               int startLine = StringUtil.countMatches(prefix, '\n');
               int startChar = prefix.length() - prefix.lastIndexOf('\n') - 1;
               int endLine = StringUtil.countMatches(postfix, '\n');
               int endChar = postfix.length() - postfix.lastIndexOf('\n') - 1;

               Range range = Range.create(startLine, startChar, endLine, endChar);
               deltas_.add(new EditDelta(EditType.DELETION, range, null));

               // Advance position in original text
               originalPos.append(delta.value);
            }
            else
            {
               // Unchanged text - advance position in original text
               originalPos.append(delta.value);
            }
         }
      }

      public boolean isDeletionOnly()
      {
         return hasDeletions_ && !hasAdditions_;
      }

      public boolean isInsertionOnly()
      {
         return hasAdditions_ && !hasDeletions_;
      }

      /**
       * Returns true if this consists only of single-line insertions:
       * - Only additions (no deletions)
       * - Each insertion text doesn't contain newlines
       */
      public boolean isSingleLineInsertions()
      {
         if (!isInsertionOnly())
            return false;

         // Check that all additions are single-line
         for (EditDelta delta : deltas_)
         {
            if (delta.type == EditType.ADDITION)
            {
               if (delta.text == null || delta.text.contains("\n"))
                  return false;
            }
         }

         return true;
      }

      /**
       * Returns true if this is a single-line replacement:
       * - Exactly one deletion and one insertion
       * - Both are single-line (no newlines in the text)
       */
      public boolean isSingleLineReplacement()
      {
         if (!hasAdditions_ || !hasDeletions_)
            return false;

         EditDelta deletion = null;
         EditDelta addition = null;

         for (EditDelta delta : deltas_)
         {
            if (delta.type == EditType.DELETION)
            {
               if (deletion != null)
                  return false; // More than one deletion
               deletion = delta;
            }
            else if (delta.type == EditType.ADDITION)
            {
               if (addition != null)
                  return false; // More than one addition
               addition = delta;
            }
         }

         if (deletion == null || addition == null)
            return false;

         // Check that deletion is single-line
         if (deletion.range.getStart().getRow() != deletion.range.getEnd().getRow())
            return false;

         // Check that addition text doesn't contain newlines
         if (addition.text != null && addition.text.contains("\n"))
            return false;

         return true;
      }

      /**
       * Returns the single deletion delta, or null if there isn't exactly one.
       */
      public EditDelta getSingleDeletion()
      {
         EditDelta deletion = null;
         for (EditDelta delta : deltas_)
         {
            if (delta.type == EditType.DELETION)
            {
               if (deletion != null)
                  return null; // More than one
               deletion = delta;
            }
         }
         return deletion;
      }

      /**
       * Returns the single addition delta, or null if there isn't exactly one.
       */
      public EditDelta getSingleAddition()
      {
         EditDelta addition = null;
         for (EditDelta delta : deltas_)
         {
            if (delta.type == EditType.ADDITION)
            {
               if (addition != null)
                  return null; // More than one
               addition = delta;
            }
         }
         return addition;
      }

      /**
       * Returns all addition deltas.
       */
      public List<EditDelta> getAdditions()
      {
         List<EditDelta> additions = new ArrayList<>();
         for (EditDelta delta : deltas_)
         {
            if (delta.type == EditType.ADDITION)
               additions.add(delta);
         }
         return additions;
      }

      public List<EditDelta> getDeltas()
      {
         return deltas_;
      }

      /**
       * Returns true if all deltas are single-line (no delta spans multiple lines
       * and no insertion text contains newlines).
       */
      public boolean isSingleLineDeltas()
      {
         for (EditDelta delta : deltas_)
         {
            if (delta.type == EditType.DELETION)
            {
               // Check if deletion spans multiple lines
               if (delta.range.getStart().getRow() != delta.range.getEnd().getRow())
                  return false;
            }
            else if (delta.type == EditType.ADDITION)
            {
               // Check if insertion text contains newlines
               if (delta.text != null && delta.text.contains("\n"))
                  return false;
            }
         }
         return true;
      }

      private boolean hasAdditions_ = false;
      private boolean hasDeletions_ = false;
      private List<EditDelta> deltas_ = new ArrayList<>();
   }

   /**
    * Shows an inline edit suggestion diff view for the given completion.
    */
   /**
    * Shows an edit suggestion in the editor.
    * This is the main entry point for displaying edit suggestions from the API.
    * The suggestion is always shown immediately (autoshow behavior).
    * 
    * @param completion The completion containing the range and replacement text
    */
   public void showEditSuggestion(AssistantCompletion completion)
   {
      showEditSuggestionImpl(completion, true, false);
   }
   
   /**
    * Internal implementation for showing edit suggestions.
    * 
    * @param completion The completion containing the range and replacement text
    * @param autoshow If true, render the suggestion immediately; if false, show gutter icon only
    * @param notifyServer Whether to notify the server that the completion was shown
    *                     (true for Assistant/Copilot flow, false for API callers)
    */
   private void showEditSuggestionImpl(AssistantCompletion completion, boolean autoshow, boolean notifyServer)
   {
      resetSuggestion();
      
      // The completion data gets modified when doing partial (word-by-word)
      // completions, so we need to use a copy and preserve the original
      // (which we need to send back to the server as-is in some language-server methods).
      AssistantCompletion normalized = normalizeSuggestion(completion);
      
      // Check if this is a zero-width range (insertion at cursor)
      if (normalized.range.start.line == normalized.range.end.line &&
          normalized.range.start.character == normalized.range.end.character)
      {
         // Ghost text at cursor position
         activeCompletion_ = new Completion(normalized);
         setNesState(normalized, SuggestionType.GHOST_TEXT, null);
         if (autoshow)
            renderNesSuggestion();
         else
            showSuggestionGutterOnly(normalized.range.start.line,
               AceEditorGutterStyles.NES_GUTTER_HIGHLIGHT);
         if (notifyServer)
            server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
         return;
      }
      
      // Get the original text from the document at the edit range
      String originalText = display_.getCode(
         Position.create(normalized.range.start.line, normalized.range.start.character),
         Position.create(normalized.range.end.line, normalized.range.end.character));
      
      // Check if this is a deletion-only change
      EditDeltas deltas = new EditDeltas(originalText, normalized.insertText);
      if (deltas.isDeletionOnly())
      {
         setNesState(normalized, SuggestionType.DELETION, deltas);
         if (autoshow)
            renderNesSuggestion();
         else
            showSuggestionGutterOnly(normalized.range.start.line,
               AceEditorGutterStyles.NES_GUTTER_DELETION);
         if (notifyServer)
            server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
         return;
      }
      
      // Check if this consists only of single-line insertions
      if (deltas.isSingleLineInsertions() && !deltas.getAdditions().isEmpty())
      {
         setNesState(normalized, SuggestionType.INSERTION, deltas);
         if (autoshow)
            renderNesSuggestion();
         else
            showSuggestionGutterOnly(normalized.range.start.line,
               AceEditorGutterStyles.NES_GUTTER_INSERTION);
         if (notifyServer)
            server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
         return;
      }
      
      // Check if this is a single-line replacement (one deletion + one insertion)
      if (deltas.isSingleLineReplacement())
      {
         EditDelta deletion = deltas.getSingleDeletion();
         EditDelta addition = deltas.getSingleAddition();
         if (deletion != null && addition != null)
         {
            setNesState(normalized, SuggestionType.REPLACEMENT, deltas);
            if (autoshow)
               renderNesSuggestion();
            else
               showSuggestionGutterOnly(normalized.range.start.line,
                  AceEditorGutterStyles.NES_GUTTER_REPLACEMENT);
            if (notifyServer)
               server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
            return;
         }
      }
      
      // For single-line deltas, use mixed in-document rendering;
      // for multiline deltas, fall back to the inline diff view.
      if (deltas.isSingleLineDeltas())
      {
         setNesState(normalized, SuggestionType.MIXED, deltas);
         if (autoshow)
            renderNesSuggestion();
         else
            showSuggestionGutterOnly(normalized.range.start.line,
               AceEditorGutterStyles.NES_GUTTER_REPLACEMENT);
         if (notifyServer)
            server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
      }
      else
      {
         setNesState(normalized, SuggestionType.DIFF, deltas);
         if (autoshow)
            showDiffViewEditSuggestion(nesCompletion_);
         else
            showSuggestionGutterOnly(normalized.range.start.line,
               AceEditorGutterStyles.NES_GUTTER_HIGHLIGHT);
         if (notifyServer)
            server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
      }
   }
   
   private void showDiffViewEditSuggestion(AssistantCompletion completion)
   {
      // Note that we can accept the diff suggestion with Tab
      Scheduler.get().scheduleDeferred(() ->
      {
         canAcceptSuggestionWithTab_ = true;
      });

      // Highlight the range in the document associated with
      // the edit suggestion
      Range editRange = Range.create(
         completion.range.start.line,
         completion.range.start.character,
         completion.range.end.line,
         completion.range.end.character);

      diffMarkerId_ = display_.addHighlight(editRange, "ace_next-edit-suggestion-highlight", "text");

      // Get the original text from the document at the edit range
      String originalText = display_.getCode(
         Position.create(completion.range.start.line,
                         completion.range.start.character),
         Position.create(completion.range.end.line,
                         completion.range.end.character));

      // Get the replacement text from the completion
      String replacementText = completion.insertText;

      // Create the diff view widget
      diffView_ = new AceEditorDiffView(originalText, replacementText, display_.getFileType())
      {
         @Override
         protected void apply()
         {
            // Get edit range using NES-specific anchored positions so the range stays
            // valid even if the document has been edited.
            Range range = Range.create(
               nesStartAnchor_.getRow(),
               nesStartAnchor_.getColumn(),
               nesEndAnchor_.getRow(),
               nesEndAnchor_.getColumn());

            // Move cursor to end of edit range
            display_.setCursorPosition(range.getEnd());

            // Perform the actual replacement
            display_.replaceRange(range, completion.insertText);

            // Notify server that completion was accepted
            server_.assistantDidAcceptCompletion(completion.command, new VoidServerRequestCallback());

            // Reset and schedule another suggestion
            reset();
            nesTimer_.schedule(20);
         }

         @Override
         protected void discard()
         {
            reset();
         }

         @Override
         public double getLineHeight()
         {
            return display_.getLineHeight();
         }
      };

      // Insert as line widget at the end row of the completion
      int row = completion.range.end.line;

      diffWidget_ = new PinnedLineWidget(
         "copilot-diff",
         display_,
         diffView_.getWidget(),
         row,
         null,
         null);
   }

   // Helper class for grouping insertion information by row
   private static class InsertionInfo
   {
      final String text;
      final int column;

      InsertionInfo(String text, int column)
      {
         this.text = text;
         this.column = column;
      }
   }

   // Helper class for storing ranges using anchors so they update on document changes
   private static class AnchoredRange
   {
      AnchoredRange(Anchor startAnchor, Anchor endAnchor)
      {
         this.startAnchor = startAnchor;
         this.endAnchor = endAnchor;
      }

      Range toRange()
      {
         return Range.create(
            startAnchor.getRow(),
            startAnchor.getColumn(),
            endAnchor.getRow(),
            endAnchor.getColumn());
      }

      void detach()
      {
         startAnchor.detach();
         endAnchor.detach();
      }

      private final Anchor startAnchor;
      private final Anchor endAnchor;
   }

   /**
    * Applies the current NES suggestion (insertion, deletion, or replacement).
    */
   private void applyNesSuggestion()
   {
      if (nesCompletion_ == null)
         return;

      // Get edit range using NES-specific anchored positions
      Range range = Range.create(
         nesStartAnchor_.getRow(),
         nesStartAnchor_.getColumn(),
         nesEndAnchor_.getRow(),
         nesEndAnchor_.getColumn());

      // Move cursor to start of range
      display_.setCursorPosition(range.getStart());

      // Perform the edit (replace range with insertText)
      display_.replaceRange(range, nesCompletion_.insertText);

      // Notify server that completion was accepted
      server_.assistantDidAcceptCompletion(nesCompletion_.command, new VoidServerRequestCallback());

      // Reset and schedule another suggestion
      reset();
      nesTimer_.schedule(20);
   }

   /**
    * Renders an edit suggestion inline using deletion highlights and insertion token splicing.
    * Handles deletion-only, insertion-only, and mixed cases with appropriate styling.
    * Assumes nesCompletion_ and nesDeltas_ are already set, and all deltas are single-line.
    */
   private void renderSuggestion()
   {
      int baseRow = nesCompletion_.range.start.line;
      int baseCol = nesCompletion_.range.start.character;

      // Track bounds for the bounding rectangle highlight
      int minRow = Integer.MAX_VALUE;
      int maxRow = Integer.MIN_VALUE;
      boolean hasDeletions = false;
      boolean hasInsertions = false;

      // First pass: add deletion highlights
      for (EditDelta delta : nesDeltas_.getDeltas())
      {
         if (delta.type != EditType.DELETION)
            continue;

         hasDeletions = true;
         Range documentRange = offsetRangeToDocument(delta.range, baseRow, baseCol);

         int markerId = display_.addHighlight(documentRange, "ace_next-edit-suggestion-deletion", "text");
         nesMarkerIds_.add(markerId);

         // Store the document range for click detection using anchors
         Anchor startAnchor = display_.createAnchor(documentRange.getStart());
         Anchor endAnchor = display_.createAnchor(documentRange.getEnd());
         nesClickableRanges_.add(new AnchoredRange(startAnchor, endAnchor));

         // Track min/max rows for bounding rectangle
         minRow = Math.min(minRow, documentRange.getStart().getRow());
         maxRow = Math.max(maxRow, documentRange.getEnd().getRow());
      }

      // Second pass: collect insertion info grouped by row
      Map<Integer, List<InsertionInfo>> insertionsByRow = new HashMap<>();

      for (EditDelta delta : nesDeltas_.getDeltas())
      {
         if (delta.type != EditType.ADDITION)
            continue;

         hasInsertions = true;

         // Compute the document position for the insertion
         Range range = delta.range;
         int insertRow = baseRow + range.getStart().getRow();
         int insertCol = range.getStart().getRow() == 0
            ? baseCol + range.getStart().getColumn()
            : range.getStart().getColumn();

         // Track the position for cleanup
         nesTokenPositions_.add(Position.create(insertRow, insertCol));

         // Store the range for click detection using anchors
         int insertEndCol = insertCol + delta.text.length();
         Anchor insertStartAnchor = display_.createAnchor(Position.create(insertRow, insertCol));
         Anchor insertEndAnchor = display_.createAnchor(Position.create(insertRow, insertEndCol));
         nesClickableRanges_.add(new AnchoredRange(insertStartAnchor, insertEndAnchor));

         // Track min/max rows for bounding rectangle
         minRow = Math.min(minRow, insertRow);
         maxRow = Math.max(maxRow, insertRow);

         // Group by row
         insertionsByRow
            .computeIfAbsent(insertRow, k -> new ArrayList<>())
            .add(new InsertionInfo(delta.text, insertCol));
      }

      // Third pass: splice insertion tokens row by row
      for (Map.Entry<Integer, List<InsertionInfo>> entry : insertionsByRow.entrySet())
      {
         int row = entry.getKey();
         List<InsertionInfo> insertions = entry.getValue();

         // Sort by column in descending order so splicing doesn't affect positions
         insertions.sort((a, b) -> Integer.compare(b.column, a.column));

         // Invalidate tokens for this row, get the token array, splice all insertions, then render
         display_.invalidateTokens(row);
         JsArray<Token> tokens = display_.getTokens(row);

         for (InsertionInfo info : insertions)
         {
            Token newToken = Token.create(info.text, "insertion_preview", 0);
            display_.spliceToken(tokens, newToken, info.column);
         }

         display_.renderTokens(row);
      }

      // Add bounding rectangle highlight and gutter icon for all affected rows
      if (minRow <= maxRow)
      {
         // Choose appropriate styling based on what's present
         String boundsClass;
         String gutterClass;
         if (hasDeletions && hasInsertions)
         {
            boundsClass = "ace_next-edit-suggestion-replacement-bounds";
            gutterClass = AceEditorGutterStyles.NES_GUTTER_REPLACEMENT;
         }
         else if (hasDeletions)
         {
            boundsClass = "ace_next-edit-suggestion-deletion-bounds";
            gutterClass = AceEditorGutterStyles.NES_GUTTER_DELETION;
         }
         else
         {
            boundsClass = "ace_next-edit-suggestion-insertion-bounds";
            gutterClass = AceEditorGutterStyles.NES_GUTTER_INSERTION;
         }

         Range boundsRange = Range.create(minRow, 0, maxRow, 0);
         nesBoundsMarkerId_ = display_.addHighlight(boundsRange, boundsClass, "fullLine");

         HandlerRegistration registration = display_.addGutterItem(minRow, gutterClass);
         nesGutterRegistrations_.add(registration);

         for (int row = minRow + 1; row <= maxRow; row++)
         {
            registration = display_.addGutterItem(row, AceEditorGutterStyles.NES_GUTTER_BACKGROUND);
            nesGutterRegistrations_.add(registration);
         }
      }
   }

   /**
    * Checks if a document position falls within any of the NES clickable ranges.
    */
   private boolean isPositionInNesRange(Position pos)
   {
      for (AnchoredRange anchoredRange : nesClickableRanges_)
      {
         if (anchoredRange.toRange().contains(pos))
            return true;
      }
      return false;
   }

   /**
    * Checks if a document change intersects with the active NES suggestion range.
    * Returns true if there is an active suggestion and the change range overlaps it.
    */
   private boolean doesChangeIntersectNesSuggestion(AceDocumentChangeEventNative nativeEvent)
   {
      if (nesStartAnchor_ == null || nesEndAnchor_ == null)
         return false;

      Range nesRange = Range.create(
         nesStartAnchor_.getRow(),
         nesStartAnchor_.getColumn(),
         nesEndAnchor_.getRow(),
         nesEndAnchor_.getColumn());

      Range changeRange = nativeEvent.getRange();

      // Ranges intersect if either contains the other's start position
      return nesRange.contains(changeRange.getStart()) ||
             changeRange.contains(nesRange.getStart());
   }

   /**
    * Converts a delta range (relative to the edit) to a document range.
    * Handles the offset logic where the first line needs column offset but subsequent lines don't.
    */
   private static Range offsetRangeToDocument(Range deltaRange, int baseRow, int baseCol)
   {
      int startRow = baseRow + deltaRange.getStart().getRow();
      int endRow = baseRow + deltaRange.getEnd().getRow();

      int startCol = deltaRange.getStart().getRow() == 0
         ? baseCol + deltaRange.getStart().getColumn()
         : deltaRange.getStart().getColumn();

      int endCol = deltaRange.getEnd().getRow() == 0
         ? baseCol + deltaRange.getEnd().getColumn()
         : deltaRange.getEnd().getColumn();

      return Range.create(startRow, startCol, endRow, endCol);
   }

   /**
    * Checks if an element is within a NES gutter cell.
    */
   private static boolean isNesSuggestionGutterCell(Element el)
   {
      // Just check for the base class since all NES gutter icons use it
      return el.hasClassName(AceEditorGutterStyles.NES_GUTTER);
   }

   /**
    * Finds the NES gutter cell element containing the given element, if any.
    */
   private static Element findNesGutterCell(Element target)
   {
      // First find the gutter annotation element
      Element annotationEl = DomUtils.findParentElement(target, true, el ->
         el.hasClassName("ace_gutter_annotation"));

      if (annotationEl == null)
         return null;

      // Then check if it's within a suggestion gutter cell
      return DomUtils.findParentElement(annotationEl, false,
         TextEditingTargetAssistantHelper::isNesSuggestionGutterCell);
   }

   /**
    * Sets the NES state and creates NES anchors.
    * Call this before renderNesSuggestion() or showSuggestionGutterOnly().
    */
   private void setNesState(AssistantCompletion completion, SuggestionType type, EditDeltas deltas)
   {
      nesCompletion_ = completion;
      nesType_ = type;
      nesDeltas_ = deltas;

      // Create NES-specific anchors for the completion range
      detachNesAnchors();
      nesStartAnchor_ = display_.createAnchor(Position.create(
         completion.range.start.line, completion.range.start.character));
      nesStartAnchor_.setInsertRight(true);
      nesEndAnchor_ = display_.createAnchor(Position.create(
         completion.range.end.line, completion.range.end.character));
      nesEndAnchor_.setInsertRight(true);
   }

   /**
    * Detaches and clears the NES anchors.
    */
   private void detachNesAnchors()
   {
      if (nesStartAnchor_ != null)
      {
         nesStartAnchor_.detach();
         nesStartAnchor_ = null;
      }
      if (nesEndAnchor_ != null)
      {
         nesEndAnchor_.detach();
         nesEndAnchor_ = null;
      }
   }

   /**
    * Shows only the gutter icon for a pending suggestion (when autoshow is disabled).
    * The full details will be shown when the user hovers over the gutter icon.
    * Assumes setNesState() has already been called.
    */
   private void showSuggestionGutterOnly(int row, String gutterClass)
   {
      pendingGutterRow_ = row;
      pendingGutterRegistration_ = display_.addGutterItem(row, gutterClass);
   }

   /**
    * Shows the full details for the pending suggestion.
    * Called when user hovers over the gutter icon (when autoshow is disabled).
    * The NES state is preserved so we can hide details on mouseleave.
    */
   private void showPendingSuggestionDetails()
   {
      if (nesType_ == SuggestionType.NONE || nesCompletion_ == null)
         return;

      // Remove the pending gutter icon (the render methods will add their own)
      if (pendingGutterRegistration_ != null)
      {
         pendingGutterRegistration_.removeHandler();
         pendingGutterRegistration_ = null;
      }

      // Render the visual elements based on type
      renderNesSuggestion();

      // Mark that details are revealed via hover
      pendingSuggestionRevealed_ = true;
   }

   /**
    * Hides the details for a pending suggestion and restores gutter-only state.
    * Called when mouse leaves the gutter icon area.
    */
   private void hidePendingSuggestionDetails()
   {
      if (!pendingSuggestionRevealed_ || nesType_ == SuggestionType.NONE)
         return;

      // Clear visual elements but preserve NES state (nesCompletion_, nesType_, nesDeltas_)
      display_.removeGhostText();
      activeCompletion_ = null;
      clearDiffState();
      clearNesVisuals();

      // Restore gutter-only state
      String gutterClass = getGutterClassForType(nesType_);
      pendingGutterRegistration_ = display_.addGutterItem(pendingGutterRow_, gutterClass);

      pendingSuggestionRevealed_ = false;
   }

   /**
    * Renders the visual elements for the current NES suggestion based on nesType_.
    * Assumes nesCompletion_, nesType_, and nesDeltas_ are already set.
    */
   private void renderNesSuggestion()
   {
      if (nesCompletion_ == null || nesType_ == SuggestionType.NONE)
         return;

      // Enable Tab acceptance for all NES types (except DIFF which handles it separately)
      if (nesType_ != SuggestionType.DIFF)
      {
         Scheduler.get().scheduleDeferred(() -> canAcceptSuggestionWithTab_ = true);
      }

      switch (nesType_)
      {
         case GHOST_TEXT:
            activeCompletion_ = new Completion(nesCompletion_);
            Position position = Position.create(
               nesCompletion_.range.start.line,
               nesCompletion_.range.start.character);
            display_.setGhostText(activeCompletion_.displayText, position);
            break;

         case DELETION:
         case INSERTION:
         case REPLACEMENT:
            if (nesDeltas_ != null)
               renderSuggestion();
            break;

         case MIXED:
         case DIFF:
            showDiffViewEditSuggestion(nesCompletion_);
            break;

         default:
            break;
      }
   }

   /**
    * Returns the appropriate gutter CSS class for a suggestion type.
    */
   private String getGutterClassForType(SuggestionType type)
   {
      switch (type)
      {
         case DELETION:
            return AceEditorGutterStyles.NES_GUTTER_DELETION;
         case INSERTION:
            return AceEditorGutterStyles.NES_GUTTER_INSERTION;
         case REPLACEMENT:
            return AceEditorGutterStyles.NES_GUTTER_REPLACEMENT;
         case GHOST_TEXT:
         case MIXED:
         case DIFF:
         default:
            return AceEditorGutterStyles.NES_GUTTER_HIGHLIGHT;
      }
   }

   /**
    * Returns the appropriate clickable CSS class for a suggestion type.
    */
   private String getClickableClassForType(SuggestionType type)
   {
      switch (type)
      {
         case DELETION:
            return "ace_deletion-clickable";
         case INSERTION:
            return "ace_insertion-clickable";
         case REPLACEMENT:
            return "ace_replacement-clickable";
         default:
            return null;
      }
   }

   /**
    * Resets ghost text and inline completion state only.
    */
   private void resetCompletion()
   {
      display_.removeGhostText();
      suggestionTimer_.cancel();
      activeCompletion_ = null;
      detachCompletionAnchors();
   }

   /**
    * Resets NES (Next Edit Suggestion) state only.
    */
   private void resetSuggestion()
   {
      clearDiffState();
      clearNesState();

      pendingSuggestionRevealed_ = false;
      pendingHideTimer_.cancel();
      if (pendingGutterRegistration_ != null)
      {
         pendingGutterRegistration_.removeHandler();
         pendingGutterRegistration_ = null;
      }
   }

   /**
    * Resets all state - both inline completions and NES.
    * Call this when both should be cleared (e.g., document changes, Escape key).
    */
   private void reset()
   {
      resetCompletion();
      resetSuggestion();
   }

   /**
    * Detaches and clears the completion anchors.
    */
   private void detachCompletionAnchors()
   {
      if (completionStartAnchor_ != null)
      {
         completionStartAnchor_.detach();
         completionStartAnchor_ = null;
      }
      if (completionEndAnchor_ != null)
      {
         completionEndAnchor_.detach();
         completionEndAnchor_ = null;
      }
   }

   /**
    * Creates anchors for the current completion range.
    * The start anchor uses insertRight=true so it moves when text is inserted
    * at the anchor position (e.g., during partial word-by-word acceptance).
    */
   private void createCompletionAnchors(int startLine, int startCharacter, int endLine, int endCharacter)
   {
      detachCompletionAnchors();
      completionStartAnchor_ = display_.createAnchor(Position.create(startLine, startCharacter));
      completionStartAnchor_.setInsertRight(true);
      completionEndAnchor_ = display_.createAnchor(Position.create(endLine, endCharacter));
      completionEndAnchor_.setInsertRight(true);
   }

   public TextEditingTargetAssistantHelper(TextEditingTarget target, List<HandlerRegistration> releaseOnDismiss)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      commandsRegistration_ = binder_.bind(commands_, this);

      target_ = target;
      display_ = target.getDocDisplay();

      registrations_ = new HandlerRegistrations();

      suggestionTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            if (assistantDisabledInThisDocument_)
               return;

            target_.withSavedDoc(() ->
            {
               requestId_ += 1;
               final int requestId = requestId_;
               final Position savedCursorPosition = display_.getCursorPosition();

               events_.fireEvent(
                     new AssistantEvent(AssistantEventType.COMPLETION_REQUESTED));

               String trigger = prefs_.assistantCompletionsTrigger().getGlobalValue();
               boolean autoInvoked = trigger.equals(UserPrefsAccessor.ASSISTANT_COMPLETIONS_TRIGGER_AUTO);
               if (completionTriggeredByCommand_)
               {
                  // users can trigger completions manually via command, even if set to auto
                  autoInvoked = false;
                  completionTriggeredByCommand_ = false;
               }

               server_.assistantGenerateCompletions(
                     target_.getId(),
                     StringUtil.notNull(target_.getPath()),
                     StringUtil.isNullOrEmpty(target_.getPath()),
                     autoInvoked,
                     display_.getCursorRow(),
                     display_.getCursorColumn(),
                     new ServerRequestCallback<AssistantGenerateCompletionsResponse>()
                     {
                        @Override
                        public void onResponseReceived(AssistantGenerateCompletionsResponse response)
                        {
                           // Check for invalidated request.
                           if (requestId_ != requestId)
                              return;

                           // Check for alternate cursor position.
                           Position currentCursorPosition = display_.getCursorPosition();
                           if (!currentCursorPosition.isEqualTo(savedCursorPosition))
                              return;

                           // Check for null completion results -- this may occur if the Copilot
                           // agent couldn't be started for some reason.
                           if (response == null)
                              return;

                           // Check whether completions are enabled in this document.
                           if (Objects.equals(response.enabled, false))
                           {
                              assistantDisabledInThisDocument_ = true;
                              events_.fireEvent(new AssistantEvent(AssistantEventType.COMPLETION_CANCELLED));
                              return;
                           }

                           // Check for error.
                           AssistantError error = response.error;
                           if (error != null)
                           {
                              // Handle 'document could not be found' errors up-front. These errors
                              // will normally self-resolve after the user starts editing the document,
                              // so it should suffice just to indicate that no completions are available.
                              int code = error.code;
                              if (code == AssistantConstants.ErrorCodes.DOCUMENT_NOT_FOUND)
                              {
                                 events_.fireEvent(new AssistantEvent(AssistantEventType.COMPLETION_RECEIVED_NONE));
                              }
                              else
                              {
                                 String message = assistant_.messageForError(error);
                                 events_.fireEvent(
                                       new AssistantEvent(
                                             AssistantEventType.COMPLETION_ERROR,
                                             message));
                                 return;
                              }
                           }

                           // Check for null result. This might occur if the completion request
                           // was cancelled by the copilot agent. But it also might just imply there
                           // weren't any completions available.
                           Any result = response.result;
                           if (result == null)
                           {
                              // For Copilot, fall back to NES; for Posit AI, just report no completions
                              if (shouldFallbackToNes())
                              {
                                 nesTimer_.schedule(20);
                              }
                              else
                              {
                                 events_.fireEvent(new AssistantEvent(AssistantEventType.COMPLETION_RECEIVED_NONE));
                              }
                              return;
                           }

                           // Check for a cancellation reason.
                           Object reason = result.asPropertyMap().get("cancellationReason");
                           if (reason != null)
                           {
                              events_.fireEvent(
                                    new AssistantEvent(AssistantEventType.COMPLETION_CANCELLED));
                              return;
                           }

                           // Otherwise, handle the response.
                           JsArrayLike<AssistantCompletion> jsCompletions =
                                 Js.cast(result.asPropertyMap().get("items"));

                           // Create a filtered list of the completions we were provided.
                           //
                           // Normally, we'd just use .asList() and .removeIf(), but apparently
                           // the implementation of the List interface backend here doesn't
                           // actually support .removeIf(), so we do it by hand.
                           List<AssistantCompletion> completions = new ArrayList<>();
                           for (int i = 0, n = jsCompletions.getLength(); i < n; i++)
                           {
                              if (isValidCompletion(jsCompletions.getAt(i)))
                              {
                                 completions.add(jsCompletions.getAt(i));
                              }
                           }

                           events_.fireEvent(new AssistantEvent(
                                 completions.isEmpty()
                                    ? AssistantEventType.COMPLETION_RECEIVED_NONE
                                    : AssistantEventType.COMPLETION_RECEIVED_SOME));

                           // If we don't have any completions available, fall back to NES for Copilot
                           if (completions.isEmpty())
                           {
                              if (shouldFallbackToNes())
                              {
                                 nesTimer_.schedule(20);
                              }
                              return;
                           }

                           // TODO: If multiple completions are available we should provide a way for
                           // the user to view/select them. For now, use the last one.
                           // https://github.com/rstudio/rstudio/issues/16055
                           AssistantCompletion completion = completions.get(completions.size() - 1);

                           // The completion data gets modified when doing partial (word-by-word)
                           // completions, so we need to use a copy and preserve the original
                           // (which we need to send back to the server as-is in some language-server methods).
                           AssistantCompletion normalized = normalizeCompletion(completion);

                           resetCompletion();
                           activeCompletion_ = new Completion(normalized);
                           createCompletionAnchors(
                              activeCompletion_.startLine,
                              activeCompletion_.startCharacter,
                              activeCompletion_.endLine,
                              activeCompletion_.endCharacter);
                           display_.setGhostText(activeCompletion_.displayText);
                           server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                        }
                     });
            });
         }
      };

      suspendTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            completionRequestsSuspended_ = false;
         }
      };

      nesTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            requestNextEditSuggestions();
         }
      };

      pendingHideTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            hidePendingSuggestionDetails();
         }
      };

      releaseOnDismiss.add(events_.addHandler(ProjectOptionsChangedEvent.TYPE, (event) ->
      {
         manageHandlers();
      }));

      releaseOnDismiss.add(prefs_.assistant().addValueChangeHandler((event) ->
      {
         manageHandlers();
      }));

      releaseOnDismiss.add(assistant_.addRuntimeStatusChangedHandler((event) ->
      {
         manageHandlers();
      }));

      Scheduler.get().scheduleDeferred(() ->
      {
         manageHandlers();
      });
   }

   private boolean isValidCompletion(AssistantCompletion completion)
   {
      // Skip this completion if the insertion text matches
      // what we already have in the document.
      String existingText = display_.getCode(
            Position.create(
               completion.range.start.line,
               completion.range.start.character),
            Position.create(
               completion.range.end.line,
               completion.range.end.character));
      if (existingText.equals(completion.insertText))
         return false;

      // Otherwise, assume it's a valid completion.
      return true;
   }

   /**
    * Returns true if we should request NES directly instead of regular completions.
    * This is the case for Posit AI when NES is enabled.
    */
   private boolean shouldRequestNesDirectly()
   {
      boolean isPositAi = StringUtil.equals(
            assistant_.getAssistantType(),
            UserPrefsAccessor.ASSISTANT_POSIT);
      boolean nesEnabled = prefs_.assistantNesEnabled().getGlobalValue();
      return isPositAi && nesEnabled;
   }

   /**
    * Returns true if we should fall back to NES when regular completions are empty.
    * This is the case for non-Posit AI assistants (e.g., Copilot).
    */
   private boolean shouldFallbackToNes()
   {
      boolean isPositAi = StringUtil.equals(
            assistant_.getAssistantType(),
            UserPrefsAccessor.ASSISTANT_POSIT);
      return !isPositAi;
   }

   private void manageHandlers()
   {
      if (!isAssistantAvailable())
      {
         display_.removeGhostText();
         registrations_.removeHandler();
         requestId_ = 0;
         suggestionTimer_.cancel();
         completionTriggeredByCommand_ = false;
         events_.fireEvent(new AssistantEvent(AssistantEventType.ASSISTANT_DISABLED));
      }
      else
      {
         registrations_.addAll(

               // click handler for next edit suggestion gutter icon. we use a capturing
               // event handler here so we can intercept the event before Ace does.
               DomUtils.addEventListener(display_.getElement(), "mousedown", true, (event) ->
               {
                  if (event.getButton() != NativeEvent.BUTTON_LEFT)
                     return;

                  Element target = event.getEventTarget().cast();

                  // Check for clicks on any NES gutter icon (uses base class)
                  Element nesGutterEl = DomUtils.findParentElement(target, true, (el) ->
                     el.hasClassName(AceEditorGutterStyles.NES_GUTTER));

                  if (nesGutterEl != null && nesType_ != SuggestionType.NONE)
                  {
                     event.stopPropagation();
                     event.preventDefault();

                     // Ghost text uses applyGhostText(), all others use applyNesSuggestion()
                     if (nesType_ == SuggestionType.GHOST_TEXT)
                        display_.applyGhostText();
                     else
                        applyNesSuggestion();
                     return;
                  }

                  // Check for Ctrl/Cmd + click on any NES highlight
                  boolean isModifierHeld = event.getCtrlKey() || event.getMetaKey();
                  if (isModifierHeld && nesCompletion_ != null)
                  {
                     // Convert mouse coordinates to document position
                     Position pos = display_.screenCoordinatesToDocumentPosition(
                        event.getClientX(),
                        event.getClientY());

                     if (isPositionInNesRange(pos))
                     {
                        event.stopPropagation();
                        event.preventDefault();
                        applyNesSuggestion();
                        return;
                     }
                  }
               }),

               // mousemove handler for showing pointer cursor when hovering over NES highlights with Ctrl/Cmd
               DomUtils.addEventListener(display_.getElement(), "mousemove", false, (event) ->
               {
                  // Track last mouse position for keydown/keyup handlers
                  lastMouseClientX_ = event.getClientX();
                  lastMouseClientY_ = event.getClientY();

                  boolean isModifierHeld = event.getCtrlKey() || event.getMetaKey();

                  // Convert mouse coordinates to document position
                  Position pos = display_.screenCoordinatesToDocumentPosition(
                     event.getClientX(),
                     event.getClientY());

                  // Remove all clickable classes first
                  display_.getElement().removeClassName("ace_deletion-clickable");
                  display_.getElement().removeClassName("ace_insertion-clickable");
                  display_.getElement().removeClassName("ace_replacement-clickable");

                  // Add appropriate clickable class based on type if hovering over NES range
                  if (nesCompletion_ != null && isModifierHeld && isPositionInNesRange(pos))
                  {
                     String clickableClass = getClickableClassForType(nesType_);
                     if (clickableClass != null)
                        display_.getElement().addClassName(clickableClass);
                  }
               }),

               // keydown handler to update cursor when modifier key is pressed
               DomUtils.addEventListener(display_.getElement(), "keydown", false, (event) ->
               {
                  // Check if Ctrl or Meta key was just pressed
                  if (event.getKeyCode() == KeyCodes.KEY_CTRL ||
                      event.getKeyCode() == 91 || // Left Meta (Cmd on Mac)
                      event.getKeyCode() == 93)   // Right Meta (Cmd on Mac)
                  {
                     Position pos = display_.screenCoordinatesToDocumentPosition(
                        lastMouseClientX_,
                        lastMouseClientY_);

                     if (nesCompletion_ != null && isPositionInNesRange(pos))
                     {
                        String clickableClass = getClickableClassForType(nesType_);
                        if (clickableClass != null)
                           display_.getElement().addClassName(clickableClass);
                     }
                  }
               }),

               // keyup handler to update cursor when modifier key is released
               DomUtils.addEventListener(display_.getElement(), "keyup", false, (event) ->
               {
                  // Check if Ctrl or Meta key was just released
                  if (event.getKeyCode() == KeyCodes.KEY_CTRL ||
                      event.getKeyCode() == 91 || // Left Meta (Cmd on Mac)
                      event.getKeyCode() == 93)   // Right Meta (Cmd on Mac)
                  {
                     display_.getElement().removeClassName("ace_deletion-clickable");
                     display_.getElement().removeClassName("ace_insertion-clickable");
                     display_.getElement().removeClassName("ace_replacement-clickable");
                  }
               }),

               // mouseover handler for showing/keeping pending suggestion details when hovering over gutter icon
               DomUtils.addEventListener(display_.getElement(), "mouseover", false, (event) ->
               {
                  Element target = event.getEventTarget().cast();
                  Element gutterCell = findNesGutterCell(target);
                  if (gutterCell != null)
                  {
                     // Cancel any pending hide since we're over a gutter icon
                     pendingHideTimer_.cancel();

                     // Show details if not already revealed
                     if (hasPendingUnrevealedSuggestion())
                        showPendingSuggestionDetails();
                  }
               }),

               // mouseout handler for hiding pending suggestion details when leaving gutter icon
               DomUtils.addEventListener(display_.getElement(), "mouseout", false, (event) ->
               {
                  if (!pendingSuggestionRevealed_)
                     return;

                  Element target = event.getEventTarget().cast();
                  Element gutterCell = findNesGutterCell(target);
                  if (gutterCell != null)
                  {
                     // Use a small delay to handle DOM changes that cause brief mouseout/mouseover cycles
                     pendingHideTimer_.schedule(100);
                  }
               }),

               display_.addDocumentChangedHandler((event) ->
               {
                  // Eagerly reset Tab acceptance flag
                  canAcceptSuggestionWithTab_ = false;

                  AceDocumentChangeEventNative nativeEvent = event.getEvent();

                  // Dismiss NES suggestion if the edit intersects the suggestion range
                  if (nativeEvent != null && doesChangeIntersectNesSuggestion(nativeEvent))
                  {
                     resetSuggestion();
                  }

                  // Avoid re-triggering on newline insertions
                  if (nativeEvent != null)
                  {
                     boolean isNewlineInsertion =
                        nativeEvent.lines.length() == 2 &&
                        nativeEvent.lines.get(0).isEmpty() &&
                        nativeEvent.lines.get(1).isEmpty();

                     if (isNewlineInsertion)
                        return;
                  }

                  // Check if we've been toggled off
                  if (!automaticCodeSuggestionsEnabled_)
                     return;

                  // Check preference value
                  String trigger = prefs_.assistantCompletionsTrigger().getGlobalValue();
                  if (trigger != UserPrefsAccessor.ASSISTANT_COMPLETIONS_TRIGGER_AUTO)
                     return;

                  // Allow one-time suppression of cursor change handler
                  if (completionRequestsSuspended_)
                     return;

                  // Don't do anything if we have a selection.
                  if (display_.hasSelection())
                  {
                     suggestionTimer_.cancel();
                     completionTriggeredByCommand_ = false;
                     return;
                  }

                  // Request completions on cursor navigation.
                  int delayMs = MathUtil.clamp(prefs_.assistantCompletionsDelay().getValue(), 10, 5000);

                  // For Posit AI with NES enabled, request NES directly instead of regular completions
                  if (shouldRequestNesDirectly())
                  {
                     nesTimer_.schedule(delayMs);
                  }
                  else
                  {
                     suggestionTimer_.schedule(delayMs);
                  }

                  // Delay handler so we can handle a Tab keypress
                  Timers.singleShot(0, () -> {
                     activeCompletion_ = null;
                     display_.removeGhostText();
                  });
               }),

               display_.addCapturingKeyDownHandler(new KeyDownHandler()
               {
                  @Override
                  public void onKeyDown(KeyDownEvent keyEvent)
                  {
                     NativeEvent event = keyEvent.getNativeEvent();

                     // If we have a pending suggestion (gutter only, not revealed),
                     // Tab should reveal it first before accepting
                     if (hasPendingUnrevealedSuggestion())
                     {
                        if (event.getKeyCode() == KeyCodes.KEY_TAB && canAcceptSuggestionWithTab_)
                        {
                           event.stopPropagation();
                           event.preventDefault();
                           showPendingSuggestionDetails();
                           return;
                        }
                        else if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
                        {
                           event.stopPropagation();
                           event.preventDefault();
                           reset();
                           return;
                        }
                     }

                     // Let diff view accept on Tab if applicable
                     if (diffView_ != null)
                     {
                        if (event.getKeyCode() == KeyCodes.KEY_TAB && canAcceptSuggestionWithTab_)
                        {
                           event.stopPropagation();
                           event.preventDefault();
                           diffView_.apply();
                           return;
                        }
                     }

                     // Let NES suggestion (deletion, insertion, replacement) accept on Tab if applicable
                     if (nesCompletion_ != null)
                     {
                        if (event.getKeyCode() == KeyCodes.KEY_TAB && canAcceptSuggestionWithTab_)
                        {
                           event.stopPropagation();
                           event.preventDefault();
                           applyNesSuggestion();
                           return;
                        }
                        else if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
                        {
                           event.stopPropagation();
                           event.preventDefault();
                           reset();
                           return;
                        }
                     }

                     // Respect suppression flag
                     if (completionRequestsSuspended_)
                        return;

                     // If ghost text is being displayed, accept it on a Tab key press.
                     // TODO: Let user choose keybinding for accepting ghost text?
                     if (activeCompletion_ == null)
                        return;

                     // TODO: If we have a completion popup, should that take precedence?
                     if (display_.isPopupVisible())
                        return;

                     // Check if the user just inserted some text matching the current
                     // ghost text. If so, we'll suppress the next cursor change handler,
                     // so we can continue presenting the current ghost text.
                     String key = EventProperty.key(keyEvent.getNativeEvent());
                     if (activeCompletion_.displayText.startsWith(key))
                     {
                        updateCompletion(key);
                        temporarilySuspendCompletionRequests();
                        return;
                     }

                     if (event.getKeyCode() == KeyCodes.KEY_TAB)
                     {
                        event.stopPropagation();
                        event.preventDefault();

                        // Accept the ghost text. Use anchored positions
                        // so the range stays valid even if the document has been edited.
                        Range aceRange = Range.create(
                              completionStartAnchor_.getRow(),
                              completionStartAnchor_.getColumn(),
                              completionEndAnchor_.getRow(),
                              completionEndAnchor_.getColumn());
                        display_.replaceRange(aceRange, activeCompletion_.insertText);

                        Position cursorPos = Position.create(
                           completionEndAnchor_.getRow(),
                           completionEndAnchor_.getColumn() + activeCompletion_.insertText.length());
                        display_.setCursorPosition(cursorPos);

                        server_.assistantDidAcceptCompletion(
                           activeCompletion_.originalCompletion.command,
                           new VoidServerRequestCallback());

                        reset();
                     }
                     else if (event.getKeyCode() == KeyCodes.KEY_BACKSPACE)
                     {
                        display_.removeGhostText();
                     }
                     else if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
                     {
                        // Don't remove ghost text if Ace's autocomplete is active
                        // Let Ace close its popup first
                        if (!display_.hasActiveAceCompleter())
                        {
                           display_.removeGhostText();
                           activeCompletion_ = null;
                        }
                     }
                     else if (display_.hasGhostText() &&
                              event.getKeyCode() == KeyCodes.KEY_RIGHT &&
                              (event.getCtrlKey() || event.getMetaKey()))
                     {
                        event.stopPropagation();
                        event.preventDefault();

                        commands_.assistantAcceptNextWord().execute();
                     }

                  }
               })

         );

      }
   }

   private void requestNextEditSuggestions()
   {
      if (!prefs_.assistantNesEnabled().getGlobalValue())
         return;

      if (completionRequestsSuspended_)
         return;

      target_.withSavedDoc(() ->
      {
         requestNextEditSuggestionsImpl();
      });
   }

   private void requestNextEditSuggestionsImpl()
   {
      // Invalidate any prior requests.
      nesId_ += 1;

      // Save current request ID.
      final int id = nesId_;

      // Notify the listeners.
      events_.fireEvent(new AssistantEvent(AssistantEventType.COMPLETION_REQUESTED));

      // Make the request.
      server_.assistantNextEditSuggestions(
         target_.getId(),
         StringUtil.notNull(target_.getPath()),
         StringUtil.isNullOrEmpty(target_.getPath()),
         display_.getCursorRow(),
         display_.getCursorColumn(),
         new ServerRequestCallback<AssistantNextEditSuggestionsResponse>()
         {
            @Override
            public void onResponseReceived(AssistantNextEditSuggestionsResponse response)
            {
               // Check for invalidated request.
               if (id != nesId_)
                  return;

               // Check for edits
               boolean hasEdits =
                  response.result != null &&
                  response.result.edits != null &&
                  response.result.edits.getLength() > 0;

               if (!hasEdits)
               {
                  events_.fireEvent(new AssistantEvent(AssistantEventType.COMPLETION_RECEIVED_NONE));
                  return;
               }

               AssistantNextEditSuggestionsResultEntry entry = response.result.edits.getAt(0);

               // Construct an Assistant completion object from the response
               AssistantCompletion completion = new AssistantCompletion();
               completion.insertText = entry.text;
               completion.range = entry.range;
               completion.command = entry.command;

               events_.fireEvent(new AssistantEvent(
                  AssistantEventType.COMPLETION_RECEIVED_SOME,
                  completion));

               // Check if autoshow is enabled
               boolean autoshow = prefs_.assistantNesAutoshow().getValue();
               
               // Display the suggestion using the common helper
               showEditSuggestionImpl(completion, autoshow, true);
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
   }

   @Handler
   public void onAssistantRequestSuggestions()
   {
      if (isAssistantAvailable() && display_.isFocused())
      {
         completionTriggeredByCommand_ = true;
         suggestionTimer_.schedule(0);
      }
   }

   @Handler
   public void onAssistantRequestCompletions()
   {
      onAssistantRequestSuggestions();
   }

   @Handler
   public void onAssistantAcceptNextWord()
   {
      if (!display_.isFocused())
         return;

      boolean hasActiveSuggestion = display_.hasGhostText() && activeCompletion_ != null;
      if (!hasActiveSuggestion)
         return;

      String text = activeCompletion_.displayText;
      Pattern pattern = Pattern.create("(?:\\b|$)");
      Match match = pattern.match(text, 1);
      if (match == null)
         return;

      String insertedWord = StringUtil.substring(text, 0, match.getIndex());
      String leftoverText = StringUtil.substring(text, match.getIndex());

      int n = insertedWord.length();

      // From the docs: "Note that the acceptedLength includes everything from the start of
      //                 insertText to the end of the accepted text. It is not the length of
      //                 the accepted text itself."
      activeCompletion_.partialAcceptedLength += n;

      activeCompletion_.displayText = leftoverText;
      activeCompletion_.insertText = leftoverText;
      activeCompletion_.startCharacter += n;
      activeCompletion_.endCharacter += n;

      Timers.singleShot(() ->
      {
         temporarilySuspendCompletionRequests();
         display_.insertCode(insertedWord, InsertionBehavior.EditorBehaviorsDisabled);
         display_.setGhostText(activeCompletion_.displayText);
         server_.assistantDidAcceptPartialCompletion(activeCompletion_.originalCompletion,
                                                   activeCompletion_.partialAcceptedLength,
                                                   new VoidServerRequestCallback());

         // Work around issue with ghost text not appearing after inserting
         // a code suggestion containing a new line
         if (insertedWord.indexOf('\n') != -1)
         {
            Timers.singleShot(20, () ->
            {
               display_.setGhostText(activeCompletion_.displayText);
            });
         }
      });
   }

   @Handler
   public void onAssistantToggleAutomaticCompletions()
   {
      if (display_.isFocused())
      {
         automaticCodeSuggestionsEnabled_ = !automaticCodeSuggestionsEnabled_;

         if (automaticCodeSuggestionsEnabled_)
         {
            events_.fireEvent(new AssistantEvent(AssistantEventType.COMPLETIONS_ENABLED));
         }
         else
         {
            events_.fireEvent(new AssistantEvent(AssistantEventType.COMPLETIONS_DISABLED));
         }
      }
   }

   @Handler
   public void onAssistantAcceptNextEditSuggestion()
   {
      if (!display_.isFocused())
         return;

      // If we have a pending suggestion that hasn't been revealed, reveal it first
      if (hasPendingUnrevealedSuggestion())
      {
         showPendingSuggestionDetails();
         return;
      }

      // Apply the appropriate suggestion type
      if (diffView_ != null)
      {
         diffView_.apply();
      }
      else if (nesCompletion_ != null)
      {
         applyNesSuggestion();
      }
      else if (display_.hasGhostText() && activeCompletion_ != null)
      {
         // Accept ghost text completion
         Range aceRange = Range.create(
               completionStartAnchor_.getRow(),
               completionStartAnchor_.getColumn(),
               completionEndAnchor_.getRow(),
               completionEndAnchor_.getColumn());
         display_.replaceRange(aceRange, activeCompletion_.insertText);

         Position cursorPos = Position.create(
            completionEndAnchor_.getRow(),
            completionEndAnchor_.getColumn() + activeCompletion_.insertText.length());
         display_.setCursorPosition(cursorPos);

         server_.assistantDidAcceptCompletion(
            activeCompletion_.originalCompletion.command,
            new VoidServerRequestCallback());

         reset();
      }
      else
      {
         // No active suggestion, request one based on NES setting
         if (prefs_.assistantNesEnabled().getGlobalValue())
         {
            requestNextEditSuggestions();
         }
         else
         {
            suggestionTimer_.schedule(0);
         }
      }
   }

   @Handler
   public void onAssistantDismissNextEditSuggestion()
   {
      if (!display_.isFocused())
         return;

      // Dismiss any active next edit suggestion
      if (hasActiveSuggestion())
      {
         reset();
      }
   }

   private void updateCompletion(String key)
   {
      int n = key.length();
      activeCompletion_.displayText = StringUtil.substring(activeCompletion_.displayText, n);
      activeCompletion_.insertText = StringUtil.substring(activeCompletion_.insertText, n);
      activeCompletion_.startCharacter += n;
      activeCompletion_.endCharacter += n;

      // Ace's ghost text uses a custom token appended to the current line,
      // and lines are eagerly re-tokenized when new text is inserted. To
      // dodge this effect, we reset the ghost text at the end of the event loop.
      Timers.singleShot(() ->
      {
         display_.setGhostText(activeCompletion_.displayText);
      });
   }

   private static String postProcessCompletion(String text)
   {
      // Exclude chunk markers from completion results
      int endChunkIndex = text.indexOf("\n```");
      if (endChunkIndex != -1)
         text = text.substring(0, endChunkIndex);

      return text;
   }

   public void onFileTypeChanged()
   {
      assistantDisabledInThisDocument_ = false;
   }

   public boolean isAssistantAvailable()
   {
      return assistant_.isRuntimeRunning();
   }

   // Normalize an inline completion by trimming overlapping prefix/suffix.
   // This is used for traditional Copilot completions displayed as ghost text.
   private AssistantCompletion normalizeCompletion(AssistantCompletion completion)
   {
      try
      {
         return normalizeCompletionImpl(completion);
      }
      catch (Exception e)
      {
         Debug.logException(e);
         return completion;
      }
   }

   private AssistantCompletion normalizeCompletionImpl(AssistantCompletion completion)
   {
      // Remove any overlap from the start of the completion.
      int lhs = 0;
      {
         int row = completion.range.start.line;
         int col = completion.range.start.character;
         String line = display_.getLine(row);

         for (; lhs < completion.insertText.length() && col + lhs < line.length(); lhs++)
         {
            char clhs = completion.insertText.charAt(lhs);
            char crhs = line.charAt(col + lhs);
            if (clhs != crhs)
               break;
         }
      }

      // Remove any overlap from the end of the completion.
      // Only do this part for single-line completions.
      int rhs = 0;
      if (completion.range.start.line == completion.range.end.line)
      {
         int row = completion.range.end.line;
         int col = completion.range.end.character;
         String line = display_.getLine(row);

         for (; rhs < completion.insertText.length() && col - rhs > 0; rhs++)
         {
            char clhs = completion.insertText.charAt(completion.insertText.length() - rhs - 1);
            char crhs = line.charAt(col - rhs - 1);
            if (clhs != crhs)
               break;
         }
      }

      AssistantCompletion normalized = JsUtil.clone(completion);
      int n = normalized.insertText.length();

      if (lhs >= n - rhs)
      {
         // The completion is entirely overlapping the existing text.
         Position cursorPos = display_.getCursorPosition();
         normalized.insertText = "";
         normalized.range.start.line = cursorPos.getRow();
         normalized.range.start.character = cursorPos.getColumn();
         normalized.range.end.line = cursorPos.getRow();
         normalized.range.end.character = cursorPos.getColumn();
      }
      else
      {
         normalized.insertText = StringUtil.substring(normalized.insertText, lhs, n - rhs);
         normalized.range.start.character += lhs;
         normalized.range.end.character -= rhs;
      }

      return normalized;
   }

   // Normalize an edit suggestion by expanding multi-line ranges.
   // This is used for NES (Next Edit Suggestions) which use diff-based rendering.
   private AssistantCompletion normalizeSuggestion(AssistantCompletion completion)
   {
      try
      {
         return normalizeSuggestionImpl(completion);
      }
      catch (Exception e)
      {
         Debug.logException(e);
         return completion;
      }
   }

   private AssistantCompletion normalizeSuggestionImpl(AssistantCompletion completion)
   {
      completion = JsUtil.clone(completion);

      if (completion.range.start.line != completion.range.end.line)
      {
         // Include text from the document up to the first character.
         String startLine = display_.getLine(completion.range.start.line);
         String prefix = startLine.substring(0, completion.range.start.character);
         completion.range.start.character = 0;
         completion.insertText = prefix + completion.insertText;

         // Include text from the document up to the end of the last line.
         String endLine = display_.getLine(completion.range.end.line);
         String suffix = endLine.substring(completion.range.end.character);
         completion.range.end.character = endLine.length();
         completion.insertText = completion.insertText + suffix;
      }

      // If both the original text and insert text end with a newline,
      // trim the trailing newline to avoid redundant replacement
      while (completion.range.end.line > completion.range.start.line &&
             completion.range.end.character == 0 &&
             completion.insertText.endsWith("\n"))
      {
         completion.insertText = completion.insertText.substring(0, completion.insertText.length() - 1);
         completion.range.end.line--;
         String newEndLine = display_.getLine(completion.range.end.line);
         completion.range.end.character = newEndLine.length();
      }

      return completion;
   }

   private void temporarilySuspendCompletionRequests()
   {
      completionRequestsSuspended_ = true;
      suspendTimer_.schedule(1200);
   }

   @Inject
   private void initialize(Assistant assistant,
                           EventBus events,
                           UserPrefs prefs,
                           Commands commands,
                           AssistantCommandBinder binder,
                           AssistantServerOperations server)
   {
      assistant_ = assistant;
      events_ = events;
      prefs_ = prefs;
      commands_ = commands;
      binder_ = binder;
      server_ = server;
   }

   /**
    * Returns true if there is any active next edit suggestion being displayed.
    */
   private boolean hasActiveSuggestion()
   {
      return diffView_ != null || nesCompletion_ != null;
   }

   /**
    * Returns true if there is a pending suggestion that hasn't been revealed yet.
    * This is when we have stored NES state but only showing the gutter icon.
    */
   private boolean hasPendingUnrevealedSuggestion()
   {
      return pendingGutterRow_ != -1 && !pendingSuggestionRevealed_;
   }

   /**
    * Resets tokens for all rows that have positions in the given list.
    * This efficiently handles multiple positions on the same row by only
    * resetting each affected row once.
    */
   private void resetTokensForPositions(List<Position> positions)
   {
      Set<Integer> rowsToReset = new HashSet<>();
      for (Position pos : positions)
         rowsToReset.add(pos.getRow());
      for (int row : rowsToReset)
         display_.resetTokens(row);
   }

   /**
    * Clears the diff view state.
    */
   private void clearDiffState()
   {
      if (diffWidget_ != null)
      {
         diffWidget_.detach();
         diffWidget_ = null;
      }
      if (diffView_ != null)
      {
         diffView_.detach();
         diffView_ = null;
      }
      if (diffMarkerId_ != -1)
      {
         display_.removeHighlight(diffMarkerId_);
         diffMarkerId_ = -1;
      }
   }

   /**
    * Clears only the visual elements for NES suggestions (markers, tokens, gutter icons).
    * Preserves nesCompletion_, nesType_, and nesDeltas_ for re-rendering on hover.
    */
   private void clearNesVisuals()
   {
      // Remove all highlight markers
      for (int markerId : nesMarkerIds_)
         display_.removeHighlight(markerId);
      nesMarkerIds_.clear();

      // Remove all gutter registrations
      for (HandlerRegistration reg : nesGutterRegistrations_)
         reg.removeHandler();
      nesGutterRegistrations_.clear();

      // Detach and clear clickable range anchors
      for (AnchoredRange anchoredRange : nesClickableRanges_)
         anchoredRange.detach();
      nesClickableRanges_.clear();

      // Reset tokens for all tracked positions
      resetTokensForPositions(nesTokenPositions_);
      nesTokenPositions_.clear();

      // Remove bounds marker if present
      if (nesBoundsMarkerId_ != -1)
      {
         display_.removeHighlight(nesBoundsMarkerId_);
         nesBoundsMarkerId_ = -1;
      }

      // Remove clickable class names
      display_.getElement().removeClassName("ace_deletion-clickable");
      display_.getElement().removeClassName("ace_insertion-clickable");
      display_.getElement().removeClassName("ace_replacement-clickable");
   }

   /**
    * Clears the NES state completely (visuals and stored data).
    */
   private void clearNesState()
   {
      clearNesVisuals();
      detachNesAnchors();
      nesCompletion_ = null;
      nesType_ = SuggestionType.NONE;
      nesDeltas_ = null;
      pendingGutterRow_ = -1;
   }

   public void onDismiss()
   {
      // Cancel all timers
      suggestionTimer_.cancel();
      suspendTimer_.cancel();
      nesTimer_.cancel();
      pendingHideTimer_.cancel();

      // Clear NES state (markers, gutter registrations, anchors, etc.)
      clearNesState();

      // Clear diff view state
      clearDiffState();

      // Detach completion anchors
      detachCompletionAnchors();

      // Remove ghost text
      display_.removeGhostText();

      // Remove pending gutter registration
      if (pendingGutterRegistration_ != null)
      {
         pendingGutterRegistration_.removeHandler();
         pendingGutterRegistration_ = null;
      }

      // Remove handler registrations
      registrations_.removeHandler();

      // Unbind command handlers
      commandsRegistration_.removeHandler();
   }

   private final TextEditingTarget target_;
   private final DocDisplay display_;
   private final Timer suggestionTimer_;
   private final Timer suspendTimer_;
   private final Timer nesTimer_;
   private final Timer pendingHideTimer_;
   private int nesId_ = 0;
   private boolean completionTriggeredByCommand_ = false;
   private final HandlerRegistrations registrations_;
   private final HandlerRegistration commandsRegistration_;

   private int requestId_;
   private boolean completionRequestsSuspended_;
   private boolean assistantDisabledInThisDocument_;

   // Diff view state (kept separate as it's a fundamentally different UI)
   private AceEditorDiffView diffView_;
   private PinnedLineWidget diffWidget_;
   private int diffMarkerId_ = -1;

   // Ghost text completion state
   private boolean canAcceptSuggestionWithTab_ = false;
   private Completion activeCompletion_;

   // Unified NES (Next Edit Suggestion) state
   private AssistantCompletion nesCompletion_;
   private SuggestionType nesType_ = SuggestionType.NONE;
   private EditDeltas nesDeltas_;
   private List<Integer> nesMarkerIds_ = new ArrayList<>();
   private List<HandlerRegistration> nesGutterRegistrations_ = new ArrayList<>();
   private List<AnchoredRange> nesClickableRanges_ = new ArrayList<>();
   private List<Position> nesTokenPositions_ = new ArrayList<>();
   private int nesBoundsMarkerId_ = -1;
   private Anchor nesStartAnchor_;
   private Anchor nesEndAnchor_;

   // Mouse tracking
   private int lastMouseClientX_ = 0;
   private int lastMouseClientY_ = 0;

   // Ghost text completion anchors (separate from NES anchors)
   private Anchor completionStartAnchor_;
   private Anchor completionEndAnchor_;

   // Autoshow and pending suggestion state
   private boolean automaticCodeSuggestionsEnabled_ = true;
   private int pendingGutterRow_ = -1;
   private HandlerRegistration pendingGutterRegistration_;
   private boolean pendingSuggestionRevealed_ = false;

   // Injected ----
   private Assistant assistant_;
   private EventBus events_;
   private UserPrefs prefs_;
   private Commands commands_;
   private AssistantCommandBinder binder_;
   private AssistantServerOperations server_;
}
