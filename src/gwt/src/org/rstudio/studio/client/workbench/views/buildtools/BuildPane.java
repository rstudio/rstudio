/*
 * BuildPane.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.CheckableMenuItem;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.compile.CompileOutputBufferWithHighlight;
import org.rstudio.studio.client.common.compile.CompilePanel;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.buildtools.model.BookdownFormats;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;

public class BuildPane extends WorkbenchPane 
      implements BuildPresenter.Display
{
   @Inject
   public BuildPane(Commands commands,
                    Session session,
                    BuildServerOperations server)
   {
      super("Build");
      commands_ = commands;
      session_ = session;
      server_ = server;
      compilePanel_ = new CompilePanel(new CompileOutputBufferWithHighlight());
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      SessionInfo sessionInfo =  session_.getSessionInfo();
      String type = sessionInfo.getBuildToolsType();
      boolean pkg = type.equals(SessionInfo.BUILD_TOOLS_PACKAGE);
      boolean makefile = type.equals(SessionInfo.BUILD_TOOLS_MAKEFILE);
      boolean website = type.equals(SessionInfo.BUILD_TOOLS_WEBSITE);
      
      
      // always include build all
      ToolbarButton buildAllButton = commands_.buildAll().createToolbarButton();
      if (website)
      {
         if (sessionInfo.getBuildToolsBookdownWebsite())
         {
            buildAllButton.setText("Build Book");
         }
         else
         {
            buildAllButton.setText("Build Website");
         }
      }
      toolbar.addLeftWidget(buildAllButton);
      
      // book build menu
      if (sessionInfo.getBuildToolsBookdownWebsite())
      {
         BookdownBuildPopupMenu buildPopupMenu = new BookdownBuildPopupMenu();
         ToolbarButton buildMenuButton = new ToolbarButton(buildPopupMenu, true);
         toolbar.addLeftWidget(buildMenuButton);
      }
      
      toolbar.addLeftSeparator();
      
      // packages get check package
      if (pkg)
      {
         toolbar.addLeftWidget(commands_.checkPackage().createToolbarButton());
         toolbar.addLeftSeparator();
      }
      
      // create more menu
      if (makefile || website || pkg)
      {
         ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
         if (makefile || website)
         {
            if (makefile)
               moreMenu.addItem(commands_.rebuildAll().createMenuItem(false));
            moreMenu.addItem(commands_.cleanAll().createMenuItem(false));
            moreMenu.addSeparator();
         }
         
         // packages get additional commands 
         else if (pkg)
         {
            moreMenu.addItem(commands_.devtoolsLoadAll().createMenuItem(false));
            moreMenu.addItem(commands_.rebuildAll().createMenuItem(false));
            moreMenu.addSeparator();
            moreMenu.addItem(commands_.testPackage().createMenuItem(false));
            moreMenu.addSeparator();
            moreMenu.addItem(commands_.checkPackage().createMenuItem(false));
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
                                      new ImageResource2x(StandardIcons.INSTANCE.more_actions2x()),
                                      moreMenu);
         toolbar.addLeftWidget(moreButton);
      }
      
      // connect compile panel
      compilePanel_.connectToolbar(toolbar);
     
      
      return toolbar;
   }
   
   class BookdownBuildPopupMenu extends ToolbarPopupMenu
   {
      @Override
      public void getDynamicPopupMenu(final 
            ToolbarPopupMenu.DynamicPopupMenuCallback callback)
      {
         clearItems();
         
         server_.getBookdownFormats(new SimpleRequestCallback<BookdownFormats>() {

            @Override
            public void onResponseReceived(BookdownFormats formats)
            {
               String defaultFormat = formats.getOutputFormat();
               JsArrayString allFormats = formats.getAllOututFormats(); 
               MenuItem allMenu = new FormatMenuItem(
                  "all", "All Formats", defaultFormat.equals("all"));
               addItem(allMenu);
               addSeparator();    
               for (int i = 0; i<allFormats.length(); i++)
               {
                  String format = allFormats.get(i);
                  addItem(new FormatMenuItem(format, 
                                             defaultFormat.equals(format)));
               }
               callback.onPopupMenu(BookdownBuildPopupMenu.this);
            }
         });
      }
      
      class FormatMenuItem extends CheckableMenuItem
      {
         public FormatMenuItem(String format, boolean isChecked)
         {
            this(format, format, isChecked);
         }

         public FormatMenuItem(String format, String label, boolean isChecked)
         {
            super(label);
            format_ = format;
            label_ = label;
            isChecked_ = isChecked;
            onStateChanged();
         }
         
         @Override
         public String getLabel()
         {
            return label_;
         }

         @Override
         public boolean isChecked()
         {
            return isChecked_;
         }

         @Override
         public void onInvoked()
         {
            SelectionCommitEvent.fire(buildSubType(), format_);
         }
         
         private String format_;
         private String label_;
         private boolean isChecked_;
      }
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
   public void showOutput(CompileOutput output, boolean scrollToBottom)
   {
      compilePanel_.showOutput(output, scrollToBottom);
   }
   
   @Override
   public void showErrors(String basePath,
                          JsArray<SourceMarker> errors, 
                          boolean ensureVisible,
                          int autoSelect)
   {
      compilePanel_.showErrors(basePath, errors, autoSelect);
      
      if (ensureVisible && SourceMarker.showErrorList(errors))
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
   public HasSelectionCommitHandlers<String> buildSubType()
   {
      return new HasSelectionCommitHandlers<String>() {

         @Override
         public void fireEvent(GwtEvent<?> event)
         {
            BuildPane.this.fireEvent(event);
         }

         @Override
         public HandlerRegistration addSelectionCommitHandler(
               SelectionCommitHandler<String> handler)
         {
            return BuildPane.this.addHandler(handler, 
                                             SelectionCommitEvent.getType());
         }
         
      };
   }
   
   @Override
   public void scrollToBottom()
   {
      compilePanel_.scrollToBottom();   
   }
 
   private Commands commands_;
   private Session session_;
   private BuildServerOperations server_;
   
   CompilePanel compilePanel_;

}
