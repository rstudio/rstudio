/*
 * 
 * DocTabLayoutPanel.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;
import org.rstudio.core.client.events.HasTabCloseHandlers;
import org.rstudio.core.client.events.HasTabClosedHandlers;
import org.rstudio.core.client.events.HasTabClosingHandlers;
import org.rstudio.core.client.events.HasTabReorderHandlers;
import org.rstudio.core.client.events.TabCloseEvent;
import org.rstudio.core.client.events.TabCloseHandler;
import org.rstudio.core.client.events.TabClosedEvent;
import org.rstudio.core.client.events.TabClosedHandler;
import org.rstudio.core.client.events.TabClosingEvent;
import org.rstudio.core.client.events.TabClosingHandler;
import org.rstudio.core.client.events.TabReorderEvent;
import org.rstudio.core.client.events.TabReorderHandler;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragStartedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocWindowChangedEvent;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;


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
      dragManager_ = new DragManager();
      
      // listen for global drag events (these are broadcasted from other windows
      // to notify us of incoming drags)
      EventBus events = RStudioGinjector.INSTANCE.getEventBus();
      events.addHandler(DocTabDragStartedEvent.TYPE, dragManager_);

      // sink drag-related events on the tab bar element; unfortunately
      // GWT does not provide bits for the drag-related events, and 
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            Element tabBar = getTabBarElement();
            DOM.sinkBitlessEvent(tabBar, "dragenter");
            DOM.sinkBitlessEvent(tabBar, "dragover");
            DOM.sinkBitlessEvent(tabBar, "dragend");
            DOM.sinkBitlessEvent(tabBar, "dragleave");
            DOM.sinkBitlessEvent(tabBar, "drop");
            Event.setEventListener(tabBar, dragManager_);
         }
      });
   }

   @Override
   public void add(final Widget child, String text)
   {
      add(child, null, null, text, null, null);
   }

   public void add(final Widget child, String docId, String text)
   {
      add(child, null, docId, text, null, null);
   }
   
   public void add(final Widget child,
                   ImageResource icon,
                   String docId,
                   final String text,
                   String tooltip,
                   Integer position)
   {
      if (closeableTabs_)
      {
         DocTab tab = new DocTab(icon, docId, text, tooltip, 
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
         });
         docTabs_.add(tab);
         if (position == null || position < 0)
            super.add(child, tab);
         else
            super.insert(child, tab, position);
      }
      else
      {
         if (position == null || position < 0)
            super.add(child, text);
         else
            super.insert(child, text, position);
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

      docTabs_.remove(index);
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
   
   public void cancelTabDrag()
   {
      dragManager_.endDrag(null, true);
   }

   private class DragManager implements EventListener,
      DocTabDragStartedEvent.Handler
   {
      @Override
      public void onBrowserEvent(Event event)
      {
         if (event.getType() == "dragenter")
         {
            if (!dragging_)
               beginDrag(event);
            event.preventDefault();
         }
         else if (event.getType() == "dragover")
         {
            if (dragging_)
               drag(event);
            event.preventDefault();
         }
         else if (event.getType() == "drop")
         {
            endDrag(event, false);
            event.preventDefault();
         }
         else if (event.getType() == "dragend")
         {
            if (dragging_)
            {
               endDrag(event, true);
            }
         }
         else if (event.getType() == "dragleave")
         {
            if (!dragging_)
               return;

            // look at where the cursor is now--if it's inside the tab panel,
            // do nothing, but if it's outside the tab panel, treat that as
            // a cancel
            Element ele = DomUtils.elementFromPoint(event.getClientX(), 
                  event.getClientY());
            do
            {
               if (ele.getClassName().contains("gwt-TabLayoutPanelTabs"))
               {
                  return;
               }
               ele = ele.getParentElement();
            } while (ele != null && ele != Document.get().getBody());
            
            // cursor moved outside tab panel
            endDrag(event, true);
         }
      }

      @Override
      public void onDocTabDragStarted(DocTabDragStartedEvent event)
      {
         initDragWidth_ = event.getWidth();
         initDragDocId_ = event.getDocId();
      }
      
      public void beginDrag(Event evt)
      {
         String docId = initDragDocId_;
         int dragTabWidth = initDragWidth_;
         
         // set drag element state
         dragging_ = true;
         dragTabsHost_ = getTabBarElement();
         dragScrollHost_ = dragTabsHost_.getParentElement();
         outOfBounds_ = 0;
         candidatePos_ = null;

         // figure out which tab the cursor is over so we can use this as the
         // start position in the drag
         Element ele = DomUtils.elementFromPoint(evt.getClientX(), 
               evt.getClientY());
         do
         {
            if (ele.getClassName().contains("gwt-TabLayoutPanelTabs"))
            {
               // the cursor is over the tab panel itself--append to end
               candidatePos_ = docTabs_.size() - 1;
               break;
            }
            else if (ele.getClassName().contains("gwt-TabLayoutPanelTab "))
            {
               // the cursor is inside a tab--figure out which one
               for (int i = 0; i < dragTabsHost_.getChildCount(); i++)
               {
                  Node node = dragTabsHost_.getChild(i);
                  if (node.getNodeType() == Node.ELEMENT_NODE &&
                      Element.as(node) == ele)
                  {
                     candidatePos_ = i;
                     break;
                  }
               }
               break;
            }
            ele = ele.getParentElement();
         } while (ele != null && ele != Document.get().getBody());
         
         // couldn't figure out where to drop tab
         if (candidatePos_ == null)
            return;

         startPos_ = candidatePos_;

         // the relative position of the last node determines how far we
         // can drag--add 10px so it stretches a little
         dragMax_ = DomUtils.leftRelativeTo(dragTabsHost_, 
               getLastChildElement(dragTabsHost_)) + 
               getLastChildElement(dragTabsHost_).getClientWidth() + 
               10;
         lastCursorX_= evt.getClientX();
         lastElementX_ = DomUtils.leftRelativeTo(
               dragTabsHost_, Element.as(dragTabsHost_.getChild(candidatePos_)));
         
         // attempt to ascertain whether the element being dragged is one of
         // our own documents
         for (DocTab tab: docTabs_)
         {
            if (tab.getDocId() == docId)
            {
               dragElement_ = 
                     tab.getElement().getParentElement().getParentElement();
               break;
            }
         }
         
         // if we're dragging one of our own tabs, snap it out of the 
         // tabset
         if (dragElement_ != null)
         {
            dragElement_.getStyle().setPosition(Position.ABSOLUTE);
            dragElement_.getStyle().setLeft(lastElementX_, Unit.PX);
            dragElement_.getStyle().setZIndex(100);
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                 dragElement_.getStyle().setDisplay(Display.NONE);
               }
            });
         }

         // create the placeholder that shows where this tab will go when the
         // mouse is released
         dragPlaceholder_ = Document.get().createDivElement();
         dragPlaceholder_.getStyle().setWidth(dragTabWidth, Unit.PX);
         dragPlaceholder_.getStyle().setHeight(100, Unit.PCT);
         dragPlaceholder_.getStyle().setDisplay(Display.INLINE_BLOCK);
         dragPlaceholder_.getStyle().setPosition(Position.RELATIVE);
         dragPlaceholder_.getStyle().setFloat(Float.LEFT);
         dragPlaceholder_.getStyle().setBorderStyle(BorderStyle.DOTTED);
         dragPlaceholder_.getStyle().setBorderColor("#414243");
         dragPlaceholder_.getStyle().setBorderWidth(2, Unit.PX);
         dragPlaceholder_.getStyle().setProperty("boxSizing", "border-box");
         dragPlaceholder_.getStyle().setProperty("borderRadius", "3px");
         dragPlaceholder_.getStyle().setProperty("borderBottom", "0px");
         dragTabsHost_.insertAfter(dragPlaceholder_, 
               dragTabsHost_.getChild(candidatePos_));
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
               Debug.devlog("out of bounds by " + outOfBounds_);
               return;
            }
         }

         int targetLeft = lastElementX_ + offset;
         int targetRight = targetLeft + initDragWidth_;
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
                  (initDragWidth_ + SCROLL_THRESHOLD);
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
            int minOverlap = Math.min(initDragWidth_ / 2, 
                  ele.getClientWidth() / 2);
            
            // a little complicated: compute the number of overlapping pixels
            // with this element; if the overlap is more than half of our width
            // (or the width of the candidate), it's swapping time
            if (Math.min(lastElementX_ + initDragWidth_, right) - 
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
               if (dragElement_ != null)
               {
                  destPos_ = startPos_ <= candidatePos_ ? 
                        candidatePos_ - 1 : candidatePos_;
               }
               else
               {
                  destPos_ = candidatePos_;
               }
               Debug.devlog("new dest pos: " + destPos_);
            }
         }
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
      
      public void endDrag(final Event evt, boolean cancel)
      {
         if (!dragging_)
            return;
         
         // remove the properties used to position for dragging
         if (dragElement_ != null)
         {
            dragElement_.getStyle().clearLeft();
            dragElement_.getStyle().clearPosition();
            dragElement_.getStyle().clearZIndex();
            dragElement_.getStyle().clearDisplay();
            
            // insert this tab where the placeholder landed if we're not
            // cancelling
            if (!cancel)
            {
               dragTabsHost_.removeChild(dragElement_);
               dragTabsHost_.insertAfter(dragElement_, dragPlaceholder_);
            }
         }
         
         if (dragPlaceholder_ != null)
         {
            dragTabsHost_.removeChild(dragPlaceholder_);
            dragPlaceholder_ = null;
         }

         // finish dragging
         DOM.releaseCapture(getElement());
         dragging_ = false;
         
         if (dragElement_ != null && !cancel)
         {
            // unsink mousedown/mouseup and simulate a click to activate the tab
            if (evt != null)
               simulateClick(evt);
            
            // let observer know we moved; adjust the destination position one to
            // the left if we're right of the start position to account for the
            // position of the tab prior to movement
            if (startPos_ != destPos_)
            {
               TabReorderEvent event = new TabReorderEvent(startPos_, destPos_);
               fireEvent(event);
            }
         }
         
         // this is the case when we adopt someone else's doc
         if (dragElement_ == null && evt != null && !cancel)
         {
            // pull the document ID and source window out
            String data = evt.getDataTransfer().getData("text");
            if (StringUtil.isNullOrEmpty(data))
               return;
            
            // the data format is docID|windowID; windowID can be omitted if
            // the main window is the origin
            String pieces[] = data.split("\\|");
            if (pieces.length < 1)
               return;
            
            RStudioGinjector.INSTANCE.getEventBus().fireEvent(new
                  DocWindowChangedEvent(pieces[0], 
                        pieces.length > 1 ? pieces[1] : "", 
                              destPos_));
         }
         
         dragElement_ = null;
      }

      private void simulateClick(Event evt)
      {
         /*
         DomEvent.fireNativeEvent(Document.get().createClickEvent(0,
               evt.getScreenX(), evt.getScreenY(), 
               evt.getClientX(), evt.getClientY(), 
               evt.getCtrlKey(), evt.getAltKey(), 
               evt.getShiftKey(), evt.getMetaKey()), this.getParent());
               */
      }
      
      private boolean dragging_ = false;
      private int lastCursorX_ = 0;
      private int lastElementX_ = 0;
      private int startPos_ = 0;
      private Integer candidatePos_ = null;
      private int destPos_ = 0;
      private int dragMax_ = 0;
      private int outOfBounds_ = 0;
      private int initDragWidth_;
      private Element dragElement_;
      private Element dragTabsHost_;
      private Element dragScrollHost_;
      private Element dragPlaceholder_;
      private final static int SCROLL_THRESHOLD = 25;
      private String initDragDocId_;
   }

   private class DocTab extends Composite
   {
      private DocTab(ImageResource icon,
                     String docId,
                     String title,
                     String tooltip,
                     TabCloseObserver closeHandler)
      {
         docId_ = docId;
         
         final HorizontalPanel layoutPanel = new HorizontalPanel();
         layoutPanel.setStylePrimaryName(styles_.tabLayout());
         layoutPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
         layoutPanel.getElement().setDraggable("true");
         layoutPanel.addDomHandler(new DragStartHandler()
         {
            @Override
            public void onDragStart(DragStartEvent evt)
            {
               evt.getDataTransfer().setData("text", docId_ + "|" + 
                  RStudioGinjector.INSTANCE.getSourceWindowManager()
                                           .getSourceWindowId());
               RStudioGinjector.INSTANCE.getEventBus().fireEvent(new
                     DocTabDragStartedEvent(docId_, 
                           getElement().getClientWidth()));
            }
         }, DragStartEvent.getType());

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

         this.sinkEvents(Event.ONMOUSEMOVE |
               Event.ONMOUSEUP |
               Event.ONLOSECAPTURE);
         closeHandler_ = closeHandler;
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
      
      public String getDocId()
      {
         return docId_;
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
               event.preventDefault();
               event.stopPropagation();
               break;   
            }
         }
         super.onBrowserEvent(event);
      }

      
      private TabCloseObserver closeHandler_;
      private Element closeElement_;
      private final Label label_;
      private final String docId_;

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
   
   @Override
   public void onBrowserEvent(Event event) 
   {  
      super.onBrowserEvent(event);
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
   private DragManager dragManager_;
   private ArrayList<DocTab> docTabs_ = new ArrayList<DocTab>();
}
