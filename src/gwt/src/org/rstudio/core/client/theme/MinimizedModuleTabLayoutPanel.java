/*
 * MinimizedModuleTabLayoutPanel.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

/**
 * This is the minimized version of the Data|File|Plot|Package|Help group
 */
public class MinimizedModuleTabLayoutPanel
      extends MinimizedWindowFrame
   implements HasSelectionHandlers<Integer>
{
   public MinimizedModuleTabLayoutPanel(String accessibleName)
   {
      super(null, accessibleName, new HorizontalPanel());
      accessibleName_ = accessibleName;
      addStyleName(ThemeResources.INSTANCE.themeStyles().moduleTabPanel());
      addStyleName(ThemeResources.INSTANCE.themeStyles().minimized());
   }

   public void setTabs(String[] tabNames)
   {
      HorizontalPanel horiz = (HorizontalPanel) getExtraWidget();
      horiz.clear();
      Roles.getTablistRole().set(horiz.getElement());
      Roles.getTablistRole().setAriaLabelProperty(horiz.getElement(), accessibleName_ + " minimized");

      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      for (int i = 0; i < tabNames.length; i++)
      {
         String tabName = tabNames[i];
         if (tabName == null)
            continue;
         ModuleTabLayoutPanel.ModuleTab tab
               = new ModuleTabLayoutPanel.ModuleTab(tabName, styles, false, true /*minimized*/);
         tab.addStyleName("gwt-TabLayoutPanelTab");
         tab.getElement().setId(ElementIds.getUniqueElementId(tab.getTabId()));
         final Integer thisIndex = i;
         tab.addClickHandler(event ->
         {
            event.preventDefault();
            SelectionEvent.fire(
                  MinimizedModuleTabLayoutPanel.this,
                  thisIndex);
         });
         horiz.add(tab);
      }
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }
   
   private final String accessibleName_;
}
