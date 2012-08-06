/*
 * BuildPane.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.compile.CompileError;
import org.rstudio.studio.client.common.compile.CompilePanel;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

public class BuildPane extends WorkbenchPane implements BuildPresenter.Display
{
   @Inject
   public BuildPane(Commands commands,
                    Session session)
   {
      super("Build");
      commands_ = commands;
      session_ = session;
      compilePanel_ = new CompilePanel();
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      // always include build all
      toolbar.addLeftWidget(commands_.buildAll().createToolbarButton());
      toolbar.addLeftSeparator();
      
      // packages get check package
      String type = session_.getSessionInfo().getBuildToolsType();
      if (type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         toolbar.addLeftWidget(commands_.checkPackage().createToolbarButton());
         toolbar.addLeftSeparator();
      }
      
      // create more menu
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
      if (type.equals(SessionInfo.BUILD_TOOLS_MAKEFILE))
      {
         moreMenu.addItem(commands_.rebuildAll().createMenuItem(false));
         moreMenu.addItem(commands_.cleanAll().createMenuItem(false));
         moreMenu.addSeparator();
      }
      
      // packages get additional commands 
      else if (type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         moreMenu.addItem(commands_.devtoolsLoadAll().createMenuItem(false));
         moreMenu.addSeparator();
         moreMenu.addItem(commands_.rebuildAll().createMenuItem(false));
         moreMenu.addSeparator();
         moreMenu.addItem(commands_.buildSourcePackage().createMenuItem(false));
         moreMenu.addItem(commands_.buildBinaryPackage().createMenuItem(false));
         moreMenu.addSeparator();
         moreMenu.addItem(commands_.roxygenizePackage().createMenuItem(false));   
         moreMenu.addSeparator();
      }
      moreMenu.addItem(commands_.buildToolsProjectSetup().createMenuItem(false));
      
      // add more menu
      ToolbarButton moreButton = new ToolbarButton(
                                   "More",
                                   StandardIcons.INSTANCE.more_actions(),
                                   moreMenu);
      toolbar.addLeftWidget(moreButton);
      
      // connect compile panel
      compilePanel_.connectToolbar(toolbar);
     
      
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {      
      return compilePanel_;
   }
   
   @Override
   public void buildStarted()
   {
      compilePanel_.compileStarted(null);  
   }

   @Override
   public void showOutput(String output)
   {
      compilePanel_.showOutput(output);   
   }
   
   @Override
   public void showErrors(JsArray<CompileError> errors, boolean ensureVisible)
   {
      compilePanel_.showErrors(errors);
      
      if (ensureVisible && CompileError.includesErrorType(errors))
         ensureVisible();
   }
   
   @Override
   public void buildCompleted()
   {
      compilePanel_.compileCompleted();  
   }
   
   @Override
   public HasClickHandlers stopButton()
   {
      return compilePanel_.stopButton();
   }
   
   @Override
   public HasSelectionCommitHandlers<CodeNavigationTarget> errorList()
   {
      return compilePanel_.errorList();
   }
   
   @Override
   public void scrollToBottom()
   {
      compilePanel_.scrollToBottom();   
   }
 
   private Commands commands_;
   private Session session_;
   
   CompilePanel compilePanel_;

}
