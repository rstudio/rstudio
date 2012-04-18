/*
 * ApplicationUncaughtExceptionHandler.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application;

import org.rstudio.core.client.CsvWriter;
import org.rstudio.studio.client.server.LogEntryType;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.VoidServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationUncaughtExceptionHandler 
                                    implements UncaughtExceptionHandler
{
   @Inject
   public ApplicationUncaughtExceptionHandler(Server server)
   {
      server_ = server;
   }

   public void register()
   {
      // set uncaught exception handler (first save default so we can call it)
      defaultUncaughtExceptionHandler_ = GWT.getUncaughtExceptionHandler();
      GWT.setUncaughtExceptionHandler(this);
   }
   
   public void onUncaughtException(Throwable e)
   {
      logException(e, "Uncaught Exception");
   }

   private void logException(Throwable e, String label)
   {
      try
      {
         // call the default handler if there is one
         if (defaultUncaughtExceptionHandler_ != null)
            defaultUncaughtExceptionHandler_.onUncaughtException(e);
         
         // NOTE: we use use | as the logical line delimiter because server log
         // entries cannont contain newlines)
         
         // uncaught exception
         StringBuilder message = new StringBuilder();
         message.append(label).append(": ");

         CsvWriter csv = new CsvWriter();
         csv.writeValue(GWT.getPermutationStrongName());
         csv.writeValue(e.toString());

         StringBuilder stackTrace = new StringBuilder();
         writeStackTrace(e, stackTrace, false);

         csv.writeValue(stackTrace.toString());

         message.append(csv.getValue());
         
         // log to server
         server_.log(LogEntryType.ERROR, 
                     message.toString(),
                     new VoidServerRequestCallback());

         if (e instanceof UmbrellaException)
         {
            UmbrellaException ue = (UmbrellaException)e;
            for (Throwable t : ue.getCauses())
               if (t != null)
                  logException(t, "Nested Exception");
         }
      }
      catch(Throwable throwable)
      {
         // make sure exceptions never escape the uncaught handler
      }
   }

   private void writeStackTrace(Throwable e,
                                StringBuilder stackTrace,
                                boolean includeMessage)
   {
      if (e == null)
         return;

      if (includeMessage)
         stackTrace.append("\n").append(e.toString()).append("\n");

      // stack frame
      StackTraceElement[] stack = e.getStackTrace();
      if (stack != null)
      {
         for (int i=0; i<stack.length; i++)
         {
            if (i > 0)
               stackTrace.append("\n");
            stackTrace.append("    at ");
            stackTrace.append(stack[i].toString());
         }
      }
   }

   
   private final Server server_;
   private UncaughtExceptionHandler defaultUncaughtExceptionHandler_ = null;
}
