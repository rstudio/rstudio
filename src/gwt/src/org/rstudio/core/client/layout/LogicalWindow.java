/*
 * LogicalWindow.java
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
package org.rstudio.core.client.layout;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.HasWindowStateChangeHandlers;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.theme.MinimizedWindowFrame;
import org.rstudio.core.client.theme.WindowFrame;

import static org.rstudio.core.client.layout.WindowState.*;

/**
 * Represents the combination of states and objects that model a single
 * logical window in the DualWindowLayoutPanel.
 */
public class LogicalWindow implements HasWindowStateChangeHandlers,
                                      WindowStateChangeEvent.Handler,
                                      EnsureHeightEvent.Handler
{
   public LogicalWindow(WindowFrame normal,
                        MinimizedWindowFrame minimized)
   {
      normal_ = normal;
      minimized_ = minimized;

      normal_.addWindowStateChangeHandler(this);
      normal_.addEnsureHeightHandler(this);
      minimized_.addWindowStateChangeHandler(this);

      // A tab surfacing while the window is already open claims it (e.g.
      // Render output arriving in a pane a job raised earlier), so a later
      // hand-back must not put the pane away.
      normal_.addEnsureVisibleHandler(event -> clearAutoRaisedFromMinimize());

      // User interaction keeps an automatically raised pane open, including
      // selecting a tab or typing directly into its editor. Capture before
      // those controls can stop propagation; focus events also arise during
      // automatic activation and must not cancel the pending restoration.
      DomUtils.addEventListener(
            normal_.getElement(),
            "mousedown",
            true,
            event -> clearAutoRaisedFromMinimize());
      DomUtils.addEventListener(
            normal_.getElement(),
            "keydown",
            true,
            event -> clearAutoRaisedFromMinimize());
   }

   public WindowFrame getNormal()
   {
      return normal_;
   }

   public MinimizedWindowFrame getMinimized()
   {
      return minimized_;
   }

   public void focus()
   {
      assert state_ != MINIMIZE && state_ != HIDE;
      normal_.focus();
   }

   public void showWindowFocusIndicator(boolean showFocusIndicator)
   {
      if (normal_ != null)
         normal_.showWindowFocusIndicator(showFocusIndicator);
      if (minimized_ != null)
         minimized_.showWindowFocusIndicator(showFocusIndicator);
   }

   public boolean visible()
   {
      switch (state_)
      {
         case HIDE:
         case MINIMIZE:
            return false;
         default:
            return true;
      }
   }

   public Widget getActiveWidget()
   {
      switch (state_)
      {
         case EXCLUSIVE:
         case MAXIMIZE:
         case NORMAL:
            return normal_;
         case MINIMIZE:
            return minimized_;
         case HIDE:
            return null;
      }
      assert false;
      throw new IllegalStateException("Unknown state " + state_);
   }

   public HandlerRegistration addWindowStateChangeHandler(WindowStateChangeEvent.Handler handler)
   {
      return events_.addHandler(WindowStateChangeEvent.TYPE, handler);
   }

   public HandlerRegistration addEnsureHeightHandler(EnsureHeightEvent.Handler handler)
   {
      return events_.addHandler(EnsureHeightEvent.TYPE, handler);
   }

   public void onWindowStateChange(WindowStateChangeEvent event)
   {
      WindowState newState = event.getNewState();

      // remember a tab surfacing itself out of MINIMIZE; any other request (a
      // frame button, an owner) supersedes a pending auto-raise
      autoRaisedFromMinimize_ =
            event.isEnsureVisible() && state_ == MINIMIZE && newState == NORMAL;
      if (state_ == EXCLUSIVE && newState == MAXIMIZE)
         newState = NORMAL;
      if (newState == state_)
         newState = NORMAL;
      events_.fireEvent(new WindowStateChangeEvent(newState, event.skipFocusChange()));
   }

   public void transitionToState(WindowState newState)
   {
      // an owner or a sibling's transition (maximize, zoom, splitter snap)
      // moving this window out of NORMAL supersedes a pending auto-raise
      if (newState != WindowState.NORMAL)
         autoRaisedFromMinimize_ = false;

      normal_.setMaximizedDependentState(newState);
      normal_.setExclusiveDependentState(newState);
      normal_.setLogicalState(newState);
      state_ = newState;

      if (getActiveWidget() == normal_)
         normal_.onBeforeShow();
   }

   public WindowState getState()
   {
      return state_;
   }

   /**
    * True when the most recent change to this window was a tab raising it out
    * of MINIMIZE on its own (an ensure-visible or ensure-height request, e.g.
    * render output surfacing), rather than the user or an owner asking for it.
    * Lets a later "return to the console" put the pane back the way the user
    * left it.
    */
   public boolean wasAutoRaisedFromMinimize()
   {
      return autoRaisedFromMinimize_;
   }

   /**
    * An explicit activation supersedes an automatic raise, even when the
    * window is already visible and no state change is needed.
    */
   public void clearAutoRaisedFromMinimize()
   {
      autoRaisedFromMinimize_ = false;
   }

   @Override
   public void onEnsureHeight(EnsureHeightEvent event)
   {
      // EXCLUSIVE means an owner (PaneManager) has this window zoomed. The
      // conversions below must not fire state changes at a zoomed window:
      // that drives the quadrant state machine while the zoom bookkeeping
      // still points here, and stale bookkeeping re-zooms the next pane that
      // raises itself (#18448). A MAXIMIZED request on an EXCLUSIVE window is
      // already satisfied -- it fills the window. A NORMAL request means
      // un-zoom, which is what the frame's maximize gesture does while
      // zoomed, so route it there and let the owner end the zoom (an
      // unhooked frame's default action is remapped EXCLUSIVE + MAXIMIZE ->
      // NORMAL by onWindowStateChange, preserving the old behavior).
      //
      // Non-EXCLUSIVE windows keep the direct conversions. In particular,
      // MAXIMIZED must not route through the frame's maximize action: an
      // ensure-height is a vertical request, but the sidebar's maximize
      // action is a horizontal column zoom (layoutZoomSidebar), so e.g. a
      // Viewer preview in the sidebar would collapse every other column.
      if (event.getHeight() == EnsureHeightEvent.MAXIMIZED)
      {
         if (getState() != WindowState.MAXIMIZE &&
             getState() != WindowState.EXCLUSIVE)
         {
            autoRaisedFromMinimize_ = false;
            events_.fireEvent(new WindowStateChangeEvent(WindowState.MAXIMIZE));
         }
      }
      else if (event.getHeight() == EnsureHeightEvent.NORMAL)
      {
         if (getState() == WindowState.EXCLUSIVE)
         {
            normal_.maximize();
         }
         else if (getState() != WindowState.NORMAL)
         {
            autoRaisedFromMinimize_ = getState() == WindowState.MINIMIZE;
            events_.fireEvent(new WindowStateChangeEvent(WindowState.NORMAL));
         }
      }
      else
      {
         events_.fireEvent(event);
      }
   }

   private HandlerManager events_ = new HandlerManager(this);
   private WindowFrame normal_;
   private MinimizedWindowFrame minimized_;
   private WindowState state_;
   private boolean autoRaisedFromMinimize_ = false;
}
