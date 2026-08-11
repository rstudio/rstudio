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
      if (state_ == EXCLUSIVE && newState == MAXIMIZE)
         newState = NORMAL;
      if (newState == state_)
         newState = NORMAL;
      events_.fireEvent(new WindowStateChangeEvent(newState, event.skipFocusChange()));
   }

   public void transitionToState(WindowState newState)
   {
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

   @Override
   public void onEnsureHeight(EnsureHeightEvent event)
   {
      // Route the MAXIMIZED and NORMAL conversions through the frame's
      // maximize action rather than firing state changes at this window
      // directly. Firing directly bypasses the owner's zoom bookkeeping
      // (PaneManager), which leaves a pane zoom tracked while the layout no
      // longer shows one, and a stale zoom re-zooms the next pane that raises
      // itself (#18448). An unhooked frame's maximize() falls back to the
      // default action, which reaches the same state changes as before.
      if (event.getHeight() == EnsureHeightEvent.MAXIMIZED)
      {
         // EXCLUSIVE already fills the window, so the height request is
         // satisfied; the frame's maximize gesture would mean "restore" there.
         if (getState() != WindowState.MAXIMIZE &&
             getState() != WindowState.EXCLUSIVE)
         {
            normal_.maximize();
         }
      }
      else if (event.getHeight() == EnsureHeightEvent.NORMAL)
      {
         if (getState() == WindowState.EXCLUSIVE)
         {
            // While zoomed, the maximize gesture means "restore": the owner
            // ends the zoom, or the default action's MAXIMIZE is remapped to
            // NORMAL by onWindowStateChange.
            normal_.maximize();
         }
         else if (getState() != WindowState.NORMAL)
         {
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
}
