/*
 * AddinsToolbarButton.java
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
package org.rstudio.studio.client.application.ui.addins;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.MapUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CustomMenuItemSeparator;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

public class AddinsToolbarButton extends ToolbarMenuButton
{
   public AddinsToolbarButton()
   {
      super("Addins",
            ToolbarButton.NoTitle,
            CoreResources.INSTANCE.iconEmpty(),
            new ScrollableToolbarPopupMenu() 
            {
               @Override
               protected int getMaxHeight()
               {
                  return 500;
               }
            },
            false);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      ElementIds.assignElementId(this, ElementIds.ADDINS_TOOLBAR_BUTTON);
      menu_ = getMenu();
      
      menu_.setAutoHideRedundantSeparators(false);
      
      addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            populate();
         }
      });
      
      addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (event.isAttached())
            {
               menuWidth_ = menu_.getMenuTableElement().getOffsetWidth();
            }
            else
            {
               searchWidget_.setValue("");
            }
         }
      });
      
      searchWidget_ = new SearchWidget("Search for addins");
      searchValueChangeTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            onSearchValueChange();
         }
      };
      
      searchEl_ = searchWidget_.getElement();
      
      searchEl_.getStyle().setMarginLeft(10, Unit.PX);
      searchEl_.getStyle().setMarginRight(10, Unit.PX);
      searchEl_.getStyle().setMarginTop(-2, Unit.PX);
      
      DOM.sinkEvents(searchEl_, Event.KEYEVENTS);
      DOM.setEventListener(searchEl_, new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            searchValueChangeTimer_.schedule(200);
         }
      });
      
   }
   
   private void populate()
   {
      menu_.clearItems();
      
      Map<String, List<RAddin>> addinsByPkg = organizeAddins();
      
      if (addinsByPkg.isEmpty())
      {
         populateEmptyMenu();
         return;
      }
      
      MapUtil.forEach(addinsByPkg, new MapUtil.ForEachCommand<String, List<RAddin>>()
      {
         private int separatorCount_ = 0;
         
         @Override
         public void execute(final String pkg, final List<RAddin> addins)
         {
            menu_.addSeparator(new CustomMenuItemSeparator()
            {
               @Override
               public Element createMainElement()
               {
                  Label label = new Label(pkg);
                  label.addStyleName(ThemeStyles.INSTANCE.menuSubheader());
                  label.getElement().getStyle().setPaddingLeft(2, Unit.PX);
                  if (separatorCount_++ == 0)
                     return createSearchSeparator(label);
                  else
                     return label.getElement();
               }
            });
            menu_.addSeparator();
            
            addins.sort(new Comparator<RAddin>()
            {
               @Override
               public int compare(RAddin o1, RAddin o2)
               {
                  return Integer.compare(o1.getOrdinal(), o2.getOrdinal());
               }
            });
            
            for (RAddin addin : addins)
               menu_.addItem(menuItem(addin));
         }
      });
      
      menu_.selectFirst();
      
      Element tableEl = menu_.getMenuTableElement();
      if (tableEl != null)
      {
         int menuWidth = Math.max(MIN_WIDTH_PX, menuWidth_);
         tableEl.getStyle().setWidth(menuWidth, Unit.PX);
      }
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            searchWidget_.getInputElement().focus();
         }
      });
   }
   
   private void populateEmptyMenu()
   {
      menu_.addSeparator(new CustomMenuItemSeparator()
      {
         @Override
         public Element createMainElement()
         {
            Label label = new Label("No addins found");
            label.addStyleName(ThemeStyles.INSTANCE.menuSubheader());
            label.getElement().getStyle().setPaddingLeft(2, Unit.PX);
            return createSearchSeparator(label);
         }
      });
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            searchWidget_.getInputElement().focus();
         }
      });
   }
   
   private MenuItem menuItem(final RAddin addin)
   {
      MenuItem menuItem = new MenuItem(
            addin.getName(),
            new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  executor_.execute(addin);
               }
            });
      
      menuItem.setTitle(addin.getDescription());
      return menuItem;
   }
   
   private Map<String, List<RAddin>> organizeAddins()
   {
      Map<String, List<RAddin>> organized = new HashMap<String, List<RAddin>>();
      
      String query = searchWidget_.getValue().toLowerCase().trim();
      
      RAddins addins = manager_.getRAddins();
      for (String key : JsUtil.asIterable(addins.keys()))
      {
         RAddin addin = addins.get(key);
         
         if (addin.getName().toLowerCase().indexOf(query) == -1 &&
             addin.getPackage().toLowerCase().indexOf(query) == -1)
         {
            continue;
         }
         
         String[] splat = key.split("::");
         String pkg = splat[0];
         
         if (!organized.containsKey(pkg))
            organized.put(pkg, new ArrayList<RAddin>());
         
         List<RAddin> addinList = organized.get(pkg);
         addinList.add(addin);
      }
      
      return organized;
   }
   
   private Element createSearchSeparator(Label label)
   {
      final Element searchEl = searchWidget_.getElement();
      final Element labelEl = label.getElement();
      
      labelEl.getStyle().setFloat(Style.Float.LEFT);
      searchEl.getStyle().setFloat(Style.Float.RIGHT);
      
      Element container = DOM.createDiv();
      container.appendChild(labelEl);
      container.appendChild(searchEl);
      return container;
   }
   
   private void onSearchValueChange()
   {
      populate();
   }
   
   @Inject
   private void initialize(AddinsCommandManager manager,
                           AddinExecutor executor)
   {
      manager_ = manager;
      executor_ = executor;
   }
   
   private final ToolbarPopupMenu menu_;
   private final SearchWidget searchWidget_;
   private final Element searchEl_;
   private final Timer searchValueChangeTimer_;
   
   private int menuWidth_ = 0;
   
   // Injected ----
   private AddinsCommandManager manager_;
   private AddinExecutor executor_;
   
   private static final int MIN_WIDTH_PX = 260;
}
