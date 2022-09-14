/*
 * HorizontalFlexPanel.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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

public class HorizontalFlexPanel extends FlowPanel
{
   public HorizontalFlexPanel() {
      super();
      this.getElement().getStyle().setDisplay(Display.FLEX);
      this.getElement().getStyle().setProperty("alignItems", "center");
      this.getElement().getStyle().setProperty("gap", "4px");
   }
}
