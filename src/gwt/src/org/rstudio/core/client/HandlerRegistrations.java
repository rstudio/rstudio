/*
 * HandlerRegistrations.java
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
package org.rstudio.core.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.shared.HandlerRegistration;

public class HandlerRegistrations implements HandlerRegistration
{
   public static void add(List<HandlerRegistration> handlers, HandlerRegistration... registrations)
   {
      for (HandlerRegistration registration : registrations)
      {
         handlers.add(registration);
      }
   }

   public HandlerRegistrations(HandlerRegistration... registrations)
   {
      registrations_ = new ArrayList<HandlerRegistration>();
      add(registrations);
   }
   
   public void add(HandlerRegistration... registrations)
   {
      for (HandlerRegistration registration : registrations)
      {
         add(registration);
      }
   }

   public void detach()
   {
      for (HandlerRegistration registration : registrations_)
      {
         registration.removeHandler();
      }
      registrations_.clear();
   }

   public void removeHandler()
   {
      detach();
   }

   private final List<HandlerRegistration> registrations_;
}
