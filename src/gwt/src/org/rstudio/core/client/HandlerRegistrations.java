package org.rstudio.core.client;

import com.google.gwt.event.shared.HandlerRegistration;

import java.util.ArrayList;

public class HandlerRegistrations implements HandlerRegistration
{
   public HandlerRegistrations()
   {
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

   private final ArrayList<HandlerRegistration> registrations_ =
         new ArrayList<HandlerRegistration>();
}
