/*
 * ApplicationAgreementDialog.java
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
package org.rstudio.studio.client.application.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ShowContentDialog;
import org.rstudio.core.client.widget.ThemedButton;

public class ApplicationAgreementDialog extends ShowContentDialog
{
   public ApplicationAgreementDialog(String title,
                                   String contents, 
                                   Operation doNotAcceptOperation,
                                   Operation acceptOperation)
   {
      super(title, 
            contents,
            new Size(800, 1000));
      
      doNotAcceptOperation_ = doNotAcceptOperation;
      acceptOperation_ = acceptOperation ;
   }
   
   @Override
   protected void addButtons()
   {
      // default button is do not accept
      ThemedButton doNotAcceptButton = new ThemedButton("I Do Not Agree", 
                                                        new ClickHandler() {
         public void onClick(ClickEvent event) {
            closeDialog();
            doNotAcceptOperation_.execute();
         }
      });
      addOkButton(doNotAcceptButton);
      
      // accept button
      ThemedButton acceptButton = new ThemedButton("I Agree",
                                                   new ClickHandler() {

         public void onClick(ClickEvent event)
         {
            closeDialog();
            acceptOperation_.execute();  
         }
      });
      addButton(acceptButton);
   }
   
   private Operation doNotAcceptOperation_;
   private Operation acceptOperation_;
}
