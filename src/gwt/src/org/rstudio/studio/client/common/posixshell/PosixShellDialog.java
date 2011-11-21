/*
 * PosixShellDialog.java
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
package org.rstudio.studio.client.common.posixshell;

import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class PosixShellDialog extends ModalDialogBase
                                      implements PosixShell.Observer
{
   @Inject
   public PosixShellDialog(Provider<PosixShell> pPosixShell)
   {
      super();
      setText("Shell");
      pPosixShell_ = pPosixShell;
      
      ThemedButton closeButton = new ThemedButton("Close", new ClickHandler() {
         public void onClick(ClickEvent event) {
            
            if (posixShell_ != null)
               posixShell_.terminate();
            
            closeDialog();
         }
      });

      addCancelButton(closeButton); 
      
   }

   @Override
   protected Widget createMainWidget()
   {
      posixShell_ = pPosixShell_.get();
      posixShell_.setObserver(this);
      
      
      
      Widget shellWidget = posixShell_.getWidget();
      shellWidget.setSize("500px", "400px");
      return shellWidget;
   }
   
   @Override
   protected void onDialogShown()
   { 
      ((CanFocus)posixShell_.getWidget()).focus();
   }
   
   @Override
   public void onShellExited()
   {
      closeDialog();
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      if (posixShell_ != null)
         posixShell_.detachEventBusHandlers();
   }
   
  
   private final Provider<PosixShell> pPosixShell_;
   private PosixShell posixShell_;
   
  
}
