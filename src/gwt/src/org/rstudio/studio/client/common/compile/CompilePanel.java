/*
 * CompilePanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


package org.rstudio.studio.client.common.compile;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.widget.LeftRightToggleButton;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;

public class CompilePanel extends Composite
{
   public CompilePanel()
   {
      this(new CompileOutputBuffer());
   }
   
   public CompilePanel(CompileOutputDisplay outputDisplay)
   {
      
      panel_ = new SimplePanel();
      outputDisplay_ = outputDisplay;
      
      panel_.setWidget(outputDisplay_.asWidget());
      errorList_ = new SourceMarkerList();
      
      initWidget(panel_);
   }
   
   public void connectToolbar(Toolbar toolbar)
   {
      Commands commands = RStudioGinjector.INSTANCE.getCommands();
      ImageResource stopImage = commands.interruptR().getImageResource();
      stopButton_ = new ToolbarButton(stopImage, null);
      stopButton_.setVisible(false);
      toolbar.addRightWidget(stopButton_);
      
      showOutputButton_ = new LeftRightToggleButton("Output", "Issues", false);
      showOutputButton_.setVisible(false);
      showOutputButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
           showOutputButton_.setVisible(false);
           showErrorsButton_.setVisible(true);
           panel_.setWidget(outputDisplay_.asWidget());
           outputDisplay_.scrollToBottom();
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
   }
   
   // NOTE: targetFileName enables optional suppressing of the file
   // name header in the error list -- it's fine to pass null for this
   // and in that case there will always be a header
   public void compileStarted(String targetFileName)
   {
      clearAll();
      
      targetFileName_ = targetFileName;

      showOutputButton_.setVisible(false);
      showErrorsButton_.setVisible(false);
      if (canStop_)
        stopButton_.setVisible(true);
   }

   public void clearAll()
   {
      targetFileName_ = null;
      showOutputButton_.setVisible(false);
      showErrorsButton_.setVisible(false);
      stopButton_.setVisible(false);
      outputDisplay_.clear();
      errorList_.clear();
      panel_.setWidget(outputDisplay_.asWidget());  
   }
   
   public void showOutput(CompileOutput output, boolean scrollToBottom)
   {
      switch(output.getType())
      {
      case CompileOutput.kCommand:
         outputDisplay_.writeCommand(output.getOutput());
         break;
      case CompileOutput.kNormal:
         outputDisplay_.writeOutput(output.getOutput());
         break;
      case CompileOutput.kError:
         outputDisplay_.writeError(output.getOutput());
         break;
      }
   }
   
   public void showOutput(String output)
   {
      outputDisplay_.writeOutput(output);
   }
   
   public void showErrors(String basePath, 
                          JsArray<SourceMarker> errors,
                          int autoSelect)
   {
      showErrors(basePath, errors, autoSelect, false);
   }
   
   public void showErrors(String basePath, 
                          JsArray<SourceMarker> errors,
                          int autoSelect,
                          boolean alwaysShowList)
   {
      errorList_.showMarkers(targetFileName_, 
                             basePath, 
                             errors,
                             autoSelect);

      if (alwaysShowList || SourceMarker.showErrorList(errors))
      {
         panel_.setWidget(errorList_);
         showOutputButton_.setVisible(true);
      }
      else
      {
         showErrorsButton_.setVisible(true);
      }
   }  
 
   
   public void scrollToBottom()
   {
      outputDisplay_.scrollToBottom();
   }

   public void compileCompleted()
   {
      stopButton_.setVisible(false);
      
      if (isErrorPanelShowing())
         errorList_.focus();
   }
   
   public HasClickHandlers stopButton()
   {
      return stopButton_;
   }
   
   public HasSelectionCommitHandlers<CodeNavigationTarget> errorList()
   {
      return errorList_;
   }
   
   public void setCanStop(boolean canStop)
   {
      canStop_ = canStop;
   }
   
   private boolean isErrorPanelShowing()
   {
      return errorList_.isAttached();
   }

   private String targetFileName_;
   
   private ToolbarButton stopButton_;
   private LeftRightToggleButton showOutputButton_;
   private LeftRightToggleButton showErrorsButton_;
   private SimplePanel panel_;
   private CompileOutputDisplay outputDisplay_;
   private SourceMarkerList errorList_;
   private boolean canStop_ = true;
}
