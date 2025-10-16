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
import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.ElementIds;
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
                  // Handle right-side sidebar case differently for proper resizing
                  if (sidebar_ != null && !"left".equals(sidebarLocation_))
                  {
                     // Add sidebar first using addEast (rightmost position)
                     double sidebarPct = (double)state.getSplitterPos()[idx++]
                                  / state.getPanelWidth();
                     addEast(sidebar_, sidebarPct * offsetWidth);
                     
                     // Get right widget width
                     double rightPct = (double)state.getSplitterPos()[idx++]
                                  / state.getPanelWidth();
                     
                     // Calculate center width: total - left widgets - sidebar - right
                     double centerWidth = offsetWidth;
                     // Subtract all left widget widths that were already added
                     int leftStartIdx = sidebar_ != null && "left".equals(sidebarLocation_) ? 1 : 0;
                     for (int i = 0; i < leftList_.size(); i++)
                     {
                        double leftPct = (double)state.getSplitterPos()[leftStartIdx + i] / state.getPanelWidth();
                        centerWidth -= leftPct * offsetWidth;
                     }
                     // Subtract sidebar and right widths
                     centerWidth -= sidebarPct * offsetWidth;
                     centerWidth -= rightPct * offsetWidth;
                     
                     // Add center using addWest (last addWest call)
                     addWest(center_, centerWidth);
                     
                     // Add right using add() (last thing added)
                     add(right_);
                  }
                  else
                  {
                     // No sidebar on right - use original logic
                     pct = (double)state.getSplitterPos()[idx++]
                                  / state.getPanelWidth();
                     addEast(right_, pct * offsetWidth);
                     add(center_);
                  }
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
                  // Handle right-side sidebar case differently for proper resizing
                  if (sidebar_ != null && !"left".equals(sidebarLocation_))
                  {
                     // Add sidebar first using addEast (rightmost position)
                     int sidebarWidth = state.getSplitterPos()[idx++];
                     addEast(sidebar_, sidebarWidth);
                     
                     // Get right widget width
                     int rightWidth = state.getSplitterPos()[idx++];
                     
                     // Calculate remaining width for center (total - left widgets - sidebar - right)
                     int centerWidth = state.getPanelWidth();
                     // Subtract all left widget widths that were already added
                     int leftStartIdx = sidebar_ != null && "left".equals(sidebarLocation_) ? 1 : 0;
                     for (int i = 0; i < leftList_.size(); i++)
                        centerWidth -= state.getSplitterPos()[leftStartIdx + i];
                     // Subtract sidebar and right widths
                     centerWidth -= sidebarWidth;
                     centerWidth -= rightWidth;
                     
                     // Add center using addWest (last addWest call)  
                     addWest(center_, centerWidth);
                     
                     // Add right using add() (last thing added)  
                     add(right_);
                  }
                  else
                  {
                     // No sidebar on right - use original logic
                     addEast(right_, state.getSplitterPos()[idx++]);
                     add(center_);
                  }
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
               
               // Handle right-side sidebar case differently for proper resizing
               if (sidebar_ != null && !"left".equals(sidebarLocation_))
               {
                  // Add sidebar first using addEast (rightmost position)
                  addEast(sidebar_, splitWidth * 0.8); // Sidebar slightly narrower
                  
                  // Add center using addWest (last addWest call)
                  addWest(center_, splitWidth);
                  
                  // Add right using add() (last thing added)
                  add(right_);
               }
               else
               {
                  // No sidebar on right - use original logic
                  addEast(right_, splitWidth);
                  add(center_);
               }
            }

            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  enforceBoundaries();
                  setSplitterAttributes();
                  deferredSaveWidthPercent();
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
      initialize(leftList_, center_, right_, sidebar_, sidebarLocation_);
   }
   
   public boolean hasSidebarWidget()
   {
      return sidebar_ != null;
   }

   public int getSidebarWidth()
   {
      if (sidebar_ != null && sidebar_.getOffsetWidth() > 0)
         return sidebar_.getOffsetWidth();
      return -1; // No sidebar or not yet rendered
   }

   public void setSidebarWidth(int width)
   {
      if (sidebar_ != null && width > 0)
      {
         LayoutData layoutData = (LayoutData) sidebar_.getLayoutData();
         if (layoutData != null)
         {
            layoutData.size = width;
            forceLayout();
            deferredSaveWidthPercent();
         }
      }
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

   /**
    * Set appropriate aria-labels and elementIds on all splitters based on their position.
    */
   private void setSplitterAttributes()
   {
      // Set label for sidebar splitter if sidebar exists
      if (sidebar_ != null)
      {
         Element splitterElem = getAssociatedSplitterElement(sidebar_);
         if (splitterElem != null)
         {
            splitterElem.setId(ElementIds.getElementId(ElementIds.SIDEBAR_COLUMN_SPLITTER));
            splitterElem.setAttribute("aria-label", "sidebar column splitter");
         }
      }

      // Set labels for source column splitters
      for (int i = 0; i < leftList_.size(); i++)
      {
         Element splitterElem = getAssociatedSplitterElement(leftList_.get(i));
         if (splitterElem != null)
         {
            splitterElem.setId(ElementIds.getElementId(ElementIds.SOURCE_COLUMN_SPLITTER + (i + 1)));
            splitterElem.setAttribute("aria-label", "source column " + (i + 1) + " splitter");
         }
      }

      // Set label for the middle splitter (between center and right columns)
      // Which widget has the splitter depends on layout:
      // - If sidebar is on right: center_ has the splitter (right_ is CENTER with add())
      // - If sidebar is not on right: right_ has the splitter (center_ is CENTER with add())
      Widget middleSplitterWidget;
      if (sidebar_ != null && !"left".equals(sidebarLocation_))
      {
         middleSplitterWidget = center_;
      }
      else
      {
         middleSplitterWidget = right_;
      }

      Element splitterElem = getAssociatedSplitterElement(middleSplitterWidget);
      if (splitterElem != null)
      {
         splitterElem.setId(ElementIds.getElementId(ElementIds.MIDDLE_COLUMN_SPLITTER));
         splitterElem.setAttribute("aria-label", "middle column splitter");
      }
   }

   private void clearForRefresh()
   {
      remove(center_);
      remove(right_);
      if (sidebar_ != null)
         remove(sidebar_);
      for (Widget w : leftList_)
         remove(w);
      widgetPercentages_.clear();
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
            widgetPercentages_.clear();
            int panelWidth = getOffsetWidth();
            assert panelWidth > 0;
            assert isVisible() && isAttached();
            if (panelWidth > 0)
            {
               // Store percentage for sidebar if present
               if (sidebar_ != null)
                  widgetPercentages_.put(sidebar_, (double)sidebar_.getOffsetWidth() / panelWidth);

               // Store percentages for all left widgets
               for (Widget w : leftList_)
                  widgetPercentages_.put(w, (double)w.getOffsetWidth() / panelWidth);

               // When sidebar is on right, center is a WEST widget and needs proportional resizing
               // When sidebar is not on right, center is CENTER widget and fills remaining space
               if (sidebar_ != null && !"left".equals(sidebarLocation_))
                  widgetPercentages_.put(center_, (double)center_.getOffsetWidth() / panelWidth);
               else
                  widgetPercentages_.put(right_, (double)right_.getOffsetWidth() / panelWidth);
            }
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
          && !widgetPercentages_.isEmpty())
      {
         // Apply proportional resizing to all widgets
         for (Map.Entry<Widget, Double> entry : widgetPercentages_.entrySet())
         {
            Widget widget = entry.getKey();
            Double percentage = entry.getValue();
            LayoutData layoutData = (LayoutData) widget.getLayoutData();
            if (layoutData != null)
            {
               layoutData.size = percentage * offsetWidth;
            }
         }

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
   
   private Map<Widget, Double> widgetPercentages_ = new HashMap<>();
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
