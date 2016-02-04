/*
 * ChunkOutputWidget.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutput;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ChunkOutputWidget extends Composite
{

   private static ChunkOutputWidgetUiBinder uiBinder = GWT
         .create(ChunkOutputWidgetUiBinder.class);

   interface ChunkOutputWidgetUiBinder
         extends UiBinder<Widget, ChunkOutputWidget>
   {
      
   }

   public ChunkOutputWidget(CommandWithArg<Integer> onRenderCompleted)
   {
      initWidget(uiBinder.createAndBindUi(this));
      applyCachedEditorStyle();
      
      onRenderCompleted_ = onRenderCompleted;
      
      interrupt_.setResource(RStudioGinjector.INSTANCE.getCommands()
            .interruptR().getImageResource());
   }

   public void showChunkOutput(RmdChunkOutput output)
   {
      // clean up old frame if needed
      if (frame_ != null)
         frame_.removeFromParent();

      frame_ = new ChunkOutputFrame();
      frame_.getElement().getStyle().setHeight(100, Unit.PCT);
      frame_.getElement().getStyle().setWidth(100, Unit.PCT);
      root_.add(frame_);

      frame_.loadUrl(output.getUrl(), new Command() 
      {
         @Override
         public void execute()
         {
            if (state_ != CHUNK_RENDERING)
               return;
            state_ = CHUNK_RENDERED;
            applyCachedEditorStyle();
            onRenderCompleted_.execute(
                  frame_.getDocument().getDocumentElement().getScrollHeight());
         };
      });
      interrupt_.setVisible(false);
      state_ = CHUNK_RENDERING;
   }
   
   public void setChunkExecuting()
   {
      if (state_ == CHUNK_EXECUTING)
         return;
      state_ = CHUNK_EXECUTING;
      getElement().getStyle().setBackgroundColor(s_busyColor);
      interrupt_.setVisible(true);
   }
   
   public void applyCachedEditorStyle()
   {
      if (!isEditorStyleCached())
         return;
      Style frameStyle = getElement().getStyle();
      frameStyle.setBorderColor(s_outlineColor);
      frameStyle.setBackgroundColor(s_backgroundColor);
      if (state_ == CHUNK_RENDERED)
      {
         Style bodyStyle = frame_.getDocument().getBody().getStyle();
         bodyStyle.setColor(s_color);
      }
   }
   
   public static void cacheEditorStyle(Element editorContainer, 
         Style editorStyle)
   {
      s_backgroundColor = editorStyle.getBackgroundColor();
      s_color = editorStyle.getColor();
      s_outlineColor = DomUtils.extractCssValue("ace_print-margin", 
            "backgroundColor");
      JsArrayString classes = JsArrayString.createArray().cast();
      classes.push("ace_marker-layer");
      classes.push("ace_foreign_line");
      s_busyColor = DomUtils.extractCssValue(classes, "backgroundColor");
   }
   
   public static boolean isEditorStyleCached()
   {
      return s_backgroundColor != null &&
             s_color != null &&
             s_outlineColor != null;
   }
   
   @UiField Image interrupt_;
   @UiField HTMLPanel root_;
   
   private ChunkOutputFrame frame_;
   
   private int state_ = CHUNK_EMPTY;
   
   private CommandWithArg<Integer> onRenderCompleted_;

   private static String s_outlineColor    = null;
   private static String s_backgroundColor = null;
   private static String s_color           = null;
   private static String s_busyColor       = null;
   
   public final static int CHUNK_EMPTY     = 0;
   public final static int CHUNK_EXECUTING = 1;
   public final static int CHUNK_RENDERING = 2;
   public final static int CHUNK_RENDERED  = 3;
}
