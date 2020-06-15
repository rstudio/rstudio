/*
 * MainSplitPanel.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.ui;

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
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.NotifyingSplitLayoutPanel;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import java.util.ArrayList;

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
         if (a.hasSplitterPos() && a.getSplitterPos() != b.getSplitterPos())
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
   }

   @Inject
   public MainSplitPanel(EventBus events,
                         Session session,
                         UserPrefs uiPrefs)
   {
      super(
         RStudioThemes.isFlat(uiPrefs) ? 7 : 3,
         events);
      
      session_ = session;
      addSplitterResizedHandler(this);
   }

   public void initialize(ArrayList<Widget> leftList, Widget center, Widget right)
   {
      leftList_ = leftList;
      center_ = center;
      right_ = right;

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
            if (state != null && state.hasSplitterPos() &&
                state.getSplitterCount() == leftList_.size() + 1)
            {
               if (state.hasPanelWidth() && state.hasWindowWidth()
                   && state.getWindowWidth() != Window.getClientWidth())
               {
                  int delta = state.getWindowWidth() - state.getPanelWidth();
                  int offsetWidth = Window.getClientWidth() - delta;
                  double pct = (double)state.getSplitterPos()[0]
                               / state.getPanelWidth();

                  addEast(right_, pct * offsetWidth);
                  for (int i = 0; i < leftList_.size(); i++)
                  {
                     pct = (double)state.getSplitterPos()[i]
                            / state.getPanelWidth();
                     addWest(leftList_.get(i), pct * offsetWidth);
                  }
               }
               else
               {
                  addEast(right_, state.getSplitterPos()[0]);
                  for (int i = 0; i < leftList_.size(); i++)
                     addWest(leftList_.get(i), state.getSplitterPos()[i]);
               }
            }
            else
            {
               // When there are only two panels, make the left side slightly larger than the right,
               // otherwise divide the space equally.
               double splitWidth = (leftList_.isEmpty()) ?
                                   Window.getClientWidth() * 0.45 :
                                   Window.getClientWidth() / (2 + leftList_.size());

               addEast(right_, splitWidth);

               for (Widget w : leftList_)
                  addWest(w, splitWidth);
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

            int[] splitterArray = new int[leftList_.size() + 1];
            splitterArray[0] = right_.getOffsetWidth();
            for (int i = 1; i < leftList_.size(); i++)
            {
               splitterArray[i] = splitterArray[i-1] + leftList_.get(i).getOffsetWidth();
            }
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

   public void resetLeftWidgets(ArrayList<Widget> list)
   {
      clearForRefresh();
      leftList_ = new ArrayList<>(list);
      initialize(leftList_, center_, right_);
   }

   public void addLeftWidget(Widget widget)
   {
      clearForRefresh();
      leftList_.add(widget);
      initialize(leftList_, center_, right_);
   }

   public void removeLeftWidget(Widget widget)
   {
      clearForRefresh();
      leftList_.remove(widget);
      initialize(leftList_, center_, right_);
   }

   public void onSplitterResized(SplitterResizedEvent event)
   {
      enforceBoundaries();
      deferredSaveWidthPercent();
   }

   public void focusSplitter()
   {
      Element splitter = getAssociatedSplitterElement(right_);
      if (splitter != null)
         splitter.focus();
   }

   private void clearForRefresh()
   {
      remove(center_);
      remove(right_);
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
   @SuppressWarnings("unused")
   private ArrayList<Widget> leftList_;
   private Widget center_;
   private Widget right_;
   private static final String GROUP_WORKBENCH = "workbenchp";
   private static final String KEY_RIGHTPANESIZE = "rightpanesize";
   private Command layoutCommand_;
}
