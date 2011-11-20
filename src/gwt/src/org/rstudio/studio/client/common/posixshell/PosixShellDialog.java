package org.rstudio.studio.client.common.posixshell;

import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

public class PosixShellDialog extends ModalDialogBase
{
   public PosixShellDialog()
   {
      super();
      setText("Shell");
      
      ThemedButton closeButton = new ThemedButton("Close", new ClickHandler() {
         public void onClick(ClickEvent event) {
            closeDialog();
         }
      });
      addOkButton(closeButton); 
      
   }

   @Override
   protected Widget createMainWidget()
   {
    
      return null;
   }

}
