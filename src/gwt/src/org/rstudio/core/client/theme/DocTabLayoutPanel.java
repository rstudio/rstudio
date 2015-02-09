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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;

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
                 HasTabClosedHandlers,
                 HasTabReorderHandlers
{
   public interface TabCloseObserver
   {
      public void onTabClose();
   }
   
   public interface TabMoveObserver
   {
      public void onTabMove(Widget tab, int oldPos, int newPos);
   }

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
   
   public void add(final Widget child,
                   ImageResource icon,
                   final String text,
                   String tooltip)
   {
      if (closeableTabs_)
      {
         DocTab tab = new DocTab(icon, text, tooltip, 
         new TabCloseObserver()
         {
            public void onTabClose()
            {
               int index = getWidgetIndex(child);
               if (index >= 0)
               {
                  tryCloseTab(index, null);
               }
            }
         }, 
         new TabMoveObserver()
         {
            @Override
            public void onTabMove(Widget tab, int oldPos, int newPos)
            {
               TabReorderEvent event = new TabReorderEvent(oldPos, newPos);
               fireEvent(event);
            }
         });
         super.add(child, tab);
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

      Element selectedTab = (Element) DomUtils.findNode(
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
                        .contains("gwt-TabLayoutPanelTab-selected");
               }
            });
      if (selectedTab == null)
      {
         return;
      }
      selectedTab = selectedTab.getFirstChildElement()
                               .getFirstChildElement();

      Element tabBar = getTabBarElement();

      if (!isVisible() || !isAttached() || tabBar.getOffsetWidth() == 0)
         return; // not yet loaded

      final Element tabBarParent = tabBar.getParentElement();

      final int start = tabBarParent.getScrollLeft();
      int end = DomUtils.ensureVisibleHoriz(tabBarParent,
                                                  selectedTab,
                                                  padding_,
                                                  padding_ + rightMargin_,
                                                  true);

      // When tabs are closed, the overall width shrinks, and this can lead
      // to cases where there's too much empty space on the screen
      Node lastTab = getLastChildElement(tabBar);
      if (lastTab == null || lastTab.getNodeType() != Node.ELEMENT_NODE)
         return;
      int edge = DomUtils.getRelativePosition(tabBarParent, Element.as(lastTab)).x 
            + Element.as(lastTab).getOffsetWidth();
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
                     TabCloseObserver closeHandler,
                     TabMoveObserver moveHandler)
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
         label_.addStyleName(styles_.docTabLabel());
         contentPanel_.add(label_);

         appendDirtyMarker();

         Image img = new Image(ThemeResources.INSTANCE.closeTab());
         img.setStylePrimaryName(styles_.closeTabButton());
         img.addStyleName(ThemeStyles.INSTANCE.handCursor());
         contentPanel_.add(img);

         layoutPanel.add(contentPanel_);

         HTML right = new HTML();
         right.setStylePrimaryName(styles_.tabLayoutRight());
         layoutPanel.add(right);

         initWidget(layoutPanel);

         this.sinkEvents(Event.ONMOUSEDOWN | 
                        Event.ONMOUSEMOVE | 
                        Event.ONMOUSEUP |
                        Event.ONLOSECAPTURE);
         closeHandler_ = closeHandler;
         moveHandler_ = moveHandler;
         closeElement_ = img.getElement();
      }
      
      private void appendDirtyMarker()
      {
         Label marker = new Label("*");
         marker.setStyleName(styles_.dirtyTabIndicator());
         contentPanel_.insert(marker, 2);
      }

      public void replaceTitle(String title)
      {
         label_.setText(title);
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
      
      private Image imageForIcon(ImageResource icon)
      {
         Image image = new Image(icon);
         image.setStylePrimaryName(styles_.docTabIcon());
         return image;
      }

      @Override
      public void onBrowserEvent(Event event) 
      {  
         switch(DOM.eventGetType(event))
         {
            case Event.ONMOUSEDOWN: 
            {
               // handle mousedowns as drag initiations (unless user is reaching
               // for the close button)
               if (event.getButton() == Event.BUTTON_LEFT && 
                     Element.as(event.getEventTarget()) != closeElement_)
               {
                  beginDrag(event);
                  event.preventDefault();
                  event.stopPropagation();
               }
               break;
            }
           
            case Event.ONMOUSEMOVE: 
            {
               if (dragging_) 
               {
                  drag(event);
                  event.preventDefault();
                  event.stopPropagation();
               }
               break;
            }
           
            case Event.ONMOUSEUP:
            {
               // middlemouse should close a tab
               if (event.getButton() == Event.BUTTON_MIDDLE)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  closeHandler_.onTabClose();
                  break;
               }
               
               // note: fallthrough
            }
            case Event.ONLOSECAPTURE: 
            {
               if (Element.as(event.getEventTarget()) == closeElement_)
               {
                  // handle click on close button
                  closeHandler_.onTabClose();
               }
               else if (dragging_)
               {
                  // complete dragging
                  endDrag(event);
               }
               event.preventDefault();
               event.stopPropagation();
               break;   
            }
         }
         super.onBrowserEvent(event);
      }

      private void beginDrag(Event evt)
      {
         // set drag element state
         dragging_ = true;
         dragElement_ = getElement().getParentElement().getParentElement();
         dragTabsHost_ = dragElement_.getParentElement();
         dragScrollHost_ = dragTabsHost_.getParentElement();
         outOfBounds_ = 0;
         candidatePos_ = 0;

         // find the current position of this tab among its siblings--we'll use
         // this later to determine whether to shift siblings left or right
         for (int i = 0; i < dragTabsHost_.getChildCount(); i++)
         {
            Node node = dragTabsHost_.getChild(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
               if (Element.as(node) == dragElement_)
               {
                  candidatePos_ = i;
                  destPos_ = i;
               }
               // the relative position of the last node determines how far we
               // can drag--add 10px so it stretches a little
               dragMax_ = DomUtils.leftRelativeTo(dragTabsHost_, Element.as(node)) 
                     + (Element.as(node).getClientWidth() - dragElement_.getClientWidth())
                     + 10;
            }
         }
         startPos_ = candidatePos_;

         // snap the element out of the tabset
         lastElementX_ = DomUtils.leftRelativeTo(dragTabsHost_, dragElement_);
         lastCursorX_= evt.getClientX();
         dragElement_.getStyle().setPosition(Position.ABSOLUTE);
         dragElement_.getStyle().setLeft(lastElementX_, Unit.PX);
         dragElement_.getStyle().setZIndex(100);
         DOM.setCapture(getElement());

         // create the placeholder that shows where this tab will go when the
         // mouse is released
         dragPlaceholder_ = Document.get().createDivElement();
         dragPlaceholder_.getStyle().setWidth(dragElement_.getClientWidth(), 
               Unit.PX);
         dragPlaceholder_.getStyle().setHeight(2, Unit.PX);
         dragPlaceholder_.getStyle().setDisplay(Display.INLINE_BLOCK);
         dragPlaceholder_.getStyle().setPosition(Position.RELATIVE);
         dragPlaceholder_.getStyle().setFloat(Float.LEFT);
         dragTabsHost_.insertAfter(dragPlaceholder_, dragElement_);
      }
      
      private void endDrag(final Event evt)
      {
         // remove the properties used to position for dragging
         dragElement_.getStyle().clearLeft();
         dragElement_.getStyle().clearPosition();
         dragElement_.getStyle().clearZIndex();
         
         // insert this tab where the placeholder landed
         dragTabsHost_.removeChild(dragElement_);
         dragTabsHost_.insertAfter(dragElement_, dragPlaceholder_);
         dragTabsHost_.removeChild(dragPlaceholder_);

         // finish dragging
         DOM.releaseCapture(getElement());
         dragging_ = false;
         
         // unsink mousedown/mouseup and simulate a click to activate the tab
         simulateClick(evt);
         
         // let observer know we moved; adjust the destination position one to
         // the left if we're right of the start position to account for the
         // position of the tab prior to movement
         if (startPos_ != destPos_)
         {
            moveHandler_.onTabMove(this, startPos_, destPos_);
         }
      }
      
      private void simulateClick(Event evt)
      {
         this.unsinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP);
         DomEvent.fireNativeEvent(Document.get().createClickEvent(0,
               evt.getScreenX(), evt.getScreenY(), 
               evt.getClientX(), evt.getClientY(), 
               evt.getCtrlKey(), evt.getAltKey(), 
               evt.getShiftKey(), evt.getMetaKey()), this.getParent());
         this.sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP);
         
      }
      
      private boolean autoScroll(int dir)
      {
         // move while the mouse is still out of bounds 
         if (dragging_ && outOfBounds_ != 0)
         {
            // if mouse is far out of bounds, use it to accelerate movement
            if (Math.abs(outOfBounds_) > 100)
            {
               dir *= 1 + Math.round(Math.abs(outOfBounds_) / 75);
            }
            // move if there's moving to be done
            if (dir < 0 && lastElementX_ > 0 ||
                dir > 0 && lastElementX_ < dragMax_)
            {
               commitPosition(lastElementX_ + dir);

               // scroll if there's scrolling to be done
               int left = dragScrollHost_.getScrollLeft();
               if ((dir < 0 && left > 0) || 
                   (dir > 0 && left < dragScrollHost_.getScrollWidth() - 
                        dragScrollHost_.getClientWidth()))
               {
                  dragScrollHost_.setScrollLeft(left + dir);
               }
            }
            return true;
         }
         return false;
      }
      
      private void drag(Event evt) 
      {
         int offset = evt.getClientX() - lastCursorX_;
         lastCursorX_ = evt.getClientX();
         // cursor is outside the tab area
         if (outOfBounds_ != 0)
         {
            // did the cursor move back in bounds? 
            if (outOfBounds_ + offset > 0 != outOfBounds_ > 0)
            {
               outOfBounds_ = 0;
               offset = outOfBounds_ + offset;
            }
            else 
            {
               // cursor is still out of bounds
               outOfBounds_ += offset;
               return;
            }
         }
         int targetLeft = lastElementX_ + offset;
         int targetRight = targetLeft + dragElement_.getClientWidth();
         int scrollLeft = dragScrollHost_.getScrollLeft();
         if (targetLeft < 0)
         {
            // dragged past the beginning - lock to beginning
            targetLeft = 0;
            outOfBounds_ += offset;
         }
         else if (targetLeft > dragMax_)
         {
            // dragged past the end - lock to the end
            targetLeft = dragMax_;
            outOfBounds_ += offset;
         }
         if (targetLeft - scrollLeft < SCROLL_THRESHOLD &&
               scrollLeft > 0)
         {
            // dragged past scroll threshold, to the left--autoscroll 
            outOfBounds_ = (targetLeft - scrollLeft) - SCROLL_THRESHOLD;
            targetLeft = scrollLeft + SCROLL_THRESHOLD;
            Scheduler.get().scheduleFixedPeriod(new RepeatingCommand()
            {
               @Override
               public boolean execute()
               {
                  return autoScroll(-1);
               }
            }, 5);
         } 
         else if (targetRight + SCROLL_THRESHOLD > scrollLeft + 
                     dragScrollHost_.getClientWidth() &&
                  scrollLeft < dragScrollHost_.getScrollWidth() - 
                     dragScrollHost_.getClientWidth())
         {
            // dragged past scroll threshold, to the right--autoscroll 
            outOfBounds_ = (targetRight + SCROLL_THRESHOLD) - 
                  (scrollLeft + dragScrollHost_.getClientWidth());
            targetLeft = scrollLeft + dragScrollHost_.getClientWidth() - 
                  (dragElement_.getClientWidth() + SCROLL_THRESHOLD);
            Scheduler.get().scheduleFixedPeriod(new RepeatingCommand()
            {
               @Override
               public boolean execute()
               {
                  return autoScroll(1);
               }
            }, 5);
         } 
         commitPosition(targetLeft);
      }
      
      private void commitPosition(int pos)
      {
         lastElementX_ = pos;

         // move element to its new position
         dragElement_.getStyle().setLeft(lastElementX_, Unit.PX);

         // check to see if we're overlapping with another tab 
         for (int i = 0; i < dragTabsHost_.getChildCount(); i++)
         {
            // skip non-element DOM nodes
            Node node = dragTabsHost_.getChild(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
               continue;
            }
            // skip the element we're dragging and elements that are not tabs
            Element ele = (Element)node;
            if (ele == dragElement_ || 
                ele.getClassName().indexOf("gwt-TabLayoutPanelTab") < 0)
            {
               continue;
            }

            int left = DomUtils.leftRelativeTo(dragTabsHost_, ele);
            int right = left + ele.getClientWidth();
            int minOverlap = Math.min(dragElement_.getClientWidth() / 2, 
                  ele.getClientWidth() / 2);

            // a little complicated: compute the number of overlapping pixels
            // with this element; if the overlap is more than half of our width
            // (or the width of the candidate), it's swapping time
            if (Math.min(lastElementX_ + dragElement_.getClientWidth(), right) - 
                Math.max(lastElementX_, left) >= minOverlap)
            {
               dragTabsHost_.removeChild(dragPlaceholder_);
               if (candidatePos_ > i)
               {
                  dragTabsHost_.insertBefore(dragPlaceholder_, ele);
               }
               else
               {
                  dragTabsHost_.insertAfter(dragPlaceholder_, ele);
               }
               candidatePos_ = i;

               // account for the extra element when moving to the right of the
               // original location
               destPos_ = startPos_ <= candidatePos_ ? 
                     candidatePos_ - 1 : candidatePos_;
            }
         }
         
      }
      
      private TabCloseObserver closeHandler_;
      private TabMoveObserver moveHandler_;
      private Element closeElement_;
      private boolean dragging_ = false;
      private int lastCursorX_ = 0;
      private int lastElementX_ = 0;
      private int startPos_ = 0;
      private int candidatePos_ = 0;
      private int destPos_ = 0;
      private int dragMax_ = 0;
      private int outOfBounds_ = 0;
      private Element dragElement_;
      private Element dragTabsHost_;
      private Element dragScrollHost_;
      private Element dragPlaceholder_;
      private final Label label_;
      private final static int SCROLL_THRESHOLD = 25;

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

   @Override
   public HandlerRegistration addTabReorderHandler(TabReorderHandler handler)
   {
      return addHandler(handler, TabReorderEvent.TYPE);
   }

   public int getTabsEffectiveWidth()
   {
      if (getWidgetCount() == 0)
         return 0;

      Element parent = getTabBarElement();
      if (parent == null)
      {
         return 0;
      }
      Element lastChild = getLastChildElement(parent);
      if (lastChild == null)
      {
         return 0;
      }
      return lastChild.getOffsetLeft() + lastChild.getOffsetWidth();
   }
   
   
   private Element getTabBarElement()
   {
      return (Element) DomUtils.findNode(
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
   }
   
   private Element getLastChildElement(Element parent)
   {
      Node lastTab = parent.getLastChild();
      while (lastTab.getNodeType() != Node.ELEMENT_NODE)
      {
         lastTab = lastTab.getPreviousSibling();
      }
      return Element.as(lastTab);
   }

   public static final int BAR_HEIGHT = 24;

   private final boolean closeableTabs_;
   private int padding_;
   private int rightMargin_;
   private final ThemeStyles styles_;
   private Animation currentAnimation_;
}
