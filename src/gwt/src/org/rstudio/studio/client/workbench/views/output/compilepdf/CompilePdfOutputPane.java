/*
 * CompilePdfOutputPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.compile.CompileError;
import org.rstudio.studio.client.common.compile.CompilePanel;
import org.rstudio.studio.client.common.compile.errorlist.CompileErrorList;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

public class CompilePdfOutputPane extends WorkbenchPane
      implements CompilePdfOutputPresenter.Display
{
   @Inject
   public CompilePdfOutputPane()
   {
      super("Compile PDF");
      compilePanel_ = new CompilePanel();
      ensureWidget();
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
      
      ImageResource showLogImage = StandardIcons.INSTANCE.show_log();
      showLogButton_ = new ToolbarButton("View Log", 
                                         showLogImage, 
                                         (ClickHandler) null);
      showLogButton_.getElement().getStyle().setMarginBottom(3, Unit.PX);
      showLogButton_.setTitle("View the LaTeX compilation log");
      showLogSeparator_ = toolbar.addLeftSeparator();
      setShowLogVisible(false);
      toolbar.addLeftWidget(showLogButton_);
      
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
   public void showOutput(String output)
   {
      compilePanel_.showOutput(output);
   }
   

   @Override
   public void showErrors(JsArray<CompileError> errors)
   {
      compilePanel_.showErrors(null, errors, CompileErrorList.AUTO_SELECT_FIRST);
      
      if (CompileError.includesErrorType(errors))
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
   
   private void setShowLogVisible(boolean visible)
   {
      showLogSeparator_.setVisible(visible);
      showLogButton_.setVisible(visible);
   }
   
   private ToolbarFileLabel fileLabel_;
   private Widget showLogSeparator_;
   private ToolbarButton showLogButton_;

   
   private CompilePanel compilePanel_;
}
