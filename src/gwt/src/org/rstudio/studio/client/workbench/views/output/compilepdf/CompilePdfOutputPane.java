/*
 * CompilePdfOutputPane.java
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
package org.rstudio.studio.client.workbench.views.output.compilepdf;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.shell.ShellWidget;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.compilepdf.model.CompilePdfError;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class CompilePdfOutputPane extends WorkbenchPane
      implements CompilePdfOutputPresenter.Display,
                 HasSelectionCommitHandlers<CompilePdfError>
{
   @Inject
   public CompilePdfOutputPane(FileTypeRegistry fileTypeRegistry)
   {
      super("Compile PDF");
      fileTypeRegistry_ = fileTypeRegistry;
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      panel_ = new SimplePanel();
      
      outputWidget_ = new ShellWidget(new AceEditor());
      outputWidget_.setSize("100%", "100%");
      outputWidget_.setMaxOutputLines(1000);
      outputWidget_.setReadOnly(true);
      outputWidget_.setSuppressPendingInput(true);
      panel_.setWidget(outputWidget_);
      
      lbErrors_ = new ListBox();
      lbErrors_.setVisibleItemCount(100);
      lbErrors_.setSize("100%", "100%");
      lbErrors_.addDoubleClickHandler(new DoubleClickHandler() 
      {
         @Override
         public void onDoubleClick(DoubleClickEvent event)
         {
            CompilePdfError error = errors_.get(lbErrors_.getSelectedIndex());  
            SelectionCommitEvent.fire(CompilePdfOutputPane.this, error);
         }
      });
      
      return panel_;
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      fileImage_ = new Image(); 
      toolbar.addLeftWidget(fileImage_);
      
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar.addLeftWidget(fileLabel_);
      
      ImageResource stopImage = RStudioGinjector.INSTANCE.getCommands()
                                               .interruptR().getImageResource();
      stopButton_ = new ToolbarButton(stopImage, null);
      stopButton_.setVisible(false);
      toolbar.addRightWidget(stopButton_);
      return toolbar;
   }
  
   @Override
   public void compileStarted(String fileName)
   {
      clearAll();
      
      fileImage_.setResource(fileTypeRegistry_.getIconForFilename(fileName));
      
      String shortFileName = StringUtil.shortPathName(
                                 FileSystemItem.createFile(fileName), 
                                 ThemeStyles.INSTANCE.subtitle(), 
                                 350);
      
      fileLabel_.setText(shortFileName);
      
      stopButton_.setVisible(true);
   }

   @Override
   public void clearAll()
   {
      outputWidget_.clearOutput();
      lbErrors_.clear();
      panel_.setWidget(outputWidget_);  
   }
   
   @Override
   public void showOutput(String output)
   {
      outputWidget_.consoleWriteOutput(output);  
   }
   

   @Override
   public void showErrors(JsArray<CompilePdfError> errors)
   {
      errors_ = errors;
      
      lbErrors_.clear();
      for (int i=0; i<errors.length(); i++)
         lbErrors_.addItem(errors.get(i).asString(),
                           errors.get(i).toSource());
      
      panel_.setWidget(lbErrors_);
   }

   @Override
   public void compileCompleted()
   {
      stopButton_.setVisible(false);
   }
   
   @Override
   public HasClickHandlers stopButton()
   {
      return stopButton_;
   }
  
   @Override
   public HasSelectionCommitHandlers<CompilePdfError> errorList()
   {
      return this;
   }
   
   @Override
   public HandlerRegistration addSelectionCommitHandler(
                           SelectionCommitHandler<CompilePdfError> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }
  
   
   private Image fileImage_;
   private ToolbarLabel fileLabel_;
   
   private ToolbarButton stopButton_;
   private SimplePanel panel_;
   private ShellWidget outputWidget_;
   private JsArray<CompilePdfError> errors_;
   private ListBox lbErrors_;
   private FileTypeRegistry fileTypeRegistry_;
}
