/*
 * AceEditorPopupDiffView.java
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

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.common.filetypes.TextFileType;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Abstract popup panel that displays a diff view of code using an embedded AceEditor.
 * Subclasses should implement accept() and discard() methods to handle user actions.
 */
public abstract class AceEditorPopupDiffView extends PopupPanel
{
   public AceEditorPopupDiffView(String text, TextFileType fileType)
   {
      super(false, false); // disable auto-hide, we'll handle it manually
      setStyleName("ace-editor-popup-diff-view");

      mainPanel_ = new FlowPanel();
      mainPanel_.getElement().getStyle().setProperty("boxShadow", "0 2px 8px rgba(0,0,0,0.15)");

      // Add padding to create a larger mouse target area around the popup
      // Use larger top padding to bridge the gap to the highlight
      mainPanel_.getElement().getStyle().setProperty("padding", "20px 10px 10px 10px");
      mainPanel_.getElement().getStyle().setProperty("marginTop", "-25px"); // Extend hit area upward to overlap highlight

      // Create inner container that holds both editor and status bar
      innerContainer_ = new SimplePanel();
      innerContainer_.getElement().getStyle().setProperty("backgroundColor", "rgba(128, 160, 128, 0.2)");
      innerContainer_.getElement().getStyle().setProperty("marginTop", "2px");
      innerContainer_.getElement().getStyle().setProperty("marginBottom", "4px");

      // Create a flow panel for the inner content
      FlowPanel innerContent = new FlowPanel();

      // Create editor container
      editorContainer_ = new SimplePanel();
      editorContainer_.getElement().getStyle().setProperty("overflow", "auto");
      editorContainer_.getElement().getStyle().setProperty("filter", "contrast(0.85)");

      // Create editor instance
      editor_ = new AceEditor();
      editor_.setReadOnly(true);
      editor_.setShowLineNumbers(false);
      editor_.setFileType(fileType);
      editor_.setCode(text, false);
      editorContainer_.setWidget(editor_.asWidget());

      // Track editor focus to prevent auto-hide while interacting with it
      editor_.addEditorFocusHandler((event) ->
      {
         editorHasFocus_ = true;
      });

      editor_.addEditorBlurHandler((event) ->
      {
         editorHasFocus_ = false;
         checkAutoHide();
      });

      // Create status bar with clickable text - style to match Ace editor
      statusBar_ = new HorizontalPanel();
      statusBar_.getElement().getStyle().setProperty("width", "100%");
      statusBar_.getElement().getStyle().setProperty("height", "24px");
      statusBar_.getElement().getStyle().setProperty("maxHeight", "24px");
      statusBar_.getElement().getStyle().setProperty("padding", "2px 8px");
      statusBar_.getElement().getStyle().setProperty("display", "flex");
      statusBar_.getElement().getStyle().setProperty("alignItems", "center");
      statusBar_.getElement().getStyle().setProperty("justifyContent", "flex-end");
      statusBar_.getElement().getStyle().setProperty("boxSizing", "border-box");
      statusBar_.setSpacing(12);

      Label discardLabel = new Label("Discard");
      discardLabel.getElement().getStyle().setProperty("fontSize", "11px");
      discardLabel.getElement().getStyle().setProperty("cursor", "pointer");
      discardLabel.getElement().getStyle().setProperty("color", "#999");
      discardLabel.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            discard();
            hide();
         }
      });

      Label applyLabel = new Label("Apply");
      applyLabel.getElement().getStyle().setProperty("fontSize", "11px");
      applyLabel.getElement().getStyle().setProperty("cursor", "pointer");
      applyLabel.getElement().getStyle().setProperty("color", "#4a90e2");
      applyLabel.getElement().getStyle().setProperty("fontWeight", "500");
      applyLabel.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            accept();
            hide();
         }
      });

      statusBar_.add(discardLabel);
      statusBar_.add(applyLabel);

      // Add editor and status bar to inner content
      innerContent.add(editorContainer_);
      innerContent.add(statusBar_);

      // Set inner content into inner container
      innerContainer_.setWidget(innerContent);

      // Add inner container to main panel
      mainPanel_.add(innerContainer_);

      setWidget(mainPanel_);

      // Add mouse tracking to handle auto-hide
      DomUtils.addEventListener(mainPanel_.getElement(), "mouseenter", false, (event) ->
      {
         mouseOverPopup_ = true;
      });

      DomUtils.addEventListener(mainPanel_.getElement(), "mouseleave", false, (event) ->
      {
         mouseOverPopup_ = false;
         checkAutoHide();
      });
   }

   /**
    * Shows the popup and computes dimensions based on content.
    * @param text the text to display in the editor
    */
   public void showWithContent(String text)
   {
      mouseOverPopup_ = false;
      editorHasFocus_ = false;

      // Display the text in the editor
      editor_.setCode(text, false);

      // Compute height based on number of lines
      int lineCount = countLines(text);
      int lineHeight = getLineHeightFromEditor();
      int maxLines = 20; // Maximum lines to show before scrolling
      int actualLines = Math.min(lineCount, maxLines);
      int editorHeight = actualLines * lineHeight;

      // Compute width based on longest line
      int longestLineLength = getLongestLineLength(text);
      double charWidth = getCharWidthFromEditor();
      int minWidth = 200; // Minimum width
      int maxWidth = 800; // Maximum width
      int gutterWidth = 10; // Small padding for no line numbers
      int scrollbarWidth = 20; // Extra space for potential scrollbar
      int calculatedWidth = (int) (longestLineLength * charWidth) + gutterWidth + scrollbarWidth;
      int editorWidth = Math.max(minWidth, Math.min(maxWidth, calculatedWidth));

      // Set the editor container dimensions
      editorContainer_.getElement().getStyle().setProperty("height", editorHeight + "px");
      editorContainer_.getElement().getStyle().setProperty("maxHeight", (maxLines * lineHeight) + "px");
      editorContainer_.getElement().getStyle().setProperty("width", editorWidth + "px");

      // Set the main panel width to match
      mainPanel_.getElement().getStyle().setProperty("width", editorWidth + "px");

      show();

      // Force the editor to resize after showing and apply theme styling
      Scheduler.get().scheduleDeferred(() ->
      {
         editor_.onResize();
         applyStatusBarStyling();
      });
   }

   /**
    * Sets whether the mouse is over the highlight region.
    */
   public void setMouseOverHighlight(boolean mouseOver)
   {
      mouseOverHighlight_ = mouseOver;
      if (!mouseOver)
      {
         checkAutoHide();
      }
   }

   /**
    * Called when the user accepts the diff.
    */
   protected abstract void accept();

   /**
    * Called when the user discards the diff.
    */
   protected abstract void discard();

   private void checkAutoHide()
   {
      // Delay the check slightly to allow mouseenter events to fire first
      // This prevents premature hiding when moving from highlight to popup
      Scheduler.get().scheduleDeferred(() ->
      {
         // Hide the popup if mouse is not over either the popup or the highlight,
         // and the editor doesn't have focus
         if (!mouseOverPopup_ && !mouseOverHighlight_ && !editorHasFocus_)
         {
            hide();
         }
      });
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
      if (text == null || text.isEmpty())
         return 1;

      int count = 1;
      for (int i = 0; i < text.length(); i++)
      {
         if (text.charAt(i) == '\n')
            count++;
      }
      return count;
   }

   private int getLongestLineLength(String text)
   {
      if (text == null || text.isEmpty())
         return 0;

      int maxLength = 0;
      int currentLength = 0;

      for (int i = 0; i < text.length(); i++)
      {
         if (text.charAt(i) == '\n')
         {
            maxLength = Math.max(maxLength, currentLength);
            currentLength = 0;
         }
         else if (text.charAt(i) == '\t')
         {
            // Assume tab width of 4 for calculation
            currentLength += 4;
         }
         else
         {
            currentLength++;
         }
      }

      // Check the last line if text doesn't end with newline
      maxLength = Math.max(maxLength, currentLength);

      return maxLength;
   }

   private int getLineHeightFromEditor()
   {
      // Try to get line height from the editor's renderer
      // Default to a reasonable fallback if we can't get it
      try
      {
         AceEditorWidget widget = editor_.getWidget();
         double lineHeight = widget.getEditor().getRenderer().getLineHeight();
         return (int) Math.ceil(lineHeight);
      }
      catch (Exception e)
      {
         // Fallback to a reasonable default (typically 15-16px)
         return 16;
      }
   }

   private double getCharWidthFromEditor()
   {
      // Try to get character width from the editor's renderer
      // Default to a reasonable fallback if we can't get it
      try
      {
         AceEditorWidget widget = editor_.getWidget();
         return widget.getEditor().getRenderer().getCharacterWidth();
      }
      catch (Exception e)
      {
         // Fallback to a reasonable default (typically 7-8px for most fonts)
         return 7.5;
      }
   }

   private AceEditor editor_;
   private FlowPanel mainPanel_;
   private SimplePanel innerContainer_;
   private SimplePanel editorContainer_;
   private HorizontalPanel statusBar_;
   private boolean mouseOverPopup_ = false;
   private boolean mouseOverHighlight_ = false;
   private boolean editorHasFocus_ = false;
}
