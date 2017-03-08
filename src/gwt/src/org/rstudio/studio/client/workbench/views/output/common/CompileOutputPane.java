/*
 * CompileOutputPane.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.common;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.compile.CompileOutputBufferWithHighlight;
import org.rstudio.studio.client.common.compile.CompilePanel;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

public class CompileOutputPane extends WorkbenchPane
      implements CompileOutputPaneDisplay
{
   @Inject
   public CompileOutputPane(@Assisted("taskName") String taskName, 
                            @Assisted("logTitle") String logTitle)
   {
      super(taskName);
      compilePanel_ = new CompilePanel(new CompileOutputBufferWithHighlight());
      ensureWidget();
      logTitle_ = logTitle;
   }

   @Override
   protected Widget createMainWidget()
   { 
      return compilePanel_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      fileLabel_ = new ToolbarFileLabel(toolbar, 200);
      
      ImageResource showLogImage = new ImageResource2x(StandardIcons.INSTANCE.show_log2x());
      if (hasLogs_)
      {
         showLogButton_ = new ToolbarButton("View Log", 
                                            showLogImage, 
                                            (ClickHandler) null);
         showLogButton_.getElement().getStyle().setMarginBottom(3, Unit.PX);
         showLogButton_.setTitle(logTitle_);
         showLogSeparator_ = toolbar.addLeftSeparator();
         setShowLogVisible(false);
         toolbar.addLeftWidget(showLogButton_);
      }
      
      compilePanel_.connectToolbar(toolbar);
     
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
     
      fileLabel_.setFileName(fileName);
      setShowLogVisible(false);
      
      compilePanel_.compileStarted(fileName);
   }

   @Override
   public void clearAll()
   {
      setShowLogVisible(false);
      
      compilePanel_.clearAll();
  
   }
   
   @Override
   public void showOutput(CompileOutput output, boolean scrollToBottom)
   {
      compilePanel_.showOutput(output, scrollToBottom);
   }
   

   @Override
   public void showErrors(JsArray<SourceMarker> errors)
   {
      compilePanel_.showErrors(null, errors, SourceMarkerList.AUTO_SELECT_FIRST);
      
      if (SourceMarker.showErrorList(errors))
         ensureVisible(true);
   }

   @Override
   public boolean isEffectivelyVisible()
   {
      return DomUtils.isEffectivelyVisible(getElement());
   }

   @Override
   public void scrollToBottom()
   {
      compilePanel_.scrollToBottom();   
   }

   @Override
   public void compileCompleted()
   {
      compilePanel_.compileCompleted();
      
      setShowLogVisible(true);
   }
   
   @Override
   public HasClickHandlers stopButton()
   {
      return compilePanel_.stopButton();
   }
   
   @Override 
   public HasClickHandlers showLogButton()
   {
      return showLogButton_;
   }
  
   @Override
   public HasSelectionCommitHandlers<CodeNavigationTarget> errorList()
   {
      return compilePanel_.errorList();
   }
   
   @Override
   public void setHasLogs(boolean logs)
   {
      hasLogs_ = logs;
   }
   
   @Override
   public void setCanStop(boolean canStop)
   {
      compilePanel_.setCanStop(canStop);
   }

   private void setShowLogVisible(boolean visible)
   {
      if (!hasLogs_)
         return;

      showLogSeparator_.setVisible(visible);
      showLogButton_.setVisible(visible);
   }
   
   private ToolbarFileLabel fileLabel_;
   private Widget showLogSeparator_;
   private ToolbarButton showLogButton_;
   
   private CompilePanel compilePanel_;
   private String logTitle_;
   private boolean hasLogs_ = true;
}