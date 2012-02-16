/*
 * RnwWeaveSelectWidget.java
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
package org.rstudio.studio.client.common.rnw;


import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.TexCapabilities;
import org.rstudio.studio.client.workbench.views.source.model.TexServerOperations;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;

import com.google.inject.Inject;

public class RnwWeaveSelectWidget extends SelectWidget
{
   public RnwWeaveSelectWidget()
   { 
      super("Weave Rnw files using:", rnwWeaveRegistry_.getTypeNames());
  
      HelpButton helpButton = new HelpButton("rnw_weave_method");
      Style style = helpButton.getElement().getStyle();
      style.setMarginTop(3, Unit.PX);
      style.setMarginLeft(4, Unit.PX);
      addWidget(helpButton);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      this.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            RnwWeave weave = rnwWeaveRegistry_.findTypeIgnoreCase(getValue());
            verifyAvailable(weave);
            
         } 
      });
   }  
   
   protected void verifyAvailable(final RnwWeave weave)
   {
      // first check if it was already available at startup
      TexCapabilities texCap = session_.getSessionInfo().getTexCapabilities();
      if (texCap.isRnwWeaveAvailable(weave))
         return;
      
      server_.getTexCapabilities(new ServerRequestCallback<TexCapabilities>() {

         @Override
         public void onResponseReceived(TexCapabilities capabilities)
         {
            if (!capabilities.isRnwWeaveAvailable(weave))
            {
               globalDisplay_.showYesNoMessage(
                  MessageDialog.QUESTION,
                  "Confirm Change", 
                  "The " + weave.getPackageName() + " package is required " +
                  "for " + weave.getName() + " weaving, " +
                  "however it is not currently installed. You should " +
                  "ensure that " + weave.getPackageName() + " is installed " +
                  "prior to compiling a PDF." +
                  "\n\nAre you sure you want to change this option?",
                  false,
                  new Operation() { 
                     @Override
                     public void execute() 
                     { 
                     }
                   },
                  new Operation() {
                     @Override
                     public void execute()
                     {
                        setValue(rnwWeaveRegistry_.getTypes().get(0).getName());
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
                   TexServerOperations server,
                   Session session)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
      session_ = session;
   }
   
   
   private TexServerOperations server_;
   private GlobalDisplay globalDisplay_;
   private Session session_;
   
   public static final RnwWeaveRegistry rnwWeaveRegistry_ = 
                           RStudioGinjector.INSTANCE.getRnwWeaveRegistry();
}
