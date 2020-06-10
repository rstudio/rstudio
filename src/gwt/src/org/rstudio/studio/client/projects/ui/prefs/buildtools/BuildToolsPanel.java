/*
 * BuildToolsPanel.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialogResources;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class BuildToolsPanel extends VerticalPanel
{
   public BuildToolsPanel()
   {
   }
   
   abstract void load(RProjectOptions options);
   abstract void save(RProjectOptions options);
   
   protected void provideDefaults()
   {
   }
   
   boolean validate()
   {
      return true;
   }
   
   protected abstract class PathSelector extends TextBoxWithButton
   {
      public PathSelector(String label,
                          final String emptyLabel,
                          ElementIds.TextBoxButtonId uniqueId)
      {
         super(label, emptyLabel, "Browse...", null, uniqueId, true, null);
      }
      
      
      protected String getProjectPath(FileSystemItem projDir, 
                                      FileSystemItem path)
      {
         String proj = projDir.getPath();
         if (path.getPath().startsWith(proj + "/"))
         {
            return path.getPath().substring(proj.length() + 1);
         }
         else
         {
           return path.getPath();
         }
      }
   }
   
   protected class DirectorySelector extends PathSelector
   {
      public DirectorySelector(String label)
      {
         super(label, "(Project Root)", ElementIds.TextBoxButtonId.PROJECT_ROOT);
         
         addStyleName(RES.styles().directorySelector());
         
         // allow user to clear out a value
         setReadOnly(false);
         
         getTextBox().addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event)
            {
               if (getTextBox().getText().length() == 0)
                  getTextBox().setText("(Project Root)");
            }
            
         });  
         
         addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event)
            {
               final FileSystemItem projDir = RStudioGinjector.INSTANCE.
                     getSession().getSessionInfo().getActiveProjectDir();
           
               RStudioGinjector.INSTANCE.getFileDialogs().chooseFolder(
                 "Choose Directory",
                 RStudioGinjector.INSTANCE.getRemoteFileSystemContext(),
                 projDir,
                 new ProgressOperationWithInput<FileSystemItem>()
                 {
                    public void execute(FileSystemItem input,
                                        ProgressIndicator indicator)
                    {
                       if (input == null)
                          return;

                       indicator.onCompleted();
                       
                       setText(getProjectPath(projDir, input));
                    }
                 });
            }
         });
      }
      
      // allow user to set the value to empty string
      @Override
      public String getText()
      {
         if (getTextBox().getText().trim().isEmpty())
            return "";
         else
            return super.getText();
      }
   }
   
   protected class FileSelector extends PathSelector
   {
      public FileSelector(String label, ElementIds.TextBoxButtonId uniqueId)
      {
         super(label, "(None)", uniqueId);
         
         addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event)
            {
               final FileSystemItem projDir = RStudioGinjector.INSTANCE.
                     getSession().getSessionInfo().getActiveProjectDir();
           
               RStudioGinjector.INSTANCE.getFileDialogs().openFile(
                 "Choose File",
                 RStudioGinjector.INSTANCE.getRemoteFileSystemContext(),
                 projDir,
                 new ProgressOperationWithInput<FileSystemItem>()
                 {
                    public void execute(FileSystemItem input,
                                        ProgressIndicator indicator)
                    {
                       if (input == null)
                          return;

                       indicator.onCompleted();
                       
                       setText(getProjectPath(projDir, input));
                    }
                 });
            }
         });
      }
      
      
   }
   
   protected class AdditionalArguments extends Composite
   {
      AdditionalArguments(String label)
      {
         SafeHtml safeLabel = new SafeHtmlBuilder().appendHtmlConstant(label).toSafeHtml();

         VerticalPanel panel = new VerticalPanel();
         panel.addStyleName(RES.styles().buildToolsAdditionalArguments());

         FormLabel captionWidget = new FormLabel();
         captionWidget.getElement().setInnerSafeHtml(safeLabel);
         panel.add(captionWidget);

         textBox_ = new TextBox();
         DomUtils.disableSpellcheck(textBox_);
         panel.add(textBox_);
         captionWidget.setFor(textBox_);

         initWidget(panel);
      }
      
      public void setText(String text)
      {
         textBox_.setText(text);
      }
      
      public String getText()
      {
         return textBox_.getText().trim();
      }
      
      private final TextBox textBox_;
   }
   
   protected CheckBox checkBox(String caption)
   {
      CheckBox chk = new CheckBox(caption);
      chk.addStyleName(RES.styles().buildToolsCheckBox());
      return chk;
   }
   
   protected static ProjectPreferencesDialogResources RES = 
         ProjectPreferencesDialogResources.INSTANCE;
}
