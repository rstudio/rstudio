/*
 * LogicalWindow.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.HasWindowStateChangeHandlers;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.events.WindowStateChangeHandler;
import org.rstudio.core.client.theme.MinimizedWindowFrame;
import org.rstudio.core.client.theme.WindowFrame;

import static org.rstudio.core.client.layout.WindowState.*;

/**
 * Represents the combination of states and objects that model a single
 * logical window in the DualWindowLayoutPanel.
 */
public class LogicalWindow implements HasWindowStateChangeHandlers,
                                      WindowStateChangeHandler,
                                      EnsureHeightHandler
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

   public HandlerRegistration addWindowStateChangeHandler(
         WindowStateChangeHandler handler)
   {
      return events_.addHandler(WindowStateChangeEvent.TYPE, handler);
   }
   
   public HandlerRegistration addEnsureHeightHandler(
         EnsureHeightHandler handler)
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
      events_.fireEvent(new WindowStateChangeEvent(newState));
   }

   public void transitionToState(WindowState newState)
   {
      if (newState == MAXIMIZE)
         normal_.addStyleDependentName("maximized");
      else
         normal_.removeStyleDependentName("maximized");

      if (newState == EXCLUSIVE)
         normal_.addStyleDependentName("exclusive");
      else
         normal_.removeStyleDependentName("exclusive");

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
