/*
 * WeaveRnwSelectWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.prefs;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.TexCapabilities;
import org.rstudio.studio.client.workbench.views.source.model.TexServerOperations;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;

import com.google.inject.Inject;

public class WeaveRnwSelectWidget extends SelectWidget
{
   public WeaveRnwSelectWidget()
   {
      super("Default method for weaving Rnw files:", 
            new String[] { SWEAVE, KNITR }); 
     
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      this.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            if (getValue().equals(KNITR))
               verifyKnitrAvailable();
            
         } 
      });
   }  
   
   protected void verifyKnitrAvailable()
   {
      server_.getTexCapabilities(new ServerRequestCallback<TexCapabilities>() {

         @Override
         public void onResponseReceived(TexCapabilities capabilities)
         {
            if (!capabilities.isKnitrInstalled())
            {
               globalDisplay_.showYesNoMessage(
                  MessageDialog.QUESTION,
                  "Confirm Change", 
                  "The knitr package is required for knitr weaving, " +
                  "however it is not currently installed. You should " +
                  "ensure that knitr is installed prior to compiling a PDF." +
                  "\n\nAre you sure you want to change this option?",
                  false,
                  new Operation() { public void execute() { }},
                  new Operation() {
                     @Override
                     public void execute()
                     {
                        setValue(SWEAVE);
                     }  
                  },
                  false );
                     
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
         
      });
   }

   @Inject
   void initialize(GlobalDisplay globalDisplay,
                   TexServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
   }
   
   
   private TexServerOperations server_;
   private GlobalDisplay globalDisplay_;
   
   private final static String SWEAVE = "Sweave";
   private final static String KNITR = "knitr";
}
