/*
 * SavePlotAsImageTargetEditor.java
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
package org.rstudio.studio.client.workbench.exportplot;

import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageFormat;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.ListBox;

public class SavePlotAsImageTargetEditor extends Composite
{
   public SavePlotAsImageTargetEditor(String defaultFormat,
                                 SavePlotAsImageContext context)
   {
      ExportPlotResources.Styles styles = ExportPlotResources.INSTANCE.styles();

      HorizontalPanel panel = new HorizontalPanel();
      panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

      imageFormatListBox_ = new ListBox();
      FormLabel imageFormatLabel = new FormLabel(constants_.imageFormatLabel(), imageFormatListBox_);
      imageFormatLabel.addStyleName(styles.exportTargetLabel());

      panel.add(imageFormatLabel);
      JsArray<SavePlotAsImageFormat> formats = context.getFormats();
      int selectedIndex = 0;
      for (int i = 0; i < formats.length(); i++)
      {
         SavePlotAsImageFormat format = formats.get(i);
         if (format.getExtension() == defaultFormat)
            selectedIndex = i;
         imageFormatListBox_.addItem(format.getName(), format.getExtension());
      }
      imageFormatListBox_.setSelectedIndex(selectedIndex);
      imageFormatListBox_.addStyleName(styles.imageFormatListBox());
      panel.add(imageFormatListBox_);

      initWidget(panel);
   }

   public String getFormat()
   {
      return imageFormatListBox_.getValue(
                                 imageFormatListBox_.getSelectedIndex());
   }

   public String getDefaultExtension()
   {
      return "." + getFormat();
   }

   private ListBox imageFormatListBox_;
   private static final ExportPlotConstants constants_ = GWT.create(ExportPlotConstants.class);
}
