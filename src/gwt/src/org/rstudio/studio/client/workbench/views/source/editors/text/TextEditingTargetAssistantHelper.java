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
import org.rstudio.core.client.command.KeyboardHelper;
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
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantCompletionCommand;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantError;
import org.rstudio.studio.client.workbench.assistant.server.AssistantServerOperations;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceDocumentChangeEventNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

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

   /**
    * Unified edit suggestion class that consolidates all suggestion state.
    * Replaces both the old Completion class and separate NES state fields.
    */
   private static class EditSuggestion
   {
      public EditSuggestion(AssistantCompletion completion)
      {
         // Copilot includes trailing '```' for some reason in some cases,
         // remove those if we're inserting in an R document.
         this.originalInsertText = postProcessCompletion(completion.insertText);
         this.insertText = this.originalInsertText;
         this.displayText = this.insertText;

         this.startLine = completion.range.start.line;
         this.originalStartCharacter = completion.range.start.character;
         this.startCharacter = this.originalStartCharacter;
         this.endLine = completion.range.end.line;
         this.originalEndCharacter = completion.range.end.character;
         this.endCharacter = this.originalEndCharacter;

         this.command = completion.command;
         this.originalCompletion = completion;
         this.partialAcceptedLength = 0;
         this.type = SuggestionType.GHOST_TEXT;
         this.deltas = null;
         this.isRevealed = false;
      }

      /**
       * Resets mutable display state back to original values.
       * Called when re-revealing a suggestion after it was hidden.
       */
      public void resetDisplayState()
      {
         this.insertText = this.originalInsertText;
         this.displayText = this.insertText;
         this.startCharacter = this.originalStartCharacter;
         this.endCharacter = this.originalEndCharacter;
         this.partialAcceptedLength = 0;
      }

      // Mutable text state (changes as user types matching prefix)
      public String insertText;
      public String displayText;

      // Position (start can change for partial accepts)
      public int startLine;
      public int startCharacter;
      public int endLine;
      public int endCharacter;

      // Original values for reset
      private final String originalInsertText;
      private final int originalStartCharacter;
      private final int originalEndCharacter;

      // Command to execute on acceptance (may be null)
      public final AssistantCompletionCommand command;

      // Original completion - ONLY use for assistantDidAcceptPartialCompletion RPC
      public final AssistantCompletion originalCompletion;

      // Partial acceptance tracking
      public int partialAcceptedLength;

      // Suggestion type and deltas (for NES)
      public SuggestionType type;
      public EditDeltas deltas;

      // Whether the suggestion is currently being displayed (ghost text or diff visible)
      public boolean isRevealed;

      // Anchors that track the suggestion position as the document changes
      // These must be set externally via setAnchors() since they require display_ access
      public Anchor startAnchor;
      public Anchor endAnchor;

      /**
       * Detaches and clears the anchors.
       */
      public void detachAnchors()
      {
         if (startAnchor != null)
         {
            startAnchor.detach();
            startAnchor = null;
         }
         if (endAnchor != null)
         {
            endAnchor.detach();
            endAnchor = null;
         }
      }
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
         setEditSuggestion(normalized, SuggestionType.GHOST_TEXT, null);
         if (autoshow)
            renderEditSuggestion();
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
         setEditSuggestion(normalized, SuggestionType.DELETION, deltas);
         if (autoshow)
            renderEditSuggestion();
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
         setEditSuggestion(normalized, SuggestionType.INSERTION, deltas);
         if (autoshow)
            renderEditSuggestion();
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
            setEditSuggestion(normalized, SuggestionType.REPLACEMENT, deltas);
            if (autoshow)
               renderEditSuggestion();
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
         setEditSuggestion(normalized, SuggestionType.MIXED, deltas);
         if (autoshow)
            renderEditSuggestion();
         else
            showSuggestionGutterOnly(normalized.range.start.line,
               AceEditorGutterStyles.NES_GUTTER_REPLACEMENT);
         if (notifyServer)
            server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
      }
      else
      {
         setEditSuggestion(normalized, SuggestionType.DIFF, deltas);
         if (autoshow)
            showDiffViewEditSuggestion();
         else
            showSuggestionGutterOnly(normalized.range.start.line,
               AceEditorGutterStyles.NES_GUTTER_HIGHLIGHT);
         if (notifyServer)
            server_.assistantDidShowCompletion(completion, new VoidServerRequestCallback());
      }
   }
   
   private void showDiffViewEditSuggestion()
   {
      // Note that we can accept the diff suggestion with Tab
      Scheduler.get().scheduleDeferred(() ->
      {
         canAcceptSuggestionWithTab_ = true;
      });

      // Highlight the range in the document associated with
      // the edit suggestion
      Range editRange = Range.create(
         editSuggestion_.startLine,
         editSuggestion_.startCharacter,
         editSuggestion_.endLine,
         editSuggestion_.endCharacter);

      diffMarkerId_ = display_.addHighlight(editRange, "ace_next-edit-suggestion-highlight", "text");

      // Get the original text from the document at the edit range
      String originalText = display_.getCode(
         Position.create(editSuggestion_.startLine,
                         editSuggestion_.startCharacter),
         Position.create(editSuggestion_.endLine,
                         editSuggestion_.endCharacter));

      // Get the replacement text from the suggestion
      String replacementText = editSuggestion_.insertText;

      // Capture values for use in the inner class
      final AssistantCompletionCommand command = editSuggestion_.command;

      // Create the diff view widget
      diffView_ = new AceEditorDiffView(originalText, replacementText, display_.getFileType())
      {
         @Override
         protected void apply()
         {
            // Suspend the document change handler so our edit doesn't reset suggestion state
            ignoreNextDocumentChangeEvents();

            // Get edit range using anchored positions so the range stays
            // valid even if the document has been edited.
            Range range = Range.create(
               editSuggestion_.startAnchor.getRow(),
               editSuggestion_.startAnchor.getColumn(),
               editSuggestion_.endAnchor.getRow(),
               editSuggestion_.endAnchor.getColumn());

            // Move cursor to end of edit range
            display_.setCursorPosition(range.getEnd());

            // Perform the actual replacement
            display_.replaceRange(range, replacementText);

            // Notify server that completion was accepted
            if (command != null)
               server_.assistantDidAcceptCompletion(command, new VoidServerRequestCallback());

            // Reset and schedule another suggestion
            resetSuggestion();
            nesTimer_.schedule(20);
         }

         @Override
         protected void discard()
         {
            resetSuggestion();
         }

         @Override
         public double getLineHeight()
         {
            return display_.getLineHeight();
         }
      };

      // Insert as line widget at the end row
      int row = editSuggestion_.endLine;

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
    * Applies the current edit suggestion (ghost text, insertion, deletion, or replacement).
    * Works for all suggestion types with anchors set.
    */
   private void acceptEditSuggestion()
   {
      // Bail if we don't have a suggestion or anchors
      if (editSuggestion_ == null || editSuggestion_.startAnchor == null)
         return;

      // Suspend the document change handler so our edit doesn't reset suggestion state
      ignoreNextDocumentChangeEvents();

      // Get edit range using anchored positions.
      // For ghost text (pure insertion), use startAnchor for both start and end,
      // since endAnchor doesn't track forward as user types prefix matches.
      Range range;
      boolean isGhostText = editSuggestion_.type == SuggestionType.GHOST_TEXT;
      if (isGhostText)
      {
         range = Range.create(
            editSuggestion_.startAnchor.getRow(),
            editSuggestion_.startAnchor.getColumn(),
            editSuggestion_.startAnchor.getRow(),
            editSuggestion_.startAnchor.getColumn());
      }
      else
      {
         range = Range.create(
            editSuggestion_.startAnchor.getRow(),
            editSuggestion_.startAnchor.getColumn(),
            editSuggestion_.endAnchor.getRow(),
            editSuggestion_.endAnchor.getColumn());
      }

      // Move cursor to start of range, then perform the edit
      display_.setCursorPosition(range.getStart());
      display_.replaceRange(range, editSuggestion_.insertText);

      // Notify server that completion was accepted
      if (editSuggestion_.command != null)
         server_.assistantDidAcceptCompletion(editSuggestion_.command, new VoidServerRequestCallback());

      // For NES suggestions (not ghost text), schedule another suggestion
      boolean isNesSuggestion = editSuggestion_.type != SuggestionType.GHOST_TEXT;

      resetSuggestion();

      if (isNesSuggestion)
         nesTimer_.schedule(20);
   }

   /**
    * Renders an edit suggestion inline using deletion highlights and insertion token splicing.
    * Handles deletion-only, insertion-only, and mixed cases with appropriate styling.
    * Assumes editSuggestion_ and editSuggestion_.deltas are already set, and all deltas are single-line.
    */
   private void renderSuggestion()
   {
      int baseRow = editSuggestion_.startLine;
      int baseCol = editSuggestion_.startCharacter;

      // Track bounds for the bounding rectangle highlight
      int minRow = Integer.MAX_VALUE;
      int maxRow = Integer.MIN_VALUE;
      boolean hasDeletions = false;
      boolean hasInsertions = false;

      // First pass: add deletion highlights
      for (EditDelta delta : editSuggestion_.deltas.getDeltas())
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

      for (EditDelta delta : editSuggestion_.deltas.getDeltas())
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

      // Third pass: add synthetic tokens for insertions, grouped by row
      for (Map.Entry<Integer, List<InsertionInfo>> entry : insertionsByRow.entrySet())
      {
         int row = entry.getKey();
         List<InsertionInfo> insertions = entry.getValue();

         // Add synthetic tokens for each insertion on this row
         for (InsertionInfo info : insertions)
         {
            display_.addSyntheticToken(row, info.column, info.text, "insertion_preview");
         }

         // Re-render the row to show the synthetic tokens
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
   private boolean doesChangeIntersectSuggestion(AceDocumentChangeEventNative nativeEvent)
   {
      if (editSuggestion_ == null || editSuggestion_.startAnchor == null || editSuggestion_.endAnchor == null)
         return false;

      Range nesRange = Range.create(
         editSuggestion_.startAnchor.getRow(),
         editSuggestion_.startAnchor.getColumn(),
         editSuggestion_.endAnchor.getRow(),
         editSuggestion_.endAnchor.getColumn());

      Range changeRange = nativeEvent.getRange();
      boolean nesContains = nesRange.contains(changeRange.getStart());
      boolean changeContains = changeRange.containsRightExclusive(nesRange.getStart());
      return nesContains || changeContains;
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
    * Sets the edit suggestion state and creates suggestion anchors.
    * Call this before renderEditSuggestion() or showSuggestionGutterOnly().
    */
   private void setEditSuggestion(AssistantCompletion completion, SuggestionType type, EditDeltas deltas)
   {
      editSuggestion_ = new EditSuggestion(completion);
      editSuggestion_.type = type;
      editSuggestion_.deltas = deltas;

      // Create anchors for the suggestion range
      createSuggestionAnchors(
         completion.range.start.line,
         completion.range.start.character,
         completion.range.end.line,
         completion.range.end.character);
   }

   /**
    * Shows only the gutter icon for a pending suggestion (when autoshow is disabled).
    * The full details will be shown when the user hovers over the gutter icon.
    * Assumes setEditSuggestion() has already been called.
    */
   private void showSuggestionGutterOnly(int row, String gutterClass)
   {
      pendingGutterRow_ = row;
      pendingGutterRegistration_ = display_.addGutterItem(row, gutterClass);
   }

   /**
    * Shows the full details for the pending suggestion.
    * Called when user hovers over the gutter icon (when autoshow is disabled).
    * The edit suggestion state is preserved so we can hide details on mouseleave.
    */
   private void showPendingSuggestionDetails()
   {
      if (editSuggestion_ == null || editSuggestion_.type == SuggestionType.GHOST_TEXT)
         return;

      // Remove the pending gutter icon (the render methods will add their own)
      if (pendingGutterRegistration_ != null)
      {
         pendingGutterRegistration_.removeHandler();
         pendingGutterRegistration_ = null;
      }

      // Render the visual elements based on type
      renderEditSuggestion();

      // Mark that details are revealed via hover
      pendingSuggestionRevealed_ = true;
   }

   /**
    * Hides the details for a pending suggestion and restores gutter-only state.
    * Called when mouse leaves the gutter icon area.
    */
   private void hidePendingSuggestionDetails()
   {
      if (!pendingSuggestionRevealed_ || editSuggestion_ == null || editSuggestion_.type == SuggestionType.GHOST_TEXT)
         return;

      // Clear visual elements but preserve edit suggestion state for re-reveal
      hideGhostText();
      editSuggestion_.isRevealed = false;
      editSuggestion_.resetDisplayState();
      clearDiffState();
      clearSuggestionVisuals();

      // Restore gutter-only state
      String gutterClass = getGutterClassForType(editSuggestion_.type);
      pendingGutterRegistration_ = display_.addGutterItem(pendingGutterRow_, gutterClass);

      pendingSuggestionRevealed_ = false;
   }

   /**
    * Renders the visual elements for the current edit suggestion based on type.
    * Assumes editSuggestion_ is already set.
    */
   private void renderEditSuggestion()
   {
      if (editSuggestion_ == null)
         return;

      // Enable Tab acceptance for all types (except DIFF which handles it separately)
      if (editSuggestion_.type != SuggestionType.DIFF)
      {
         Scheduler.get().scheduleDeferred(() -> canAcceptSuggestionWithTab_ = true);
      }

      editSuggestion_.isRevealed = true;

      switch (editSuggestion_.type)
      {
         case GHOST_TEXT:
            Position position = Position.create(
               editSuggestion_.startLine,
               editSuggestion_.startCharacter);
            showGhostText(editSuggestion_.displayText, position);
            break;

         case DELETION:
         case INSERTION:
         case REPLACEMENT:
            if (editSuggestion_.deltas != null)
               renderSuggestion();
            break;

         case MIXED:
         case DIFF:
            showDiffViewEditSuggestion();
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
    * Resets all edit suggestion state (ghost text, diff view, NES visuals, etc.).
    * Call this when the suggestion should be fully cleared.
    */
   private void resetSuggestion()
   {
      hideGhostText();
      suggestionTimer_.cancel();
      clearDiffState();
      clearSuggestionVisuals();

      if (editSuggestion_ != null)
      {
         editSuggestion_.detachAnchors();
         editSuggestion_ = null;
      }

      pendingSuggestionRevealed_ = false;
      pendingHideTimer_.cancel();
      if (pendingGutterRegistration_ != null)
      {
         pendingGutterRegistration_.removeHandler();
         pendingGutterRegistration_ = null;
      }
      pendingGutterRow_ = -1;
   }

   /**
    * Creates anchors for the current suggestion range.
    * The start anchor uses the default insertRight=false so it automatically
    * moves forward when text is inserted at its position (e.g., newlines or
    * partial word-by-word acceptance).
    * The end anchor uses insertRight=true so typing at the end doesn't expand
    * the suggestion range.
    */
   private void createSuggestionAnchors(int startLine, int startCharacter, int endLine, int endCharacter)
   {
      if (editSuggestion_ == null)
         return;

      editSuggestion_.detachAnchors();
      editSuggestion_.startAnchor = display_.createAnchor(Position.create(startLine, startCharacter));
      editSuggestion_.endAnchor = display_.createAnchor(Position.create(endLine, endCharacter));
      editSuggestion_.endAnchor.setInsertRight(true);
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
            if (!isAssistantAvailable())
               return;

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

                           resetSuggestion();
                           editSuggestion_ = new EditSuggestion(normalized);
                           editSuggestion_.type = SuggestionType.GHOST_TEXT;
                           editSuggestion_.isRevealed = true;
                           createSuggestionAnchors(
                              editSuggestion_.startLine,
                              editSuggestion_.startCharacter,
                              editSuggestion_.endLine,
                              editSuggestion_.endCharacter);
                           showGhostText(editSuggestion_.displayText,
                              Position.create(editSuggestion_.startLine, editSuggestion_.startCharacter));
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

      // Register all event handlers for displaying/managing suggestions
      registerHandlers();

      // Check initial assistant availability
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
      // Handle assistant availability changes
      if (!isAssistantAvailable())
      {
         // Cancel any pending auto-requests
         requestId_ = 0;
         suggestionTimer_.cancel();
         nesTimer_.cancel();
         completionTriggeredByCommand_ = false;
         events_.fireEvent(new AssistantEvent(AssistantEventType.ASSISTANT_DISABLED));
      }
   }

   /**
    * Registers all event handlers needed for displaying and managing suggestions.
    * Called once from the constructor. These handlers are always active regardless
    * of whether the assistant is available, so that suggestions created through
    * alternate mechanisms (e.g., automation tests) work correctly.
    */
   private void registerHandlers()
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

                  if (nesGutterEl != null && editSuggestion_ != null && editSuggestion_.type != SuggestionType.GHOST_TEXT)
                  {
                     event.stopPropagation();
                     event.preventDefault();
                     acceptEditSuggestion();
                     return;
                  }

                  // Check for Ctrl/Cmd + click on any NES highlight
                  boolean isModifierHeld = event.getCtrlKey() || event.getMetaKey();
                  if (isModifierHeld && editSuggestion_ != null)
                  {
                     // Convert mouse coordinates to document position
                     Position pos = display_.screenCoordinatesToDocumentPosition(
                        event.getClientX(),
                        event.getClientY());

                     if (isPositionInNesRange(pos))
                     {
                        event.stopPropagation();
                        event.preventDefault();
                        acceptEditSuggestion();
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
                  if (editSuggestion_ != null && isModifierHeld && isPositionInNesRange(pos))
                  {
                     String clickableClass = getClickableClassForType(editSuggestion_.type);
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

                     if (editSuggestion_ != null && isPositionInNesRange(pos))
                     {
                        String clickableClass = getClickableClassForType(editSuggestion_.type);
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
                  if (!ignoreNextDocumentChangeEvents_)
                  {
                     if (nativeEvent != null && doesChangeIntersectSuggestion(nativeEvent))
                     {
                        resetSuggestion();
                     }
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

                  // The rest of this handler auto-triggers new suggestions.
                  // Only proceed if assistant is available.
                  if (!isAssistantAvailable())
                     return;

                  // Check if we've been toggled off
                  if (!automaticCodeSuggestionsEnabled_)
                     return;

                  // Check preference value
                  String trigger = prefs_.assistantCompletionsTrigger().getGlobalValue();
                  if (trigger != UserPrefsAccessor.ASSISTANT_COMPLETIONS_TRIGGER_AUTO)
                     return;

                  // Respect suppression flag
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
               }),

               display_.addCapturingKeyDownHandler(new KeyDownHandler()
               {
                  @Override
                  public void onKeyDown(KeyDownEvent keyEvent)
                  {
                     NativeEvent event = keyEvent.getNativeEvent();

                     // Ignore modifier-only key events
                     if (KeyboardHelper.isModifierKey(event.getKeyCode()))
                        return;

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
                           resetSuggestion();
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

                     // Let edit suggestion (deletion, insertion, replacement) accept on Tab if applicable
                     if (editSuggestion_ != null && editSuggestion_.type != SuggestionType.GHOST_TEXT)
                     {
                        if (event.getKeyCode() == KeyCodes.KEY_TAB && canAcceptSuggestionWithTab_)
                        {
                           event.stopPropagation();
                           event.preventDefault();
                           acceptEditSuggestion();
                           return;
                        }
                        else if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
                        {
                           event.stopPropagation();
                           event.preventDefault();
                           resetSuggestion();
                           return;
                        }
                     }

                     // Respect suppression flag
                     if (completionRequestsSuspended_)
                        return;

                     // If ghost text is being displayed, accept it on a Tab key press.
                     // TODO: Let user choose keybinding for accepting ghost text?
                     if (editSuggestion_ == null || !editSuggestion_.isRevealed)
                        return;

                     // TODO: If we have a completion popup, should that take precedence?
                     if (display_.isPopupVisible())
                        return;

                     // For ghost text suggestions, check if the user typed a prefix match.
                     // If so, update the suggestion text and suppress the document change handler.
                     // This only applies to ghost text (not NES edit suggestions).
                     boolean isGhostText = editSuggestion_.type == SuggestionType.GHOST_TEXT;
                     if (isGhostText)
                     {
                        String key = EventProperty.key(keyEvent.getNativeEvent());
                        if (editSuggestion_.displayText.startsWith(key))
                        {
                           updateCompletion(key);
                           ignoreNextDocumentChangeEvents();
                           return;
                        }
                     }

                     if (event.getKeyCode() == KeyCodes.KEY_TAB)
                     {
                        event.stopPropagation();
                        event.preventDefault();
                        acceptEditSuggestion();
                     }
                     else if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
                     {
                        // Don't remove suggestion if Ace's autocomplete is active;
                        // Let Ace close its popup first
                        if (!display_.hasActiveAceCompleter())
                        {
                           hideGhostText();
                           editSuggestion_ = null;
                        }
                     }
                  }
               })

         );
   }

   private void requestNextEditSuggestions()
   {
      if (!isAssistantAvailable())
         return;

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

      boolean hasActiveSuggestion = hasGhostTextVisible() && editSuggestion_ != null && editSuggestion_.isRevealed;
      if (!hasActiveSuggestion)
         return;

      String text = editSuggestion_.displayText;
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
      editSuggestion_.partialAcceptedLength += n;

      editSuggestion_.displayText = leftoverText;
      editSuggestion_.insertText = leftoverText;
      editSuggestion_.startCharacter += n;
      editSuggestion_.endCharacter += n;

      // Bail if no suggestion anchors are set
      if (editSuggestion_.startAnchor == null)
         return;

      Timers.singleShot(() ->
      {
         ignoreNextDocumentChangeEvents();

         // Use anchored position to insert at the ghost text location,
         // not the current cursor position
         Range insertRange = Range.create(
            editSuggestion_.startAnchor.getRow(),
            editSuggestion_.startAnchor.getColumn(),
            editSuggestion_.startAnchor.getRow(),
            editSuggestion_.startAnchor.getColumn());
         display_.replaceRange(insertRange, insertedWord);

         // The anchor automatically moves forward after insertion (insertRight=false),
         // so just read its new position
         Position newStart = editSuggestion_.startAnchor.getPosition();

         // Move cursor to end of inserted word and show remaining ghost text
         display_.setCursorPosition(newStart);
         showGhostText(editSuggestion_.displayText, newStart);

         server_.assistantDidAcceptPartialCompletion(editSuggestion_.originalCompletion,
                                                   editSuggestion_.partialAcceptedLength,
                                                   new VoidServerRequestCallback());
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
      else if (editSuggestion_ != null && editSuggestion_.type != SuggestionType.GHOST_TEXT)
      {
         acceptEditSuggestion();
      }
      else if (hasGhostTextVisible() && editSuggestion_ != null && editSuggestion_.isRevealed)
      {
         acceptEditSuggestion();
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
         resetSuggestion();
      }
   }

   private void updateCompletion(String key)
   {
      int n = key.length();
      editSuggestion_.displayText = StringUtil.substring(editSuggestion_.displayText, n);
      editSuggestion_.insertText = StringUtil.substring(editSuggestion_.insertText, n);
      editSuggestion_.startCharacter += n;
      editSuggestion_.endCharacter += n;
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

   private void ignoreNextDocumentChangeEvents()
   {
      ignoreNextDocumentChangeEvents_ = true;
      Scheduler.get().scheduleDeferred(() -> ignoreNextDocumentChangeEvents_ = false);
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
    * Returns true if there is any active edit suggestion being displayed.
    */
   private boolean hasActiveSuggestion()
   {
      return diffView_ != null || editSuggestion_ != null;
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
    * Re-renders tokens for all rows that have positions in the given list.
    * This efficiently handles multiple positions on the same row by only
    * rendering each affected row once. Also removes synthetic tokens for
    * those rows before rendering.
    */
   private void renderTokensForPositions(List<Position> positions)
   {
      Set<Integer> rowsToRender = new HashSet<>();
      for (Position pos : positions)
         rowsToRender.add(pos.getRow());
      for (int row : rowsToRender)
      {
         display_.removeSyntheticTokensForRow(row);
         display_.renderTokens(row);
      }
   }

   /**
    * Shows ghost text at the specified position.
    * For single-line text, uses token splicing.
    * For multi-line text, uses token splicing for line 1 and a line widget for remaining lines.
    */
   private void showGhostText(String text, Position position)
   {
      // First, remove any existing ghost text
      hideGhostText();

      if (text == null || text.isEmpty())
         return;

      int row = position.getRow();
      int column = position.getColumn();
      ghostTextVisible_ = true;

      // Split text into lines
      String[] lines = text.split("\n", -1);

      // Register the synthetic token with the session (persists across re-tokenization)
      // This ensures coordinate conversion works correctly even after document edits
      display_.addSyntheticToken(row, column, lines[0], "ghost_text");
      display_.renderTokens(row);

      // If multi-line, create a line widget for remaining lines
      if (lines.length > 1)
      {
         StringBuilder html = new StringBuilder();
         html.append("<div class=\"ace_ghost_text\">");
         for (int i = 1; i < lines.length; i++)
         {
            if (i > 1)
               html.append("<br/>");
            // Escape HTML and preserve whitespace
            String escapedLine = lines[i]
               .replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;")
               .replace(" ", "&nbsp;");
            html.append(escapedLine);
         }
         html.append("</div>");

         ghostTextLineWidget_ = LineWidget.create("ghost_text", row, html.toString());
         ghostTextLineWidget_.setCoverGutter(false);
         display_.addLineWidget(ghostTextLineWidget_);
      }
   }

   /**
    * Removes any visible ghost text (both token and line widget).
    */
   private void hideGhostText()
   {
      if (ghostTextVisible_ && editSuggestion_ != null && editSuggestion_.startAnchor != null)
      {
         int row = editSuggestion_.startAnchor.getRow();
         display_.removeSyntheticTokensForRow(row);
         display_.renderTokens(row);
      }
      ghostTextVisible_ = false;

      if (ghostTextLineWidget_ != null)
      {
         display_.removeLineWidget(ghostTextLineWidget_);
         ghostTextLineWidget_ = null;
      }
   }

   /**
    * Returns true if ghost text is currently visible.
    */
   private boolean hasGhostTextVisible()
   {
      return ghostTextVisible_;
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
    * Preserves editSuggestion_ for re-rendering on hover.
    */
   private void clearSuggestionVisuals()
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
      renderTokensForPositions(nesTokenPositions_);
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

   public void onDismiss()
   {
      // Cancel all timers and reset suggestion state
      suspendTimer_.cancel();
      nesTimer_.cancel();
      resetSuggestion();

      // Clear diff view state
      clearDiffState();

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
   private boolean ignoreNextDocumentChangeEvents_;
   private boolean assistantDisabledInThisDocument_;

   // Diff view state (kept separate as it's a fundamentally different UI)
   private AceEditorDiffView diffView_;
   private PinnedLineWidget diffWidget_;
   private int diffMarkerId_ = -1;

   // Unified edit suggestion state (consolidates ghost text and NES)
   private boolean canAcceptSuggestionWithTab_ = false;
   private EditSuggestion editSuggestion_;

   // Custom ghost text rendering state (replaces Ace's ghost text API)
   private boolean ghostTextVisible_ = false;
   private LineWidget ghostTextLineWidget_;

   // NES visual state (markers, gutter items, etc.)
   private List<Integer> nesMarkerIds_ = new ArrayList<>();
   private List<HandlerRegistration> nesGutterRegistrations_ = new ArrayList<>();
   private List<AnchoredRange> nesClickableRanges_ = new ArrayList<>();
   private List<Position> nesTokenPositions_ = new ArrayList<>();
   private int nesBoundsMarkerId_ = -1;

   // Mouse tracking
   private int lastMouseClientX_ = 0;
   private int lastMouseClientY_ = 0;

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
