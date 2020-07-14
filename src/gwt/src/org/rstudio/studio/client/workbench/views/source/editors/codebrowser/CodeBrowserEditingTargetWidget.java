/*
 * CodeBrowserEditingTargetWidget.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.InfoBar;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
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
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetFindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CommandClickEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

public class CodeBrowserEditingTargetWidget extends ResizeComposite
                              implements CodeBrowserEditingTarget.Display
{
   public CodeBrowserEditingTargetWidget(Commands commands,
                                         SourceColumn column,
                                         final GlobalDisplay globalDisplay,
                                         final EventBus eventBus,
                                         final CodeToolsServerOperations server,
                                         final DocDisplay docDisplay)
   {
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;
      column_ = column;
      
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
      Roles.getTabpanelRole().set(panel_.getElement());
      setAccessibleName(null);
      docDisplay_.setReadOnly(true);
      
      docDisplay_.addCommandClickHandler(new CommandClickEvent.Handler()
      {
         @Override
         public void onCommandClick(CommandClickEvent event)
         {
            // force cursor position
            Position position = event.getEvent().getDocumentPosition();
            docDisplay_.setCursorPosition(position);
            
            // go to definition
            docDisplay_.goToDefinition();
         }
      });
       
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
                  goToDefinition();
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
         public void goToDefinition()
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
   public void setAccessibleName(String name)
   {
      if (StringUtil.isNullOrEmpty(name))
         name = "Untitled Source Viewer";
      Roles.getTabpanelRole().setAriaLabelProperty(panel_.getElement(), name + " Source Viewer");
   }

   private void showWarningImpl(final Command command)
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
      command.execute();
      panel_.insertNorth(warningBar_, warningBar_.getHeight(), null);
   }
   
   @Override
   public void showReadOnlyWarning(final List<String> alternatives)
   {
      showWarningImpl(() -> warningBar_.showReadOnlyWarning(alternatives));
   }
   
   @Override
   public void showRequiredPackagesMissingWarning(List<String> packages)
   {
      // no-op for code browser targets
   }
   
   @Override
   public void showTexInstallationMissingWarning(String message)
   {
      // no-op for code browser targets
   }
   
   @Override
   public void showPanmirrorFormatChanged(Command onReload)
   {
      // no-op for code browser targets
   }
   
   @Override
   public void showWarningBar(final String warning)
   {
      showWarningImpl(() -> warningBar_.setText(warning));
   }
   
   @Override
   public void showWarningBar(String warning, String actionLabel, Command command)
   {
      showWarningImpl(() -> warningBar_.setTextWithAction(warning, actionLabel, command));
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
      Toolbar toolbar = new EditingTargetToolbar(commands_, true, column_);

      // Buttons are unique to a source column so require SourceAppCommands
      SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();

      toolbar.addLeftWidget(
         mgr.getSourceCommand(commands_.printSourceDoc(), column_).createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(findReplace_.createFindReplaceButton());
     
      ImageResource icon = new ImageResource2x(ThemeResources.INSTANCE.codeTransform2x());

      ToolbarPopupMenu menu = new ToolbarPopupMenu();
      menu.addItem(commands_.goToHelp().createMenuItem(false));
      menu.addItem(commands_.goToDefinition().createMenuItem(false));
      ToolbarMenuButton codeTools = new ToolbarMenuButton(ToolbarButton.NoText, "Code Tools", icon, menu);
      toolbar.addLeftWidget(codeTools);
      
      toolbar.addRightWidget(
         mgr.getSourceCommand(commands_.executeCode(), column_).createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(
         mgr.getSourceCommand(commands_.executeLastCode(), column_).createToolbarButton());
      
      return toolbar;
   }
   
   private Toolbar createSecondaryToolbar()
   {
      SecondaryToolbar toolbar = new SecondaryToolbar("Code Browser Second");
      
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
   private SourceColumn column_;
  
}
