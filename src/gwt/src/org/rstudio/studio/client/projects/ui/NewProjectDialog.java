package org.rstudio.studio.client.projects.ui;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.GlobalDisplay;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewProjectDialog extends ModalDialog<NewProjectDialog.Result>
{
   public class Result
   {
      public Result(String projectFile, 
                    String newDefaultProjectLocation)
      {
         projectFile_ = projectFile;
         newDefaultProjectLocation_ = newDefaultProjectLocation;
      }
      
      public String getProjectFile()
      {
         return projectFile_;
      }
      
      public String getNewDefaultProjectLocation()
      {
         return newDefaultProjectLocation_;
      }
      
      private String projectFile_;
      private String newDefaultProjectLocation_;
   }
   
   
   public NewProjectDialog(
         GlobalDisplay globalDisplay,
         FileSystemItem defaultNewProjectLocation,
         ProgressOperationWithInput<NewProjectDialog.Result> operation)
   {
      super("New Project", operation);
      
      globalDisplay_ = globalDisplay;
      defaultNewProjectLocation_ = defaultNewProjectLocation;
    
      setOkButtonCaption("Create Project");
   }
   
   @Override
   protected NewProjectDialog.Result collectInput()
   {
      if (newDirButton_.getValue())
      {
         String name = txtProjectName_.getText().trim();
         String dir = newProjectParent_.getText();
         if (name.length() > 0 && dir.length() > 0)
         {
            String projDir = FileSystemItem.createDir(dir).completePath(name);
            String projFile = projFileFromDir(projDir);
            String newDefaultLocation = null;
            if (!dir.equals(defaultNewProjectLocation_))
               newDefaultLocation = dir;
            return new Result(projFile, newDefaultLocation);
         }
         else
         {
            return null;
         }
      }
      else
      {
         String dir = existingProjectDir_.getText();
         if (dir.length() > 0)
         {
            return new Result(projFileFromDir(dir), null);
         }
         else
         {
            return null;
         }
      }
   }
   
   @Override
   protected boolean validate(NewProjectDialog.Result input)
   {
      if (input == null)
      {
         if (newDirButton_.getValue())
         {
            if (txtProjectName_.getText().trim().length() == 0)
            {
               globalDisplay_.showMessage(
                     MessageDialog.WARNING,
                     "Error", 
                     "You must specify a name for the new project directory.",
                     txtProjectName_);
            }
         }
         else
         {
            if (existingProjectDir_.getText().trim().length() == 0)
            {
               globalDisplay_.showMessage(
                     MessageDialog.WARNING,
                     "Error", 
                     "You must specify an existing working directory to " +
                     "create the new project within.");
            }      
         }
         
         return false;
      }
      else
      {
         return true;
      }
         
   }

   @Override
   protected Widget createMainWidget()
   {
      Styles styles = RESOURCES.styles();
      
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.addStyleName(styles.mainWidget());
      
      newDirButton_ = new RadioButton(
            "Type", 
            "Create a new empty project");
      newDirButton_.addStyleName(styles.projectTypeRadioButton());
      newDirButton_.setValue(true);
      newDirButton_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            manageEnabled();
            
            if (event.getValue())
               txtProjectName_.setFocus(true);
         }
      });
     
      verticalPanel.add(newDirButton_); 
      
      VerticalPanel emptyLocationPanel = new VerticalPanel();
      emptyLocationPanel.addStyleName(styles.newProjectLocationPanel());
      
      // project name
      lblNewProjectName_ = new Label("Project directory name:");
      emptyLocationPanel.add(lblNewProjectName_);
      txtProjectName_ = new TextBox();
      txtProjectName_.addStyleName(styles.projectNameTextBox());
      emptyLocationPanel.add(txtProjectName_); 
      
      // project dir
      newProjectParent_ = new DirectoryChooserTextBox("Create in:", 
                                                      txtProjectName_);
      newProjectParent_.setText(defaultNewProjectLocation_.getPath());
      
      emptyLocationPanel.add(newProjectParent_);
      
      verticalPanel.add(emptyLocationPanel);
      
      verticalPanel.add(new HTML("<br/>"));
      
      existingDirButton_ = new RadioButton(
            "Type",
            "Create a project based on an existing working directory");
      existingDirButton_.addStyleName(styles.projectTypeRadioButton());
      existingDirButton_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            manageEnabled();
            
            if (event.getValue() && existingProjectDir_.getText().length() == 0)
            {
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                  @Override
                  public void execute()
                  {
                     existingProjectDir_.click();     
                  }       
               });          
            }
         }
         
      });
     
      verticalPanel.add(existingDirButton_);
      
      VerticalPanel existingLocationPanel = new VerticalPanel();
      existingLocationPanel.addStyleName(styles.newProjectLocationPanel());
      
      existingProjectDir_ = new DirectoryChooserTextBox("Directory:", null);
      existingLocationPanel.add(existingProjectDir_);
      verticalPanel.add(existingLocationPanel);
      
      manageEnabled();
      
      return verticalPanel;
   }
   
   @Override
   protected void onDialogShown()
   {
      txtProjectName_.setFocus(true);
   }
   
   private String projFileFromDir(String dir)
   {
      FileSystemItem dirItem = FileSystemItem.createDir(dir);
      return FileSystemItem.createFile(
        dirItem.completePath(dirItem.getStem() + ".Rproj")).getPath();
   }
   
   private void manageEnabled()
   {
      boolean createNewDir = newDirButton_.getValue();
      
      txtProjectName_.setEnabled(createNewDir);
      newProjectParent_.setEnabled(createNewDir);
      existingProjectDir_.setEnabled(!createNewDir);
      
   }
   
   static interface Styles extends CssResource
   {
      String mainWidget();
      String newProjectLocationPanel();
      String projectNameTextBox();
      String projectTypeRadioButton();
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
   
   private final GlobalDisplay globalDisplay_;
   
   private final FileSystemItem defaultNewProjectLocation_;
   
   private RadioButton newDirButton_;
   private RadioButton existingDirButton_;
   private Label lblNewProjectName_;
   private TextBox txtProjectName_;
   private TextBoxWithButton newProjectParent_;
   private TextBoxWithButton existingProjectDir_;
}
