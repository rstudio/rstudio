/*
 * DialogTabLayoutPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.theme;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.TabLayoutPanel;

public class DialogTabLayoutPanel extends TabLayoutPanel
{
   public DialogTabLayoutPanel()
   {
      super(14, Unit.PX);
      
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      addStyleName(styles.dialogTabPanel());
      
      // we need to center the tabs and overlay them on the top edge of the
      // content; to do this, it is necessary to nuke a couple of the inline
      // styles used by the default GWT tab panel. 
      Element tabOuter = (Element) getElement().getChild(1);
      tabOuter.getStyle().setOverflow(Overflow.VISIBLE);
      Element tabInner = (Element) tabOuter.getFirstChild();
      tabInner.getStyle().clearWidth();
   }

}
