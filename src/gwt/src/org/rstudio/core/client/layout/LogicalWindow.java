/*
 * LogicalWindow.java
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
      if (event.getHeight() == EnsureHeightEvent.MAXIMIZED)
      {
         if (getState() != WindowState.MAXIMIZE)
            events_.fireEvent(new WindowStateChangeEvent(WindowState.MAXIMIZE));
      }
      else if (event.getHeight() == EnsureHeightEvent.NORMAL)
      {
         if (getState() != WindowState.NORMAL)
            events_.fireEvent(new WindowStateChangeEvent(WindowState.NORMAL));
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
