/*
 * TextEditingTargetCopilotHelper.java
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
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
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
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.model.CopilotConstants;
import org.rstudio.studio.client.workbench.copilot.model.CopilotEvent;
import org.rstudio.studio.client.workbench.copilot.model.CopilotEvent.CopilotEventType;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotGenerateCompletionsResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotNextEditSuggestionsResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotNextEditSuggestionsResultEntry;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotCompletion;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotError;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.InsertionBehavior;
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

public class TextEditingTargetCopilotHelper
{
   interface CopilotCommandBinder extends CommandBinder<Commands, TextEditingTargetCopilotHelper>
   {
   }

   // A wrapper class for Copilot Completions, which is used to track partially-accepted completions.
   private static class Completion
   {
      public Completion(CopilotCompletion originalCompletion)
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
      public CopilotCompletion originalCompletion;
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
    * Enum representing the type of next-edit suggestion display.
    */
   private enum SuggestionType
   {
      NONE,
      GHOST_TEXT,
      DELETION,
      INSERTION,
      REPLACEMENT,
      DIFF
   }

   /**
    * Holds pending suggestion data for deferred display.
    * Used when autoshow is disabled to show details on hover.
    * When autoshow is disabled, we store the suggestion here first and only
    * reveal it when the user hovers over the gutter icon.
    */
   private static class PendingSuggestion
   {
      public SuggestionType type = SuggestionType.NONE;
      public CopilotCompletion completion;
      public int gutterRow = -1;

      // For deletion suggestions
      public EditDeltas deltas;

      // For insertion suggestions
      public List<EditDelta> additions;

      // For replacement suggestions
      public EditDelta singleDeletion;
      public EditDelta singleAddition;

      public void clear()
      {
         type = SuggestionType.NONE;
         completion = null;
         gutterRow = -1;
         deltas = null;
         additions = null;
         singleDeletion = null;
         singleAddition = null;
      }
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

      private boolean hasAdditions_ = false;
      private boolean hasDeletions_ = false;
      private List<EditDelta> deltas_ = new ArrayList<>();
   }

   /**
    * Shows an inline edit suggestion diff view for the given completion.
    */
   private void showEditSuggestion(CopilotCompletion completion)
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
            // Get edit range using anchored positions so the range stays
            // valid even if the document has been edited.
            Range range = Range.create(
               completionStartAnchor_.getRow(),
               completionStartAnchor_.getColumn(),
               completionEndAnchor_.getRow(),
               completionEndAnchor_.getColumn());

            // Move cursor to end of edit range
            display_.setCursorPosition(range.getEnd());

            // Perform the actual replacement
            display_.replaceRange(range, completion.insertText);

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

   /**
    * Shows a deletion-only edit suggestion by highlighting the text that would be deleted.
    * This is a simpler presentation than the full diff view, used when the edit only removes text.
    */
   private void showDeletionSuggestion(CopilotCompletion completion, EditDeltas deltas)
   {
      // Note that we can accept the suggestion with Tab
      Scheduler.get().scheduleDeferred(() ->
      {
         canAcceptSuggestionWithTab_ = true;
      });

      // Get the start position of the completion in the document
      int baseRow = completion.range.start.line;
      int baseCol = completion.range.start.character;

      // Track bounds for the bounding rectangle highlight
      int minRow = Integer.MAX_VALUE;
      int maxRow = Integer.MIN_VALUE;

      // Highlight each deletion range with a red-tinted background
      boolean addedGutterIcon = false;
      for (EditDelta delta : deltas.getDeltas())
      {
         if (delta.type != EditType.DELETION)
            continue;

         Range range = delta.range;

         // Offset the range by the completion's position in the document
         Range documentRange;
         if (range.getStart().getRow() == 0)
         {
            // First line: offset the column by baseCol
            int startCol = baseCol + range.getStart().getColumn();
            int endCol = range.getEnd().getRow() == 0
               ? baseCol + range.getEnd().getColumn()
               : range.getEnd().getColumn();
            documentRange = Range.create(
               baseRow + range.getStart().getRow(),
               startCol,
               baseRow + range.getEnd().getRow(),
               endCol);
         }
         else
         {
            // Subsequent lines: only offset the row
            documentRange = Range.create(
               baseRow + range.getStart().getRow(),
               range.getStart().getColumn(),
               baseRow + range.getEnd().getRow(),
               range.getEnd().getColumn());
         }

         int markerId = display_.addHighlight(documentRange, "ace_next-edit-suggestion-deletion", "text");
         nesMarkerIds_.add(markerId);

         // Store the document range for click detection
         nesClickableRanges_.add(documentRange);

         // Track min/max rows for bounding rectangle
         minRow = Math.min(minRow, documentRange.getStart().getRow());
         maxRow = Math.max(maxRow, documentRange.getEnd().getRow());

         // Add gutter icon only on the first row of the first deletion
         if (!addedGutterIcon)
         {
            HandlerRegistration registration = display_.addGutterItem(
               documentRange.getStart().getRow(), AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_DELETION);
            nesGutterRegistrations_.add(registration);
            addedGutterIcon = true;
         }
      }

      // Add bounding rectangle highlight for all affected rows
      if (minRow <= maxRow)
      {
         Range boundsRange = Range.create(minRow, 0, maxRow + 1, 0);
         nesBoundsMarkerId_ = display_.addHighlight(
            boundsRange, "ace_next-edit-suggestion-deletion-bounds", "fullLine");
      }

      // Store the completion and type for applying later
      nesCompletion_ = completion;
      nesType_ = SuggestionType.DELETION;
   }

   /**
    * Shows single-line insertion suggestions by rendering the insertion text inline.
    * This is used for simple insertions that don't involve deletions or multi-line text.
    */
   private void showInsertionSuggestion(CopilotCompletion completion, List<EditDelta> additionDeltas)
   {
      // Note that we can accept the suggestion with Tab
      Scheduler.get().scheduleDeferred(() ->
      {
         canAcceptSuggestionWithTab_ = true;
      });

      // Get the start position of the completion in the document
      int baseRow = completion.range.start.line;
      int baseCol = completion.range.start.character;

      // Track bounds for the bounding rectangle highlight
      int minRow = Integer.MAX_VALUE;
      int maxRow = Integer.MIN_VALUE;

      // Group additions by row so we can add multiple tokens per row efficiently.
      // We need to process them in reverse column order within each row so that
      // splicing doesn't shift the positions of subsequent insertions.
      Map<Integer, List<InsertionInfo>> insertionsByRow = new HashMap<>();

      for (EditDelta additionDelta : additionDeltas)
      {
         // Compute the document position for the insertion
         Range range = additionDelta.range;
         int insertRow = baseRow + range.getStart().getRow();
         int insertCol = range.getStart().getRow() == 0
            ? baseCol + range.getStart().getColumn()
            : range.getStart().getColumn();

         // Track the position for cleanup
         nesTokenPositions_.add(Position.create(insertRow, insertCol));

         // Store the range for click detection (covers the preview text area)
         int insertEndCol = insertCol + additionDelta.text.length();
         Range insertionRange = Range.create(insertRow, insertCol, insertRow, insertEndCol);
         nesClickableRanges_.add(insertionRange);

         // Track min/max rows for bounding rectangle
         minRow = Math.min(minRow, insertRow);
         maxRow = Math.max(maxRow, insertRow);

         // Group by row
         insertionsByRow
            .computeIfAbsent(insertRow, k -> new ArrayList<>())
            .add(new InsertionInfo(additionDelta.text, insertCol));
      }

      // Now add tokens row by row, processing multiple insertions per row efficiently
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

      // Add bounding rectangle highlight for all affected rows
      if (minRow <= maxRow)
      {
         Range boundsRange = Range.create(minRow, 0, maxRow + 1, 0);
         nesBoundsMarkerId_ = display_.addHighlight(
            boundsRange, "ace_next-edit-suggestion-insertion-bounds", "fullLine");

         // Add gutter icon on the first row
         HandlerRegistration registration = display_.addGutterItem(
            minRow, AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_INSERTION);
         nesGutterRegistrations_.add(registration);
      }

      // Store the completion and type for applying later
      nesCompletion_ = completion;
      nesType_ = SuggestionType.INSERTION;
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

   /**
    * Applies the current NES suggestion (insertion, deletion, or replacement).
    */
   private void applyNesSuggestion()
   {
      if (nesCompletion_ == null)
         return;

      // Get edit range using anchored positions
      Range range = Range.create(
         completionStartAnchor_.getRow(),
         completionStartAnchor_.getColumn(),
         completionEndAnchor_.getRow(),
         completionEndAnchor_.getColumn());

      // Move cursor to start of range
      display_.setCursorPosition(range.getStart());

      // Perform the edit (replace range with insertText)
      display_.replaceRange(range, nesCompletion_.insertText);

      // Reset and schedule another suggestion
      reset();
      nesTimer_.schedule(20);
   }

   /**
    * Shows a single-line replacement suggestion inline.
    * Displays the deleted text with strikethrough and the replacement text as a ghost-like insertion.
    */
   private void showReplacementSuggestion(CopilotCompletion completion, EditDelta deletion, EditDelta addition)
   {
      // Note that we can accept the suggestion with Tab
      Scheduler.get().scheduleDeferred(() ->
      {
         canAcceptSuggestionWithTab_ = true;
      });

      // Get the start position of the completion in the document
      int baseRow = completion.range.start.line;
      int baseCol = completion.range.start.character;

      // Compute the document range for the deletion
      Range delRange = deletion.range;
      int delStartRow = baseRow + delRange.getStart().getRow();
      int delStartCol = delRange.getStart().getRow() == 0
         ? baseCol + delRange.getStart().getColumn()
         : delRange.getStart().getColumn();
      int delEndRow = baseRow + delRange.getEnd().getRow();
      int delEndCol = delRange.getEnd().getRow() == 0
         ? baseCol + delRange.getEnd().getColumn()
         : delRange.getEnd().getColumn();

      Range deletionDocRange = Range.create(delStartRow, delStartCol, delEndRow, delEndCol);

      // Add strikethrough highlight for the deletion
      int deletionMarkerId = display_.addHighlight(
         deletionDocRange, "ace_replacement_deletion", "text");
      nesMarkerIds_.add(deletionMarkerId);

      // Store the range for click detection (the clickable area includes both deletion and insertion)
      nesClickableRanges_.add(deletionDocRange);

      // Compute the document position for the insertion (right after the deletion)
      int insertRow = delEndRow;
      int insertCol = delEndCol;

      // Add the insertion preview token
      display_.invalidateTokens(insertRow);
      JsArray<Token> tokens = display_.getTokens(insertRow);
      display_.spliceToken(tokens, Token.create(addition.text, "insertion_preview", 0), insertCol);
      display_.renderTokens(insertRow);
      nesTokenPositions_.add(Position.create(insertRow, insertCol));

      // Add bounding rectangle highlight
      Range boundsRange = Range.create(delStartRow, 0, delStartRow + 1, 0);
      nesBoundsMarkerId_ = display_.addHighlight(
         boundsRange, "ace_next-edit-suggestion-replacement-bounds", "fullLine");

      // Add gutter icon
      HandlerRegistration registration = display_.addGutterItem(
         delStartRow, AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_REPLACEMENT);
      nesGutterRegistrations_.add(registration);

      // Store the completion and type for applying later
      nesCompletion_ = completion;
      nesType_ = SuggestionType.REPLACEMENT;
   }

   /**
    * Checks if a document position falls within any of the NES clickable ranges.
    */
   private boolean isPositionInNesRange(Position pos)
   {
      for (Range range : nesClickableRanges_)
      {
         if (range.contains(pos))
            return true;
      }
      return false;
   }

   /**
    * Shows only the gutter icon for a pending suggestion (when autoshow is disabled).
    * The full details will be shown when the user hovers over the gutter icon.
    */
   private void showSuggestionGutterOnly(int row, String gutterClass, SuggestionType type,
                                          CopilotCompletion completion)
   {
      // Store the pending suggestion data
      pendingSuggestion_.type = type;
      pendingSuggestion_.completion = completion;
      pendingSuggestion_.gutterRow = row;

      // Create anchors for the completion range
      createCompletionAnchors(
         completion.range.start.line,
         completion.range.start.character,
         completion.range.end.line,
         completion.range.end.character);

      // Show the gutter icon
      pendingGutterRegistration_ = display_.addGutterItem(row, gutterClass);
   }

   /**
    * Shows the full details for the pending suggestion.
    * Called when user hovers over the gutter icon (when autoshow is disabled).
    * The pending state is preserved so we can hide details on mouseleave.
    */
   private void showPendingSuggestionDetails()
   {
      if (pendingSuggestion_.type == SuggestionType.NONE)
         return;

      CopilotCompletion completion = pendingSuggestion_.completion;
      if (completion == null)
         return;

      // Remove the pending gutter icon (the full display methods will add their own)
      if (pendingGutterRegistration_ != null)
      {
         pendingGutterRegistration_.removeHandler();
         pendingGutterRegistration_ = null;
      }

      // Show full details based on type
      switch (pendingSuggestion_.type)
      {
         case GHOST_TEXT:
            // For ghost text, set up the completion and display
            activeCompletion_ = new Completion(completion);
            Position position = Position.create(
               completion.range.start.line,
               completion.range.start.character);
            display_.setGhostText(activeCompletion_.displayText, position);
            break;

         case DELETION:
            if (pendingSuggestion_.deltas != null)
               showDeletionSuggestion(completion, pendingSuggestion_.deltas);
            break;

         case INSERTION:
            if (pendingSuggestion_.additions != null)
               showInsertionSuggestion(completion, pendingSuggestion_.additions);
            break;

         case REPLACEMENT:
            if (pendingSuggestion_.singleDeletion != null && pendingSuggestion_.singleAddition != null)
               showReplacementSuggestion(completion, pendingSuggestion_.singleDeletion,
                                         pendingSuggestion_.singleAddition);
            break;

         case DIFF:
            showEditSuggestion(completion);
            break;

         default:
            break;
      }

      // Mark that details are revealed via hover (don't clear pending state)
      pendingSuggestionRevealed_ = true;
   }

   /**
    * Hides the details for a pending suggestion and restores gutter-only state.
    * Called when mouse leaves the gutter icon area.
    */
   private void hidePendingSuggestionDetails()
   {
      if (!pendingSuggestionRevealed_ || pendingSuggestion_.type == SuggestionType.NONE)
         return;

      // Clear all visual elements (similar to reset, but preserve pendingSuggestion_)
      display_.removeGhostText();
      activeCompletion_ = null;

      // Clear all suggestion types
      clearDiffState();
      clearNesState();

      // Restore gutter-only state
      String gutterClass = getGutterClassForType(pendingSuggestion_.type);
      pendingGutterRegistration_ = display_.addGutterItem(pendingSuggestion_.gutterRow, gutterClass);

      pendingSuggestionRevealed_ = false;
   }

   /**
    * Returns the appropriate gutter CSS class for a suggestion type.
    */
   private String getGutterClassForType(SuggestionType type)
   {
      switch (type)
      {
         case DELETION:
            return AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_DELETION;
         case INSERTION:
            return AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_INSERTION;
         case REPLACEMENT:
            return AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_REPLACEMENT;
         case GHOST_TEXT:
         case DIFF:
         default:
            return AceEditorGutterStyles.NEXT_EDIT_SUGGESTION;
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
    * Resets any visible ghost text or inline diff view.
    * Call this before presenting a new suggestion to ensure only one is shown at a time.
    */
   private void reset()
   {
      // Remove ghost text
      display_.removeGhostText();
      completionTimer_.cancel();
      activeCompletion_ = null;
      detachCompletionAnchors();

      // Clear all suggestion types
      clearDiffState();
      clearNesState();

      // Clear pending suggestion (when autoshow is disabled)
      pendingSuggestion_.clear();
      pendingSuggestionRevealed_ = false;
      pendingHideTimer_.cancel();
      if (pendingGutterRegistration_ != null)
      {
         pendingGutterRegistration_.removeHandler();
         pendingGutterRegistration_ = null;
      }
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

   public TextEditingTargetCopilotHelper(TextEditingTarget target)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      binder_.bind(commands_, this);

      target_ = target;
      display_ = target.getDocDisplay();

      registrations_ = new HandlerRegistrations();
      
      completionTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            if (copilotDisabledInThisDocument_)
               return;
            
            target_.withSavedDoc(() ->
            {
               requestId_ += 1;
               final int requestId = requestId_;
               final Position savedCursorPosition = display_.getCursorPosition();
               
               events_.fireEvent(
                     new CopilotEvent(CopilotEventType.COMPLETION_REQUESTED));

               String trigger = prefs_.copilotCompletionsTrigger().getGlobalValue();
               boolean autoInvoked = trigger.equals(UserPrefsAccessor.COPILOT_COMPLETIONS_TRIGGER_AUTO);
               if (completionTriggeredByCommand_)
               {
                  // users can trigger completions manually via command, even if set to auto
                  autoInvoked = false;
                  completionTriggeredByCommand_ = false;
               }
               
               server_.copilotGenerateCompletions(
                     target_.getId(),
                     StringUtil.notNull(target_.getPath()),
                     StringUtil.isNullOrEmpty(target_.getPath()),
                     autoInvoked,
                     display_.getCursorRow(),
                     display_.getCursorColumn(),
                     new ServerRequestCallback<CopilotGenerateCompletionsResponse>()
                     {
                        @Override
                        public void onResponseReceived(CopilotGenerateCompletionsResponse response)
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
                              copilotDisabledInThisDocument_ = true;
                              events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETION_CANCELLED));
                              return;
                           }
                           
                           // Check for error.
                           CopilotError error = response.error;
                           if (error != null)
                           {
                              // Handle 'document could not be found' errors up-front. These errors
                              // will normally self-resolve after the user starts editing the document,
                              // so it should suffice just to indicate that no completions are available.
                              int code = error.code;
                              if (code == CopilotConstants.ErrorCodes.DOCUMENT_NOT_FOUND)
                              {
                                 events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETION_RECEIVED_NONE));
                              }
                              else
                              {
                                 String message = copilot_.messageForError(error);
                                 events_.fireEvent(
                                       new CopilotEvent(
                                             CopilotEventType.COMPLETION_ERROR,
                                             message));
                                 return;
                              }
                           }
                           
                           // Check for null result. This might occur if the completion request
                           // was cancelled by the copilot agent.
                           Any result = response.result;
                           if (result == null)
                           {
                              events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETION_CANCELLED));
                              return;
                           }
                           
                           // Check for a cancellation reason.
                           Object reason = result.asPropertyMap().get("cancellationReason");
                           if (reason != null)
                           {
                              events_.fireEvent(
                                    new CopilotEvent(CopilotEventType.COMPLETION_CANCELLED));
                              return;
                           }
                           
                           // Otherwise, handle the response.
                           JsArrayLike<CopilotCompletion> jsCompletions =
                                 Js.cast(result.asPropertyMap().get("items"));
                           
                           // Create a filtered list of the completions we were provided.
                           //
                           // Normally, we'd just use .asList() and .removeIf(), but apparently
                           // the implementation of the List interface backend here doesn't
                           // actually support .removeIf(), so we do it by hand.
                           List<CopilotCompletion> completions = new ArrayList<>();
                           for (int i = 0, n = jsCompletions.getLength(); i < n; i++)
                           {
                              if (isValidCompletion(jsCompletions.getAt(i)))
                              {
                                 completions.add(jsCompletions.getAt(i));
                              }
                           }

                           events_.fireEvent(new CopilotEvent(
                                 completions.isEmpty()
                                    ? CopilotEventType.COMPLETION_RECEIVED_NONE
                                    : CopilotEventType.COMPLETION_RECEIVED_SOME));
                           
                           // TODO: If multiple completions are available we should provide a way for 
                           // the user to view/select them. For now, use the last one.
                           // https://github.com/rstudio/rstudio/issues/16055
                           if (!completions.isEmpty())
                           {
                              CopilotCompletion completion = completions.get(completions.size() - 1);

                              // The completion data gets modified when doing partial (word-by-word)
                              // completions, so we need to use a copy and preserve the original
                              // (which we need to send back to the server as-is in some language-server methods).
                              CopilotCompletion normalized = normalizeCompletion(completion);

                              reset();
                              activeCompletion_ = new Completion(normalized);
                              createCompletionAnchors(
                                 activeCompletion_.startLine,
                                 activeCompletion_.startCharacter,
                                 activeCompletion_.endLine,
                                 activeCompletion_.endCharacter);
                              display_.setGhostText(activeCompletion_.displayText);
                              server_.copilotDidShowCompletion(completion, new VoidServerRequestCallback());
                           }
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

      events_.addHandler(ProjectOptionsChangedEvent.TYPE, (event) ->
      {
         manageHandlers();
      });
      
      prefs_.copilotEnabled().addValueChangeHandler((event) ->
      {
         manageHandlers();
      });
      
      Scheduler.get().scheduleDeferred(() ->
      {
         manageHandlers();
      });
      
   }

   private boolean isValidCompletion(CopilotCompletion completion)
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
   
   private void manageHandlers()
   {
      if (!copilot_.isEnabled())
      {
         display_.removeGhostText();
         registrations_.removeHandler();
         requestId_ = 0;
         completionTimer_.cancel();
         completionTriggeredByCommand_ = false;
         events_.fireEvent(new CopilotEvent(CopilotEventType.COPILOT_DISABLED));
      }
      else
      {
         registrations_.addAll(

               display_.addValueChangeHandler((event) ->
               {
                  nesTimer_.schedule(300);
               }),

               // click handler for next-edit suggestion gutter icon. we use a capturing
               // event handler here so we can intercept the event before Ace does.
               DomUtils.addEventListener(display_.getElement(), "mousedown", true, (event) ->
               {
                  if (event.getButton() != NativeEvent.BUTTON_LEFT)
                     return;

                  Element target = event.getEventTarget().cast();

                  // Check for clicks on the next-edit suggestion gutter icon.
                  Element nesEl = DomUtils.findParentElement(target, true, new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION);
                     }
                  });

                  if (nesEl != null)
                  {
                     event.stopPropagation();
                     event.preventDefault();
                     display_.applyGhostText();
                     return;
                  }

                  // Check for clicks on the deletion suggestion gutter icon
                  Element deletionGutterEl = DomUtils.findParentElement(target, true, new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_DELETION);
                     }
                  });

                  if (deletionGutterEl != null && nesType_ == SuggestionType.DELETION)
                  {
                     event.stopPropagation();
                     event.preventDefault();
                     applyNesSuggestion();
                     return;
                  }

                  // Check for clicks on the insertion suggestion gutter icon
                  Element insertionGutterEl = DomUtils.findParentElement(target, true, new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_INSERTION);
                     }
                  });

                  if (insertionGutterEl != null && nesType_ == SuggestionType.INSERTION)
                  {
                     event.stopPropagation();
                     event.preventDefault();
                     applyNesSuggestion();
                     return;
                  }

                  // Check for clicks on the replacement suggestion gutter icon
                  Element replacementGutterEl = DomUtils.findParentElement(target, true, new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_REPLACEMENT);
                     }
                  });

                  if (replacementGutterEl != null && nesType_ == SuggestionType.REPLACEMENT)
                  {
                     event.stopPropagation();
                     event.preventDefault();
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

                  // Check if over an ace_gutter_annotation element within a suggestion gutter cell
                  Element annotationEl = DomUtils.findParentElement(target, true, new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.hasClassName("ace_gutter_annotation");
                     }
                  });

                  if (annotationEl != null)
                  {
                     // Check if this annotation is within a suggestion gutter cell
                     Element gutterCell = DomUtils.findParentElement(annotationEl, false, new ElementPredicate()
                     {
                        @Override
                        public boolean test(Element el)
                        {
                           return el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION) ||
                                  el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_DELETION) ||
                                  el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_INSERTION) ||
                                  el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_REPLACEMENT);
                        }
                     });

                     if (gutterCell != null)
                     {
                        // Cancel any pending hide since we're over a gutter icon
                        pendingHideTimer_.cancel();

                        // Show details if not already revealed
                        if (hasPendingUnrevealedSuggestion())
                        {
                           showPendingSuggestionDetails();
                        }
                     }
                  }
               }),

               // mouseout handler for hiding pending suggestion details when leaving gutter icon
               DomUtils.addEventListener(display_.getElement(), "mouseout", false, (event) ->
               {
                  // Only relevant when we have a revealed pending suggestion
                  if (!pendingSuggestionRevealed_)
                     return;

                  Element target = event.getEventTarget().cast();

                  // Check if leaving an ace_gutter_annotation element within a suggestion gutter cell
                  Element annotationEl = DomUtils.findParentElement(target, true, new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.hasClassName("ace_gutter_annotation");
                     }
                  });

                  if (annotationEl != null)
                  {
                     // Check if this annotation is within a suggestion gutter cell
                     Element gutterCell = DomUtils.findParentElement(annotationEl, false, new ElementPredicate()
                     {
                        @Override
                        public boolean test(Element el)
                        {
                           return el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION) ||
                                  el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_DELETION) ||
                                  el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_INSERTION) ||
                                  el.hasClassName(AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_REPLACEMENT);
                        }
                     });

                     if (gutterCell != null)
                     {
                        // Use a small delay to handle DOM changes that cause brief mouseout/mouseover cycles
                        pendingHideTimer_.schedule(50);
                     }
                  }
               }),

               display_.addCursorChangedHandler((event) ->
               {
                  // Eagerly reset Tab acceptance flag
                  canAcceptSuggestionWithTab_ = false;

                  // Check if we've been toggled off
                  if (!automaticCodeSuggestionsEnabled_)
                     return;
                  
                  // Check preference value
                  String trigger = prefs_.copilotCompletionsTrigger().getGlobalValue();
                  if (trigger != UserPrefsAccessor.COPILOT_COMPLETIONS_TRIGGER_AUTO)
                     return;
                           
                  // Allow one-time suppression of cursor change handler
                  if (completionRequestsSuspended_)
                     return;
                  
                  // Don't do anything if we have a selection.
                  if (display_.hasSelection())
                  {
                     completionTimer_.cancel();
                     completionTriggeredByCommand_ = false;
                     return;
                  }
                  
                  // Request completions on cursor navigation.
                  int delayMs = MathUtil.clamp(prefs_.copilotCompletionsDelay().getValue(), 10, 5000);
                  completionTimer_.schedule(delayMs);

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
                        if (event.getKeyCode() == KeyCodes.KEY_TAB)
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

                        server_.copilotDidAcceptCompletion(
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

                        commands_.copilotAcceptNextWord().execute();
                     }
                     
                  }
               })

         );

      }
   }

   private void requestNextEditSuggestions()
   {
      if (!prefs_.copilotNesEnabled().getGlobalValue())
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
      events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETION_REQUESTED));

      // Make the request.
      server_.copilotNextEditSuggestions(
         target_.getId(),
         StringUtil.notNull(target_.getPath()),
         StringUtil.isNullOrEmpty(target_.getPath()),
         display_.getCursorRow(),
         display_.getCursorColumn(),
         new ServerRequestCallback<CopilotNextEditSuggestionsResponse>()
         {
            @Override
            public void onResponseReceived(CopilotNextEditSuggestionsResponse response)
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
                  events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETION_RECEIVED_NONE));
                  return;
               }

               reset();
               CopilotNextEditSuggestionsResultEntry entry = response.result.edits.getAt(0);

               // Construct a Copilot completion object from the response
               CopilotCompletion completion = new CopilotCompletion();
               completion.insertText = entry.text;
               completion.range = entry.range;
               completion.command = entry.command;

               events_.fireEvent(new CopilotEvent(
                  CopilotEventType.COMPLETION_RECEIVED_SOME,
                  completion));

               // The completion data gets modified when doing partial (word-by-word)
               // completions, so we need to use a copy and preserve the original
               // (which we need to send back to the server as-is in some language-server methods).
               CopilotCompletion normalized = normalizeCompletion(completion);
               activeCompletion_ = new Completion(normalized);
               createCompletionAnchors(
                  activeCompletion_.startLine,
                  activeCompletion_.startCharacter,
                  activeCompletion_.endLine,
                  activeCompletion_.endCharacter);

               // Check if autoshow is enabled
               boolean autoshow = prefs_.copilotNesAutoshow().getValue();

               if (normalized.range.start.line == normalized.range.end.line &&
                   normalized.range.start.character == normalized.range.end.character)
               {
                  // If the start position and end position match, then display
                  // the suggestion using ghost text at that position.
                  if (autoshow)
                  {
                     Position position = Position.create(
                        normalized.range.start.line,
                        normalized.range.start.character);
                     display_.setGhostText(activeCompletion_.displayText, position);
                  }
                  else
                  {
                     // Show only gutter icon; details shown on hover
                     showSuggestionGutterOnly(
                        normalized.range.start.line,
                        AceEditorGutterStyles.NEXT_EDIT_SUGGESTION,
                        SuggestionType.GHOST_TEXT,
                        normalized);
                  }

                  server_.copilotDidShowCompletion(completion, new VoidServerRequestCallback());
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
                  if (autoshow)
                  {
                     // For deletion-only changes, just highlight the text to be deleted
                     showDeletionSuggestion(normalized, deltas);
                  }
                  else
                  {
                     // Show only gutter icon; details shown on hover
                     pendingSuggestion_.deltas = deltas;
                     showSuggestionGutterOnly(
                        normalized.range.start.line,
                        AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_DELETION,
                        SuggestionType.DELETION,
                        normalized);
                  }
                  server_.copilotDidShowCompletion(completion, new VoidServerRequestCallback());
                  return;
               }

               // Check if this consists only of single-line insertions
               if (deltas.isSingleLineInsertions())
               {
                  // For single-line insertions, show inline token preview
                  List<EditDelta> additions = deltas.getAdditions();
                  if (!additions.isEmpty())
                  {
                     if (autoshow)
                     {
                        showInsertionSuggestion(normalized, additions);
                     }
                     else
                     {
                        // Show only gutter icon; details shown on hover
                        pendingSuggestion_.additions = additions;
                        showSuggestionGutterOnly(
                           normalized.range.start.line,
                           AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_INSERTION,
                           SuggestionType.INSERTION,
                           normalized);
                     }
                     server_.copilotDidShowCompletion(completion, new VoidServerRequestCallback());
                     return;
                  }
               }

               // Check if this is a single-line replacement (one deletion + one insertion)
               if (deltas.isSingleLineReplacement())
               {
                  EditDelta deletion = deltas.getSingleDeletion();
                  EditDelta addition = deltas.getSingleAddition();
                  if (deletion != null && addition != null)
                  {
                     if (autoshow)
                     {
                        showReplacementSuggestion(normalized, deletion, addition);
                     }
                     else
                     {
                        // Show only gutter icon; details shown on hover
                        pendingSuggestion_.singleDeletion = deletion;
                        pendingSuggestion_.singleAddition = addition;
                        showSuggestionGutterOnly(
                           normalized.range.start.line,
                           AceEditorGutterStyles.NEXT_EDIT_SUGGESTION_REPLACEMENT,
                           SuggestionType.REPLACEMENT,
                           normalized);
                     }
                     server_.copilotDidShowCompletion(completion, new VoidServerRequestCallback());
                     return;
                  }
               }

               // Otherwise, show the suggestion as an inline diff view.
               if (autoshow)
               {
                  showEditSuggestion(normalized);
               }
               else
               {
                  // Show only gutter icon; details shown on hover
                  showSuggestionGutterOnly(
                     normalized.range.start.line,
                     AceEditorGutterStyles.NEXT_EDIT_SUGGESTION,
                     SuggestionType.DIFF,
                     normalized);
               }
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
   }
   
   @Handler
   public void onCopilotRequestCompletions()
   {
      if (copilot_.isEnabled() && display_.isFocused())
      {
         completionTriggeredByCommand_ = true;
         completionTimer_.schedule(0);
      }
   }
   
   @Handler
   public void onCopilotAcceptNextWord()
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
         server_.copilotDidAcceptPartialCompletion(activeCompletion_.originalCompletion, 
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
   public void onCopilotToggleAutomaticCompletions()
   {
      if (display_.isFocused())
      {
         automaticCodeSuggestionsEnabled_ = !automaticCodeSuggestionsEnabled_;

         if (automaticCodeSuggestionsEnabled_)
         {
            events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETIONS_ENABLED));
         }
         else
         {
            events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETIONS_DISABLED));
         }
      }
   }

   @Handler
   public void onCopilotAcceptNextEditSuggestion()
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
      else
      {
         // No active suggestion, request one
         requestNextEditSuggestions();
      }
   }

   @Handler
   public void onCopilotDismissNextEditSuggestion()
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
      copilotDisabledInThisDocument_ = false;
   }

   public boolean isCopilotEnabled()
   {
      return copilot_.isEnabled();
   }
   
   // A Copilot completion will often overlap region(s) of the document.
   // Try to avoid presenting this overlap, so that only the relevant
   // portion of the completed text is presented to the user.
   private CopilotCompletion normalizeCompletion(CopilotCompletion completion)
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

   private CopilotCompletion normalizeCompletionImpl(CopilotCompletion completion)
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

      return completion;
   }

   private void temporarilySuspendCompletionRequests()
   {
      completionRequestsSuspended_ = true;
      suspendTimer_.schedule(1200);
   }

   @Inject
   private void initialize(Copilot copilot,
                           EventBus events,
                           UserPrefs prefs,
                           Commands commands,
                           CopilotCommandBinder binder,
                           CopilotServerOperations server)
   {
      copilot_ = copilot;
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
      return pendingSuggestion_.type != SuggestionType.NONE ||
             diffView_ != null ||
             nesCompletion_ != null;
   }

   /**
    * Returns true if there is a pending suggestion that hasn't been revealed yet.
    */
   private boolean hasPendingUnrevealedSuggestion()
   {
      return pendingSuggestion_.type != SuggestionType.NONE && !pendingSuggestionRevealed_;
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
    * Clears the NES (Next Edit Suggestion) state for deletion, insertion, and replacement suggestions.
    */
   private void clearNesState()
   {
      // Remove all highlight markers
      for (int markerId : nesMarkerIds_)
         display_.removeHighlight(markerId);
      nesMarkerIds_.clear();

      // Remove all gutter registrations
      for (HandlerRegistration reg : nesGutterRegistrations_)
         reg.removeHandler();
      nesGutterRegistrations_.clear();

      // Clear clickable ranges
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

      // Clear completion and type
      nesCompletion_ = null;
      nesType_ = SuggestionType.NONE;
   }

   private final TextEditingTarget target_;
   private final DocDisplay display_;
   private final Timer completionTimer_;
   private final Timer suspendTimer_;
   private final Timer nesTimer_;
   private final Timer pendingHideTimer_;
   private int nesId_ = 0;
   private boolean completionTriggeredByCommand_ = false;
   private final HandlerRegistrations registrations_;
   
   private int requestId_;
   private boolean completionRequestsSuspended_;
   private boolean copilotDisabledInThisDocument_;

   // Diff view state (kept separate as it's a fundamentally different UI)
   private AceEditorDiffView diffView_;
   private PinnedLineWidget diffWidget_;
   private int diffMarkerId_ = -1;

   // Ghost text completion state
   private boolean canAcceptSuggestionWithTab_ = false;
   private Completion activeCompletion_;

   // Unified NES (Next Edit Suggestion) state
   private CopilotCompletion nesCompletion_;
   private SuggestionType nesType_ = SuggestionType.NONE;
   private EditDeltas nesDeltas_;
   private List<Integer> nesMarkerIds_ = new ArrayList<>();
   private List<HandlerRegistration> nesGutterRegistrations_ = new ArrayList<>();
   private List<Range> nesClickableRanges_ = new ArrayList<>();
   private List<Position> nesTokenPositions_ = new ArrayList<>();
   private int nesBoundsMarkerId_ = -1;

   // Mouse tracking and anchors
   private int lastMouseClientX_ = 0;
   private int lastMouseClientY_ = 0;
   private Anchor completionStartAnchor_;
   private Anchor completionEndAnchor_;

   // Autoshow and pending suggestion state
   private boolean automaticCodeSuggestionsEnabled_ = true;
   private final PendingSuggestion pendingSuggestion_ = new PendingSuggestion();
   private HandlerRegistration pendingGutterRegistration_;
   private boolean pendingSuggestionRevealed_ = false;


   // Injected ----
   private Copilot copilot_;
   private EventBus events_;
   private UserPrefs prefs_;
   private Commands commands_;
   private CopilotCommandBinder binder_;
   private CopilotServerOperations server_;
}
