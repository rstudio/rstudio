/*
 * ChunkOutputThumbnail.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.ColorUtil;
import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
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

      // move the title down if there's no subtitle
      if (StringUtil.isNullOrEmpty(subtitle))
         title_.getElement().getStyle().setMarginTop(45, Unit.PCT);
      else
         subtitle_.setText(subtitle);

      // add a backdrop if we have one
      if (backdrop != null)
         backdrop_.add(backdrop);
      
      // apply the initial set of colors
      onEditorThemeChanged(colors);
   }

   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      if (colors == null) {
         return;
      }
      
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
            "linear-gradient(rgba(" + tuple + ", 0.0) 0%, "   +
                            "rgba(" + tuple + ", 0.65) 33%, " +
                            "rgba(" + tuple + ", 1.0) 80%, "  +
                            "rgba(" + tuple + ", 1.0) 100%)");
   }
  
   @UiField Label title_;
   @UiField Label subtitle_;
   @UiField SimplePanel backdrop_;
   @UiField SimplePanel mask_;
}
