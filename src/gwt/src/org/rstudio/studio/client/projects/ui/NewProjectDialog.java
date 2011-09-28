/*
 * NewProjectDialog.java
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
package org.rstudio.studio.client.projects.ui;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.layout.FadeInAnimation;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

public class NewProjectDialog extends ModalDialog<NewProjectDialog.Result>
{
   public class Result
   {
      public Result(String projectFile, 
                    String newDefaultProjectLocation,
                    String gitRepoUrl)
      {
         projectFile_ = projectFile;
         newDefaultProjectLocation_ = newDefaultProjectLocation;
         gitRepoUrl_ = gitRepoUrl;
      }
      
      public String getProjectFile()
      {
         return projectFile_;
      }
      
      public String getNewDefaultProjectLocation()
      {
         return newDefaultProjectLocation_;
      }

      public String getGitRepoUrl()
      {
         return gitRepoUrl_;
      }

      private final String projectFile_;
      private final String newDefaultProjectLocation_;
      private final String gitRepoUrl_;
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
            return new Result(projFile, newDefaultLocation, null);
         }
         else
         {
            return null;
         }
      }
      else if (existingDirButton_.getValue())
      {
         String dir = existingProjectDir_.getText();
         if (dir.length() > 0)
         {
            return new Result(projFileFromDir(dir), null, null);
         }
         else
         {
            return null;
         }
      }
      else if (existingRepoButton_.getValue())
      {
         String url = txtRepoUrl_.getText().trim();
         String dir = existingRepoDestDir_.getText().trim();
         if (url.length() > 0 && dir.length() > 0)
         {
            return new Result(null, dir, url);
         }
         else
         {
            return null;
         }
      }
      else
      {
         return null;
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
         else if (existingDirButton_.getValue())
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
         else if (existingRepoButton_.getValue())
         {
            if (txtRepoUrl_.getText().trim().length() == 0
                  || existingRepoDestDir_.getText().trim().length() == 0)
            {
               globalDisplay_.showMessage(
                     MessageDialog.WARNING,
                     "Error",
                     "You must specify a git repository URL and existing " +
                     "directory to create the new project within.");
            }
         }
         
         return false;
      }
      else
      {
         return true;
      }
         
   }

   interface Binder extends UiBinder<VerticalPanel, NewProjectDialog> {}

   @Override
   protected Widget createMainWidget()
   {
      // project dir
      newProjectParent_ = new DirectoryChooserTextBox("Create in:",
                                                      txtProjectName_);
      newProjectParent_.setText(defaultNewProjectLocation_.getPath());

      existingProjectDir_ = new DirectoryChooserTextBox("Directory:", null);

      existingRepoDestDir_ = new DirectoryChooserTextBox("Create in:",
                                                         txtRepoUrl_);
      existingRepoDestDir_.setText(defaultNewProjectLocation_.getPath());

      VerticalPanel verticalPanel =
            GWT.<Binder>create(Binder.class).createAndBindUi(this);

      // Comment out the next line to show git repo option
      existingRepoButton_.setVisible(false);

      newDirButton_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            manageEnabled();

            if (event.getValue())
               postAnimationCallback_ = new Command()
               {
                  @Override
                  public void execute()
                  {
                     txtProjectName_.setFocus(true);
                  }
               };
         }
      });

      existingDirButton_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            manageEnabled();

            if (event.getValue())
               postAnimationCallback_ = new Command()
               {
                  @Override
                  public void execute()
                  {
                     existingProjectDir_.focusButton();
                  }
               };
         }
      });

      existingRepoButton_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            manageEnabled();

            if (event.getValue())
               postAnimationCallback_ = new Command()
               {
                  @Override
                  public void execute()
                  {
                     txtRepoUrl_.setFocus(true);
                  }
               };
         }
      });

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
      boolean existingDir = existingDirButton_.getValue();
      boolean existingRepo = existingRepoButton_.getValue();
      
      txtProjectName_.setEnabled(createNewDir);
      newProjectParent_.setEnabled(createNewDir);

      existingProjectDir_.setEnabled(existingDir);

      txtRepoUrl_.setEnabled(existingRepo);
      existingRepoDestDir_.setEnabled(existingRepo);


      Widget widgetToHide =
            newDirControls_.isVisible() ? newDirControls_ :
            existingDirControls_.isVisible() ? existingDirControls_ :
            existingRepoControls_.isVisible() ? existingRepoControls_ :
            null;
      Widget widgetToShow =
            createNewDir ? newDirControls_ :
            existingDir ? existingDirControls_ :
            existingRepo ? existingRepoControls_ :
            null;
      if (widgetToHide != widgetToShow)
      {
         ArrayList<Widget> fadeOut = new ArrayList<Widget>();
         if (widgetToHide != null)
            fadeOut.add(widgetToHide);
         final ArrayList<Widget> fadeIn = new ArrayList<Widget>();
         if (widgetToShow != null)
            fadeIn.add(widgetToShow);

         new FadeOutAnimation(fadeOut, new Command()
         {
            @Override
            public void execute()
            {
               new FadeInAnimation(fadeIn, 1.0, new Command()
               {
                  @Override
                  public void execute()
                  {
                     if (postAnimationCallback_ != null)
                     {
                        postAnimationCallback_.execute();
                        postAnimationCallback_ = null;
                     }
                  }
               }).run(300);
            }
         }).run(300);
      }
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

   private Command postAnimationCallback_;

   @UiField
   RadioButton newDirButton_;
   @UiField
   RadioButton existingDirButton_;
   @UiField
   Label lblNewProjectName_;
   @UiField
   TextBox txtProjectName_;
   @UiField(provided = true)
   DirectoryChooserTextBox newProjectParent_;
   @UiField(provided = true)
   DirectoryChooserTextBox existingProjectDir_;
   @UiField
   VerticalPanel newDirControls_;
   @UiField
   VerticalPanel existingDirControls_;
   @UiField
   RadioButton existingRepoButton_;
   @UiField
   VerticalPanel existingRepoControls_;
   @UiField(provided = true)
   DirectoryChooserTextBox existingRepoDestDir_;
   @UiField
   Label lblRepoUrl;
   @UiField
   TextBox txtRepoUrl_;
}
