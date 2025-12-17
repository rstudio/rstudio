/*
 * ModuleTabLayoutPanel.java
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
package org.rstudio.core.client.theme;

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.ProgressSpinner;
import org.rstudio.core.client.widget.model.ProvidesBusy;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.events.BusyEvent;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class ModuleTabLayoutPanel extends TabLayoutPanel
{
   public static class ModuleTab extends Composite implements BusyEvent.Handler
   {
      public ModuleTab(String title, ThemeStyles styles, boolean canClose, boolean minimized)
      {
         HorizontalPanel layoutPanel = new HorizontalPanel();
         layoutPanel.setStylePrimaryName(styles.tabLayout());

         String minimized_id = "";
         if (minimized)
            minimized_id = "_minimized";

         // Determine a base element ID based on the tab's title; make available to be
         // associated with actual tab element when ModuleTab is attached to the tab layout panel
         tabId_ = ElementIds.WORKBENCH_TAB + minimized_id + "_" + ElementIds.idSafeString(title);

         HTML left = new HTML();
         left.setStylePrimaryName(styles.tabLayoutLeft());
         layoutPanel.add(left);

         HorizontalPanel center = new HorizontalPanel();
         center.setStylePrimaryName(styles.rstheme_tabLayoutCenter());
         Label label = new Label(title, false);
         center.add(label);
         if (canClose)
         {
            closeButton_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.closeTab2x()));
            closeButton_.setStylePrimaryName(styles.closeTabButton());
            closeButton_.addStyleName(ThemeStyles.INSTANCE.handCursor());
            closeButton_.setAltText(constants_.closeButtonText(title));
            center.add(closeButton_);
         }
         layoutPanel.add(center);

         HTML right = new HTML();
         right.setStylePrimaryName(styles.tabLayoutRight());
         layoutPanel.add(right);

         MouseDownHandler onMouseDown = mouseDownEvent ->
         {
            // Stop double-click of tab from selecting the tab title text
            mouseDownEvent.preventDefault();
         };
         addDomHandler(onMouseDown, MouseDownEvent.getType());

         initWidget(layoutPanel);
      }

      public Widget getWidget()
      {
         return super.getWidget();
      }

      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }

      public HandlerRegistration addCloseButtonClickHandler(ClickHandler handler)
      {
         return closeButton_.addClickHandler(handler);
      }

      @Override
      public void onBusy(BusyEvent event)
      {
         setBusy(event.isBusy());
      }

      private void setBusy(boolean isBusy)
      {
         if (isBusy)
         {
            if (busySpinner_ == null)
            {
               HorizontalPanel center = (HorizontalPanel)closeButton_.getParent();

               busySpinner_ = new ProgressSpinner(center.getElement());

               busySpinner_.setHeight("9px");
               busySpinner_.setWidth("9px");
               busySpinner_.getElement().getStyle().setMarginLeft(4, Unit.PX);
               busySpinner_.getElement().getStyle().setMarginTop(6, Unit.PX);

               if (center != null)
                  center.add(busySpinner_);
            }
            closeButton_.setVisible(false);
            busySpinner_.setVisible(true);
         }
         else
         {
            if (busySpinner_ != null) {
               busySpinner_.removeFromParent();
               busySpinner_ = null;
            }
            closeButton_.setVisible(true);
         }
      }

      public String getTabId()
      {
         return tabId_;
      }

      private Image closeButton_;
      private ProgressSpinner busySpinner_;
      private final String tabId_;
   }

   public ModuleTabLayoutPanel(final WindowFrame owner, String tabListName)
   {
      super(BAR_HEIGHT, Style.Unit.PX, tabListName);
      owner_ = owner;
      styles_ = ThemeResources.INSTANCE.themeStyles();
      addStyleName(styles_.moduleTabPanel());

      MouseDownHandler onMouseDown = mouseDownEvent ->
      {
         if (!isWithinTopBand(mouseDownEvent.getNativeEvent()))
            return;

         // Stop click-drag selection from working in top band
         mouseDownEvent.preventDefault();
      };
      addDomHandler(onMouseDown, MouseDownEvent.getType());

      ClickHandler onClick = clickEvent ->
      {
         if (!isWithinTopBand(clickEvent.getNativeEvent()))
            return;

         clickEvent.preventDefault();
         clickEvent.stopPropagation();
         if (doubleClickState_.checkForDoubleClick(clickEvent.getNativeEvent()))
         {
            owner.fireEvent(new WindowStateChangeEvent(WindowState.MAXIMIZE));
         }
      };
      addDomHandler(onClick, ClickEvent.getType());
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      // Defer to ensure DOM is ready (same pattern as DocTabLayoutPanel)
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            Element tabBar = getTabBarElement();
            if (tabBar == null)
               return;

            Element tabBarParent = tabBar.getParentElement();

            // Enable scroll container
            tabBarParent.getStyle().setOverflowX(Overflow.HIDDEN);

            // Update tab bar width
            updateTabBarWidth();

            // Sink wheel events (same pattern as DocTabLayoutPanel)
            DOM.sinkBitlessEvent(tabBar, "mousewheel");
            DOM.sinkBitlessEvent(tabBar, "wheel");
            Event.setEventListener(tabBar, new WheelEventListener());
         }
      });
   }

   private boolean isWithinTopBand(NativeEvent event)
   {
      int clientPos = event.getClientY();
      int topBounds = getAbsoluteTop();
      int bottomBounds = topBounds + BAR_HEIGHT;
      return clientPos >= topBounds && clientPos <= bottomBounds;
   }

   private Element getTabBarElement()
   {
      return (Element) DomUtils.findNode(
            getElement(),
            true,
            false,
            new DomUtils.NodePredicate()
            {
               public boolean test(Node n)
               {
                  if (n.getNodeType() != Node.ELEMENT_NODE)
                     return false;
                  return ((Element) n).getClassName()
                        .contains("gwt-TabLayoutPanelTabs");
               }
            });
   }

   private void updateTabBarWidth()
   {
      Element tabBar = getTabBarElement();
      if (tabBar == null)
         return;

      Element tabBarParent = tabBar.getParentElement();
      int contentWidth = getTabsContentWidth(tabBar);
      int parentWidth = tabBarParent.getOffsetWidth();

      // Tab bar must be at least as wide as parent (for background/border)
      // and have extra space so last tab can scroll into view
      int minWidth = parentWidth;
      int neededWidth = contentWidth + BUTTON_AREA_WIDTH;
      int tabBarWidth = Math.max(minWidth, neededWidth);

      tabBar.getStyle().setWidth(tabBarWidth, Unit.PX);
   }

   private int getTabsContentWidth(Element tabBar)
   {
      int width = 0;
      for (int i = 0; i < tabBar.getChildCount(); i++)
      {
         Node child = tabBar.getChild(i);
         if (child.getNodeType() == Node.ELEMENT_NODE)
            width += ((Element) child).getOffsetWidth();
      }
      return width;
   }

   private int getMaxScroll()
   {
      Element tabBar = getTabBarElement();
      if (tabBar == null)
         return 0;
      Element tabBarParent = tabBar.getParentElement();
      int contentWidth = getTabsContentWidth(tabBar);
      int effectiveViewport = tabBarParent.getOffsetWidth() - BUTTON_AREA_WIDTH;
      return Math.max(0, contentWidth - effectiveViewport);
   }

   private void scrollBy(int delta, boolean animate)
   {
      Element tabBar = getTabBarElement();
      if (tabBar == null)
         return;
      Element tabBarParent = tabBar.getParentElement();
      int currentScroll = tabBarParent.getScrollLeft();
      int targetScroll = currentScroll + delta;
      scrollTo(targetScroll, animate);
   }

   private void scrollTo(int targetScroll, boolean animate)
   {
      Element tabBar = getTabBarElement();
      if (tabBar == null)
         return;
      Element tabBarParent = tabBar.getParentElement();

      int maxScroll = getMaxScroll();
      int newScroll = Math.max(0, Math.min(targetScroll, maxScroll));

      if (animate && !reducedMotion())
      {
         animateScrollTo(tabBarParent, newScroll);
      }
      else
      {
         tabBarParent.setScrollLeft(newScroll);
      }
   }

   private void animateScrollTo(final Element tabBarParent, final int targetScroll)
   {
      if (currentAnimation_ != null)
         currentAnimation_.cancel();

      final int startScroll = tabBarParent.getScrollLeft();
      final int delta = targetScroll - startScroll;

      currentAnimation_ = new Animation()
      {
         @Override
         protected void onUpdate(double progress)
         {
            tabBarParent.setScrollLeft(startScroll + (int)(delta * progress));
         }

         @Override
         protected void onComplete()
         {
            tabBarParent.setScrollLeft(targetScroll);
            currentAnimation_ = null;
         }
      };

      currentAnimation_.run(200);  // 200ms duration
   }

   private boolean reducedMotion()
   {
      return RStudioGinjector.INSTANCE.getUserPrefs().reducedMotion().getValue();
   }

   @Override
   public void add(Widget child, String text)
   {
      add(child, text, false);
   }

   @Override
   public void add(Widget child, String text, boolean asHtml)
   {
      add(child, text, asHtml, null);
   }

   public void add(Widget child, String text, boolean asHtml, ClickHandler closeHandler)
   {
      add(child, text, asHtml, closeHandler, null);
   }

   public void add(final Widget child, String text, boolean asHtml,
                   ClickHandler closeHandler, ProvidesBusy providesBusy)
   {
      if (asHtml)
         throw new UnsupportedOperationException("HTML tab names not supported");

      ModuleTab tab = new ModuleTab(text, styles_, closeHandler != null, false /*minimized*/);
      super.add(child, tab);
      setTabId(child, ElementIds.getUniqueElementId(tab.getTabId()));

      if (closeHandler != null)
         tab.addCloseButtonClickHandler(closeHandler);

      if (providesBusy != null)
         providesBusy.addBusyHandler(tab);

      // Update tab bar width after adding
      Scheduler.get().scheduleDeferred(() -> updateTabBarWidth());
   }

   @Override
   public boolean remove(int index)
   {
      boolean result = super.remove(index);
      if (result)
      {
         Scheduler.get().scheduleDeferred(() -> {
            updateTabBarWidth();
            ensureSelectedTabIsVisible(!reducedMotion());
         });
      }
      return result;
   }

   @Override
   public void selectTab(int index)
   {
      super.selectTab(Math.max(0, Math.min(index, getWidgetCount() - 1)));
      if (index == 0)
         owner_.addStyleName(styles_.firstTabSelected());
      else
         owner_.removeStyleName(styles_.firstTabSelected());

      // Scroll selected tab into view
      ensureSelectedTabIsVisible(!reducedMotion());
   }

   private void ensureSelectedTabIsVisible(boolean animate)
   {
      Element tabBar = getTabBarElement();
      if (tabBar == null || !isVisible() || !isAttached())
         return;

      int selectedIndex = getSelectedIndex();
      if (selectedIndex < 0)
         return;

      // Get the selected tab widget's element
      Widget tabWidget = getTabWidget(selectedIndex);
      if (tabWidget == null)
         return;

      Element selectedTab = tabWidget.getElement();
      Element tabBarParent = tabBar.getParentElement();

      int tabLeft = selectedTab.getOffsetLeft();
      int tabRight = tabLeft + selectedTab.getOffsetWidth();
      int scrollLeft = tabBarParent.getScrollLeft();
      int viewportWidth = tabBarParent.getOffsetWidth() - BUTTON_AREA_WIDTH;

      int newScroll = scrollLeft;

      // If tab is to the left of viewport, scroll left
      if (tabLeft < scrollLeft)
         newScroll = tabLeft;
      // If tab is to the right of viewport, scroll right
      else if (tabRight > scrollLeft + viewportWidth)
         newScroll = tabRight - viewportWidth;

      if (newScroll != scrollLeft)
         scrollTo(newScroll, animate);
   }

   private class WheelEventListener implements EventListener
   {
      @Override
      public void onBrowserEvent(Event event)
      {
         String type = event.getType();
         if ("mousewheel".equals(type) || "wheel".equals(type))
         {
            // Get scroll delta (positive = scroll down, negative = scroll up)
            double deltaY = event.getDeltaY();
            if (deltaY != 0)
            {
               scrollBy(deltaY > 0 ? SCROLL_AMOUNT : -SCROLL_AMOUNT, !reducedMotion());
               event.preventDefault();
            }
         }
      }
   }

   @Override
   public void onResize()
   {
      super.onResize();
      updateTabBarWidth();
      ensureSelectedTabIsVisible(false);  // No animation on resize
   }

   @Override
   public void add(Widget child, Widget tab)
   {
      throw new UnsupportedOperationException();
   }

   private final ThemeStyles styles_;

   private final DoubleClickState doubleClickState_ = new DoubleClickState();
   public static final int BAR_HEIGHT = 23;
   private static final int BUTTON_AREA_WIDTH = 55;  // Space for min/max buttons
   private static final int SCROLL_AMOUNT = 100;     // Pixels per scroll event
   private final WindowFrame owner_;
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
   private Animation currentAnimation_;
}
