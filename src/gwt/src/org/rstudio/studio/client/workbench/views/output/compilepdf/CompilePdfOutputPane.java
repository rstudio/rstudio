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
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.shell.ShellWidget;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.compilepdf.model.CompilePdfError;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class CompilePdfOutputPane extends WorkbenchPane
      implements CompilePdfOutputPresenter.Display
{
   @Inject
   public CompilePdfOutputPane()
   {
      super("Compile PDF");
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      SimplePanel panel = new SimplePanel();
      
      outputWidget_ = new ShellWidget(new AceEditor());
      outputWidget_.setSize("100%", "100%");
      outputWidget_.setMaxOutputLines(1000);
      outputWidget_.setReadOnly(true);
      outputWidget_.setSuppressPendingInput(true);
      panel.setWidget(outputWidget_);
      
      return panel;
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      return toolbar;
   }
  
   @Override
   public void clearOutput()
   {
      outputWidget_.clearOutput();
      
   }
   
   @Override
   public void showOutput(String output)
   {
      outputWidget_.consoleWriteOutput(output);
      
   }

   @Override
   public void showErrors(JsArray<CompilePdfError> errors)
   {
      outputWidget_.consoleWriteOutput("\n");
      for (int i=0; i<errors.length(); i++)
         outputWidget_.consoleWriteOutput(errors.get(i).asString() + "\n");
   }
   
   private ShellWidget outputWidget_;
}
