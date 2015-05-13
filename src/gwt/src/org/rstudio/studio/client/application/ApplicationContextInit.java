/*
 * ApplicationContextInit.java
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

package org.rstudio.studio.client.application;

import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationContextInit
{
   @Inject
   public ApplicationContextInit(GlobalDisplay globalDisplay,
                                 ApplicationServerOperations server)
   {
      // save references
      globalDisplay_ = globalDisplay;
      server_ = server;
   }
   
   
   public void initialize(final Command onSuccess,
                          final Command onFailure)
   {
      if (Desktop.isDesktop())
      {
         onSuccess.execute();
      }
      else
      {
         server_.contextInit(GWT.getHostPageBaseURL(), 
                             new ServerRequestCallback<String>() {
               
            @Override
            public void onResponseReceived(String redirectToURL)
            {
               if (redirectToURL != null)
                  Window.Location.replace(redirectToURL);
               else
                  onSuccess.execute();
            }
   
            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage("Context Initialization Error", 
                                               error.getUserMessage());
               onFailure.execute();
            }
         });
      }
   }
    
   private final GlobalDisplay globalDisplay_;
   private final ApplicationServerOperations server_;
}
