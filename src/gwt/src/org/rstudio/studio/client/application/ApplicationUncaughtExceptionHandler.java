/*
 * ApplicationUncaughtExceptionHandler.java
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
package org.rstudio.studio.client.application;

import org.rstudio.studio.client.server.ClientException;
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
      logException(e);
   }

   private void logException(Throwable e)
   {
      try
      {
         // call the default handler if there is one
         if (defaultUncaughtExceptionHandler_ != null)
            defaultUncaughtExceptionHandler_.onUncaughtException(e);
         
         // log uncaught exception
         server_.logException(ClientException.create(unwrap(e)),
                              new VoidServerRequestCallback());

      }
      catch(Throwable throwable)
      {
         // make sure exceptions never escape the uncaught handler
      }
   }
   
   private Throwable unwrap(Throwable e)
   {
      if (e instanceof UmbrellaException) 
      {   
         UmbrellaException ue = (UmbrellaException) e;  
         if(ue.getCauses().size() == 1)   
            return unwrap(ue.getCauses().iterator().next());  
      }
      
      return e;
   }

   
   private final Server server_;
   private UncaughtExceptionHandler defaultUncaughtExceptionHandler_ = null;
}
