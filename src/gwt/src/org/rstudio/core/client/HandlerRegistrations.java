/*
 * HandlerRegistrations.java
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
package org.rstudio.core.client;

import com.google.gwt.event.shared.HandlerRegistration;

import java.util.ArrayList;

public class HandlerRegistrations implements HandlerRegistration
{
   public HandlerRegistrations(HandlerRegistration... registrations)
   {
      for (HandlerRegistration reg : registrations)
         add(reg);
   }

   public void add(HandlerRegistration reg)
   {
      registrations_.add(reg);
   }

   public void removeHandler()
   {
      while (registrations_.size() > 0)
         registrations_.remove(0).removeHandler();
   }

   private final ArrayList<HandlerRegistration> registrations_ = new ArrayList<>();
}
