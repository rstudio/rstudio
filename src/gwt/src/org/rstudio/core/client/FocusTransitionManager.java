/*
 * FocusTransitionManager.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.core.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rstudio.core.client.widget.CanFocus;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Widget;

public class FocusTransitionManager
{
   public FocusTransitionManager()
   {
      fwd_ = new HashMap<Widget, Widget>();
      bwd_ = new HashMap<Widget, Widget>();
      registration_ = new HashSet<Widget>();
   }

   public final void add(Widget source, Widget target)
   {
      ensureFocusable(source);
      ensureFocusable(target);
      fwd_.put(source, target);
      bwd_.put(target, source);
      listen(source, target);
   }

   public final Widget get(Widget widget, boolean forward)
   {
      Map<Widget, Widget> source = forward ? fwd_ : bwd_;
      if (!source.containsKey(widget))
         return null;
      return source.get(widget);
   }

   private final void listen(Widget... widgets)
   {
      for (Widget widget : widgets)
         register(widget);
   }

   private void register(final Widget widget)
   {
      if (registration_.contains(widget))
         return;

      registration_.add(widget);
      widget.addDomHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_TAB)
            {
               boolean forward = !event.isShiftKeyDown();
               Widget target = get(widget, forward);
               if (target == null)
                  return;

               if (target instanceof CanFocus)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  focus((CanFocus) target);
               }
               else if (target instanceof Focusable)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  focus((Focusable) target);
               }
            }
         }
      }, KeyDownEvent.getType());
   }
   
   private void ensureFocusable(Widget widget)
   {
      boolean focusable =
            widget instanceof Focusable ||
            widget instanceof CanFocus;
      
      if (focusable)
         return;
      
      String message =
            "widget '" + widget.getClass().getCanonicalName() + "' is not focusable";
      Debug.logWarning(message);
   }

   private void focus(CanFocus focusable)
   {
      focusable.focus();
   }

   private void focus(Focusable focusable)
   {
      focusable.setFocus(true);
   }

   private final Map<Widget, Widget> fwd_;
   private final Map<Widget, Widget> bwd_;
   private final Set<Widget> registration_;
}

