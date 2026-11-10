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

import com.google.gwt.aria.client.AutocompleteValue;
import com.google.gwt.aria.client.ComboboxRole;
import com.google.gwt.aria.client.ExpandedValue;
import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.MapUtil;
import org.rstudio.core.client.StringUtil;
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
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
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
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
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

      searchWidget_ = new SearchWidget(constants_.searchForAddinsLabel());
      searchValueChangeTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            // SearchWidget reports its first key as a change even when the text
            // is unchanged; rebuilding then would reset the highlight.
            if (!searchWidget_.getValue().equals(populatedQuery_))
               populate();
         }
      };
      searchWidget_.addValueChangeHandler(event -> searchValueChangeTimer_.schedule(200));
      searchWidget_.addKeyDownHandler(this::onSearchKeyDown);

      addSearchHeader();
      initComboboxRole();
   }

   // The search box sits above the menu rather than inside it, since a menu may
   // contain only items, groups, and separators.
   private void addSearchHeader()
   {
      Style style = searchWidget_.getElement().getStyle();
      style.setMarginTop(4, Unit.PX);
      style.setMarginBottom(4, Unit.PX);
      style.setMarginLeft(10, Unit.PX);
      style.setMarginRight(10, Unit.PX);

      Widget menuWidget = menu_.getWidget();
      FlowPanel panel = new FlowPanel();
      panel.add(searchWidget_);
      panel.add(menuWidget);
      menu_.setWidget(panel);
   }

   // Focus stays in the search box, so it hands the menu's navigation keys to the
   // menu. Page Up, Page Down, and Escape already reach the menu from anywhere.
   private void onSearchKeyDown(KeyDownEvent event)
   {
      switch (event.getNativeKeyCode())
      {
         case KeyCodes.KEY_UP:
            menu_.moveSelectionUp();
            break;
         case KeyCodes.KEY_DOWN:
            menu_.moveSelectionDown();
            break;
         case KeyCodes.KEY_HOME:
            menu_.selectFirst();
            break;
         case KeyCodes.KEY_END:
            menu_.selectLast();
            break;
         case KeyCodes.KEY_ENTER:
            menu_.activateSelectedItem();
            break;
         case KeyCodes.KEY_TAB:
            // Hiding the menu removes the focused search box, so first put focus
            // back on this button; the default action then moves on from here.
            setFocus(true);
            menu_.hide();
            return;
         default:
            return;
      }
      event.preventDefault();
   }

   // Focus stays in the search box while the arrow keys move the highlight, so the
   // search box, not the menu, has to tell screen readers which add-in is highlighted.
   private void initComboboxRole()
   {
      final Element input = searchWidget_.getInputElement();
      Element menuEl = menu_.getMenuBarElement();
      if (StringUtil.isNullOrEmpty(menuEl.getId()))
         menuEl.setId(DOM.createUniqueId());

      ComboboxRole role = Roles.getComboboxRole();
      role.set(input);
      role.setAriaExpandedState(input, ExpandedValue.TRUE);
      role.setAriaAutocompleteProperty(input, AutocompleteValue.LIST);
      role.setAriaControlsProperty(input, Id.of(menuEl));

      ((ScrollableToolbarPopupMenu) menu_).addSelectionHandler(event ->
      {
         MenuItem item = event.getSelectedItem();
         if (item == null)
            role.removeAriaActivedescendantProperty(input);
         else
            role.setAriaActivedescendantProperty(input, Id.of(item.getElement()));
      });
   }

   private void populate()
   {
      populatedQuery_ = searchWidget_.getValue();
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
            menu_.addSeparator(new CustomMenuItemSeparator()
            {
               @Override
               public Element createMainElement()
               {
                  return createSubheader(pkg);
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
            return createSubheader(constants_.noAddinsFound());
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
      Map<String, List<RAddin>> organized = new HashMap<>();

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
            organized.put(pkg, new ArrayList<>());

         List<RAddin> addinList = organized.get(pkg);
         addinList.add(addin);
      }

      return organized;
   }

   private Element createSubheader(String text)
   {
      Label label = new Label(text);
      label.addStyleName(ThemeStyles.INSTANCE.menuSubheader());
      label.getElement().getStyle().setPaddingLeft(2, Unit.PX);
      return label.getElement();
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
   private final Timer searchValueChangeTimer_;

   private int menuWidth_ = 0;
   private String populatedQuery_ = "";

   // Injected ----
   private AddinsCommandManager manager_;
   private AddinExecutor executor_;

   private static final int MIN_WIDTH_PX = 260;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
