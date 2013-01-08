/*
 * DefaultCRANMirror.java
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
package org.rstudio.studio.client.common.mirrors;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultCRANMirror 
{
   @Inject
   public DefaultCRANMirror(MirrorsServerOperations server,
                            GlobalDisplay globalDisplay)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }
   
   public void choose(OperationWithInput<CRANMirror> onChosen)
   {
      new ChooseMirrorDialog<CRANMirror>(globalDisplay_, 
                                         mirrorDS_, 
                                         onChosen).showModal();
   }
   
   public void configure(final Command onConfigured)
   {
      // show dialog
      new ChooseMirrorDialog<CRANMirror>(
         globalDisplay_,  
         mirrorDS_,
         new OperationWithInput<CRANMirror>() {
            @Override
            public void execute(final CRANMirror mirror)
            {
               server_.setCRANMirror(
                  mirror,
                  new SimpleRequestCallback<Void>("Error Setting CRAN Mirror") {
                      @Override
                      public void onResponseReceived(Void response)
                      {
                         // successfully set, call onConfigured
                         onConfigured.execute();
                      }
                  });             
             }
           }).showModal();
   }
   
   private final MirrorsServerOperations server_;
   
   private final GlobalDisplay globalDisplay_;
   
   private final ChooseMirrorDialog.Source<CRANMirror> mirrorDS_ = 
      new ChooseMirrorDialog.Source<CRANMirror>() {
         
         @Override
         public String getType()
         {
            return "CRAN";
         }

         @Override
         public String getLabel(CRANMirror mirror)
         {
            return mirror.getName() + " - " + mirror.getHost();
         }

         @Override
         public String getURL(CRANMirror mirror)
         {
            return mirror.getURL();
         }

         @Override
         public void requestData(
               ServerRequestCallback<JsArray<CRANMirror>> requestCallback)
         {
            server_.getCRANMirrors(requestCallback);
         }
    };
   
}
