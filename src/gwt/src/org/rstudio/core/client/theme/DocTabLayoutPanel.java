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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;
import org.rstudio.core.client.dom.ElementEx;
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
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragInitiatedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragStartedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragStateChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocWindowChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocInitiatedEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;

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
      events_ = RStudioGinjector.INSTANCE.getEventBus();
      events_.addHandler(DocTabDragStartedEvent.TYPE, dragManager_);

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
            DOM.sinkBitlessEvent(tabBar, "mousewheel");
            DOM.sinkBitlessEvent(tabBar, "wheel");
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
   
   public void moveTab(int index, int delta)
   {
      // do no work if we haven't been asked to move anywhere
      if (delta == 0)
         return;

      Element tabHost = getTabBarElement();
      
      // ignore moving left if the tab is already the leftmost tab (same for
      // right)
      int dest = index + delta;
      if (dest < 0 || dest >= tabHost.getChildCount())
         return;
      
      // rearrange the DOM 
      Element tab = Element.as(tabHost.getChild(index));
      Element prev = Element.as(tabHost.getChild(dest));
      tabHost.removeChild(tab);
      if (delta > 0)
         tabHost.insertAfter(tab, prev);
      else
         tabHost.insertBefore(tab, prev);

      // fire the tab reorder event (this syncs the editor state)
      TabReorderEvent event = new TabReorderEvent(index, dest);
      fireEvent(event);
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

   public void cancelTabDrag()
   {
      dragManager_.endDrag(null, DragManager.ACTION_CANCEL);
   }

   private class DragManager implements EventListener, 
                                        DocTabDragStartedEvent.Handler
   {
      @Override
      public void onBrowserEvent(Event event)
      {
         if (event.getType() == "dragenter")
         {
            if (dropPoint_ != null && event.getClientX() == dropPoint_.getX() &&
                  event.getClientY() == dropPoint_.getY())
            {
               // Very occasionally (~5%?), dropping a tab will generate a
               // superfluous "dragenter" event immediately after the drop event
               // at exactly the same coordinates. We want to ignore this since
               // it will send us into dragging state; to do so, we cache the 
               // coordinates when a tab is dropped and suppress entering drag
               // mode from those coordinates very briefly (note that this won't
               // keep the user from immediately dragging the tab around since
               // you need to move the cursor in some way to initiate a drag,
               // invalidating the coordinates.)
               dropPoint_ = null;
               return;
            }

            if (curState_ == STATE_EXTERNAL)
            {
               // element that was being dragged around outside boundaries
               // has come back in; clear it and treat like a new drag
               dragElement_.getStyle().clearOpacity();
               dragElement_ = null;
               curState_ = STATE_NONE;
            }
            if (curState_ == STATE_NONE)
            {
               // if we know what we're dragging, initiate it; otherwise, let 
               // the event continue unimpeded (so we won't appear as a drag
               // target)
               if (initDragParams_ == null)
                  return;
               else
                  beginDrag(event);
            }
            event.preventDefault();
         }
         else if (event.getType() == "dragover")
         {
            if (curState_ == STATE_DRAGGING)
               drag(event);
            event.preventDefault();
         }
         else if (event.getType() == "drop")
         {
            endDrag(event, ACTION_COMMIT);
            event.preventDefault();

            // cache the drop point for 250ms (see comments in event handler for
            // dragenter)
            dropPoint_ = new Point(event.getClientX(), event.getClientY());
            Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand()
            {
               @Override
               public boolean execute()
               {
                  dropPoint_ = null;
                  return false;
               }
            }, 250);
         }
         else if (event.getType() == "dragend")
         {
            if (curState_ != STATE_NONE)
            {
               endDrag(event, ACTION_CANCEL);
            }
            event.preventDefault();
         }
         else if (event.getType() == "dragleave")
         {
            if (curState_ == STATE_NONE)
               return;
            
            // when a drag leaves the window entirely, we get a dragleave event
            // at 0, 0 (which we always want to treat as a cancel)
            if (!(event.getClientX() == 0 && event.getClientY() == 0))
            {
               // look at where the cursor is now--if it's inside the tab panel,
               // do nothing, but if it's outside the tab panel, treat that as
               // a cancel
               Element ele = DomUtils.elementFromPoint(event.getClientX(), 
                     event.getClientY());
               while (ele != null && ele != Document.get().getBody())
               {
                  if (ele.getClassName().contains("gwt-TabLayoutPanelTabs"))
                  {
                     return;
                  }
                  ele = ele.getParentElement();
               } 
            }
            
            if (dragElement_ != null)
            {
               // dim the element being drag to hint that it'll be moved
               endDrag(event, ACTION_EXTERNAL);
            }
            else
            {
               // if this wasn't our element, we can cancel the drag entirely
               endDrag(event, ACTION_CANCEL);
            }
         }
         else if (event.getType() == "wheel" ||
                  event.getType() == "mousewheel")
         {
            // extract the delta from the wheel event (note that this could be
            // zero)
            JsObject evt = event.cast();
            double delta = evt.getDouble(event.getType() == "wheel" ?
                  "deltaY" : "wheelDeltaY");
            
            // translate wheel scroll into tab selection; don't wrap
            int idx = getSelectedIndex();
            if (delta > 0 && idx < (getWidgetCount() - 1))
               selectTab(idx + 1);
            else if (delta < 0 && idx > 0)
               selectTab(idx - 1);
         }
      }

      @Override
      public void onDocTabDragStarted(DocTabDragStartedEvent event)
      {
         initDragParams_ = event.getDragParams();
         initDragWidth_ = initDragParams_.getTabWidth();
      }
      
      private void beginDrag(Event evt)
      {
         // skip if we don't know what we're dragging -- these parameters 
         // should get injected by the editor but might not if a failure occurs
         // during its processing of the DocTabDragInitiatedEvent.
         if (initDragParams_ == null)
            return;
         
         String docId = initDragParams_.getDocId();
         int dragTabWidth = initDragWidth_;
         
         // set drag element state
         dragTabsHost_ = getTabBarElement();
         dragScrollHost_ = dragTabsHost_.getParentElement();
         outOfBounds_ = 0;
         candidatePos_ = null;
         startPos_ = null;
         
         // attempt to determine which tab the cursor is over
         Point hostPos = DomUtils.getRelativePosition(
               Document.get().getBody(), dragTabsHost_);
         int dragX = evt.getClientX() - hostPos.getX();
         for (int i = 0; i < dragTabsHost_.getChildCount(); i++)
         {
            Node node = dragTabsHost_.getChild(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
               int left = DomUtils.leftRelativeTo(dragTabsHost_, 
                                                  Element.as(node)) -
                          dragScrollHost_.getScrollLeft(); 
               int right = left + Element.as(node).getOffsetWidth();
               if (left <= dragX && dragX <= right) 
               {
                  candidatePos_ = i;
                  break;
               }
            }
         }
         
         // let the rest of the IDE know we're dragging (this will enable us to
         // disable drag targets that might otherwise be happy to accept the
         // data)
         curState_ = STATE_DRAGGING;
         events_.fireEvent(new DocTabDragStateChangedEvent(
               DocTabDragStateChangedEvent.STATE_DRAGGING));

         // the relative position of the last node determines how far we
         // can drag
         dragMax_ = DomUtils.leftRelativeTo(dragTabsHost_, 
               getLastChildElement(dragTabsHost_)) + 
               getLastChildElement(dragTabsHost_).getClientWidth();
         lastCursorX_= evt.getClientX();
         
         // account for cursor starting out of bounds (e.g. dragging into 
         // empty space on the right of the panel)
         if (lastCursorX_ > dragMax_ + (initDragParams_.getCursorOffset()))
            outOfBounds_ = (lastCursorX_ - dragMax_) - 
               initDragParams_.getCursorOffset();
         
         // attempt to ascertain whether the element being dragged is one of
         // our own documents
         for (int i = 0; i < getWidgetCount(); i++)
         {
            DocTab tab = (DocTab)getTabWidget(i);
            if (tab.getDocId() == docId)
            {
               dragElement_ = 
                     tab.getElement().getParentElement().getParentElement();
               break;
            }
         }
         
         // if we couldn't find the horizontal drag position in any tab, append
         // to the end
         if (candidatePos_ == null)
         {
            candidatePos_ = dragTabsHost_.getChildCount();
         }
         
         destPos_ = candidatePos_;

         // if we're dragging one of our own documents, figure out its physical
         // position 
         if (dragElement_ != null)
         {
            for (int i = 0; i < dragTabsHost_.getChildCount(); i++) 
            {
               if (dragTabsHost_.getChild(i) == dragElement_)
               {
                  startPos_ = i;
                  break;
               }
            }
         }
         
         // compute the start location for the drag
         if (candidatePos_ >= dragTabsHost_.getChildCount())
         {
            Element lastTab = getLastChildElement(dragTabsHost_);
            lastElementX_ = DomUtils.leftRelativeTo(dragTabsHost_, lastTab) +
                  lastTab.getOffsetWidth();
         }
         else
         {
            lastElementX_ = DomUtils.leftRelativeTo(
                  dragTabsHost_, Element.as(dragTabsHost_.getChild(candidatePos_)));
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
                  // we may not still be dragging when the event loop comes
                  // back, so be sure there's an element before hiding it
                  if (dragElement_ != null)
                    dragElement_.getStyle().setDisplay(Display.NONE);
               }
            });
         }

         // create the placeholder that shows where this tab will go when the
         // mouse is released
         dragPlaceholder_ = Document.get().createDivElement();
         dragPlaceholder_.getStyle().setWidth(
               dragTabWidth - 4, Unit.PX);
         dragPlaceholder_.getStyle().setHeight(
               dragTabsHost_.getClientHeight() - 3, Unit.PX);
         dragPlaceholder_.getStyle().setDisplay(Display.INLINE_BLOCK);
         dragPlaceholder_.getStyle().setPosition(Position.RELATIVE);
         dragPlaceholder_.getStyle().setFloat(Float.LEFT);
         dragPlaceholder_.getStyle().setBorderStyle(BorderStyle.DOTTED);
         dragPlaceholder_.getStyle().setBorderColor("#A1A2A3");
         dragPlaceholder_.getStyle().setBorderWidth(1, Unit.PX);
         dragPlaceholder_.getStyle().setMarginLeft(1, Unit.PX);
         dragPlaceholder_.getStyle().setMarginRight(1, Unit.PX);
         dragPlaceholder_.getStyle().setProperty("borderTopLeftRadius", "4px");
         dragPlaceholder_.getStyle().setProperty("borderTopRightRadius", "4px");
         dragPlaceholder_.getStyle().setProperty("borderBottom", "0px");
         if (candidatePos_ < dragTabsHost_.getChildCount())
         {
            dragTabsHost_.insertBefore(dragPlaceholder_, 
                  dragTabsHost_.getChild(candidatePos_));
         }
         else
         {
            dragTabsHost_.appendChild(dragPlaceholder_);
         }
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

            // skip the current candidate (no point in testing it for swap)
            if (i == candidatePos_)
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
               if (dragElement_ != null && startPos_ != null)
               {
                  destPos_ = startPos_ <= candidatePos_ ? 
                        candidatePos_ - 1 : candidatePos_;
               }
               else
               {
                  destPos_ = candidatePos_;
               }
            }
         }
      }

      private boolean autoScroll(int dir)
      {
         // move while the mouse is still out of bounds 
         if (curState_ == STATE_DRAGGING && outOfBounds_ != 0)
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
      
      public void endDrag(final Event evt, int action)
      {
         if (curState_ == STATE_NONE)
            return;
         
         // remove the properties used to position for dragging
         if (dragElement_ != null)
         {
            dragElement_.getStyle().clearLeft();
            dragElement_.getStyle().clearPosition();
            dragElement_.getStyle().clearZIndex();
            dragElement_.getStyle().clearDisplay();
            dragElement_.getStyle().clearOpacity();
            
            // insert this tab where the placeholder landed if we're not
            // cancelling
            if (action == ACTION_COMMIT)
            {
               dragTabsHost_.removeChild(dragElement_);
               dragTabsHost_.insertAfter(dragElement_, dragPlaceholder_);
            }
         }
         
         // remove the placeholder
         if (dragPlaceholder_ != null)
         {
            dragTabsHost_.removeChild(dragPlaceholder_);
            dragPlaceholder_ = null;
         }
         
         if (dragElement_ != null && action == ACTION_EXTERNAL)
         {
            // if we own the dragged tab, change to external drag state
            dragElement_.getStyle().setOpacity(0.4);
            curState_ = STATE_EXTERNAL;
         }
         else
         {
            // otherwise, we're back to pristine
            curState_ = STATE_NONE;
            events_.fireEvent(new DocTabDragStateChangedEvent(
                  DocTabDragStateChangedEvent.STATE_NONE));
         }

         if (dragElement_ != null && action == ACTION_COMMIT)
         {
            // let observer know we moved; adjust the destination position one to
            // the left if we're right of the start position to account for the
            // position of the tab prior to movement
            if (startPos_ != null && startPos_ != destPos_)
            {
               TabReorderEvent event = new TabReorderEvent(startPos_, destPos_);
               fireEvent(event);
            }
         }
         
         // this is the case when we adopt someone else's doc
         if (dragElement_ == null && evt != null && action == ACTION_COMMIT)
         {
            // pull the document ID and source window out
            String data = evt.getDataTransfer().getData(
                  getDataTransferFormat());
            if (StringUtil.isNullOrEmpty(data))
               return;
            
            // the data format is docID|windowID; windowID can be omitted if
            // the main window is the origin
            String pieces[] = data.split("\\|");
            if (pieces.length < 1)
               return;
            
            events_.fireEvent(new DocWindowChangedEvent(pieces[0],
                  pieces.length > 1 ? pieces[1] : "", 
                  initDragParams_, null, destPos_));
         }
         
         // this is the case when our own drag ends; if it ended outside our
         // window and outside all satellites, treat it as a tab tear-off
         if (dragElement_ != null && evt != null && action == ACTION_CANCEL)
         {
            // if this is the last tab in satellite, we don't want to tear
            // it out
            boolean isLastSatelliteTab = getWidgetCount() == 1 && 
                  Satellite.isCurrentWindowSatellite();

            // did the user drag the tab outside this doc?
            if (!isLastSatelliteTab &&
                DomUtils.elementFromPoint(evt.getClientX(), 
                  evt.getClientY()) == null)
            {
               // did it end in any RStudio satellite window?
               String targetWindowName;
               Satellite satellite = RStudioGinjector.INSTANCE.getSatellite();
               if (Satellite.isCurrentWindowSatellite())
               {
                  // this is a satellite, ask the main window 
                  targetWindowName = satellite.getWindowAtPoint(
                        evt.getScreenX(), evt.getScreenY());
               }
               else
               {
                  // this is the main window, query our own satellites
                  targetWindowName = 
                     RStudioGinjector.INSTANCE.getSatelliteManager()
                         .getWindowAtPoint(evt.getScreenX(), evt.getScreenY());
               }
               if (targetWindowName == null)
               {
                  // it was dragged over nothing RStudio owns--pop it out
                  events_.fireEvent(new PopoutDocInitiatedEvent(
                        initDragParams_.getDocId(), new Point(
                              evt.getScreenX(), evt.getScreenY())));
               }
            }
         }
         
         if (curState_ != STATE_EXTERNAL)
         {
            // if we're in an end state, clear the drag element
            dragElement_ = null;
         }
      }

      private int lastCursorX_ = 0;
      private int lastElementX_ = 0;
      private Integer startPos_ = null;
      private Integer candidatePos_ = null;
      private int destPos_ = 0;
      private int dragMax_ = 0;
      private int outOfBounds_ = 0;
      private int initDragWidth_;
      private DocTabDragParams initDragParams_;
      private Element dragElement_;
      private Element dragTabsHost_;
      private Element dragScrollHost_;
      private Element dragPlaceholder_;
      private int curState_ = STATE_NONE;
      private Point dropPoint_;

      private final static int SCROLL_THRESHOLD = 25;
      
      // No drag operation is taking place 
      private final static int STATE_NONE = 0;
      
      // A tab is being dragged inside this tab panel
      private final static int STATE_DRAGGING = 1;
      
      // A tab from this panel is being dragged elsewhere
      private final static int STATE_EXTERNAL = 2;
      
      // the drag operation should be cancelled
      private final static int ACTION_CANCEL = 0;
      
      // the drag operation should be continued outside this window
      private final static int ACTION_EXTERNAL = 1;
      
      // the drag operation should be committed in this window
      private final static int ACTION_COMMIT = 2;
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
               evt.getDataTransfer().setData(
                     getDataTransferFormat(), docId_ + "|" + 
                  SourceWindowManager.getSourceWindowId());
               JsObject dt = evt.getDataTransfer().cast();
               dt.setString("effectAllowed", "move");

               // figure out where the cursor is inside the element; because the
               // drag shows an image of the tab, knowing where the cursor is
               // inside that image is necessary for us to know the screen
               // location of the dragged image
               int evtX = evt.getNativeEvent().getClientX();
               ElementEx ele = getElement().cast();
               
               // if the drag leaves the window, the destination is going to
               // need to know information we don't have here (such as the
               // cursor position in the editor); this event gets handled by
               // the editor, which adds the needed information and broadcasts
               // it to all the windows.
               events_.fireEvent(new DocTabDragInitiatedEvent(docId_, 
                           getElement().getClientWidth(), 
                           evtX - ele.getBoundingClientRect().getLeft()));
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

         Image img = new Image(new ImageResource2x(ThemeResources.INSTANCE.closeTab2x()));
         img.setStylePrimaryName(styles_.closeTabButton());
         img.addStyleName(ThemeStyles.INSTANCE.handCursor());
         contentPanel_.add(img);

         layoutPanel.add(contentPanel_);

         HTML right = new HTML();
         right.setStylePrimaryName(styles_.tabLayoutRight());
         layoutPanel.add(right);

         initWidget(layoutPanel);

         this.sinkEvents(Event.ONCLICK);
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
            case Event.ONCLICK:
            {
               // tabs can be closed by (a) middle mouse (anywhere), or (b)
               // left click on close element
               if (event.getButton() == Event.BUTTON_MIDDLE || 
                   (Element.as(event.getEventTarget()) == closeElement_ &&
                    event.getButton() == Event.BUTTON_LEFT))
               {
                  closeHandler_.onTabClose();
                  event.stopPropagation();
                  event.preventDefault();
               }
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
   
   private String getDataTransferFormat()
   {
      // IE only supports textual data; for other browsers, though, use our own
      // format so it doesn't activate text drag targets in other apps
      if (BrowseCap.INSTANCE.isInternetExplorer()) 
         return "text";
      else
         return "application/rstudio-tab";
   }

   public static final int BAR_HEIGHT = 24;

   private final boolean closeableTabs_;
   private final EventBus events_;
   
   private int padding_;
   private int rightMargin_;
   private final ThemeStyles styles_;
   private Animation currentAnimation_;
   private DragManager dragManager_;
}
