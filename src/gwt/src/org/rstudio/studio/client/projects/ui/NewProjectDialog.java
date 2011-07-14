package org.rstudio.studio.client.projects.ui;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewProjectDialog extends ModalDialog<Boolean>
{
   public NewProjectDialog(OperationWithInput<Boolean> operation)
   {
      super("New Project", operation);
    
      setOkButtonCaption("OK");
   }
   
   @Override
   protected Boolean collectInput()
   {
      return newDirButton_.getValue();
   }
   
   @Override
   protected boolean validate(Boolean input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.addStyleName(RESOURCES.styles().mainWidget());
      
      newDirButton_ = new RadioButton(
            "Type", 
            "Create a new empty project:");
      newDirButton_.setValue(true);
      verticalPanel.add(newDirButton_); 
      
      VerticalPanel locationPanel = new VerticalPanel();
      locationPanel.addStyleName(RESOURCES.styles().newProjectLocationPanel());
      
      lblNewProjectName_ = new Label("New project name:");
      
      locationPanel.add(lblNewProjectName_);
      txtProjectName_ = new TextBox();
      txtProjectName_.addStyleName(RESOURCES.styles().projectNameTextBox());
      locationPanel.add(txtProjectName_);
      
      verticalPanel.add(locationPanel);
      
     
      verticalPanel.add(new HTML("<br/>"));
      
      existingDirButton_ = new RadioButton(
            "Type",
            "Create a project based on an existing working directory:");
     
      verticalPanel.add(existingDirButton_);
      verticalPanel.add(new HTML("<br/>"));
      
      
      
      return verticalPanel;
   }

  

   
   static interface Styles extends CssResource
   {
      String mainWidget();
      String newProjectLocationPanel();
      String projectNameTextBox();
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
   
   private RadioButton newDirButton_;
   private RadioButton existingDirButton_;
   private Label lblNewProjectName_;
   private TextBox txtProjectName_;
}
