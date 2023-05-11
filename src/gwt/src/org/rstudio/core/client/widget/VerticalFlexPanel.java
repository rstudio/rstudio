/*
 * VerticalFlexPanel.java
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

package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.ui.FlowPanel;

public class VerticalFlexPanel extends FlowPanel
{
   public VerticalFlexPanel() 
   {
      this(4);
   }

   public VerticalFlexPanel(int gap)
   {
      super();
      this.getElement().getStyle().setDisplay(Display.FLEX);
      this.getElement().getStyle().setProperty("alignItems", "stretch");
      this.getElement().getStyle().setProperty("justifyContent", "center");
      this.getElement().getStyle().setProperty("flexDirection", "column");
      this.getElement().getStyle().setProperty("gap", gap + "px");
   }
}
