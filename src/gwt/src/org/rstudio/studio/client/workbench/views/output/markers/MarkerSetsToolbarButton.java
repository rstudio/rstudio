/*
 * MarkerSetsToolbarButton.java
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
package org.rstudio.studio.client.workbench.views.output.markers;

import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.MenuItem;

public class MarkerSetsToolbarButton extends ToolbarMenuButton
                                     implements HasValueChangeHandlers<String>
{
   public MarkerSetsToolbarButton()
   {
      super(ToolbarButton.NoText,
            ToolbarButton.NoTitle,
            StandardIcons.INSTANCE.empty_command(),
            new ScrollableToolbarPopupMenu());
     
      updateActiveMarkerSet(null);
      
      setTitle("Switch active marker list");
   }
   
   public void updateActiveMarkerSet(String set)
   {
      if (set == null)
         setText("(No markers)");
      else
         setText(set);
   }
    
   public void updateAvailableMarkerSets(String[] sets)
   {  
      ToolbarPopupMenu menu = getMenu();
      menu.clearItems();
      for (final String set : sets)
      {
         // command for selection
         Scheduler.ScheduledCommand cmd =  new Scheduler.ScheduledCommand()
         {  
            @Override
            public void execute()
            {
               ValueChangeEvent.fire(MarkerSetsToolbarButton.this, set); 
            }
         };
         
         SafeHtml menuHTML = new SafeHtmlBuilder()
                              .appendHtmlConstant(set).toSafeHtml();
         menu.addItem(new MenuItem(menuHTML, cmd));
          
      }
   }
   
   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }
}
