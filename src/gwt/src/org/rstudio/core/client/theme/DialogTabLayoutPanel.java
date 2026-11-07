/*
 * DialogTabLayoutPanel.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;

public class DialogTabLayoutPanel extends TabLayoutPanel
{
   public DialogTabLayoutPanel(String tabListLabel)
   {
      this(tabListLabel, true);
   }

   public DialogTabLayoutPanel(String tabListLabel, boolean showTabs)
   {
      super(showTabs ? 14 : 0, Unit.PX, tabListLabel);
      showTabs_ = showTabs;

      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      addStyleName(styles.dialogTabPanel());

      if (showTabs)
      {
         // we need to center the tabs and overlay them on the top edge of the
         // content; to do this, it is necessary to nuke a couple of the inline
         // styles used by the default GWT tab panel.
         Element tabOuter = (Element) getElement().getChild(1);
         tabOuter.getStyle().setOverflow(Overflow.VISIBLE);
         Element tabInner = (Element) tabOuter.getFirstChild();
         tabInner.getStyle().clearWidth();
      }
      else
      {
         // Hide the tab bar's layer rather than its element: each layout pass
         // clears an inline display:none on visible layers, which left the
         // tab focusable and announced by screen readers.
         LayoutPanel panel = (LayoutPanel) getWidget();
         panel.setWidgetVisible(panel.getWidget(0), false);
         addStyleName(styles.dialogTabPanelNoTabs());
      }
   }

   public void add(VerticalTabPanel child, String text, String tabId)
   {
      super.add(child, text);
      setTabId(child, ElementIds.getElementId(VerticalTabPanel.getTabId(tabId)));

      // Without a visible tab, the content isn't a tabpanel of anything.
      if (!showTabs_)
      {
         Roles.getTabpanelRole().remove(child.getElement());
         Roles.getTabpanelRole().removeAriaLabelledbyProperty(child.getElement());
      }
   }

   /**
    * Add a widget as a tab panel, applying appropriate Aria annotations
    * @param child widget to add as a tab panel
    * @param text name of the tab
    * @param tabId unique identifier for element
    */
   public void addWidgetPanel(Widget child, String text, String tabId)
   {
      super.add(child, text);
      setTabId(child, ElementIds.getElementId(VerticalTabPanel.getTabId(tabId)));
      if (!showTabs_)
         return;
      Roles.getTabpanelRole().set(child.getElement());
      Roles.getTabpanelRole().setAriaLabelledbyProperty(child.getElement(),
            Id.of(ElementIds.getElementId(VerticalTabPanel.getTabId(tabId))));
   }

   private final boolean showTabs_;
}
