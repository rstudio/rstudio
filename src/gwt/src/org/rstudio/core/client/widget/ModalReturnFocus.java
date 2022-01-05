/*
 * ModalReturnFocus.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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


package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.List;


import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;

public class ModalReturnFocus
{

   public interface ReturnFocusHandler
   {
      boolean returnFocus(Element el);
   }

   public static HandlerRegistration registerReturnFocusHandler(final ReturnFocusHandler handler)
   {
      FOCUS_HANDLERS.add(handler);

      return new HandlerRegistration()
      {
         @Override
         public void removeHandler()
         {
            FOCUS_HANDLERS.remove(handler);
         }
      };
   }
   
   public static void returnFocus(Element element)
   {
      // iterate over focus handlers (in reverse order so
      // most recently added handlers are executed first)
      // and see if a registered handler can fire
      for (int i = 0, n = FOCUS_HANDLERS.size(); i < n; i++)
      {
         try
         {
            // first, try running a registered focus handler
            ReturnFocusHandler handler = FOCUS_HANDLERS.get(n - i - 1);
            if (handler.returnFocus(element))
               return;

         }
         catch (Exception e)
         {
            // swallow exceptions (attempts to focus an element can
            // fail for a multitude of reasons and those reasons are
            // usually not actionable by the user)
         }
      }

      try
      {
         // if no registered handler fired, then just focus element
         element.focus();
      }
      catch (Exception e)
      {
         // swallow exceptions
      }

   }
   
   private static final List<ReturnFocusHandler> FOCUS_HANDLERS = new ArrayList<>();

}
