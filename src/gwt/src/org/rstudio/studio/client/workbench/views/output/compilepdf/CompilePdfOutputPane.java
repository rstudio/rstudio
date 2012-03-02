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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.compilepdf.CompilePdfErrorList;
import org.rstudio.studio.client.common.compilepdf.CompilePdfOutputBuffer;
import org.rstudio.studio.client.common.compilepdf.CompilePdfResources;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfError;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

public class CompilePdfOutputPane extends WorkbenchPane
      implements CompilePdfOutputPresenter.Display
{
   @Inject
   public CompilePdfOutputPane(FileTypeRegistry fileTypeRegistry)
   {
      super("Compile PDF");
      fileTypeRegistry_ = fileTypeRegistry;
      res_ = GWT.create(CompilePdfResources.class);
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      panel_ = new SimplePanel();
      outputBuffer_ = new CompilePdfOutputBuffer();
      panel_.setWidget(outputBuffer_);

      errorList_ = new CompilePdfErrorList();
      
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
      fileLabel_.addStyleName(res_.styles().fileLabel());
      toolbar.addLeftWidget(fileLabel_);
      
      Commands commands = RStudioGinjector.INSTANCE.getCommands();
      ImageResource stopImage = commands.interruptR().getImageResource();
      stopButton_ = new ToolbarButton(stopImage, null);
      stopButton_.setVisible(false);
      toolbar.addRightWidget(stopButton_);
      
      ImageResource showLogImage = res_.showLogCommand();
      showLogButton_ = new ToolbarButton("View Log", 
                                         showLogImage, 
                                         (ClickHandler) null);
      showLogButton_.getElement().getStyle().setMarginBottom(3, Unit.PX);
      showLogButton_.setTitle("View the LaTeX compilation log");
      showLogSeparator_ = toolbar.addLeftSeparator();
      setShowLogVisible(false);
      toolbar.addLeftWidget(showLogButton_);
      
      showOutputButton_ = new LeftRightToggleButton("Output", "Issues", false);
      showOutputButton_.setVisible(false);
      showOutputButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
           showOutputButton_.setVisible(false);
           showErrorsButton_.setVisible(true);
           panel_.setWidget(outputBuffer_);
           outputBuffer_.scrollToBottom();
         }
      });
      toolbar.addRightWidget(showOutputButton_);
       
      showErrorsButton_ = new LeftRightToggleButton("Output", "Issues",  true);
      showErrorsButton_.setVisible(false);
      showErrorsButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
           showOutputButton_.setVisible(true);
           showErrorsButton_.setVisible(false);
           panel_.setWidget(errorList_);
         }
      });
      toolbar.addRightWidget(showErrorsButton_);
     
      return toolbar;
   }

   @Override
   public void ensureVisible(boolean activate)
   {
      fireEvent(new EnsureVisibleEvent(activate));
   }

   @Override
   public void compileStarted(String fileName)
   {
      clearAll();
      
      fileName_ = fileName;

      fileImage_.setResource(fileTypeRegistry_.getIconForFilename(fileName));
      
      String shortFileName = StringUtil.shortPathName(
                                 FileSystemItem.createFile(fileName), 
                                 ThemeStyles.INSTANCE.subtitle(), 
                                 350);
      
      fileLabel_.setText(shortFileName);
      
      showOutputButton_.setVisible(false);
      showErrorsButton_.setVisible(false);
      stopButton_.setVisible(true);
      setShowLogVisible(false);
   }

   @Override
   public void clearAll()
   {
      fileName_ = null;
      showOutputButton_.setVisible(false);
      showErrorsButton_.setVisible(false);
      stopButton_.setVisible(false);
      setShowLogVisible(false);
      outputBuffer_.clear();
      errorList_.clear();
      panel_.setWidget(outputBuffer_);  
   }
   
   @Override
   public void showOutput(String output)
   {
      outputBuffer_.append(output);
   }
   

   @Override
   public void showErrors(JsArray<CompilePdfError> errors)
   {
      errorList_.showErrors(fileName_, errors);

      if (CompilePdfError.includesErrorType(errors))
      {
         panel_.setWidget(errorList_);
         showOutputButton_.setVisible(true);
         ensureVisible(true);
      }
      else
      {
         showErrorsButton_.setVisible(true);
      }
   }

   @Override
   public boolean isErrorPanelShowing()
   {
      return errorList_.isAttached();
   }

   @Override
   public boolean isEffectivelyVisible()
   {
      return DomUtils.isEffectivelyVisible(getElement());
   }

   @Override
   public void scrollToBottom()
   {
      outputBuffer_.scrollToBottom();
   }

   @Override
   public void compileCompleted()
   {
      stopButton_.setVisible(false);
      setShowLogVisible(true);
      
      if (isErrorPanelShowing())
      {
         errorList_.selectFirstItem();
         errorList_.focus();
      }
   }
   
   @Override
   public HasClickHandlers stopButton()
   {
      return stopButton_;
   }
   
   @Override 
   public HasClickHandlers showLogButton()
   {
      return showLogButton_;
   }
  
   @Override
   public HasSelectionCommitHandlers<CodeNavigationTarget> errorList()
   {
      return errorList_;
   }
   
   private void setShowLogVisible(boolean visible)
   {
      showLogSeparator_.setVisible(visible);
      showLogButton_.setVisible(visible);
   }
   
   private Image fileImage_;
   private ToolbarLabel fileLabel_;
   private ToolbarButton stopButton_;
   private Widget showLogSeparator_;
   private ToolbarButton showLogButton_;
   private LeftRightToggleButton showOutputButton_;
   private LeftRightToggleButton showErrorsButton_;
   private SimplePanel panel_;

   private FileTypeRegistry fileTypeRegistry_;
   private CompilePdfResources res_;
   private String fileName_;
   
   private CompilePdfOutputBuffer outputBuffer_;
   private CompilePdfErrorList errorList_;
}
