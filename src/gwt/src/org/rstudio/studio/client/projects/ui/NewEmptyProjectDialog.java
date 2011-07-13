package org.rstudio.studio.client.projects.ui;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewEmptyProjectDialog extends ModalDialog<FileSystemItem>
{
   public NewEmptyProjectDialog(OperationWithInput<FileSystemItem> operation)
   {
      super("New Project", operation);
    
      setOkButtonCaption("Continue");
   }
   @Override
   protected FileSystemItem collectInput()
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   
   
   @Override
   protected boolean validate(FileSystemItem input)
   {
      // TODO Auto-generated method stub
      return false;
   }
   
   
   
   
  
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.addStyleName(RESOURCES.styles().mainWidget());
      
     
      
      
      return verticalPanel;
   }

  

   
   static interface Styles extends CssResource
   {
      String mainWidget();
     
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("NewProjectDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }  
}
