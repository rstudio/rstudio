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
            closeDialog();
         }
      });

      addCancelButton(closeButton); 
      
   }

   @Override
   protected Widget createMainWidget()
   {
      posixShell_ = pPosixShell_.get();
      posixShell_.SetObserver(this);
      
      
      
      Widget shellWidget = posixShell_.getWidget();
      shellWidget.setSize("500px", "400px");
      return shellWidget;
   }

   @Override
   protected void onDialogShown()
   { 
      ((CanFocus)posixShell_.getWidget()).focus();
   }
   
  
   private final Provider<PosixShell> pPosixShell_;
   private PosixShell posixShell_;
}
