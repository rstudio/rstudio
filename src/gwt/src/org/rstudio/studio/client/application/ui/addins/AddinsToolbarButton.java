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
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CustomMenuItemSeparator;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
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
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import elemental2.dom.HTMLElement;
import jsinterop.base.Js;

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
            applyPendingSearch();
         }
      };
      searchWidget_.addValueChangeHandler(event -> searchValueChangeTimer_.schedule(200));
      searchWidget_.addKeyDownHandler(this::onSearchKeyDown);

      // Closing the menu removes the focused search box. Put focus back on this
      // button rather than leave it on the page body; Tab's default action then
      // moves on from here.
      menu_.addCloseHandler(event ->
      {
         Element focused = DomUtils.getActiveElement();
         if (focused == null ||
             focused == Document.get().getBody() ||
             menu_.getElement().isOrHasChild(focused))
         {
            setFocus(true);
         }
      });

      addSearchHeader();
      initComboboxRole();

      // Name the menu after its button, as in the menu button pattern
      Roles.getMenuRole().setAriaLabelledbyProperty(menu_.getMenuBarElement(),
            Id.of(getElement()));

      // The menu takes focus whenever it opens, but typing belongs in the search
      // box, which hands the navigation keys to the menu. Redirecting the menu's
      // focus, rather than focusing the search box after opening, doesn't depend
      // on which runs last; Safari defers the menu's focus.
      HTMLElement popupEl = Js.cast(menu_.getElement());
      Object menuEl = menu_.getMenuBarElement();
      popupEl.addEventListener("focus", event ->
      {
         if (event.target != menuEl)
            return;

         // Caught while capturing, so the menu never sees it: its own focus
         // handling would highlight the first add-in, and screen readers would
         // then announce that add-in instead of the search box.
         event.stopPropagation();
         searchWidget_.getInputElement().focus();
      }, true);
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

      // Fill the row, rather than the shared search style's fixed 120px
      Style boxStyle = searchWidget_.getElement().getFirstChildElement().getStyle();
      boxStyle.setProperty("width", "auto");
      boxStyle.setMarginRight(0, Unit.PX);

      // Not a credential field. In Safari, 1Password honors this only on the
      // menu's first opening and shows its field icon on later ones.
      searchWidget_.getInputElement().setAttribute("data-1p-ignore", "true");

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
      int key = event.getNativeKeyCode();
      if (key == KeyCodes.KEY_TAB)
      {
         menu_.hide();
         return;
      }

      // Keys with modifiers, such as Shift+Home, edit the search text instead
      boolean navigation =
            key == KeyCodes.KEY_UP || key == KeyCodes.KEY_DOWN ||
            key == KeyCodes.KEY_HOME || key == KeyCodes.KEY_END ||
            key == KeyCodes.KEY_ENTER;
      if (!navigation || event.isAnyModifierKeyDown())
         return;

      applyPendingSearch();
      switch (key)
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
      }
      event.preventDefault();
   }

   // Rebuilds the menu for the search text now, rather than when the search
   // timer fires, so a navigation key acts on the list for the text typed so
   // far, with its first match highlighted. SearchWidget reports its first key
   // as a change even when the text is unchanged; that must not reset the highlight.
   private void applyPendingSearch()
   {
      searchValueChangeTimer_.cancel();
      if (!searchWidget_.getValue().equals(populatedQuery_))
      {
         populate();
         menu_.selectFirst();
      }
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

      menu_.addSelectionHandler(event ->
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
            addSubheader(pkg);
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

               // The arrow keys skip the package heading, so the package's first
               // add-in names it for screen readers
               if (i == 0)
               {
                  Roles.getMenuitemRole().setAriaLabelProperty(item.getElement(),
                        constants_.addinInPackageLabel(pkg, addin.getName()));
               }
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
      addSubheader(constants_.noAddinsFound());
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

   private void addSubheader(String text)
   {
      MenuItemSeparator subheader = new CustomMenuItemSeparator()
      {
         @Override
         public Element createMainElement()
         {
            Label label = new Label(text);
            label.addStyleName(ThemeStyles.INSTANCE.menuSubheader());
            label.getElement().getStyle().setPaddingLeft(2, Unit.PX);
            return label.getElement();
         }
      };

      // A heading, not a divider: as a separator, screen readers announce it as a
      // "splitter". The divider below each package heading keeps that role.
      Element el = subheader.getElement();
      el.removeAttribute("role");
      el.removeAttribute("aria-disabled");
      menu_.addSeparator(subheader);
   }

   @Inject
   private void initialize(AddinsCommandManager manager,
                           AddinExecutor executor)
   {
      manager_ = manager;
      executor_ = executor;
   }

   private final ScrollableToolbarPopupMenu menu_;
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
