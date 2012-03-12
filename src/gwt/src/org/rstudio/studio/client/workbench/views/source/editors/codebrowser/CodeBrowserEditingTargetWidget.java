/*
 * CodeBrowserEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.regex.Pattern.ReplaceOperation;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
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
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorLineWithCursorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetFindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

public class CodeBrowserEditingTargetWidget extends ResizeComposite
                              implements CodeBrowserEditingTarget.Display
{
   public CodeBrowserEditingTargetWidget(Commands commands,
                                         final GlobalDisplay globalDisplay,
                                         final EventBus eventBus,
                                         final UIPrefs uiPrefs,
                                         final CodeToolsServerOperations server,
                                         final DocDisplay docDisplay)
   {
      commands_ = commands;
      uiPrefs_ = uiPrefs;
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
         public boolean previewKeyDown(NativeEvent event)
         {
            int modifier = KeyboardShortcut.getModifierValue(event);
            if (modifier == KeyboardShortcut.NONE)
            {
               if (event.getKeyCode() == 112) // F1
               {
                  InputEditorLineWithCursorPosition linePos = 
                        InputEditorUtil.getLineWithCursorPosition(docDisplay);
              
                  server.getHelpAtCursor(
                     linePos.getLine(), linePos.getPosition(),
                     new SimpleRequestCallback<Void>("Help"));  
               }
               else if (event.getKeyCode() == 113) // F2
               {
                  goToFunctionDefinition();
               }
            }
            
            return false;
         }
         
         @Override
         public void commandClick()
         {
            goToFunctionDefinition();
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
         public boolean previewKeyPress(char charCode)
         {
            return false;
         }
         
         @Override
         public void close()
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
      docDisplay_.setCode(formatCode(functionDef), false); 
      docDisplay_.focus();
      contextWidget_.setCurrentFunction(functionDef);
   }
   
   @Override
   public void showFind()
   {
      findReplace_.showFindReplace();
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
      Toolbar toolbar = new EditingTargetToolbar(commands_);
      
      toolbar.addLeftWidget(commands_.printSourceDoc().createToolbarButton());  
      toolbar.addLeftWidget(findReplace_.createFindReplaceButton());
      toolbar.addLeftWidget(commands_.goToFunctionDefinition().createToolbarButton());
      
      Label readOnlyLabel = new Label("(Read-only)");
      readOnlyLabel.addStyleName(RES.styles().readOnly());
      toolbar.addRightWidget(readOnlyLabel);
    
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
         
      return toolbar;
   }
   
   private String formatCode(SearchPathFunctionDefinition functionDef)
   {
      // deal with null
      String code = functionDef.getCode();
      if (code == null)
         return "";
      
      // if this is from a source ref then leave it alone
      if (functionDef.isCodeFromSrcAttrib())
         return code;
      
      // determine the replacement text based on the user's current
      // editing preferences
      String replaceText = "\t";
      if (uiPrefs_.useSpacesForTab().getValue())
      {
         StringBuilder replaceBuilder = new StringBuilder();
         for (int i=0; i<uiPrefs_.numSpacesForTab().getValue(); i++)
            replaceBuilder.append(' ');
         replaceText = replaceBuilder.toString();
      }
      
      // create regex pattern used to find leading space
      // NOTE: the 4 spaces comes from the implementation of printtab2buff
      // in deparse.c -- it is hard-coded to use 4 spaces for the first 4 
      // levels of indentation and then 2 spaces for subsequent levels.
      final String replaceWith = replaceText;
      Pattern pattern = Pattern.create("^(    ){1,4}");
      code = pattern.replaceAll(code, new ReplaceOperation()
      {
         @Override
         public String replace(Match m)
         {
            return m.getValue().replace("    ", replaceWith);
         }
      });
      Pattern pattern2 = Pattern.create("^\t{4}(  )+");
      code = pattern2.replaceAll(code, new ReplaceOperation()
      {
         @Override
         public String replace(Match m)
         {
            return m.getValue().replace("  ",  replaceWith);
         }
      });

      return code;
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
   private final UIPrefs uiPrefs_;
   private final DocDisplay docDisplay_;
   private final TextEditingTargetFindReplace findReplace_;
   private String currentFunctionNamespace_ = null;
}
