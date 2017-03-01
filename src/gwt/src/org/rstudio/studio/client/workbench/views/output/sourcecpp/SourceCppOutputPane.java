/*
 * SourceCppOutputPane.java
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
package org.rstudio.studio.client.workbench.views.output.sourcecpp;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.compile.CompileOutputBufferWithHighlight;
import org.rstudio.studio.client.common.compile.CompilePanel;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.model.SourceCppState;

public class SourceCppOutputPane extends WorkbenchPane
      implements SourceCppOutputPresenter.Display
{
   @Inject
   public SourceCppOutputPane()
   {
      super("Source Cpp");
      compilePanel_ = new CompilePanel(new CompileOutputBufferWithHighlight());
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
      compilePanel_.connectToolbar(toolbar);
      return toolbar;
   }

   @Override
   public void ensureVisible(boolean activate)
   {
      fireEvent(new EnsureVisibleEvent(activate));
   }

   @Override
   public void clearAll()
   { 
      compilePanel_.clearAll();
   }
   
   @Override
   public void showResults(SourceCppState state)
   {
      fileLabel_.setFileName(state.getTargetFile());
      
      JsArray<CompileOutput> outputs = state.getOutputs();
      for (int i=0; i<outputs.length(); i++)
         compilePanel_.showOutput(outputs.get(i), false);
      
      if (state.getErrors().length() > 0)
      {
         compilePanel_.showErrors(null, 
                                  state.getErrors(), 
                                  SourceMarkerList.AUTO_SELECT_FIRST,
                                  true);
      }
      compilePanel_.scrollToBottom();
   }
    
   @Override
   public void scrollToBottom()
   {
      compilePanel_.scrollToBottom();   
   }


   @Override
   public HasSelectionCommitHandlers<CodeNavigationTarget> errorList()
   {
      return compilePanel_.errorList();
   }
 
   private ToolbarFileLabel fileLabel_;
   private CompilePanel compilePanel_;
}
