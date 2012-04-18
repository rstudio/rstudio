/*
 * PrefsWidgetHelper.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.prefs;

import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.SelectWidget;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;

public class PrefsWidgetHelper
{
   public static void addHelpButton(SelectWidget selectWidget, 
                                    String rstudioLinkName)
   {
      selectWidget.addWidget(createHelpButton(rstudioLinkName));
   }
  
   public static HelpButton createHelpButton(String rstudioLinkName)
   {
      HelpButton helpButton = new HelpButton(rstudioLinkName);
      Style style = helpButton.getElement().getStyle();
      style.setMarginTop(3, Unit.PX);
      style.setMarginLeft(4, Unit.PX);
      return helpButton;
   }
}
