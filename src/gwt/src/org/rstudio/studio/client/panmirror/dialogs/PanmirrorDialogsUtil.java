/*
 * PanmirrorDialogsUtil.java
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


package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.FormLabel;

import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class PanmirrorDialogsUtil
{
   public static TextBox addTextBox(VerticalTabPanel panel, String id, String label, String initialValue)
   {
      return addTextBox(panel, id, new FormLabel(label), initialValue);
   }
   
   public static TextBox addTextBox(VerticalTabPanel panel, String id, FormLabel label, String initialValue)
   {
      panel.add(label);
      TextBox textBox = new TextBox();
      textBox.getElement().setId(id);
      label.setFor(textBox);
      setFullWidthStyles(textBox);
      textBox.setText(initialValue);
      panel.add(textBox);
      return textBox;
   }
   
   public static void setFullWidthStyles(Widget widget)
   {
      widget.addStyleName(RES.styles().fullWidth());
      widget.addStyleName(RES.styles().spaced());
   }
   
   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;

}
