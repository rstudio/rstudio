package org.rstudio.studio.client.common.posixshell;

import org.rstudio.studio.client.common.shell.ShellDisplay;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PosixShell
{
   public interface Display extends ShellDisplay
   { 
   }
   
   public interface Observer 
   {
      
   }
   
   @Inject
   public PosixShell(Display display)
   {
      display_ = display;
   
      
      
     
      
   }
   
   public void SetObserver(Observer observer)
   {
      observer_ = observer;
   }
   
   public Widget getWidget()
   {
      return display_.getShellWidget();
   }
   
   
   
  
   private final Display display_;
   @SuppressWarnings("unused")
   private Observer observer_ = null;
}
