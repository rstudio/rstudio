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


import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.http.client.URL;
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
      // if this is the desktop then just execute the success path right away
      if (Desktop.isDesktop())
      {
         onSuccess.execute();
      }
      else
      {
         // get project and context id url parameters
         String project = Window.Location.getParameter("proj");
         final String contextId = Window.Location.getParameter("ctx");
         server_.contextInit(project, contextId, 
                             new ServerRequestCallback<String>() {
               
            @Override
            public void onResponseReceived(String responseContextId)
            {
               // if the returned context id is null then the server 
               // doesn't support contexts. if it's the same as the 
               // current url parameter then we've got the right context.
               // in either case we move on by executing onSuccess
               if ((responseContextId == null) ||
                   responseContextId.equals(contextId))
               {
                  onSuccess.execute();
               }
               
               // othewise reload with the correct context id
               else
               {
                  // get the url
                  String url = Window.Location.getHref();
                  
                  // if we already have a context id then just replace it
                  if (url.contains("ctx="))
                  {
                     responseContextId = URL.encodeQueryString(responseContextId);
                     url = Pattern.replace("ctx=[\\w]+", 
                                           "ctx=" + responseContextId, 
                                           true);
                  }
                  else 
                  {
                     url = URIUtils.addQueryParam(url, 
                                                  "ctx", 
                                                  responseContextId);
                  }
                  
                  // reload
                  Window.Location.replace(url);
               }
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
