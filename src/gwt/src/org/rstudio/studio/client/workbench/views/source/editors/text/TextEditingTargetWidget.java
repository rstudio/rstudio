/*
 * TextEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ImageMenuItem;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.events.RmdOutputFormatChangedEvent;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.ui.ShinyViewerTypePopupMenu;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.edit.ui.EditDialog;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.Display;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarWidget;

public class TextEditingTargetWidget
      extends ResizeComposite
      implements Display, RequiresVisibilityChanged
{
   public TextEditingTargetWidget(Commands commands,
                                  UIPrefs uiPrefs,
                                  FileTypeRegistry fileTypeRegistry,
                                  DocDisplay editor,
                                  TextFileType fileType,
                                  String extendedType,
                                  EventBus events,
                                  Session session)
   {
      commands_ = commands;
      uiPrefs_ = uiPrefs;
      session_ = session;
      fileTypeRegistry_ = fileTypeRegistry;
      editor_ = editor;
      extendedType_ = extendedType;
      sourceOnSave_ = new CheckBox();
      srcOnSaveLabel_ =
                  new CheckboxLabel(sourceOnSave_, "Source on Save").getLabel();
      statusBar_ = new StatusBarWidget();
      shinyViewerMenu_ = RStudioGinjector.INSTANCE.getShinyViewerTypePopupMenu();
      handlerManager_ = new HandlerManager(this);
      
      findReplace_ = new TextEditingTargetFindReplace(
         new TextEditingTargetFindReplace.Container()
         {  
            @Override
            public AceEditor getEditor()
            {
               return (AceEditor)editor_;
            }
            
            @Override
            public void insertFindReplace(FindReplaceBar findReplaceBar)
            {
               Widget beforeWidget = null;
               if (warningBar_ != null && warningBar_.isAttached())
                  beforeWidget = warningBar_;
               panel_.insertNorth(findReplaceBar,
                                  findReplaceBar.getHeight(),
                                  beforeWidget);
               
            }
            
            @Override
            public void removeFindReplace(FindReplaceBar findReplaceBar)
            {
               panel_.remove(findReplaceBar);
            } 
         });
      
      panel_ = new PanelWithToolbars(toolbar_ = createToolbar(fileType),
                                    editor.asWidget(),
                                    statusBar_);
      adaptToFileType(fileType);

      initWidget(panel_);
   }

   private StatusBarWidget statusBar_;

   private Toolbar createToolbar(TextFileType fileType)
   {
      Toolbar toolbar = new EditingTargetToolbar(commands_);
       
      toolbar.addLeftWidget(commands_.saveSourceDoc().createToolbarButton());
      sourceOnSave_.getElement().getStyle().setMarginRight(0, Unit.PX);
      toolbar.addLeftWidget(sourceOnSave_);
      srcOnSaveLabel_.getElement().getStyle().setMarginRight(9, Unit.PX);
      toolbar.addLeftWidget(srcOnSaveLabel_);

      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.checkSpelling().createToolbarButton());
      
      toolbar.addLeftWidget(findReplace_.createFindReplaceButton());
      toolbar.addLeftWidget(createCodeTransformMenuButton());
      
      notebookSeparatorWidget_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(notebookToolbarButton_ = 
            commands_.compileNotebook().createToolbarButton());
      
      int mod = BrowseCap.hasMetaKey() ? KeyboardShortcut.META : 
         KeyboardShortcut.CTRL;
      String cmdText = 
        new KeyboardShortcut(mod + KeyboardShortcut.SHIFT, 'K').toString(true);
      cmdText = DomUtils.htmlToText(cmdText);
      notebookToolbarButton_.setTitle("Compile Notebook (" + cmdText + ")");
      
      texSeparatorWidget_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(texToolbarButton_ = createLatexFormatButton());
      
      ToolbarPopupMenu helpMenu = new ToolbarPopupMenu();
      helpMenu.addItem(commands_.usingRMarkdownHelp().createMenuItem(false));
      helpMenu.addItem(commands_.authoringRPresentationsHelp().createMenuItem(false));
      helpMenu.addSeparator();
      helpMenu.addItem(commands_.markdownHelp().createMenuItem(false));
      helpMenuButton_ = new ToolbarButton(null, 
                                          StandardIcons.INSTANCE.help(), 
                                          helpMenu);
      toolbar.addLeftWidget(helpMenuButton_);
      toolbar.addLeftWidget(rcppHelpButton_ = commands_.rcppHelp().createToolbarButton());
      
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(previewHTMLButton_ = commands_.previewHTML().createToolbarButton());
      knitDocumentButton_ = commands_.knitDocument().createToolbarButton(false);
      knitDocumentButton_.getElement().getStyle().setMarginRight(2, Unit.PX);
      toolbar.addLeftWidget(knitDocumentButton_);
      toolbar.addLeftWidget(compilePdfButton_ = commands_.compilePDF().createToolbarButton());
      rmdFormatButton_ = new ToolbarPopupMenuButton(false, true);
      toolbar.addLeftWidget(rmdFormatButton_);
      toolbar.addLeftWidget(editRmdFormatButton_ = commands_.editRmdFormatOptions().createToolbarButton(false));

      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.synctexSearch().createToolbarButton());

      toolbar.addRightWidget(runButton_ = commands_.executeCode().createToolbarButton(false));
      toolbar.addRightSeparator();
      toolbar.addRightWidget(runLastButton_ = commands_.executeLastCode().createToolbarButton(false));
      toolbar.addRightSeparator();
      final String SOURCE_BUTTON_TITLE = "Source the active document"; 
      
      sourceButton_ = new ToolbarButton(
            "Source", 
            commands_.sourceActiveDocument().getImageResource(), 
            new ClickHandler() 
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  if (uiPrefs_.sourceWithEcho().getValue())
                     commands_.sourceActiveDocumentWithEcho().execute();
                  else
                     commands_.sourceActiveDocument().execute();
               }
            });
      
      sourceButton_.setTitle(SOURCE_BUTTON_TITLE);
      toolbar.addRightWidget(sourceButton_);
      
      uiPrefs_.sourceWithEcho().addValueChangeHandler(
                                       new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            if (event.getValue())
               sourceButton_.setTitle(SOURCE_BUTTON_TITLE + " (with echo)");
            else
               sourceButton_.setTitle(SOURCE_BUTTON_TITLE);
         }
      });
            
      ToolbarPopupMenu sourceMenu = new ToolbarPopupMenu();
      sourceMenu.addItem(commands_.sourceActiveDocument().createMenuItem(false));
      sourceMenu.addItem(commands_.sourceActiveDocumentWithEcho().createMenuItem(false));
         
      sourceMenuButton_ = new ToolbarButton(sourceMenu, true);
      toolbar.addRightWidget(sourceMenuButton_);  

      //toolbar.addRightSeparator();
     
      ToolbarPopupMenu chunksMenu = new ToolbarPopupMenu();
      chunksMenu.addItem(commands_.insertChunk().createMenuItem(false));
      chunksMenu.addSeparator();
      chunksMenu.addItem(commands_.jumpTo().createMenuItem(false));
      chunksMenu.addSeparator();
      chunksMenu.addItem(commands_.executePreviousChunks().createMenuItem(false));
      chunksMenu.addItem(commands_.executeCurrentChunk().createMenuItem(false));
      chunksMenu.addItem(commands_.executeNextChunk().createMenuItem(false));
      chunksMenu.addSeparator();
      chunksMenu.addItem(commands_.executeAllCode().createMenuItem(false));
      chunksButton_ = new ToolbarButton(
                       "Chunks",  
                       StandardIcons.INSTANCE.chunk_menu(), 
                       chunksMenu, 
                       true);
      toolbar.addRightWidget(chunksButton_);
      
      ToolbarPopupMenu shinyLaunchMenu = shinyViewerMenu_;
      shinyLaunchButton_ = new ToolbarButton(
                       shinyLaunchMenu, 
                       true);
      shinyLaunchButton_.setVisible(false);
      toolbar.addRightWidget(shinyLaunchButton_);
      if (SessionUtils.showPublishUi(session_, uiPrefs_))
      {
         toolbar.addRightSeparator();
         publishButton_ = new RSConnectPublishButton(
               RSConnect.CONTENT_TYPE_APP, false, commands_.rsconnectDeploy());
         toolbar.addRightWidget(publishButton_);
      }
      
      return toolbar;
   }
   
   private ToolbarButton createLatexFormatButton()
   {
      ToolbarPopupMenu texMenu = new TextEditingTargetLatexFormatMenu(editor_,
                                                                      uiPrefs_);
    
      ToolbarButton texButton = new ToolbarButton(
                           "Format", 
                           fileTypeRegistry_.getIconForFilename("foo.tex"), 
                           texMenu, 
                           false);
      return texButton;
   }
   
   private Widget createCodeTransformMenuButton()
   {
      if (codeTransform_ == null)
      {
         ImageResource icon = ThemeResources.INSTANCE.codeTransform();

         ToolbarPopupMenu menu = new ToolbarPopupMenu();
         menu.addItem(commands_.codeCompletion().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.goToHelp().createMenuItem(false));
         menu.addItem(commands_.goToFunctionDefinition().createMenuItem(false));
         menu.addItem(commands_.findUsages().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.extractFunction().createMenuItem(false));
         menu.addItem(commands_.extractLocalVariable().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.reflowComment().createMenuItem(false));
         menu.addItem(commands_.commentUncomment().createMenuItem(false));
         menu.addItem(commands_.insertRoxygenSkeleton().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.reindent().createMenuItem(false));
         menu.addItem(commands_.reformatCode().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.showDiagnosticsActiveDocument().createMenuItem(false));
         menu.addItem(commands_.showDiagnosticsProject().createMenuItem(false));
         codeTransform_ = new ToolbarButton("", icon, menu);
         codeTransform_.setTitle("Code Tools");
      }
      
      return codeTransform_;
   }
   
   public void adaptToExtendedFileType(String extendedType)
   {
      extendedType_ = extendedType;
      adaptToFileType(editor_.getFileType());
   }

   public void adaptToFileType(TextFileType fileType)
   {
      editor_.setFileType(fileType);
      boolean canCompilePdf = fileType.canCompilePDF();
      boolean canKnitToHTML = fileType.canKnitToHTML();
      boolean canCompileNotebook = fileType.canCompileNotebook();
      boolean canSource = fileType.canSource();
      boolean canSourceWithEcho = fileType.canSourceWithEcho();
      boolean canSourceOnSave = fileType.canSourceOnSave();
      boolean canExecuteCode = fileType.canExecuteCode();
      boolean canExecuteChunks = fileType.canExecuteChunks();
      boolean isMarkdown = fileType.isMarkdown();
      boolean isPlainMarkdown = fileType.isPlainMarkdown();
      boolean isRPresentation = fileType.isRpres();
      boolean isCpp = fileType.isCpp();
      boolean isScript = fileType.isScript();
      boolean isRMarkdown2 = extendedType_.equals("rmarkdown");
      boolean canPreviewFromR = fileType.canPreviewFromR();
      
      // don't show the run buttons for cpp files, or R files in Shiny
      runButton_.setVisible(canExecuteCode && !isCpp && !isShinyFile());
      runLastButton_.setVisible(runButton_.isVisible());
      
      sourceOnSave_.setVisible(canSourceOnSave);
      srcOnSaveLabel_.setVisible(canSourceOnSave);
      if (fileType.isRd() || canPreviewFromR)
         srcOnSaveLabel_.setText(fileType.getPreviewButtonText() + " on Save");
      else
         srcOnSaveLabel_.setText("Source on Save");
      codeTransform_.setVisible(
            (canExecuteCode && !fileType.canAuthorContent()) ||
            fileType.isC() || fileType.isStan());   
     
      sourceButton_.setVisible(canSource && !isPlainMarkdown);
      sourceMenuButton_.setVisible(canSourceWithEcho && 
                                   !isPlainMarkdown && 
                                   !isScript &&
                                   !canPreviewFromR);
   
      texSeparatorWidget_.setVisible(canCompilePdf);
      texToolbarButton_.setVisible(canCompilePdf);
      compilePdfButton_.setVisible(canCompilePdf);
      chunksButton_.setVisible(canExecuteChunks);
      
      notebookSeparatorWidget_.setVisible(canCompileNotebook);
      notebookToolbarButton_.setVisible(canCompileNotebook);
      
      knitDocumentButton_.setVisible(canKnitToHTML);
      
      rmdFormatButton_.setVisible(isRMarkdown2);
      editRmdFormatButton_.setVisible(isRMarkdown2);
      editRmdFormatButton_.setEnabled(isRMarkdown2);

      helpMenuButton_.setVisible(isMarkdown || isRPresentation);
      rcppHelpButton_.setVisible(isCpp);
      
      if (isShinyFile())
      {
         sourceOnSave_.setVisible(false);
         srcOnSaveLabel_.setVisible(false);
         runButton_.setVisible(false);
         sourceMenuButton_.setVisible(false);
         chunksButton_.setVisible(false);
         shinyLaunchButton_.setVisible(true);
         setSourceButtonFromShinyState();
      }
      else
      {
         setSourceButtonFromScriptState(isScript, 
                                        canPreviewFromR,
                                        fileType.getPreviewButtonText());
      }
      
      toolbar_.invalidateSeparators();
   }
   
   private boolean isShinyFile()
   {
      return extendedType_.equals("shiny");
   }

   public HasValue<Boolean> getSourceOnSave()
   {
      return sourceOnSave_;
   }

   public void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }
   
   @Override
   public void onResize() 
   {
      super.onResize();
      
      manageToolbarSizes();
     
   }

   private void manageToolbarSizes()
   {
      // sometimes width is passed in as 0 (not sure why)
      int width = getOffsetWidth();
      if (width == 0)
         return;
      
      texToolbarButton_.setText(width < 520 ? "" : "Format");
      runButton_.setText(((width < 480) || isShinyFile()) ? "" : "Run");
      compilePdfButton_.setText(width < 450 ? "" : "Compile PDF");
      previewHTMLButton_.setText(width < 450 ? "" : previewCommandText_);                                                       
      knitDocumentButton_.setText(width < 450 ? "" : knitCommandText_);
      
      if (editor_.getFileType().isRd() || editor_.getFileType().canPreviewFromR())
      {
         String preview = editor_.getFileType().getPreviewButtonText();
         srcOnSaveLabel_.setText(width < 450 ? preview : preview + " on Save");
      }
      else
         srcOnSaveLabel_.setText(width < 450 ? "Source" : "Source on Save");
      sourceButton_.setText(width < 400 ? "" : sourceCommandText_);
      chunksButton_.setText(width < 400 ? "" : "Chunks");
   }
   
   
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

   public void hideWarningBar()
   {
      if (warningBar_ != null)
      {
         panel_.remove(warningBar_);
      }
   }

   public void showFindReplace(boolean defaultForward)
   {
      findReplace_.showFindReplace(defaultForward);
   }
   
   @Override
   public void findNext()
   {
      findReplace_.findNext();
   }
   
   @Override
   public void findSelectAll()
   {
      findReplace_.selectAll();
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
   public void replaceAndFind()
   {
      findReplace_.replaceAndFind();
   }

   public void onActivate()
   {
      editor_.onActivate();
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {

         @Override
         public void execute()
         {
            manageToolbarSizes(); 
         }
      });
   }

   public void setFontSize(double size)
   {
      editor_.setFontSize(size);
   }

   public StatusBar getStatusBar()
   {
      return statusBar_;
   }

   @Override
   public void debug_dumpContents()
   {
      String dump = editor_.debug_getDocumentDump();
      new EditDialog(dump, false, false, new ProgressOperationWithInput<String>()
      {
         @Override
         public void execute(String input, ProgressIndicator indicator)
         {
            indicator.onCompleted();
         }
      }).showModal();
   }

   @Override
   public void debug_importDump()
   {
      new EditDialog("", false, false, new ProgressOperationWithInput<String>()
      {
         @Override
         public void execute(String input, ProgressIndicator indicator)
         {
            indicator.onCompleted();
            if (input == null)
               return;

            input = input.replaceAll("[ \\r\\n]+", " ");
            String[] chars = input.split(" ");

            StringBuilder sb = new StringBuilder();
            for (String s : chars)
            {
               if (s.equals("."))
                  sb.append('\n');
               else
                  sb.append((char)Integer.parseInt(s));
            }

            editor_.debug_setSessionValueDirectly(sb.toString());
         }
      }).showModal();
   }

   // Called by the owning TextEditingTarget to notify the widget that the 
   // Shiny application associated with this widget has changed state.
   @Override
   public void onShinyApplicationStateChanged(String state)
   {
      shinyAppState_ = state;
      setSourceButtonFromShinyState();
   }
   
   @Override
   public void setFormatOptions(TextFileType fileType,
                                List<String> options, 
                                List<String> values, 
                                List<String> extensions, 
                                String selectedOption)
   {
      rmdFormatButton_.clearMenu();
      int parenPos = selectedOption.indexOf('(');
      boolean hasSubFormat = false;
      if (parenPos != -1)
      {
         selectedOption = selectedOption.substring(0, parenPos).trim();
         hasSubFormat = true;
      }
      setFormatText(selectedOption);
      String prefix = fileType.isPlainMarkdown() ? "Preview " : "Knit ";
      for (int i = 0; i < Math.min(options.size(), values.size()); i++)
      {
         ImageResource img = fileTypeRegistry_.getIconForFilename("output." + 
                     extensions.get(i));
         final String valueName = values.get(i);
         ScheduledCommand cmd = new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               handlerManager_.fireEvent(
                     new RmdOutputFormatChangedEvent(valueName));
            }
         };
         MenuItem item = ImageMenuItem.create(img, 
                                              prefix + options.get(i), 
                                              cmd, 2);
         rmdFormatButton_.addMenuItem(item, values.get(i));
      }
      if (!hasSubFormat && selectedOption.equals("HTML"))
      {
         rmdFormatButton_.getMenu().addSeparator();
         addRmdViewerMenuItems(rmdFormatButton_.getMenu());
      }
      setFormatOptionsVisible(true);
      if (publishButton_ != null)
         publishButton_.setIsStatic(true);
      isShiny_ = false;
   }

   @Override
   public void setFormatOptionsVisible(boolean visible)
   {
      if (!visible)
      {
         setFormatText("");
      }
      rmdFormatButton_.setVisible(visible);
      editRmdFormatButton_.setVisible(visible);
      rmdFormatButton_.setEnabled(visible);
      editRmdFormatButton_.setEnabled(visible);
   }
   
   @Override
   public void setIsShinyFormat(boolean isPresentation)
   {
      if (isPresentation)
      {
         rmdFormatButton_.setVisible(false);
      }
      else
      {
         rmdFormatButton_.setVisible(true);
         rmdFormatButton_.clearMenu();
         addRmdViewerMenuItems(rmdFormatButton_.getMenu());
      }
      String docType = isPresentation ? "Presentation" : "Document";
      
      knitCommandText_ = "Run " + docType;
      knitDocumentButton_.setTitle("View the current " + docType.toLowerCase() + 
            " with Shiny (" +
            DomUtils.htmlToText(
                  commands_.knitDocument().getShortcutPrettyHtml()) + ")");
      knitDocumentButton_.setText(knitCommandText_);
      knitDocumentButton_.setLeftImage(StandardIcons.INSTANCE.run());
      isShiny_ = true;
      if (publishButton_ != null)
         publishButton_.setIsStatic(false);
   }
   
   @Override
   public void setPublishPath(int contentType, String publishPath)
   {
      if (publishButton_ != null)
      {
         if (contentType == RSConnect.CONTENT_TYPE_APP)
         {
            publishButton_.setContentPath(publishPath, "");
            publishButton_.setContentType(contentType);
         }
         else
         {
            publishButton_.setRmd(publishPath, !isShiny_);
         }
      }
   }

   private void setFormatText(String text)
   {
      if (text.length() > 0)
         text = " " + text;
      knitCommandText_ = "Knit" + text;
      knitDocumentButton_.setText(knitCommandText_);
      knitDocumentButton_.setLeftImage(
            commands_.knitDocument().getImageResource());
      knitDocumentButton_.setTitle(commands_.knitDocument().getTooltip());
      previewCommandText_ = "Preview" + text;
      previewHTMLButton_.setText(previewCommandText_);
   }
   
   private void setSourceButtonFromScriptState(boolean isScript, 
                                               boolean canPreviewFromR,
                                               String previewButtonText)
   {
      sourceCommandText_ = commands_.sourceActiveDocument().getButtonLabel();
      String sourceCommandDesc = commands_.sourceActiveDocument().getDesc();
      if (isScript)
      {
         sourceCommandText_ = "Run Script";
         sourceCommandDesc = "Save changes and run the current script";
         sourceButton_.setLeftImage(
                           commands_.debugContinue().getImageResource());
      }
      else if (canPreviewFromR)
      {
         sourceCommandText_ = previewButtonText;
         sourceCommandDesc = "Save changes and preview";
         sourceButton_.setLeftImage(
                           commands_.debugContinue().getImageResource());
      }
      
      sourceButton_.setTitle(sourceCommandDesc);
      sourceButton_.setText(sourceCommandText_);
   }

   public void setSourceButtonFromShinyState()
   {
      sourceCommandText_ = commands_.sourceActiveDocument().getButtonLabel();
      String sourceCommandDesc = commands_.sourceActiveDocument().getDesc();
      if (isShinyFile())
      {
         if (shinyAppState_.equals(ShinyApplicationParams.STATE_STARTED)) 
         {
            sourceCommandText_ = "Reload App";
            sourceCommandDesc = "Save changes and reload the Shiny application";
            sourceButton_.setLeftImage(
                  commands_.reloadShinyApp().getImageResource());
         }
         else if (shinyAppState_.equals(ShinyApplicationParams.STATE_STOPPED))
         {
            sourceCommandText_ = "Run App";
            sourceCommandDesc = "Run the Shiny application";
            sourceButton_.setLeftImage(
                  commands_.debugContinue().getImageResource());
         }
      }
      sourceButton_.setTitle(sourceCommandDesc);
      sourceButton_.setText(sourceCommandText_);
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }
   
   @Override
   public HandlerRegistration addEnsureHeightHandler(
         EnsureHeightHandler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   public void onVisibilityChanged(boolean visible)
   {
      editor_.onVisibilityChanged(visible);
   }
   
   @Override
   public HandlerRegistration addRmdFormatChangedHandler(
         RmdOutputFormatChangedEvent.Handler handler)
   {
      return handlerManager_.addHandler(
            RmdOutputFormatChangedEvent.TYPE, handler);
   }
   
   private void addRmdViewerMenuItems(ToolbarPopupMenu menu)
   {
      if (rmdViewerPaneMenuItem_ == null)
         rmdViewerPaneMenuItem_ = new UIPrefMenuItem<Integer>(
               uiPrefs_.rmdViewerType(),
               RmdOutput.RMD_VIEWER_TYPE_PANE, 
               "View in Pane", uiPrefs_);
      if (rmdViewerWindowMenuItem_ == null)
         rmdViewerWindowMenuItem_ = new UIPrefMenuItem<Integer>(
               uiPrefs_.rmdViewerType(),
               RmdOutput.RMD_VIEWER_TYPE_WINDOW, 
               "View in Window", uiPrefs_);
      menu.addItem(rmdViewerPaneMenuItem_);
      menu.addItem(rmdViewerWindowMenuItem_);
   }
   
   private final Commands commands_;
   private final UIPrefs uiPrefs_;
   private final Session session_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final DocDisplay editor_;
   private final ShinyViewerTypePopupMenu shinyViewerMenu_;
   private String extendedType_;
   private CheckBox sourceOnSave_;
   private PanelWithToolbars panel_;
   private Toolbar toolbar_;
   private InfoBar warningBar_;
   private final TextEditingTargetFindReplace findReplace_;
   private ToolbarButton codeTransform_;
   private ToolbarButton compilePdfButton_;
   private ToolbarButton previewHTMLButton_;
   private ToolbarButton knitDocumentButton_;
   private ToolbarButton runButton_;
   private ToolbarButton runLastButton_;
   private ToolbarButton sourceButton_;
   private ToolbarButton sourceMenuButton_;
   private ToolbarButton chunksButton_;
   private ToolbarButton helpMenuButton_;
   private ToolbarButton rcppHelpButton_;
   private ToolbarButton shinyLaunchButton_;
   private ToolbarButton editRmdFormatButton_;
   private ToolbarPopupMenuButton rmdFormatButton_;
   private RSConnectPublishButton publishButton_;
   private MenuItem rmdViewerPaneMenuItem_;
   private MenuItem rmdViewerWindowMenuItem_;
   private HandlerManager handlerManager_;
   
   private Widget texSeparatorWidget_;
   private ToolbarButton texToolbarButton_;
   private Widget notebookSeparatorWidget_;
   private ToolbarButton notebookToolbarButton_;
   private Label srcOnSaveLabel_;

   private String shinyAppState_ = ShinyApplicationParams.STATE_STOPPED;
   private String sourceCommandText_ = "Source";
   private String knitCommandText_ = "Knit";
   private String previewCommandText_ = "Preview";
   private boolean isShiny_ = false;
}
