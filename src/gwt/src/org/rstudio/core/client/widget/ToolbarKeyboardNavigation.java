/*
 * ToolbarKeyboardNavigation.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.KeyCodes;

import elemental2.core.JsArray;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.MutationObserver;
import elemental2.dom.MutationObserverInit;
import elemental2.dom.MutationRecord;
import elemental2.dom.Node;
import jsinterop.base.Js;

/**
 * Keyboard navigation for a toolbar, following the WAI-ARIA toolbar pattern:
 * Left, Right, Home and End move focus among its items, without wrapping, and
 * Tab stops once per group of items, on the one focused last.
 *
 * A text field keeps those keys for editing, so it can't be arrowed out of. It
 * has a Tab stop of its own and splits the items around it into separate groups.
 */
class ToolbarKeyboardNavigation
{
   ToolbarKeyboardNavigation(Element root)
   {
      root_ = root;
      observer_ = new MutationObserver(this::onMutations);
   }

   void attach()
   {
      elemental2.dom.Element root = Js.uncheckedCast(root_);
      root.addEventListener("keydown", keyDownListener_);
      root.addEventListener("focusin", focusInListener_);

      MutationObserverInit init = MutationObserverInit.create();
      init.setChildList(true);
      init.setSubtree(true);
      init.setAttributes(true);
      init.setAttributeFilter(OBSERVED_ATTRIBUTES);
      observer_.observe(Js.<Node>uncheckedCast(root_), init);

      updateTabStops(null);
   }

   void detach()
   {
      elemental2.dom.Element root = Js.uncheckedCast(root_);
      root.removeEventListener("keydown", keyDownListener_);
      root.removeEventListener("focusin", focusInListener_);
      observer_.disconnect();
   }

   /**
    * Focus the first Tab stop, which is the item last focused in the first group.
    */
   boolean focusFirstTabStop()
   {
      updateTabStops(null);
      for (Element item : getItems())
      {
         if (item.getTabIndex() == 0)
         {
            item.focus();
            return true;
         }
      }
      return false;
   }

   private void onKeyDown(Event event)
   {
      NativeEvent nativeEvent = Js.uncheckedCast(event);
      if (nativeEvent.getAltKey() || nativeEvent.getCtrlKey() ||
          nativeEvent.getMetaKey() || nativeEvent.getShiftKey())
         return;

      int keyCode = nativeEvent.getKeyCode();
      if (keyCode != KeyCodes.KEY_LEFT && keyCode != KeyCodes.KEY_RIGHT &&
          keyCode != KeyCodes.KEY_HOME && keyCode != KeyCodes.KEY_END)
         return;

      Element target = Js.uncheckedCast(event.target);
      if (isTextField(target))
         return;
      List<Element> items = getItems();
      int index = items.indexOf(target);
      if (index < 0)
         return;

      int next;
      if (keyCode == KeyCodes.KEY_LEFT)
         next = Math.max(index - 1, 0);
      else if (keyCode == KeyCodes.KEY_RIGHT)
         next = Math.min(index + 1, items.size() - 1);
      else if (keyCode == KeyCodes.KEY_HOME)
         next = 0;
      else
         next = items.size() - 1;

      event.preventDefault();
      event.stopPropagation();
      items.get(next).focus();
   }

   private void onFocusIn(Event event)
   {
      updateTabStops(Js.uncheckedCast(event.target));
   }

   private Object onMutations(JsArray<MutationRecord> records, MutationObserver observer)
   {
      // A widget moved out of the toolbar must not stay out of the Tab order.
      for (MutationRecord record : Js.<MutationRecord[]>uncheckedCast(records))
      {
         if (!"childList".equals(record.type))
            continue;
         for (int i = 0; i < record.removedNodes.length; i++)
         {
            Node node = record.removedNodes.item(i);
            if (node.nodeType != Node.ELEMENT_NODE)
               continue;
            Element removed = Js.uncheckedCast(node);
            if (!root_.isOrHasChild(removed))
               release(removed);
         }
      }

      // Items come and go and get disabled; keep a Tab stop on each group.
      updateTabStops(null);
      return null;
   }

   private void release(Element removed)
   {
      List<Element> managed = new ArrayList<>();
      if (removed.hasAttribute(MANAGED))
         managed.add(removed);
      NodeList<Element> descendants = DomUtils.querySelectorAll(removed, "[" + MANAGED + "]");
      for (int i = 0; i < descendants.getLength(); i++)
         managed.add(descendants.getItem(i));

      for (Element el : managed)
      {
         el.removeAttribute(MANAGED);
         el.removeAttribute(LAST_FOCUSED);
         el.setTabIndex(0);
      }
   }

   // Gives each group one Tab stop, on its last-focused item if that is still
   // an item, else on its first. A focused item becomes its group's stop.
   private void updateTabStops(Element focused)
   {
      List<Element> items = getItems();
      for (List<Element> group : getGroups(items))
      {
         Element stop = group.contains(focused) ? focused : null;
         for (int i = 0; stop == null && i < group.size(); i++)
         {
            if (group.get(i).hasAttribute(LAST_FOCUSED))
               stop = group.get(i);
         }
         if (stop == null)
            stop = group.get(0);

         for (Element item : group)
         {
            item.setAttribute(MANAGED, "");
            setTabIndex(item, item == stop ? 0 : -1);
            if (item == stop)
               item.setAttribute(LAST_FOCUSED, "");
            else
               item.removeAttribute(LAST_FOCUSED);
         }
      }

      // Forget a last-focused item that has since been disabled or hidden.
      NodeList<Element> marked = DomUtils.querySelectorAll(root_, "[" + LAST_FOCUSED + "]");
      for (int i = 0; i < marked.getLength(); i++)
      {
         if (!items.contains(marked.getItem(i)))
            marked.getItem(i).removeAttribute(LAST_FOCUSED);
      }
   }

   // Writing an unchanged value would still notify the observer, which would
   // then update again, endlessly.
   private static void setTabIndex(Element el, int tabIndex)
   {
      if (el.getTabIndex() != tabIndex || !el.hasAttribute("tabindex"))
         el.setTabIndex(tabIndex);
   }

   private static List<List<Element>> getGroups(List<Element> items)
   {
      List<List<Element>> groups = new ArrayList<>();
      List<Element> group = null;
      for (Element item : items)
      {
         if (isTextField(item))
         {
            List<Element> field = new ArrayList<>();
            field.add(item);
            groups.add(field);
            group = null;
            continue;
         }
         if (group == null)
         {
            group = new ArrayList<>();
            groups.add(group);
         }
         group.add(item);
      }
      return groups;
   }

   // The toolbar's enabled, displayed controls, in document order. An element
   // that was out of the Tab order before this class managed it stays out.
   private List<Element> getItems()
   {
      List<Element> items = new ArrayList<>();
      NodeList<Element> candidates = DomUtils.querySelectorAll(root_, CANDIDATES);
      for (int i = 0; i < candidates.getLength(); i++)
      {
         Element el = candidates.getItem(i);
         if (el.getTabIndex() < 0 && !el.hasAttribute(MANAGED))
            continue;
         if (el.hasAttribute("disabled") || !isDisplayed(el))
            continue;
         items.add(el);
      }
      return items;
   }

   // Checks display rather than size, so the answer doesn't change while the
   // whole toolbar is hidden (as in a hidden pane).
   private boolean isDisplayed(Element el)
   {
      if ("hidden".equals(DomUtils.getComputedStyles(el).getVisibility()))
         return false;
      for (Element e = el; e != null && e != root_; e = e.getParentElement())
      {
         if ("none".equals(DomUtils.getComputedStyles(e).getDisplay()))
            return false;
      }
      return true;
   }

   private static boolean isTextField(Element el)
   {
      String tag = el.getTagName().toLowerCase();
      if (tag.equals("textarea") || tag.equals("select"))
         return true;
      if (tag.equals("input"))
         return !NON_TEXT_INPUT_TYPES.contains(el.getAttribute("type").toLowerCase());
      return "true".equals(el.getAttribute("contenteditable"));
   }

   private final Element root_;
   private final MutationObserver observer_;
   private final EventListener keyDownListener_ = this::onKeyDown;
   private final EventListener focusInListener_ = this::onFocusIn;

   private static final String CANDIDATES =
         "button, input, select, textarea, a[href], [tabindex], [contenteditable=\"true\"]";
   private static final List<String> NON_TEXT_INPUT_TYPES = Arrays.asList(
         "button", "checkbox", "radio", "submit", "reset", "image", "color", "file");
   private static final String[] OBSERVED_ATTRIBUTES =
         {"class", "disabled", "hidden", "style", "tabindex"};

   // Marks an item whose tabindex this class set, so -1 doesn't exclude it.
   private static final String MANAGED = "data-rs-toolbar-item";
   private static final String LAST_FOCUSED = "data-rs-toolbar-last-focused";
}
