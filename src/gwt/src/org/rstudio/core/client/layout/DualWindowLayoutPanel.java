/*
 * DualWindowLayoutPanel.java
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
package org.rstudio.core.client.layout;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;
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
         implements WindowStateChangeEvent.Handler
   {
      public WindowStateChangeManager(Session session)
      {
         session_ = session;
      }

      public void onWindowStateChange(WindowStateChangeEvent event)
      {
         switch (event.getNewState())
         {
            case EXCLUSIVE:
               windowA_.transitionToState(EXCLUSIVE);
               windowB_.transitionToState(HIDE);
               layout(windowA_, windowB_, event.skipFocusChange());
               break;
            case MAXIMIZE:
               windowA_.transitionToState(MAXIMIZE);
               windowB_.transitionToState(MINIMIZE);
               layout(windowA_, windowB_, event.skipFocusChange());
               break;
            case MINIMIZE:
               windowA_.transitionToState(MINIMIZE);
               windowB_.transitionToState(MAXIMIZE);
               layout(windowA_, windowB_, event.skipFocusChange());
               break;
            case NORMAL:
               windowA_.transitionToState(NORMAL);
               windowB_.transitionToState(NORMAL);
               layout(windowA_, windowB_, event.skipFocusChange());
               break;
            case HIDE:
               windowA_.transitionToState(HIDE);
               windowB_.transitionToState(EXCLUSIVE);
               layout(windowA_, windowB_, event.skipFocusChange());
               break;
         }

         // Defer this because layout changes are deferred by LayoutPanel.
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            public void execute()
            {
               session_.persistClientState();
            }
         });
      }

      private final Session session_;
   }

   private static class State extends JavaScriptObject
   {
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
      public WindowLayoutStateValue(ClientInitState clientState,
                                    String clientStateKeyName,
                                    WindowState topWindowDefaultState,
                                    int defaultSplitterPos)
      {
         super("windowlayoutstate",
               clientStateKeyName,
               ClientState.PROJECT_PERSISTENT,
               clientState,
               true);
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
         NormalHeight currentHeight = normalHeight_;
         if (layout_.isSplitterVisible())
         {
            currentHeight = new NormalHeight(layout_.getSplitterBottom(),
                                             layout_.getOffsetHeight(),
                                             Window.getClientHeight());
         }

         State state = JsObject.createJsObject().cast();
         state.setSplitterPos(currentHeight.getHeight());
         state.setTopWindowState(windowA_.getState().toString());
         state.setPanelHeight(currentHeight.getContainerHeight(getOffsetHeight()));
         state.setWindowHeight(currentHeight.getWindowHeight(Window.getClientHeight()));
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
      private final WindowState topWindowDefaultState_;
      private final int defaultSplitterPos_;
   }

   public DualWindowLayoutPanel(final EventBus eventBus,
                                final LogicalWindow windowA,
                                final LogicalWindow windowB,
                                Session session,
                                String clientStateKeyName,
                                final WindowState topWindowDefaultState,
                                final int defaultSplitterPos,
                                final int splitterSize)
   {
      windowA_ = windowA;
      windowB_ = windowB;
      session_ = session;
      setSize("100%", "100%");
      layout_ = new BinarySplitLayoutPanel(clientStateKeyName, new Widget[] {
            windowA.getNormal(), windowA.getMinimized(),
            windowB.getNormal(), windowB.getMinimized()}, splitterSize);
      layout_.setSize("100%", "100%");

      topWindowStateChangeManager_ = new WindowStateChangeManager(session);
      bottomWindowStateChangeManager_ = (WindowStateChangeEvent event) ->
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
         windowA_.onWindowStateChange(
                                 new WindowStateChangeEvent(topState));
      };

      hookEvents();

      new WindowLayoutStateValue(session.getSessionInfo().getClientState(),
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
               WindowState topState = resizePanes(layout_.getSplitterBottom());

               // we're already in normal if the splitter is being invoked
               if (topState != WindowState.NORMAL)
               {
                  topWindowStateChangeManager_.onWindowStateChange(
                     new WindowStateChangeEvent(topState));
               }

               eventBus.fireEvent(new GlassVisibilityEvent(false));
            }
         });
      }
   }

   public void focusSplitter()
   {
      layout_.focusSplitter();
   }

   // resize the panes based on the specified bottom height and return the
   // new window state for the top pane (this implements snap to minimize)
   private WindowState resizePanes(int bottom)
   {
      WindowState topState = null;

      int height = layout_.getOffsetHeight();

      // If the height of upper or lower panel is smaller than this
      // then that panel will minimize
      final int MIN_HEIGHT = 60;

      if (bottom < MIN_HEIGHT)
      {
         topState = WindowState.MAXIMIZE;
         normalHeight_ = snapMinimizeNormalHeight_;
      }
      else if (bottom >= height - MIN_HEIGHT)
      {
         topState = WindowState.MINIMIZE;
         normalHeight_ = snapMinimizeNormalHeight_;
      }
      else
      {
         topState = WindowState.NORMAL;
         normalHeight_ = new NormalHeight(bottom,
                                          height,
                                          Window.getClientHeight());
      }

      session_.persistClientState();

      return topState;
   }



   private void hookEvents()
   {
      registrations_.add(
            windowA_.addWindowStateChangeHandler(topWindowStateChangeManager_));
      registrations_.add(
            windowB_.addWindowStateChangeHandler(bottomWindowStateChangeManager_));
      registrations_.add(
         windowA_.addEnsureHeightHandler(new EnsureHeightChangeManager(true)));
      registrations_.add(
         windowB_.addEnsureHeightHandler(new EnsureHeightChangeManager(false)));
   }

   private void unhookEvents()
   {
      registrations_.removeHandler();
   }

   public void replaceWindows(LogicalWindow windowA,
                              LogicalWindow windowB)
   {
      // If there is nothing to replace, we don't want to reset the state and potentially open
      // something the user had minimized.
      if (windowA == windowA_ &&
          windowB == windowB_)
         return;

      unhookEvents();
      windowA_ = windowA;
      windowB_ = windowB;
      hookEvents();

      layout_.setWidgets(new Widget[] {
            windowA_.getNormal(), windowA_.getMinimized(),
            windowB_.getNormal(), windowB_.getMinimized() });

      Scheduler.get().scheduleFinally(() ->
      {
         windowA_.onWindowStateChange(new WindowStateChangeEvent(NORMAL));
      });
   }

   public void onResize()
   {
      if (layout_ != null)
      {
         layout_.onResize();
      }
   }

   private void layout(final LogicalWindow top,
                       final LogicalWindow bottom,
                       boolean keepFocus)
   {
      boolean reducedMotion = RStudioGinjector.INSTANCE.getUserPrefs().reducedMotion().getValue();
      AnimationHelper.create(layout_,
                             top,
                             bottom,
                             normalHeight_.getHeightScaledTo(getOffsetHeight()),
                             layout_.getSplitterHeight(),
                             isVisible() && isAttached() && !reducedMotion,
                             keepFocus).animate();
   }

   public void setTopWindowState(WindowState state)
   {
      topWindowStateChangeManager_.onWindowStateChange(
            new WindowStateChangeEvent(state));
   }

   private class EnsureHeightChangeManager implements EnsureHeightEvent.Handler
   {
      public EnsureHeightChangeManager(boolean isTopWindow)
      {
         isTopWindow_ = isTopWindow;
      }

      @Override
      public void onEnsureHeight(EnsureHeightEvent event)
      {
         // constants
         final int FRAME = 52;
         final int MINIMUM = 160;

         // get the target window and target height
         LogicalWindow targetWindow = isTopWindow_ ? windowA_ : windowB_;
         int targetHeight = event.getHeight() + FRAME;

         // ignore if we are already maximized
         if (targetWindow.getState() == WindowState.MAXIMIZE)
            return;

         // ignore if we are already high enough
         if (targetWindow.getActiveWidget().getOffsetHeight() >= targetHeight)
            return;

         // calculate height of other pane
         int aHeight = windowA_.getActiveWidget().getOffsetHeight();
         int bHeight = windowB_.getActiveWidget().getOffsetHeight();
         int chromeHeight = layout_.getOffsetHeight() - aHeight - bHeight;
         int otherHeight = layout_.getOffsetHeight() -
                           chromeHeight -
                           targetHeight;

         // see if we need to offset to achieve minimum other height
         int offset = 0;
         if (otherHeight < MINIMUM)
            offset = MINIMUM - otherHeight;

         // determine the height (only the bottom can be sizes explicitly
         // so for the top we need to derive it's height from the implied
         // bottom height that we already computed)
         int height = isTopWindow_ ? (otherHeight + offset) :
                                     (targetHeight - offset);

         // ignore if this will reduce our size
         if (height <= targetWindow.getActiveWidget().getOffsetHeight())
            return;

         // resize bottom
         WindowState topState = resizePanes(height);

         if (topState != null)
         {
            topWindowStateChangeManager_.onWindowStateChange(
               new WindowStateChangeEvent(topState));
         }
      }

      private boolean isTopWindow_;

   }

   private BinarySplitLayoutPanel layout_;
   private NormalHeight normalHeight_;
   private LogicalWindow windowA_;
   private LogicalWindow windowB_;
   private final Session session_;
   private WindowStateChangeManager topWindowStateChangeManager_;
   private WindowStateChangeEvent.Handler bottomWindowStateChangeManager_;
   private HandlerRegistrations registrations_ = new HandlerRegistrations();

   private NormalHeight snapMinimizeNormalHeight_;
}
