/*
 * AceEditorDiffView.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.diff.JsDiff;
import org.rstudio.core.client.diff.JsDiff.Delta;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import jsinterop.base.JsArrayLike;

public abstract class AceEditorDiffView
{
   protected abstract void apply();
   protected abstract void discard();

   // Unfortunately, sizing computations here are a bit awkward.
   //
   // - Ace can't compute its line height until after it's tried to render some lines.
   // - It can't render until it's been attached to the DOM.
   // - Line widgets want to know the widget height when they're created.
   //
   // To wit, we make the editor line height an overridable method, and set it
   // to a reasonable default. Clients can override this method if they want
   // to provide a more accurate / known line height.
   public double getLineHeight()
   {
      return 19;
   }

   public AceEditorDiffView(String originalText,
                            String replacementText,
                            TextFileType fileType)
   {
      computeDeltas(originalText, replacementText);

      Styles styles = RES.styles();
      styles.ensureInjected();

      mainPanel_ = new FlowPanel();
      mainPanel_.setStyleName(styles.aceEditorDiffView());

      // Create editor container
      editorContainer_ = new SimplePanel();
      editorContainer_.setStyleName(styles.editorContainer());

      // Create editor instance
      editor_ = new AceEditor();
      editor_.setReadOnly(true);
      editor_.setShowLineNumbers(false);
      editor_.setFileType(fileType);
      editor_.setCode(editorText_, false);
      editor_.setPadding(8);
      editor_.getElement().getStyle().setMarginTop(6, Unit.PX);
      editorContainer_.setWidget(editor_.asWidget());

      // Set editor height based on number of lines
      int lineCount = StringUtil.countMatches(editorText_, '\n') + 1;
      editor_.getElement().getStyle().setHeight(lineCount * getLineHeight(), Unit.PX);

      // Set up line height -- needs to happen after load, since font size is not known
      // until the Ace renderer has done a first render pass.
      Scheduler.get().scheduleDeferred(() ->
      {
         applyStatusBarStyling();
         highlightDiffs();
      });

      // Create status bar with clickable text - style to match Ace editor
      // Note that we need to use 'raw' event listeners as GWT event listeners
      // will not function within line widgets
      statusBar_ = new HorizontalPanel();
      statusBar_.setStyleName(styles.statusBar());
      statusBar_.setSpacing(12);

      Label discardLabel = new Label("Discard");
      discardLabel.setStyleName(styles.discardLabel());
      DomUtils.addEventListener(discardLabel.getElement(), "click", false, (event) ->
      {
         discard();
      });

      Label applyLabel = new Label("Apply");
      applyLabel.setStyleName(styles.applyLabel());
      DomUtils.addEventListener(applyLabel.getElement(), "click", false, (event) ->
      {
         apply();
      });

      statusBar_.add(discardLabel);
      statusBar_.add(applyLabel);

      // Add editor and status bar directly to main panel
      mainPanel_.add(editorContainer_);
      mainPanel_.add(statusBar_);
   }

   private void computeDeltas(String originalText, String replacementText)
   {
      // Compute diffs.
      JsArrayLike<Delta> deltas = JsDiff.diffChars(originalText, replacementText);
      StringBuilder builder = new StringBuilder();

      // Iterate through the diffs, and build ranges for additions and deletions.
      for (int i = 0, n = deltas.getLength(); i < n; i++)
      {
         // If this was an addition or a removal, create a range and add a marker.
         Delta delta = deltas.getAt(i);
         if (delta.added || delta.removed)
         {
            // Compute the range in the replacement text. Do this by computing the
            // start position of the current builder prefix, and the end position
            // after adding the current delta value.
            String prefix  = builder.toString();
            String postfix = prefix + delta.value;
            int startLine = countLines(prefix) - 1;
            int startChar = prefix.length() - prefix.lastIndexOf('\n') - 1;
            int endLine = countLines(postfix) - 1;
            int endChar = postfix.length() - postfix.lastIndexOf('\n') - 1;
            Range range = Range.create(startLine, startChar, endLine, endChar);
            if (delta.added)
               additions_.add(range);
            else
               deletions_.add(range);
         }

         // Build the prefix string.
         builder.append(delta.value);
      }

      // Set the editor text.
      editorText_ = builder.toString();
   }

   /**
    * Returns the widget that can be added to the display.
    */
   public FlowPanel getWidget()
   {
      return mainPanel_;
   }

   /**
    * Highlights the differences in the editor using Ace markers.
    */
   private void highlightDiffs()
   {
      // Clear any existing markers first
      clearDiffMarkers();

      // Apply addition markers
      for (Range range : additions_)
      {
         int markerId = editor_.getSession().addMarker(range, "ace_diff-added", "text", false);
         diffMarkerIds_.add(markerId);
      }

      // Apply deletion markers
      for (Range range : deletions_)
      {
         int markerId = editor_.getSession().addMarker(range, "ace_diff-removed", "text", false);
         diffMarkerIds_.add(markerId);
      }
   }

   /**
    * Clears all diff highlight markers from the editor.
    */
   private void clearDiffMarkers()
   {
      for (int markerId : diffMarkerIds_)
      {
         editor_.getSession().removeMarker(markerId);
      }

      diffMarkerIds_.clear();
   }

   /**
    * Cleans up resources when the widget is detached.
    */
   public void detach()
   {
      clearDiffMarkers();
   }

   private void applyStatusBarStyling()
   {
      // Get the background color from the editor's content area
      try
      {
         Element editorElement = editor_.getElement();
         Element aceContent = DomUtils.getElementsByClassName(editorElement, "ace_scroller").length > 0
               ? DomUtils.getElementsByClassName(editorElement, "ace_scroller")[0]
               : null;

         if (aceContent != null)
         {
            String bgColor = aceContent.getStyle().getBackgroundColor();
            if (bgColor != null && !bgColor.isEmpty())
            {
               statusBar_.getElement().getStyle().setProperty("backgroundColor", bgColor);
            }
         }
      }
      catch (Exception e)
      {
         // Ignore styling errors - will just use default
      }
   }

   private int countLines(String text)
   {
      return StringUtil.countMatches(text, '\n') + 1;
   }

   private AceEditor editor_;
   private FlowPanel mainPanel_;
   private SimplePanel editorContainer_;
   private HorizontalPanel statusBar_;
   private String editorText_;
   private List<Range> additions_ = new ArrayList<>();
   private List<Range> deletions_ = new ArrayList<>();
   private List<Integer> diffMarkerIds_ = new ArrayList<>();

   // Boilerplate ----

   public interface Resources extends ClientBundle
   {
      @Source("AceEditorDiffView.css")
      Styles styles();
   }

   public interface Styles extends CssResource
   {
      String aceEditorDiffView();
      String mainPanel();
      String innerContainer();
      String editorContainer();
      String statusBar();
      String discardLabel();
      String applyLabel();
   }

   private static final Resources RES = GWT.create(Resources.class);

   static
   {
      RES.styles().ensureInjected();
   }

}
