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

import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
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
      // create the shell and its observer
      posixShell_ = pPosixShell_.get();
      posixShell_.setObserver(this);
      
      // pick a reasonable size for the widget (90 chars wide, then we'll
      // send 85 as the width of the console in our call to start)
      int preferredWidth = DomMetrics.measureCode("0123456789").width * 9;
      Size preferredSize = new Size(preferredWidth, 
                                    Window.getClientHeight());

      // compute the editor size
      Size editorSize = DomMetrics.adjustedElementSize(preferredSize,
                                                       preferredSize,
                                                       25,   // pad
                                                       150); // client margin
      Widget shellWidget = posixShell_.getWidget();
      shellWidget.setSize(editorSize.width + "px", editorSize.height + "px");
      
      // start the shell
      posixShell_.start(85);
      
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
