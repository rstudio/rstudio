/*
 * DialogTabLayoutPanel.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import org.rstudio.core.client.widget.FocusHelper;

import java.util.ArrayList;

public class DialogTabLayoutPanel extends TabLayoutPanel
{
   public DialogTabLayoutPanel(String tabListLabel)
   {
      super(14, Unit.PX, tabListLabel);

      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      addStyleName(styles.dialogTabPanel());

      // we need to center the tabs and overlay them on the top edge of the
      // content; to do this, it is necessary to nuke a couple of the inline
      // styles used by the default GWT tab panel.
      Element tabOuter = (Element) getElement().getChild(1);
      tabOuter.getStyle().setOverflow(Overflow.VISIBLE);
      Element tabInner = (Element) tabOuter.getFirstChild();
      tabInner.getStyle().clearWidth();

      // if the tab panel is the first focusable control in dialog we must keep the
      // selected Tab marked as first when selection changes
      focus_ = new FocusHelper(getElement());
      addSelectionHandler(selectionEvent ->
      {
         if (focus_.containsFirst())
         {
            ArrayList<Element> focusable = DomUtils.getFocusableElements(getElement());
            if (focusable.size() == 0)
            {
               Debug.logWarning("No potentially focusable controls found in DialogTabLayoutPanel");
               return;
            }
            focus_.setFirst(focusable.get(0));
         }
      });
   }

   public void add(VerticalTabPanel child, String text, String tabId)
   {
      super.add(child, text);
      setTabId(child, ElementIds.getElementId(VerticalTabPanel.getTabId(tabId)));
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
      Roles.getTabpanelRole().set(child.getElement());
      Roles.getTabpanelRole().setAriaLabelledbyProperty(child.getElement(),
            Id.of(ElementIds.getElementId(VerticalTabPanel.getTabId(tabId))));
   }

   private final FocusHelper focus_;
}
