/*
 * ChunkConditionBar.java
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
import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkConditionBar extends Composite
                               implements EditorThemeListener
{
   private static ChunkConditionBarUiBinder uiBinder = GWT
         .create(ChunkConditionBarUiBinder.class);

   interface ChunkConditionBarUiBinder
         extends UiBinder<Widget, ChunkConditionBar>
   {
   }

   public interface ConditionStyle extends CssResource
   {
      String contents();
   }

   public ChunkConditionBar(JsArray<JsArrayEx> conditions, ChunkOutputSize chunkOutputSize)
   {
      chunkOutputSize_ = chunkOutputSize;

      initWidget(uiBinder.createAndBindUi(this));
      for (int i = 0; i < conditions.length(); i++)
      {
         HorizontalPanel bar;
         VerticalPanel contents;
         if (conditions.get(i).getInt(0) == CONDITION_MESSAGE) 
         {
            bar = messageBar_;
            contents = messages_;
         }
         else if (conditions.get(i).getInt(0) == CONDITION_WARNING)
         {
            bar = warningBar_;
            contents = warnings_;
         }
         else
         {
            continue;
         }
         bar.setVisible(true);
         Label entry = new Label(conditions.get(i).getString(1));
         entry.addStyleName(style.contents());
         contents.add(entry);
      }

      if (chunkOutputSize_ != ChunkOutputSize.Full)
      {
         // limit bar width to plot width
         getElement().getStyle().setProperty("maxWidth", 
               "" + ChunkOutputUi.MAX_PLOT_WIDTH + "px");
      }
   }
   
   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      // create a background color by softening the foreground
      ColorUtil.RGBColor foreground = 
            ColorUtil.RGBColor.fromCss(colors.foreground);
      ColorUtil.RGBColor background = new ColorUtil.RGBColor(
            foreground.red(), foreground.green(), foreground.blue(), 0.075);

      panel_.getElement().getStyle().setBackgroundColor(background.asRgb());
   }

   @UiField HorizontalPanel messageBar_;
   @UiField HorizontalPanel warningBar_;
   @UiField VerticalPanel messages_;
   @UiField VerticalPanel warnings_;
   @UiField ConditionStyle style;
   @UiField VerticalPanel panel_;
   
   // symmetric with enum on server
   public final static int CONDITION_MESSAGE = 0;
   public final static int CONDITION_WARNING = 1;

   final ChunkOutputSize chunkOutputSize_;
}
