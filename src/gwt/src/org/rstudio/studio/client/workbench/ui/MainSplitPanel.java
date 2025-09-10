/*
 * MainSplitPanel.java
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
package org.rstudio.studio.client.workbench.ui;

import java.util.ArrayList;
import java.util.Arrays;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.NotifyingSplitLayoutPanel;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SplitterResizedEvent;
import com.google.gwt.user.client.ui.SplitterResizedHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class MainSplitPanel extends NotifyingSplitLayoutPanel
      implements SplitterResizedHandler
{
   private static class State extends JavaScriptObject
   {
      protected State() {}

      public native final boolean hasSplitterPos() /*-{
         return this.splitterpos && this.splitterpos.length;
      }-*/;

      public native final int[] getSplitterPos() /*-{
         return this.splitterpos;
      }-*/;

      public native final int getSplitterCount() /*-{
         return this.splitterpos.length;
      }-*/;

      public native final void setSplitterPos(int[] pos) /*-{
         this.splitterpos = pos;
      }-*/;

      public native final boolean hasPanelWidth() /*-{
         return typeof(this.panelwidth) != 'undefined';
      }-*/;

      public native final int getPanelWidth() /*-{
         return this.panelwidth;
      }-*/;

      public native final void setPanelWidth(int width) /*-{
         this.panelwidth = width;
      }-*/;

      public native final boolean hasWindowWidth() /*-{
         return typeof(this.windowwidth) != 'undefined';
      }-*/;

      public native final int getWindowWidth() /*-{
         return this.windowwidth;
      }-*/;

      public native final void setWindowWidth(int width) /*-{
         this.windowwidth = width;
      }-*/;

      public static boolean equals(State a, State b)
      {
         if (a == null ^ b == null)
            return false;
         if (a == null)
            return true;

         if (a.hasSplitterPos() ^ b.hasSplitterPos())
            return false;
         if (a.hasSplitterPos() &&
             !Arrays.equals(a.getSplitterPos(), b.getSplitterPos()))
            return false;

         if (a.hasPanelWidth() ^ b.hasPanelWidth())
            return false;
         if (a.hasPanelWidth() && a.getPanelWidth() != b.getPanelWidth())
            return false;

         if (a.hasWindowWidth() ^ b.hasWindowWidth())
            return false;
         if (a.hasWindowWidth() && a.getWindowWidth() != b.getWindowWidth())
            return false;

         return true;
      }

      public final boolean validate()
      {
         if (hasSplitterPos() && hasWindowWidth())
         {
            for (int i = 0; i < getSplitterPos().length; i++)
            {
               if (getSplitterPos()[i] < 0 ||
                   getSplitterPos()[i] > getPanelWidth())
                  return false;
            }
         }
         return true;
      }
   }

   @Inject
   public MainSplitPanel(EventBus events,
                         Session session)
   {
      super(7, events);
      
      session_ = session;
      addSplitterResizedHandler(this);
   }

   public void initialize(ArrayList<Widget> leftList, Widget center, Widget right)
   {
      initialize(leftList, center, right, null, "right");
   }
   
   public void initialize(ArrayList<Widget> leftList, Widget center, Widget right, Widget sidebar)
   {
      initialize(leftList, center, right, sidebar, "right");
   }
   
   public void initialize(ArrayList<Widget> leftList, Widget center, Widget right, Widget sidebar, String sidebarLocation)
   {
      leftList_ = leftList;
      center_ = center;
      right_ = right;
      sidebar_ = sidebar;
      sidebarLocation_ = sidebarLocation;

      new JSObjectStateValue(GROUP_WORKBENCH,
                             KEY_RIGHTPANESIZE,
                             ClientState.PERSISTENT,
                             session_.getSessionInfo().getClientState(),
                             false) {

         @Override
         protected void onInit(JsObject value)
         {
            // If we already have a set state, with the correct number of columns use that
            State state = value == null ? null : (State)value.cast();
            int expectedCount = leftList_.size() + 1 + (sidebar_ != null ? 1 : 0);
            if (state != null &&
                state.validate() &&
                state.hasSplitterPos() &&
                state.getSplitterCount() == expectedCount)
            {
               if (state.hasPanelWidth() && state.hasWindowWidth()
                   && state.getWindowWidth() != Window.getClientWidth())
               {
                  int delta = state.getWindowWidth() - state.getPanelWidth();
                  int offsetWidth = Window.getClientWidth() - delta;
                  int idx = 0;
                  double pct;
                  // Add sidebar if on left
                  if (sidebar_ != null && "left".equals(sidebarLocation_))
                  {
                     pct = (double)state.getSplitterPos()[idx++]
                                  / state.getPanelWidth();
                     addWest(sidebar_, pct * offsetWidth);
                  }
                  // Add left widgets
                  for (int i = 0; i < leftList_.size(); i++)
                  {
                     pct = (double)state.getSplitterPos()[idx++]
                            / state.getPanelWidth();
                     addWest(leftList_.get(i), pct * offsetWidth);
                  }
                  // Add sidebar if on right (add first so it's rightmost)
                  if (sidebar_ != null && !"left".equals(sidebarLocation_))
                  {
                     pct = (double)state.getSplitterPos()[idx++]
                                  / state.getPanelWidth();
                     addEast(sidebar_, pct * offsetWidth);
                  }
                  // Add right widget (after sidebar so it's to the left of sidebar)
                  pct = (double)state.getSplitterPos()[idx++]
                               / state.getPanelWidth();
                  addEast(right_, pct * offsetWidth);
               }
               else
               {
                  int idx = 0;
                  // Add sidebar if on left
                  if (sidebar_ != null && "left".equals(sidebarLocation_))
                     addWest(sidebar_, state.getSplitterPos()[idx++]);
                  // Add left widgets
                  for (int i = 0; i < leftList_.size(); i++)
                     addWest(leftList_.get(i), state.getSplitterPos()[idx++]);
                  // Add sidebar if on right (add first so it's rightmost)
                  if (sidebar_ != null && !"left".equals(sidebarLocation_))
                     addEast(sidebar_, state.getSplitterPos()[idx++]);
                  // Add right widget (after sidebar so it's to the left of sidebar)
                  addEast(right_, state.getSplitterPos()[idx++]);
               }
            }
            else
            {
               // When there are only two panels, make the left side slightly larger than the right,
               // otherwise divide the space equally.
               double splitWidth = getDefaultSplitterWidth();
               
               // Add sidebar if on left
               if (sidebar_ != null && "left".equals(sidebarLocation_))
                  addWest(sidebar_, splitWidth * 0.8); // Sidebar slightly narrower
               
               // Add left widgets
               for (Widget w : leftList_)
                  addWest(w, splitWidth);
               
               // Add sidebar if on right (add first so it's rightmost)
               if (sidebar_ != null && !"left".equals(sidebarLocation_))
                  addEast(sidebar_, splitWidth * 0.8); // Sidebar slightly narrower
               
               // Add right widget (after sidebar so it's to the left of sidebar)
               addEast(right_, splitWidth);
            }

            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  enforceBoundaries();
               }
            });
         }

         @Override
         protected JsObject getValue()
         {
            State state = JavaScriptObject.createObject().cast();
            state.setPanelWidth(getOffsetWidth());
            state.setWindowWidth(Window.getClientWidth());

            // The widget's code determines the splitter positions from the width of each widget
            // so these value represent that width rather than the actual coordinates of the
            // splitter.
            int sidebarCount = sidebar_ != null ? 1 : 0;
            int[] splitterArray = new int[leftList_.size() + 1 + sidebarCount];
            int idx = 0;
            
            // Store sidebar width if on left
            if (sidebar_ != null && "left".equals(sidebarLocation_))
               splitterArray[idx++] = sidebar_.getOffsetWidth();
            
            // Store left widget widths
            if (!leftList_.isEmpty())
            {
               for (int i = 0; i < leftList_.size(); i++)
                  splitterArray[idx++] = leftList_.get(i).getOffsetWidth();
            }
            
            // Store sidebar width if on right (before right widget in the array)
            if (sidebar_ != null && !"left".equals(sidebarLocation_))
               splitterArray[idx++] = sidebar_.getOffsetWidth();
            
            // Store right widget width
            splitterArray[idx++] = right_.getOffsetWidth();
            state.setSplitterPos(splitterArray);
            return state.cast();
         }

         @Override
         protected boolean hasChanged()
         {
            JsObject newValue = getValue();
            if (!State.equals(lastKnownValue_, (State) newValue.cast()))
            {
               lastKnownValue_ = newValue.cast();
               return true;
            }
            return false;
         }

         private State lastKnownValue_;
      };

      add(center_);
      setWidgetMinSize(right_, 0);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      deferredSaveWidthPercent();
   }

   public void addLeftWidget(Widget widget)
   {
      clearForRefresh();
      leftList_.add(0, widget);
      initialize(leftList_, center_, right_, sidebar_, sidebarLocation_);
   }

   public double getDefaultSplitterWidth()
   {
      int columnCount = 2 + leftList_.size() + (sidebar_ != null ? 1 : 0);
      return leftList_.isEmpty() && sidebar_ == null ?
         Window.getClientWidth() * 0.45 :
         Window.getClientWidth() / columnCount;
   }
   
   public double getLeftSize()
   {
      double sum = 0.0;
      for (Widget w : leftList_)
         sum += getWidgetSize(w);
      return sum;
   }
   
   public ArrayList<Double> getLeftWidgetSizes()
   {
      ArrayList<Double> result = new ArrayList<>();
      for (Widget w : leftList_)
         result.add(getWidgetSize(w));
      return result;
   }
   
   public void removeLeftWidget(Widget widget)
   {
      clearForRefresh();
      leftList_.remove(widget);
      initialize(leftList_, center_, right_, sidebar_, sidebarLocation_);
   }
   
   public void setSidebarWidget(Widget widget)
   {
      setSidebarWidget(widget, "right");
   }
   
   public void setSidebarWidget(Widget widget, String location)
   {
      clearForRefresh();
      sidebar_ = widget;
      sidebarLocation_ = location;
      initialize(leftList_, center_, right_, sidebar_, sidebarLocation_);
   }
   
   public void removeSidebarWidget()
   {
      clearForRefresh();
      sidebar_ = null;
      initialize(leftList_, center_, right_, null);
   }
   
   public boolean hasSidebarWidget()
   {
      return sidebar_ != null;
   }
   
   public void setSidebarWidth(double width)
   {
      if (sidebar_ != null)
      {
         LayoutData layoutData = (LayoutData) sidebar_.getLayoutData();
         if (layoutData != null)
         {
            layoutData.size = width;
            forceLayout();
         }
      }
   }
   
   public double getSidebarWidth()
   {
      if (sidebar_ != null)
      {
         return sidebar_.getOffsetWidth();
      }
      return 0;
   }

   public void onSplitterResized(SplitterResizedEvent event)
   {
      enforceBoundaries();
      deferredSaveWidthPercent();
   }

   public void focusSplitter(Widget widget)
   {
      Element splitter = getAssociatedSplitterElement(widget);
      if (splitter != null)
         splitter.focus();
   }

   private void clearForRefresh()
   {
      remove(center_);
      remove(right_);
      if (sidebar_ != null)
         remove(sidebar_);
      for (Widget w : leftList_)
         remove(w);
   }

   private void enforceBoundaries()
   {
      LayoutData layoutData = (LayoutData) right_.getLayoutData();
      if (layoutData != null
          && getOffsetWidth() != 0
          && layoutData.size > getOffsetWidth() - 3)
      {
         layoutData.size = getOffsetWidth() - 3;
         forceLayout();
      }
   }

   private void deferredSaveWidthPercent()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            splitPercent_ = null;
            int panelWidth = getOffsetWidth();
            assert panelWidth > 0;
            assert isVisible() && isAttached();
            if (panelWidth > 0)
               splitPercent_ = (double)right_.getOffsetWidth() / panelWidth;
            previousOffsetWidth_ = panelWidth;
         }
      });
   }

   @Override
   public void onResize()
   {
      super.onResize();

      int offsetWidth = getOffsetWidth();
      if ((previousOffsetWidth_ == null || offsetWidth != previousOffsetWidth_.intValue())
          && splitPercent_ != null)
      {
         LayoutData layoutData = (LayoutData) right_.getLayoutData();
         if (layoutData == null)
            return;
         layoutData.size = splitPercent_ * offsetWidth;

         previousOffsetWidth_ = offsetWidth;

         // Defer actually updating the layout, so that if we receive many
         // mouse events before layout/paint occurs, we'll only update once.
         if (layoutCommand_ == null) {
           layoutCommand_ = new Command() {
             public void execute() {
               layoutCommand_ = null;
               forceLayout();
             }
           };
           Scheduler.get().scheduleDeferred(layoutCommand_);
         }
      }
   }
   
   private Double splitPercent_ = null;
   private Integer previousOffsetWidth_ = null;

   private final Session session_;
   private ArrayList<Widget> leftList_;
   private Widget center_;
   private Widget right_;
   private Widget sidebar_;
   private String sidebarLocation_ = "right";
   private static final String GROUP_WORKBENCH = "workbenchp";
   private static final String KEY_RIGHTPANESIZE = "rightpanesize";
   private Command layoutCommand_;
}
