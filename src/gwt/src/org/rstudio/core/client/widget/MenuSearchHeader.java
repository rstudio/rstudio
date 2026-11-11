/*
 * MenuSearchHeader.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.aria.client.AutocompleteValue;
import com.google.gwt.aria.client.ComboboxRole;
import com.google.gwt.aria.client.ExpandedValue;
import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import elemental2.dom.HTMLElement;
import jsinterop.base.Js;

// A search box above a toolbar button's menu, for filtering its items. It sits
// above the menu rather than inside it, since a menu may contain only items,
// groups, and separators, and the menu would take a typed space as running the
// highlighted item. Focus stays in the search box, which passes the navigation
// keys to the menu and, as a combobox, names the highlighted item.
public class MenuSearchHeader
{
   // onSearch rebuilds the menu's items for getQuery()
   public MenuSearchHeader(ToolbarMenuButton button,
                           ScrollableToolbarPopupMenu menu,
                           String label,
                           Command onSearch)
   {
      button_ = button;
      menu_ = menu;
      onSearch_ = onSearch;

      searchWidget_ = new SearchWidget(label);
      searchTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            applyPendingSearch();
         }
      };
      searchWidget_.addValueChangeHandler(event -> searchTimer_.schedule(200));
      searchWidget_.addKeyDownHandler(this::onSearchKeyDown);

      addToMenu();
      initComboboxRole();
      nameMenuOnOpen();
      restoreFocusOnClose();
      redirectMenuFocus();
   }

   // The search text, as the menu is filtered by it. Text that filters the same,
   // such as with a trailing space, doesn't rebuild the menu and reset the highlight.
   public String getQuery()
   {
      return StringUtil.notNull(searchWidget_.getValue()).trim();
   }

   // Empties the search box without rebuilding the menu, which is the caller's to do
   public void clear()
   {
      // Fired, so that SearchWidget forgets the old text; otherwise typing it
      // again isn't reported as a change
      searchWidget_.setValue("", true);
      searchTimer_.cancel();
      searchedQuery_ = "";
   }

   private void addToMenu()
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

   // Focus stays in the search box while the arrow keys move the highlight, so the
   // search box, not the menu, has to tell screen readers which item is highlighted.
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

   // Names the menu after its button, as in the menu button pattern. Owners may
   // assign the button's id after constructing it, so read it as the menu opens.
   private void nameMenuOnOpen()
   {
      menu_.addAttachHandler(event ->
      {
         if (!event.isAttached())
            return;

         Element buttonEl = button_.getElement();
         if (StringUtil.isNullOrEmpty(buttonEl.getId()))
            buttonEl.setId(DOM.createUniqueId());
         Roles.getMenuRole().setAriaLabelledbyProperty(menu_.getMenuBarElement(),
               Id.of(buttonEl));
      });
   }

   // Closing the menu removes the focused search box. Put focus back on the
   // button rather than leave it on the page body; Tab's default action then
   // moves on from there.
   private void restoreFocusOnClose()
   {
      menu_.addCloseHandler(event ->
      {
         Element focused = DomUtils.getActiveElement();
         if (focused == null ||
             focused == Document.get().getBody() ||
             menu_.getElement().isOrHasChild(focused))
         {
            button_.setFocus(true);
         }
      });
   }

   // The menu takes focus whenever it opens, but typing belongs in the search
   // box. GWT focuses the menu from a setTimeout in WebKit and Chromium, which in
   // Safari ran after the search box took focus; redirecting the menu's focus
   // works whichever runs last.
   private void redirectMenuFocus()
   {
      HTMLElement popupEl = Js.cast(menu_.getElement());
      Object menuEl = menu_.getMenuBarElement();
      popupEl.addEventListener("focus", event ->
      {
         if (event.target != menuEl)
            return;

         // Caught while capturing, so the menu never sees it: its own focus
         // handling would highlight the first item, and screen readers would
         // then announce that item instead of the search box.
         event.stopPropagation();
         searchWidget_.getInputElement().focus();
      }, true);
   }

   // Page Up, Page Down, and Escape already reach the menu from anywhere
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
            // With nothing highlighted, Up starts from the last item, as in the
            // combobox pattern; the menu itself would start from the first
            if (menu_.getSelectedIndex() < 0)
               menu_.selectLast();
            else
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
      searchTimer_.cancel();
      String query = getQuery();
      if (!query.equals(searchedQuery_))
      {
         searchedQuery_ = query;
         onSearch_.execute();
         menu_.selectFirst();
      }
   }

   private final ToolbarMenuButton button_;
   private final ScrollableToolbarPopupMenu menu_;
   private final Command onSearch_;
   private final SearchWidget searchWidget_;
   private final Timer searchTimer_;

   // The query the menu was last rebuilt for
   private String searchedQuery_ = "";
}
