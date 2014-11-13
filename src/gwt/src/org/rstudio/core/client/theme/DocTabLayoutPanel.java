/*
 * DocTabLayoutPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

/**
 * A tab panel that is styled for document tabs.
 */
public class DocTabLayoutPanel
      extends TabLayoutPanel
      implements HasTabClosingHandlers,
                 HasTabCloseHandlers,
                 HasTabClosedHandlers
{
   public DocTabLayoutPanel(boolean closeableTabs,
                            int padding,
                            int rightMargin)
   {
      super(BAR_HEIGHT, Style.Unit.PX);
      closeableTabs_ = closeableTabs;
      padding_ = padding;
      rightMargin_ = rightMargin;
      styles_ = ThemeResources.INSTANCE.themeStyles();
      addStyleName(styles_.docTabPanel());
      addStyleName(styles_.moduleTabPanel());
   }
   
   @Override
   public void add(final Widget child, String text)
   {
      add(child, null, text, null);
   }
   
   private class TabDragHandler
   {
      public TabDragHandler(final DocTab tab)
      {
         tab_ = tab;
         tab.addMouseDownHandler(new MouseDownHandler()
         {
            @Override
            public void onMouseDown(MouseDownEvent arg0)
            {
               if (!dragging_)
               {
                  beginDrag(arg0);
                  arg0.preventDefault();
               }
            }
         });
      }
      
      private void beginDrag(MouseDownEvent move)
      {
         dragging_ = true;
         dragElement_ = tab_.getElement().getParentElement().getParentElement();

         dragStartX_ = DomUtils.leftRelativeTo(dragElement_.getParentElement(),
               dragElement_);
         dragElementDelta_ = move.getRelativeX(dragElement_);
         dragElement_.getStyle().setPosition(Position.ABSOLUTE);
         dragElement_.getStyle().setLeft(dragStartX_ - dragElementDelta_, Unit.PX);
         handlerReg_.add(RootPanel.get().addDomHandler(new MouseMoveHandler() 
         {
            @Override
            public void onMouseMove(MouseMoveEvent arg0)
            {
               if (dragging_)
               {
                  drag(arg0);
                  arg0.preventDefault();
               }
            }
         }, MouseMoveEvent.getType()));
         handlerReg_.add(Event.addNativePreviewHandler(new NativePreviewHandler() 
         {
            @Override
            public void onPreviewNativeEvent(NativePreviewEvent arg0)
            {
               if (arg0.getTypeInt() == Event.ONMOUSEUP)
               {
                  endDrag();
               }
            }
         }));
      }
      private void endDrag()
      {
         dragging_ = false;
         dragElement_.getStyle().clearLeft();
         dragElement_.getStyle().clearPosition();
         handlerReg_.removeHandler();
      }
      
      private void drag(MouseMoveEvent move) 
      {
         dragElement_.getStyle().setLeft(
               move.getRelativeX(dragElement_.getParentElement()) - 
               dragElementDelta_, Unit.PX);
      }
      
      private boolean dragging_ = false;
      private int dragStartX_ = 0;
      private int dragElementDelta_ = 0;
      private Element dragElement_;
      private DocTab tab_;
      private HandlerRegistrations handlerReg_;
   }

   public void add(final Widget child,
                   ImageResource icon,
                   final String text,
                   String tooltip)
   {
      if (closeableTabs_)
      {
         DocTab tab = new DocTab(icon, text, tooltip, new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               int index = getWidgetIndex(child);
               if (index >= 0)
               {
                  tryCloseTab(index, null);
               }
            }
         });
         super.add(child, tab);
         new TabDragHandler(tab);
      }
      else
      {
         super.add(child, text);
      }
   }

   public boolean tryCloseTab(int index, Command onClosed)
   {
      TabClosingEvent event = new TabClosingEvent(index);
      fireEvent(event);
      if (event.isCancelled())
         return false;

      closeTab(index, onClosed);
      return true;
   }

   public void closeTab(int index, Command onClosed)
   {
      if (remove(index))
      {
         if (onClosed != null)
            onClosed.execute();
      }
   }

   @Override
   public void selectTab(int index)
   {
      super.selectTab(index);
      ensureSelectedTabIsVisible(true);
   }

   public void ensureSelectedTabIsVisible(boolean animate)
   {
      if (currentAnimation_ != null)
      {
         currentAnimation_.cancel();
         currentAnimation_ = null;
      }

      int index = getSelectedIndex();
      if (index < 0)
         return;
      Widget tabWidget = getTabWidget(index);

      Element tabBar = (Element) DomUtils.findNode(
            getElement(),
            true,
            false,
            new NodePredicate()
            {
               public boolean test(Node n)
               {
                  if (n.getNodeType() != Node.ELEMENT_NODE)
                     return false;
                  return ((Element) n).getClassName()
                        .contains("gwt-TabLayoutPanelTabs");
               }
            });

      if (!isVisible() || !isAttached() || tabBar.getOffsetWidth() == 0)
         return; // not yet loaded

      final Element tabBarParent = tabBar.getParentElement();

      final int start = tabBarParent.getScrollLeft();
      int end = DomUtils.ensureVisibleHoriz(tabBarParent,
                                                  tabWidget.getElement(),
                                                  padding_,
                                                  padding_ + rightMargin_,
                                                  true);

      // When tabs are closed, the overall width shrinks, and this can lead
      // to cases where there's too much empty space on the screen
      Widget lastTab = getTabWidget(getWidgetCount() - 1);
      int edge = DomUtils.getRelativePosition(tabBarParent,
                                              lastTab.getElement()).x;
      edge += lastTab.getOffsetWidth();
      end = Math.min(end,
                     Math.max(0,
                              edge - (tabBarParent.getOffsetWidth() - rightMargin_)));

      if (edge <= tabBarParent.getOffsetWidth() - rightMargin_)
         end = 0;

      if (start != end)
      {
         if (!animate)
         {
            tabBarParent.setScrollLeft(end);
         }
         else
         {
            final int finalEnd = end;
            currentAnimation_ = new Animation() {
               @Override
               protected void onUpdate(double progress)
               {
                  double delta = (finalEnd - start) * progress;
                  tabBarParent.setScrollLeft((int) (start + delta));
               }

               @Override
               protected void onComplete()
               {
                  if (this == currentAnimation_)
                  {
                     tabBarParent.setScrollLeft(finalEnd);
                     currentAnimation_ = null;
                  }
               }
            };
            currentAnimation_.run(Math.max(200,
                                           Math.min(1500,
                                                    Math.abs(end - start)*2)));
         }
      }
   }

   @Override
   public void onResize()
   {
      super.onResize();
      ensureSelectedTabIsVisible(false);
   }

  
   @Override
   public boolean remove(int index)
   {
      if ((index < 0) || (index >= getWidgetCount())) {
        return false;
      }

      fireEvent(new TabCloseEvent(index));

      if (getSelectedIndex() == index)
      {
         boolean closingLastTab = index == getWidgetCount() - 1;
         int indexToSelect = closingLastTab
                             ? index - 1
                             : index + 1;
         if (indexToSelect >= 0)
            selectTab(indexToSelect);
      }

      if (!super.remove(index))
         return false;

      fireEvent(new TabClosedEvent(index));
      ensureSelectedTabIsVisible(true);
      return true;
   }

   @Override
   public void add(Widget child, String text, boolean asHtml)
   {
      if (asHtml)
         throw new UnsupportedOperationException("HTML tab names not supported");

      add(child, text);
   }

   @Override
   public void add(Widget child, Widget tab)
   {
      throw new UnsupportedOperationException("Not supported");
   }

   private class DocTab extends Composite
   {

      private DocTab(ImageResource icon,
                     String title,
                     String tooltip,
                     ClickHandler clickHandler)
      {
         HorizontalPanel layoutPanel = new HorizontalPanel();
         layoutPanel.setStylePrimaryName(styles_.tabLayout());
         layoutPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);

         HTML left = new HTML();
         left.setStylePrimaryName(styles_.tabLayoutLeft());
         layoutPanel.add(left);

         contentPanel_ = new HorizontalPanel();
         contentPanel_.setTitle(tooltip);
         contentPanel_.setStylePrimaryName(styles_.tabLayoutCenter());
         contentPanel_.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

         if (icon != null)
            contentPanel_.add(imageForIcon(icon));

         label_ = new Label(title, false);
         appendDirtyMarker();
         contentPanel_.add(label_);

         Image img = new Image(ThemeResources.INSTANCE.closeTab());
         img.setStylePrimaryName(styles_.closeTabButton());
         img.addClickHandler(clickHandler);
         contentPanel_.add(img);

         layoutPanel.add(contentPanel_);

         HTML right = new HTML();
         right.setStylePrimaryName(styles_.tabLayoutRight());
         layoutPanel.add(right);

         initWidget(layoutPanel);
      }
      
      private void appendDirtyMarker()
      {
         SpanElement span = Document.get().createSpanElement();
         span.setInnerText("*");
         span.setClassName(styles_.dirtyTabIndicator());
         label_.getElement().appendChild(span);
      }

      public void replaceTitle(String title)
      {
         label_.setText(title);
         appendDirtyMarker();
      }

      public void replaceTooltip(String tooltip)
      {
         contentPanel_.setTitle(tooltip);
      }

      public void replaceIcon(ImageResource icon)
      {
         if (contentPanel_.getWidget(0) instanceof Image)
            contentPanel_.remove(0);
         contentPanel_.insert(imageForIcon(icon), 0);
      }
      
      public void addMouseDownHandler(MouseDownHandler handler)
      {
         if (handler != null)
            contentPanel_.addDomHandler(handler, MouseDownEvent.getType());
      }

      private Image imageForIcon(ImageResource icon)
      {
         Image image = new Image(icon);
         image.setStylePrimaryName(styles_.docTabIcon());
         return image;
      }

      private final Label label_;

      private final HorizontalPanel contentPanel_;
   }
   public void replaceDocName(int index,
                              ImageResource icon,
                              String title,
                              String tooltip)
   {
      DocTab tab = (DocTab) getTabWidget(index);
      tab.replaceIcon(icon);
      tab.replaceTitle(title);
      tab.replaceTooltip(tooltip);
   }

   public HandlerRegistration addTabClosingHandler(TabClosingHandler handler)
   {
      return addHandler(handler, TabClosingEvent.TYPE);
   }
   
   @Override
   public HandlerRegistration addTabCloseHandler(
         TabCloseHandler handler)
   {
      return addHandler(handler, TabCloseEvent.TYPE);
   }

   public HandlerRegistration addTabClosedHandler(TabClosedHandler handler)
   {
      return addHandler(handler, TabClosedEvent.TYPE);
   }

   public int getTabsEffectiveWidth()
   {
      if (getWidgetCount() == 0)
         return 0;

      Widget leftTabWidget = getTabWidget(0);
      Widget rightTabWidget = getTabWidget(getWidgetCount()-1);
      return (rightTabWidget.getAbsoluteLeft() + rightTabWidget.getOffsetWidth())
            - leftTabWidget.getAbsoluteLeft();
   }

   public static final int BAR_HEIGHT = 24;

   private final boolean closeableTabs_;
   private int padding_;
   private int rightMargin_;
   private final ThemeStyles styles_;
   private Animation currentAnimation_;
}
