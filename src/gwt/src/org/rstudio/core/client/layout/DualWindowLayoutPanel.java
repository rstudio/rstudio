/*
 * DualWindowLayoutPanel.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.layout;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.events.WindowStateChangeHandler;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import static org.rstudio.core.client.layout.WindowState.*;

/**
 * This class implements the minimizing/maximizing behavior between two
 * window frames.
 */
public class DualWindowLayoutPanel extends SimplePanel
                                implements ProvidesResize,
                                           RequiresResize
{
   private static class NormalHeight
   {
      public NormalHeight(int height,
                          Integer containerHeight,
                          Integer windowHeight)
      {
         height_ = height;
         containerHeight_ = containerHeight;
         windowHeight_ = windowHeight;
      }

      public int getHeight()
      {
         return height_;
      }

      public int getContainerHeight(int defaultValue)
      {
         assert defaultValue > 0;
         if (containerHeight_ == null)
            containerHeight_ = defaultValue;
         return containerHeight_.intValue();
      }

      public int getWindowHeight(int defaultValue)
      {
         assert defaultValue > 0;
         if (windowHeight_ == null)
            windowHeight_ = defaultValue;
         return windowHeight_.intValue();
      }

      public int getHeightScaledTo(int containerHeight)
      {
         if (containerHeight_ == null
             || containerHeight_.intValue() == containerHeight
             || containerHeight <= 0)
         {
            return height_;
         }

         double pct = (double)containerHeight / containerHeight_.intValue();
         return (int)(pct * height_);         
      }

      private int height_;
      private Integer containerHeight_;
      private Integer windowHeight_;
   }

   private class WindowStateChangeManager
         implements WindowStateChangeHandler
   {
      public WindowStateChangeManager(Session session,
                                      LogicalWindow top,
                                      LogicalWindow bottom)
      {
         session_ = session;
         this.top = top;
         this.bottom = bottom;
      }

      public void onWindowStateChange(WindowStateChangeEvent event)
      {
         switch (event.getNewState())
         {
            case EXCLUSIVE:
               top.transitionToState(EXCLUSIVE);
               bottom.transitionToState(HIDE);
               layout(top, bottom);
               break;
            case MAXIMIZE:
               top.transitionToState(MAXIMIZE);
               bottom.transitionToState(MINIMIZE);
               layout(top, bottom);
               break;
            case MINIMIZE:
               top.transitionToState(MINIMIZE);
               bottom.transitionToState(MAXIMIZE);
               layout(top, bottom);
               break;
            case NORMAL:
               top.transitionToState(NORMAL);
               bottom.transitionToState(NORMAL);
               layout(top, bottom);
               break;
            case HIDE:
               top.transitionToState(HIDE);
               bottom.transitionToState(EXCLUSIVE);
               layout(top, bottom);
               break;
         }

         // Defer this because layout changes are deferred by LayoutPanel.
         DeferredCommand.addCommand(new Command()
         {
            public void execute()
            {
               session_.persistClientState();
            }
         });
      }

      private final Session session_;
      private final LogicalWindow top;
      private final LogicalWindow bottom;
   }

   private static class State extends JavaScriptObject
   {
      @SuppressWarnings("unused")
      protected State() {}

      public native final boolean hasSplitterPos() /*-{
         return typeof(this.splitterpos) != 'undefined';
      }-*/;

      public native final int getSplitterPos() /*-{
         return this.splitterpos;
      }-*/;

      public native final void setSplitterPos(int pos) /*-{
         this.splitterpos = pos;
      }-*/;

      public native final String getTopWindowState() /*-{
         return this.topwindowstate;
      }-*/;

      public native final void setTopWindowState(String state) /*-{
         this.topwindowstate = state;
      }-*/;

      public native final boolean hasPanelHeight() /*-{
         return typeof(this.panelheight) != 'undefined';
      }-*/;

      public native final int getPanelHeight() /*-{
         return this.panelheight;
      }-*/;

      public native final void setPanelHeight(int height) /*-{
         this.panelheight = height;
      }-*/;

      public native final boolean hasWindowHeight() /*-{
         return typeof(this.windowheight) != 'undefined';
      }-*/;

      public native final int getWindowHeight() /*-{
         return this.windowheight;
      }-*/;

      public native final void setWindowHeight(int height) /*-{
         this.windowheight = height;
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

         if (a.hasPanelHeight() ^ b.hasPanelHeight())
            return false;
         if (a.hasPanelHeight() && a.getPanelHeight() != b.getPanelHeight())
            return false;

         if (a.hasWindowHeight() ^ b.hasWindowHeight())
            return false;
         if (a.hasWindowHeight() && a.getWindowHeight() != b.getWindowHeight())
            return false;

         if (a.getTopWindowState() == null ^ b.getTopWindowState() == null)
            return false;
         if (a.getTopWindowState() != null
             && !a.getTopWindowState().equals(b.getTopWindowState()))
            return false;

         return true;
      }
   }

   /**
    * Helper class to make the minimized/maximized state and splitter
    * position persist across browser sessions.
    */
   private class WindowLayoutStateValue extends JSObjectStateValue
   {
      public WindowLayoutStateValue(LogicalWindow windowA,
                                    ClientInitState clientState,
                                    String clientStateKeyName,
                                    WindowState topWindowDefaultState,
                                    int defaultSplitterPos)
      {
         super("windowlayout",
               clientStateKeyName,
               true,
               clientState,
               true);
         windowA_ = windowA;
         topWindowDefaultState_ = topWindowDefaultState;
         defaultSplitterPos_ = defaultSplitterPos;

         finishInit(clientState);
      }

      @Override
      protected void onInit(JsObject value)
      {
         normalHeight_ = new NormalHeight(defaultSplitterPos_, null, null);
         WindowState topWindowState = topWindowDefaultState_;

         try
         {
            if (value != null)
            {
               State state = value.cast();
               if (state.hasSplitterPos())
               {
                  // This logic is a little tortured. At startup time, we don't
                  // have the height of this panel (getOffsetHeight()) since it
                  // isn't attached to the document yet. But if we wait until
                  // we have the height to restore the size, then the user would
                  // see some jumpiness in the UI.

                  // So instead we persist both the panel height and window
                  // height at save time, and assume that the difference between
                  // the two will remain the same, and use the new window height
                  // at load time to work backward to the new offset height.
                  // That's probably not a great assumption, it would be better
                  // to have a priori knowledge of the height of the panel. But
                  // given the low severity of the number being slightly off,
                  // this seems fine for the foreseeable future.

                  if (state.hasWindowHeight() && state.hasPanelHeight() &&
                      state.getWindowHeight() != Window.getClientHeight())
                  {
                     int deltaY = state.getWindowHeight() - state.getPanelHeight();
                     int newPanelHeight = Window.getClientHeight() - deltaY;
                     // Use percentage value
                     double pct = (double) state.getSplitterPos()
                                  / state.getPanelHeight();
                     normalHeight_ = new NormalHeight(
                                                   (int)(pct * newPanelHeight),
                                                   newPanelHeight,
                                                   Window.getClientHeight());
                  }
                  else
                  {
                     // Use absolute value
                     normalHeight_ = new NormalHeight(
                           state.getSplitterPos(),
                           state.hasPanelHeight() ? state.getPanelHeight()
                                                  : null,
                           state.hasWindowHeight() ? state.getWindowHeight()
                                                   : null);
                  }
               }
               if (state.getTopWindowState() != null)
                  topWindowState = WindowState.valueOf(state.getTopWindowState());

               lastKnownValue_ = state;
            }
         }
         catch (Exception e)
         {
            Debug.log("Error restoring dual window state: " + e.toString());
         }

         windowA_.onWindowStateChange(
               new WindowStateChangeEvent(topWindowState));
      }

      @Override
      protected JsObject getValue()
      {
         if (layout_.isSplitterVisible())
         {
            normalHeight_ = new NormalHeight(layout_.getSplitterBottom(),
                                             layout_.getOffsetHeight(),
                                             Window.getClientHeight());
         }

         State state = JsObject.createJsObject().cast();
         state.setSplitterPos(normalHeight_.getHeight());
         state.setTopWindowState(windowA_.getState().toString());
         state.setPanelHeight(normalHeight_.getContainerHeight(getOffsetHeight()));
         state.setWindowHeight(normalHeight_.getWindowHeight(Window.getClientHeight()));
         return state.cast();
      }

      @Override
      protected boolean hasChanged()
      {
         State state = getValue().cast();

         if (state.getSplitterPos() > state.getPanelHeight()
               || state.getSplitterPos() < 0)
         {
            Debug.log("Invalid splitter position detected: "
                      + state.getSplitterPos() + "/" + state.getPanelHeight());
            return false;
         }
         
         if (!State.equals(lastKnownValue_, state))
         {
            lastKnownValue_ = state;
            return true;
         }
         return false;
      }

      private State lastKnownValue_;
      private final LogicalWindow windowA_;
      private final WindowState topWindowDefaultState_;
      private final int defaultSplitterPos_;
   }

   public DualWindowLayoutPanel(final EventBus eventBus,
                                final LogicalWindow windowA,
                                final LogicalWindow windowB,
                                Session session,
                                String clientStateKeyName,
                                final WindowState topWindowDefaultState,
                                final int defaultSplitterPos)
   {
      session_ = session;
      setSize("100%", "100%");
      layout_ = new BinarySplitLayoutPanel(new Widget[] {
            windowA.getNormal(), windowA.getMinimized(),
            windowB.getNormal(), windowB.getMinimized()}, 3);
      layout_.setSize("100%", "100%");

      AnimationHelper.setParentZindex(windowA.getNormal(), -10);
      AnimationHelper.setParentZindex(windowA.getMinimized(), -10);
      AnimationHelper.setParentZindex(windowB.getNormal(), -10);
      AnimationHelper.setParentZindex(windowB.getMinimized(), -10);

      topWindowStateChangeManager_ = new WindowStateChangeManager(session,
                                                                  windowA,
                                                                  windowB);
      windowA.addWindowStateChangeHandler(topWindowStateChangeManager_);
      windowB.addWindowStateChangeHandler(
            new WindowStateChangeHandler()
            {
               public void onWindowStateChange(WindowStateChangeEvent event)
               {
                  WindowState topState;
                  switch (event.getNewState())
                  {
                     case NORMAL:
                        topState = NORMAL;
                        break;
                     case MAXIMIZE:
                        topState = MINIMIZE;
                        break;
                     case MINIMIZE:
                        topState = MAXIMIZE;
                        break;
                     case HIDE:
                        topState = EXCLUSIVE;
                        break;
                     case EXCLUSIVE:
                        topState = HIDE;
                        break;
                     default:
                        throw new IllegalArgumentException(
                              "Unknown WindowState " + event.getNewState());
                  }
                  windowA.onWindowStateChange(
                                          new WindowStateChangeEvent(topState));
               }
            });

      new WindowLayoutStateValue(windowA,
                               session.getSessionInfo().getClientState(),
                               clientStateKeyName,
                               topWindowDefaultState,
                               defaultSplitterPos);

      setWidget(layout_);

      if (eventBus != null)
      {
         layout_.addSplitterBeforeResizeHandler(new SplitterBeforeResizeHandler()
         {
            public void onSplitterBeforeResize(SplitterBeforeResizeEvent event)
            {
               // If the splitter ends up causing a minimize operation, then
               // we'll need to have saved the normal height for when the
               // user decides to restore the panel.
               snapMinimizeNormalHeight_ = new NormalHeight(
                     layout_.getSplitterBottom(),
                     layout_.getOffsetHeight(),
                     Window.getClientHeight());

               eventBus.fireEvent(new GlassVisibilityEvent(true));
            }
         });
         layout_.addSplitterResizedHandler(new SplitterResizedHandler()
         {
            public void onSplitterResized(SplitterResizedEvent event)
            {
               int bottom = layout_.getSplitterBottom();
               int height = layout_.getOffsetHeight();

               // If the height of upper or lower panel is smaller than this
               // then that panel will minimize
               final int MIN_HEIGHT = 60;
               
               if (bottom < MIN_HEIGHT)
               {
                  topWindowStateChangeManager_.onWindowStateChange(
                        new WindowStateChangeEvent(MAXIMIZE));
                  normalHeight_ = snapMinimizeNormalHeight_;
               }
               else if (bottom >= height - MIN_HEIGHT)
               {
                  topWindowStateChangeManager_.onWindowStateChange(
                        new WindowStateChangeEvent(MINIMIZE));
                  normalHeight_ = snapMinimizeNormalHeight_;
               }
               else
               {
                  normalHeight_ = new NormalHeight(bottom,
                                                   height,
                                                   Window.getClientHeight());
               }
               session_.persistClientState();

               eventBus.fireEvent(new GlassVisibilityEvent(false));
            }
         });
      }
   }

   public void onResize()
   {
      if (layout_ != null)
      {
         layout_.onResize();
      }
   }

   private void layout(final LogicalWindow top,
                       final LogicalWindow bottom)
   {
      AnimationHelper.create(layout_,
                             top,
                             bottom,
                             normalHeight_.getHeightScaledTo(getOffsetHeight()),
                             layout_.getSplitterHeight(),
                             isVisible() && isAttached()).animate();
   }

   public void setTopWindowState(WindowState state)
   {
      topWindowStateChangeManager_.onWindowStateChange(
            new WindowStateChangeEvent(state));
   }

   private BinarySplitLayoutPanel layout_;
   private NormalHeight normalHeight_;
   private final Session session_;
   private WindowStateChangeManager topWindowStateChangeManager_;

   private NormalHeight snapMinimizeNormalHeight_;
}
