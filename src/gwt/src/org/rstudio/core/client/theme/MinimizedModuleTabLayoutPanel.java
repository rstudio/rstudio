/*
 * MinimizedModuleTabLayoutPanel.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

/**
 * This is the minimized version of the Data|File|Plot|Package|Help group
 */
public class MinimizedModuleTabLayoutPanel
      extends MinimizedWindowFrame
   implements HasSelectionHandlers<Integer>
{
   public MinimizedModuleTabLayoutPanel()
   {
      super(null, new HorizontalPanel());
      addStyleName(ThemeResources.INSTANCE.themeStyles().moduleTabPanel());
      addStyleName(ThemeResources.INSTANCE.themeStyles().minimized());
   }

   public void setTabs(String[] tabNames)
   {
      HorizontalPanel horiz = (HorizontalPanel) getExtraWidget();
      horiz.clear();

      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      for (int i = 0; i < tabNames.length; i++)
      {
         String tabName = tabNames[i];
         if (tabName == null)
            continue;
         ModuleTabLayoutPanel.ModuleTab tab
               = new ModuleTabLayoutPanel.ModuleTab(tabName, styles, false);
         tab.addStyleName("gwt-TabLayoutPanelTab");
         final Integer thisIndex = i;
         tab.addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               event.preventDefault();
               SelectionEvent.fire(
                     MinimizedModuleTabLayoutPanel.this,
                     thisIndex);
            }
         });

         horiz.add(tab);
      }
   }

   public HandlerRegistration addSelectionHandler(
         SelectionHandler<Integer> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }
}
