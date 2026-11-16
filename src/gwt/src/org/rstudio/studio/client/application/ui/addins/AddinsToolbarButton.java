/*
 * AddinsToolbarButton.java
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
package org.rstudio.studio.client.application.ui.addins;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.MapUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.widget.MenuSearchHeader;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

public class AddinsToolbarButton extends ToolbarMenuButton
{
   public AddinsToolbarButton()
   {
      super(constants_.addinsText(),
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
      menu_ = (ScrollableToolbarPopupMenu) getMenu();

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
               search_.clear();
            }
         }
      });

      search_ = new MenuSearchHeader(this,
                                     menu_,
                                     constants_.searchForAddinsLabel(),
                                     this::populate);
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
         @Override
         public void execute(final String pkg, final List<RAddin> addins)
         {
            Element heading = menu_.addSubheader(pkg);
            menu_.addSeparator();

            addins.sort(new Comparator<RAddin>()
            {
               @Override
               public int compare(RAddin o1, RAddin o2)
               {
                  return Integer.compare(o1.getOrdinal(), o2.getOrdinal());
               }
            });

            for (int i = 0; i < addins.size(); i++)
            {
               RAddin addin = addins.get(i);
               MenuItem item = menuItem(addin);

               if (i == 0)
                  ToolbarPopupMenu.nameWithSubheader(item, heading);
               menu_.addItem(item);
            }
         }
      });

      Element tableEl = menu_.getMenuTableElement();
      if (tableEl != null)
      {
         int menuWidth = Math.max(MIN_WIDTH_PX, menuWidth_);
         tableEl.getStyle().setWidth(menuWidth, Unit.PX);
      }
   }

   private void populateEmptyMenu()
   {
      menu_.addSubheader(constants_.noAddinsFound());
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
      Map<String, List<RAddin>> organized = new HashMap<>();

      String query = search_.getQuery();

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
            organized.put(pkg, new ArrayList<>());

         List<RAddin> addinList = organized.get(pkg);
         addinList.add(addin);
      }

      return organized;
   }

   @Inject
   private void initialize(AddinsCommandManager manager,
                           AddinExecutor executor)
   {
      manager_ = manager;
      executor_ = executor;
   }

   private final ScrollableToolbarPopupMenu menu_;
   private final MenuSearchHeader search_;

   private int menuWidth_ = 0;

   // Injected ----
   private AddinsCommandManager manager_;
   private AddinExecutor executor_;

   private static final int MIN_WIDTH_PX = 260;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
