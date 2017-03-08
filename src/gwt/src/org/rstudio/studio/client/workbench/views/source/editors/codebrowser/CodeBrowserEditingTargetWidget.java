/*
 * CodeBrowserEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.InfoBar;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorLineWithCursorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetFindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

public class CodeBrowserEditingTargetWidget extends ResizeComposite
                              implements CodeBrowserEditingTarget.Display
{
   public CodeBrowserEditingTargetWidget(Commands commands,
                                         final GlobalDisplay globalDisplay,
                                         final EventBus eventBus,
                                         final CodeToolsServerOperations server,
                                         final DocDisplay docDisplay)
   {
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;
      
      docDisplay_ = docDisplay;
      
      findReplace_ = new TextEditingTargetFindReplace(
         new TextEditingTargetFindReplace.Container() {

            @Override
            public AceEditor getEditor()
            {
               return (AceEditor)docDisplay_;
            }

            @Override
            public void insertFindReplace(FindReplaceBar findReplaceBar)
            {
               panel_.insertNorth(findReplaceBar,
                                  findReplaceBar.getHeight(),
                                  null);
            }

            @Override
            public void removeFindReplace(FindReplaceBar findReplaceBar)
            {
               panel_.remove(findReplaceBar);
            }
           
         },
         false); // don't show replace UI
      
      panel_ = new PanelWithToolbars(createToolbar(),
                                     createSecondaryToolbar(),
                                     docDisplay_.asWidget(),
                                     null);
      panel_.setSize("100%", "100%");
      
      docDisplay_.setReadOnly(true);
       
      // setup custom completion manager for executing F1 and F2 actions
      docDisplay_.setFileType(FileTypeRegistry.R, new CompletionManager() {
         
         @Override
         public void onPaste(PasteEvent event)
         {
            // read-only; this can be a no-op
         }

         @Override
         public boolean previewKeyDown(NativeEvent event)
         {
            int modifier = KeyboardShortcut.getModifierValue(event);
            if (modifier == KeyboardShortcut.NONE)
            {
               if (event.getKeyCode() == 112) // F1
               {
                  goToHelp();
               }
               else if (event.getKeyCode() == 113) // F2
               {
                  goToFunctionDefinition();
               }
            }
            
            return false;
         }
         
         @Override
         public void goToHelp()
         {
            InputEditorLineWithCursorPosition linePos = 
                  InputEditorUtil.getLineWithCursorPosition(docDisplay);
        
            server.getHelpAtCursor(
               linePos.getLine(), linePos.getPosition(),
               new SimpleRequestCallback<Void>("Help")); 
         }
         
         @Override
         public void goToFunctionDefinition()
         {
            // determine current line and cursor position
            InputEditorLineWithCursorPosition lineWithPos = 
                      InputEditorUtil.getLineWithCursorPosition(docDisplay);
             
            // navigate to the function at this position (if any)
            navigateToFunction(lineWithPos);  
         }
         
         @Override
         public void codeCompletion()
         {
            // no-op since this is a code browser
         }

         @Override
         public boolean previewKeyPress(char charCode)
         {
            return false;
         }
         
         @Override
         public void close()
         {
         }
         
         @Override
         public void detach()
         {
         }
         
      }); 
      
      initWidget(panel_);

   }
    
   @Override
   public Widget asWidget()
   {
      return this;
   }
   
   
   @Override
   public void adaptToFileType(TextFileType fileType)
   {
      docDisplay_.setFileType(fileType, true); 
   }


   @Override
   public void setFontSize(double size)
   {
      docDisplay_.setFontSize(size);
   }
   
   @Override
   public void onActivate()
   {
      docDisplay_.onActivate();
   }
   
   @Override
   public void showFunction(SearchPathFunctionDefinition functionDef)
   {
      currentFunctionNamespace_ = functionDef.getNamespace();
      docDisplay_.setCode(functionDef.getCode(), false); 
      // don't send focus to the display for debugging; we want it to stay in
      // the console
      if (!functionDef.isActiveDebugCode())
      {
         docDisplay_.focus();
      }
      contextWidget_.setCurrentFunction(functionDef);
   }
   
   @Override
   public void showFind(boolean defaultForward)
   {
      findReplace_.showFindReplace(defaultForward);
   }
   
   @Override
   public void findNext()
   {
      findReplace_.findNext();
      
   }

   @Override
   public void findPrevious()
   {
      findReplace_.findPrevious();
   }
   
   @Override
   public void findFromSelection()
   {
      findReplace_.findFromSelection();
   }
   
   @Override
   public void scrollToLeft()
   {
      new Timer() {
         @Override
         public void run()
         {
            docDisplay_.scrollToX(0); 
         }
      }.schedule(100);
   }
   
   @Override
   public void showWarningBar(String warning)
   {
      if (warningBar_ == null)
      {
         warningBar_ = new InfoBar(InfoBar.WARNING, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               hideWarningBar();
            }
            
         });
      }
      warningBar_.setText(warning);
      panel_.insertNorth(warningBar_, warningBar_.getHeight(), null);
   }

   @Override
   public void hideWarningBar()
   {
      if (warningBar_ != null)
      {
         panel_.remove(warningBar_);
      }
   }

   private void navigateToFunction(
         InputEditorLineWithCursorPosition lineWithPos)
   {
      server_.findFunctionInSearchPath(
            lineWithPos.getLine(),
            lineWithPos.getPosition(), 
            currentFunctionNamespace_,
            new FunctionSearchRequestCallback(true));
   }
   
   private class FunctionSearchRequestCallback
                    extends ServerRequestCallback<SearchPathFunctionDefinition>
   {
      public FunctionSearchRequestCallback(boolean searchLocally)
      {
         searchLocally_ = searchLocally;
         
         // delayed progress indicator
         progress_ = new GlobalProgressDelayer(
               globalDisplay_, 1000, "Searching for function definition...");

      }
      
      @Override
      public void onResponseReceived(SearchPathFunctionDefinition def)
      {
         // dismiss progress
         progress_.dismiss();

         // if we got a hit
         if (def != null && def.getName() != null)
         {         
            // try to search for the function locally
            SourcePosition position = searchLocally_ ?
                  docDisplay_.findFunctionPositionFromCursor(def.getName()) : 
                  null;
                  
            if (position != null)
            {
               docDisplay_.navigateToPosition(position, true);
            }
            else if (def.getNamespace() != null)
            {
               docDisplay_.recordCurrentNavigationPosition();
               eventBus_.fireEvent(new CodeBrowserNavigationEvent(
                     def));        
            }
         }
      }

      @Override
      public void onError(ServerError error)
      {
         progress_.dismiss();

         globalDisplay_.showErrorMessage(
                                 "Error Searching for Function",
                                 error.getUserMessage());
      }
      
      private final boolean searchLocally_;
      private final GlobalProgressDelayer progress_;
   }
   
   private Toolbar createToolbar()
   {
      Toolbar toolbar = new EditingTargetToolbar(commands_, true);

      toolbar.addLeftWidget(commands_.printSourceDoc().createToolbarButton()); 
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(findReplace_.createFindReplaceButton());
     
      ImageResource icon = new ImageResource2x(ThemeResources.INSTANCE.codeTransform2x());

      ToolbarPopupMenu menu = new ToolbarPopupMenu();
      menu.addItem(commands_.goToHelp().createMenuItem(false));
      menu.addItem(commands_.goToFunctionDefinition().createMenuItem(false));
      ToolbarButton codeTools = new ToolbarButton("", icon, menu);
      codeTools.setTitle("Code Tools");
      toolbar.addLeftWidget(codeTools);
      
      toolbar.addRightWidget(commands_.executeCode().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.executeLastCode().createToolbarButton());
      
      return toolbar;
   }
   
   private Toolbar createSecondaryToolbar()
   {
      SecondaryToolbar toolbar = new SecondaryToolbar();
      
      contextWidget_ = new CodeBrowserContextWidget(RES.styles());
      contextWidget_.addSelectionHandler(new SelectionHandler<String> () {
         @Override
         public void onSelection(SelectionEvent<String> event)
         {
            server_.getMethodDefinition(
                              event.getSelectedItem(),
                              new FunctionSearchRequestCallback(false));
         }
         
      });
      toolbar.addLeftWidget(contextWidget_);
      
      Label readOnlyLabel = new Label("(Read-only)");
      readOnlyLabel.addStyleName(RES.styles().readOnly());
      toolbar.addRightWidget(readOnlyLabel);
         
      return toolbar;
   }
   
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   interface Resources extends ClientBundle
   {
      @Source("CodeBrowserEditingTargetWidget.css")
      Styles styles();

   }

   interface Styles extends CssResource
   {
      String captionLabel();
      String menuElement();
      String functionName();
      String functionNamespace();
      String dropDownImage();
      String readOnly(); 
   }
   
   static Resources RES = GWT.create(Resources.class);

   private final PanelWithToolbars panel_;
   private CodeBrowserContextWidget contextWidget_;
   private final CodeToolsServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Commands commands_;
   private final DocDisplay docDisplay_;
   private final TextEditingTargetFindReplace findReplace_;
   private String currentFunctionNamespace_ = null;
   private InfoBar warningBar_;
}
