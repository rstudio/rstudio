/*
 * ChunkOutputThumbnail.java
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

import org.rstudio.core.client.ColorUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkOutputThumbnail extends Composite
                                  implements EditorThemeListener
{

   private static ChunkOutputThumbnailUiBinder uiBinder = GWT
         .create(ChunkOutputThumbnailUiBinder.class);

   interface ChunkOutputThumbnailUiBinder
         extends UiBinder<Widget, ChunkOutputThumbnail>
   {
   }

   public ChunkOutputThumbnail(String title, String subtitle, Widget backdrop,
         EditorThemeListener.Colors colors)
   {
      initWidget(uiBinder.createAndBindUi(this));
      title_.setText(title);
      subtitle_.setText(subtitle);
      if (backdrop != null)
         backdrop_.add(backdrop);
      onEditorThemeChanged(colors);
   }

   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      getElement().getStyle().setBackgroundColor(colors.surface);
      setMaskColor(colors.surface);
      if (backdrop_ != null && backdrop_ instanceof EditorThemeListener)
         ((EditorThemeListener)backdrop_).onEditorThemeChanged(colors);
   }
   
   private void setMaskColor(String color)
   {
      ColorUtil.RGBColor rgb = ColorUtil.RGBColor.fromCss(color);
      String tuple = rgb.red() + ", " + rgb.green() + ", " + rgb.blue();
      mask_.getElement().getStyle().setProperty("backgroundImage", 
            "linear-gradient(rgba(" + tuple + ", 0.1) 0%, "   +
                            "rgba(" + tuple + ", 0.75) 33%, " +
                            "rgba(" + tuple + ", 1.0) 80%, "  +
                            "rgba(" + tuple + ", 1.0) 100%)");
   }
  
   @UiField Label title_;
   @UiField Label subtitle_;
   @UiField SimplePanel backdrop_;
   @UiField SimplePanel mask_;
}
