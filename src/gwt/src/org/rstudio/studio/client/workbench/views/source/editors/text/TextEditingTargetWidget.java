/*
 * TextEditingTargetWidget.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.MathUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.MouseDragHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ImageMenuItem;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.plumber.ui.PlumberViewerTypePopupMenu;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdOutputFormatChangedEvent;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.ui.ShinyTestPopupMenu;
import org.rstudio.studio.client.shiny.ui.ShinyViewerTypePopupMenu;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.edit.ui.EditDialog;
import org.rstudio.studio.client.workbench.views.source.DocumentOutlineWidget;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.Display;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarWidget;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

public class TextEditingTargetWidget
      extends ResizeComposite
      implements Display, RequiresVisibilityChanged
{
   public TextEditingTargetWidget(final TextEditingTarget target,
                                  DocUpdateSentinel docUpdateSentinel,
                                  Commands commands,
                                  UIPrefs uiPrefs,
                                  FileTypeRegistry fileTypeRegistry,
                                  final DocDisplay editor,
                                  TextFileType fileType,
                                  String extendedType,
                                  EventBus events,
                                  Session session,
                                  SourceServerOperations server)
   {
      target_ = target;
      docUpdateSentinel_ = docUpdateSentinel;
      commands_ = commands;
      uiPrefs_ = uiPrefs;
      session_ = session;
      fileTypeRegistry_ = fileTypeRegistry;
      editor_ = editor;
      extendedType_ = extendedType;
      events_ = events;
      sourceOnSave_ = new CheckBox();
      srcOnSaveLabel_ =
                  new CheckboxLabel(sourceOnSave_, "Source on Save").getLabel();
      statusBar_ = new StatusBarWidget();
      shinyViewerMenu_ = RStudioGinjector.INSTANCE.getShinyViewerTypePopupMenu();
      shinyTestMenu_ = RStudioGinjector.INSTANCE.getShinyTestPopupMenu();
      plumberViewerMenu_ = RStudioGinjector.INSTANCE.getPlumberViewerTypePopupMenu();
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
      
      editorPanel_ = new DockLayoutPanel(Unit.PX);
      docOutlineWidget_ = new DocumentOutlineWidget(target);
      
      editorPanel_.addEast(docOutlineWidget_, 0);
      editorPanel_.add(editor.asWidget());
      
      MouseDragHandler.addHandler(
            docOutlineWidget_.getLeftSeparator(),
            new MouseDragHandler()
            {
               double initialWidth_ = 0;
               
               @Override
               public boolean beginDrag(MouseDownEvent event)
               {
                  initialWidth_ = editorPanel_.getWidgetSize(docOutlineWidget_);
                  return true;
               }
               
               @Override
               public void onDrag(MouseDragEvent event)
               {
                  double initialWidth = initialWidth_;
                  double xDiff = event.getTotalDelta().getMouseX();
                  double newSize = initialWidth - xDiff;
                  
                  // We allow an extra pixel here just to 'hide' the border
                  // if the outline is maximized, since the 'separator'
                  // lives as part of the outline instead of 'between' the
                  // two widgets
                  double maxSize = editorPanel_.getOffsetWidth() + 1;
                  
                  double clamped = MathUtil.clamp(newSize, 0, maxSize);
                  
                  // If the size is below '5px', interpret this as a request
                  // to close the outline widget.
                  if (clamped < 5)
                     clamped = 0;
                  
                  editorPanel_.setWidgetSize(docOutlineWidget_, clamped);
                  toggleDocOutlineButton_.setLatched(clamped != 0);
                  editor_.onResize();
               }
               
               @Override
               public void endDrag()
               {
                  double size = editorPanel_.getWidgetSize(docOutlineWidget_);
                  
                  // We only update the preferred size if the user hasn't closed
                  // the widget.
                  if (size > 0)
                     target_.setPreferredOutlineWidgetSize(size);
                  
                  target_.setPreferredOutlineWidgetVisibility(size > 0);
               }
            });
      
      panel_ = new PanelWithToolbars(
            toolbar_ = createToolbar(fileType),
            editorPanel_,
            statusBar_);
      
      adaptToFileType(fileType);
      
      editor.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            toggleDocOutlineButton_.setLatched(
                  docOutlineWidget_.getOffsetWidth() > 0);
         }
      });

      initWidget(panel_);
   }
   
   public void initWidgetSize()
   {
      if (target_.getPreferredOutlineWidgetVisibility())
      {
         double editorSize = editorPanel_.getOffsetWidth();
         double widgetSize = target_.getPreferredOutlineWidgetSize();
         double size = Math.min(editorSize, widgetSize);
         editorPanel_.setWidgetSize(docOutlineWidget_, size);
         toggleDocOutlineButton_.setLatched(true);
      }
   }
   
   public void toggleDocumentOutline()
   {
      toggleDocOutlineButton_.click();
   }
   
   private StatusBarWidget statusBar_;

   private void createTestToolbarButtons(Toolbar toolbar)
   {
      compareTestButton_ = new ToolbarButton(
            "Compare Results", 
            commands_.shinyCompareTest().getImageResource(), 
            new ClickHandler() 
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  commands_.shinyCompareTest().execute();
               }
            });
      compareTestButton_.setTitle(commands_.shinyCompareTest().getDesc());

      toolbar.addRightWidget(compareTestButton_);
      compareTestButton_.setVisible(false);

      testThatButton_ = new ToolbarButton(
            "Run Tests", 
            commands_.testTestthatFile().getImageResource(), 
            new ClickHandler() 
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  commands_.testTestthatFile().execute();
               }
            });
      testThatButton_.setTitle(commands_.testTestthatFile().getDesc());

      toolbar.addRightWidget(testThatButton_);
      testThatButton_.setVisible(false);

      testShinyButton_ = new ToolbarButton(
            "Run Tests", 
            commands_.testShinytestFile().getImageResource(), 
            new ClickHandler() 
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  commands_.testShinytestFile().execute();
               }
            });
      testShinyButton_.setTitle(commands_.testShinytestFile().getDesc());

      toolbar.addRightWidget(testShinyButton_);
      testShinyButton_.setVisible(false);
   }

   private Toolbar createToolbar(TextFileType fileType)
   {
      Toolbar toolbar = new EditingTargetToolbar(commands_, true);

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
      notebookToolbarButton_.setTitle("Compile Report (" + cmdText + ")");
      
      texSeparatorWidget_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(texToolbarButton_ = createLatexFormatButton());
      
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(previewHTMLButton_ = commands_.previewHTML().createToolbarButton());
      knitDocumentButton_ = commands_.knitDocument().createToolbarButton(false);
      knitDocumentButton_.getElement().getStyle().setMarginRight(0, Unit.PX);
      toolbar.addLeftWidget(knitDocumentButton_);

      ToolbarPopupMenu shinyTestMenu = shinyTestMenu_;
      if (fileType.canKnitToHTML()) {
         shinyLaunchButton_ = new ToolbarButton(shinyTestMenu, true);
         toolbar.addLeftWidget(shinyLaunchButton_);
      }

      toolbar.addLeftWidget(compilePdfButton_ = commands_.compilePDF().createToolbarButton());
      rmdFormatButton_ = new ToolbarPopupMenuButton(false, true);
      rmdFormatButton_.getMenu().setAutoOpen(true);
      toolbar.addLeftWidget(rmdFormatButton_);
      
      runDocumentMenuButton_ = new ToolbarPopupMenuButton(false, true);
      addClearKnitrCacheMenu(runDocumentMenuButton_);
      runDocumentMenuButton_.addSeparator();
      runDocumentMenuButton_.addMenuItem(commands_.clearPrerenderedOutput().createMenuItem(false), "");     
      toolbar.addLeftWidget(runDocumentMenuButton_);
      runDocumentMenuButton_.addSeparator();
      runDocumentMenuButton_.addMenuItem(commands_.shinyRecordTest().createMenuItem(false), "");
      runDocumentMenuButton_.addMenuItem(commands_.shinyRunAllTests().createMenuItem(false), "");
      runDocumentMenuButton_.setVisible(false);
      
      ToolbarPopupMenu rmdOptionsMenu = new ToolbarPopupMenu();
      rmdOptionsMenu.addItem(commands_.editRmdFormatOptions().createMenuItem(false));
      
      rmdOptionsButton_ = new ToolbarButton(
            null,  
            new ImageResource2x(StandardIcons.INSTANCE.options2x()),
            rmdOptionsMenu, 
            false);
      
      toolbar.addLeftWidget(rmdOptionsButton_);

      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.synctexSearch().createToolbarButton());

      // create menu of chunk skeletons based on common engine types
      ToolbarPopupMenu insertChunksMenu = new ToolbarPopupMenu();
      insertChunksMenu.addItem(commands_.insertChunkR().createMenuItem(false));
      insertChunksMenu.addSeparator();

      insertChunksMenu.addItem(commands_.insertChunkBash().createMenuItem(false));
      insertChunksMenu.addItem(commands_.insertChunkD3().createMenuItem(false));
      insertChunksMenu.addItem(commands_.insertChunkPython().createMenuItem(false));
      insertChunksMenu.addItem(commands_.insertChunkRCPP().createMenuItem(false));
      insertChunksMenu.addItem(commands_.insertChunkSQL().createMenuItem(false));
      insertChunksMenu.addItem(commands_.insertChunkStan().createMenuItem(false));

      insertChunkMenu_ = new ToolbarButton(
                       "Insert",
                       commands_.insertChunk().getImageResource(),
                       insertChunksMenu,
                       true);

      toolbar.addRightWidget(insertChunkMenu_);

      // create button that just runs default chunk insertion
      insertChunkButton_ = commands_.insertChunk().createToolbarButton(false);
      toolbar.addRightWidget(insertChunkButton_);

      toolbar.addRightWidget(runButton_ = commands_.executeCode().createToolbarButton(false));
      toolbar.addRightSeparator();
      toolbar.addRightWidget(runLastButton_ = commands_.executeLastCode().createToolbarButton(false));
      toolbar.addRightWidget(goToPrevButton_ = commands_.goToPrevSection().createToolbarButton(false));
      toolbar.addRightWidget(goToNextButton_ = commands_.goToNextSection().createToolbarButton(false));
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

      previewJsButton_ = commands_.previewJS().createToolbarButton(false);
      toolbar.addRightWidget(previewJsButton_);
      
      previewSqlButton_ = commands_.previewSql().createToolbarButton(false);
      toolbar.addRightWidget(previewSqlButton_);

      createTestToolbarButtons(toolbar);
      
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
      sourceMenu.addSeparator();
      sourceMenu.addItem(commands_.sourceAsJob().createMenuItem(false));
         
      sourceMenuButton_ = new ToolbarButton(sourceMenu, true);
      toolbar.addRightWidget(sourceMenuButton_);  

      //toolbar.addRightSeparator();
     
      ToolbarPopupMenu chunksMenu = new ToolbarPopupMenu();
      chunksMenu.addItem(commands_.executeCode().createMenuItem(false));
      chunksMenu.addSeparator();
      chunksMenu.addItem(commands_.executeCurrentChunk().createMenuItem(false));
      chunksMenu.addItem(commands_.executeNextChunk().createMenuItem(false));
      chunksMenu.addSeparator();
      chunksMenu.addItem(commands_.executeSetupChunk().createMenuItem(false));
      chunksMenu.addItem(runSetupChunkOptionMenu_= new UIPrefMenuItem<Boolean>(
            uiPrefs_.autoRunSetupChunk(), true, "Run Setup Chunk Automatically", 
            uiPrefs_));
      chunksMenu.addSeparator();
      chunksMenu.addItem(commands_.executePreviousChunks().createMenuItem(false));
      chunksMenu.addItem(commands_.executeSubsequentChunks().createMenuItem(false));
      if (uiPrefs_.showRmdChunkOutputInline().getValue())
      {
         chunksMenu.addSeparator();
         chunksMenu.addItem(
               commands_.restartRRunAllChunks().createMenuItem(false));
         chunksMenu.addItem(
               commands_.restartRClearOutput().createMenuItem(false));
      }
      chunksMenu.addSeparator();
      chunksMenu.addItem(commands_.executeAllCode().createMenuItem(false));
      chunksButton_ = new ToolbarButton(
                       "Run",
                       commands_.executeCode().getImageResource(),
                       chunksMenu,
                       true);
      toolbar.addRightWidget(chunksButton_);
      
      ToolbarPopupMenu shinyLaunchMenu = shinyViewerMenu_;
      if (!fileType.canKnitToHTML()) {
         shinyLaunchButton_ = new ToolbarButton(shinyLaunchMenu, true);
         toolbar.addRightWidget(shinyLaunchButton_);
      }
      shinyLaunchButton_.setVisible(false);

      plumberLaunchButton_ = new ToolbarButton(plumberViewerMenu_, true);
      toolbar.addRightWidget(plumberLaunchButton_);
      plumberLaunchButton_.setVisible(false);

      if (SessionUtils.showPublishUi(session_, uiPrefs_))
      {
         toolbar.addRightSeparator();
         publishButton_ = new RSConnectPublishButton(
               RSConnectPublishButton.HOST_EDITOR,
               RSConnect.CONTENT_TYPE_APP, false, null);
         publishButton_.onPublishInvoked(() -> 
         {
            if (!StringUtil.isNullOrEmpty(target_.getPath()))
            {
              target_.save(); 
            }
         });
         toolbar.addRightWidget(publishButton_);
      }
      
      toggleDocOutlineButton_ = new LatchingToolbarButton(
         "",
            new ImageResource2x(StandardIcons.INSTANCE.outline2x()),
            new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  final double initialSize = editorPanel_.getWidgetSize(docOutlineWidget_);
                  
                  // Clicking the icon toggles the outline widget's visibility. The
                  // 'destination' below is the width we would like to set -- we
                  // animate to that position for a slightly nicer visual treatment.
                  final double destination = docOutlineWidget_.getOffsetWidth() > 5
                        ? 0
                        : Math.min(editorPanel_.getOffsetWidth(), target_.getPreferredOutlineWidgetSize());
                  
                  // Update tooltip ('Show'/'Hide' depending on current visibility)
                  String title = toggleDocOutlineButton_.getTitle();
                  if (destination != 0)
                     title = title.replace("Show ", "Hide ");
                  else
                     title = title.replace("Hide ", "Show ");
                  toggleDocOutlineButton_.setTitle(title);
                  
                  toggleDocOutlineButton_.setLatched(destination != 0);
                  
                  new Animation()
                  {
                     @Override
                     protected void onUpdate(double progress)
                     {
                        double size =
                              destination * progress +
                              initialSize * (1 - progress);
                        editorPanel_.setWidgetSize(docOutlineWidget_, size);
                        editor_.onResize();
                     }
                     
                     @Override
                     protected void onComplete()
                     {
                        if (destination == 0) editorPanel_.setWidgetSize(docOutlineWidget_, 0);
                        target_.setPreferredOutlineWidgetVisibility(destination != 0);
                     }
                  }.run(500);
               }
            });
      
      toggleDocOutlineButton_.addStyleName("rstudio-themes-inverts");

      // Time-out setting the latch just to ensure the document outline
      // has actually been appropriately rendered.
      new Timer()
      {
         @Override
         public void run()
         {
            String title = commands_.toggleDocumentOutline().getTooltip();
            title = editorPanel_.getWidgetSize(docOutlineWidget_) > 0
                  ? title.replace("Show ", "Hide ")
                  : title.replace("Hide ", "Show ");
            toggleDocOutlineButton_.setTitle(title);
            toggleDocOutlineButton_.setLatched(docOutlineWidget_.getOffsetWidth() > 0);
         }
      }.schedule(100);
      
      toolbar.addRightSeparator();
      toolbar.addRightWidget(toggleDocOutlineButton_);
      
      showWhitespaceCharactersCheckbox_ = new CheckBox("Show whitespace");
      showWhitespaceCharactersCheckbox_.setVisible(false);
      showWhitespaceCharactersCheckbox_.setValue(uiPrefs_.showInvisibles().getValue());
      showWhitespaceCharactersCheckbox_.addValueChangeHandler((ValueChangeEvent<Boolean> event) -> {
         editor_.setShowInvisibles(event.getValue());
      });
      
      if (docUpdateSentinel_ != null && docUpdateSentinel_.getPath() != null)
      {
         FileSystemItem item = FileSystemItem.createFile(docUpdateSentinel_.getPath());
         String ext = item.getExtension();
         if (".csv".equals(ext) || ".tsv".equals(ext))
            showWhitespaceCharactersCheckbox_.setVisible(true);
      }
      toolbar.addRightSeparator();
      toolbar.addRightWidget(showWhitespaceCharactersCheckbox_);
      
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
         ImageResource icon = new ImageResource2x(ThemeResources.INSTANCE.codeTransform2x());

         ToolbarPopupMenu menu = new ToolbarPopupMenu();
         menu.addItem(commands_.codeCompletion().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.goToHelp().createMenuItem(false));
         menu.addItem(commands_.goToDefinition().createMenuItem(false));
         menu.addItem(commands_.findUsages().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.extractFunction().createMenuItem(false));
         menu.addItem(commands_.extractLocalVariable().createMenuItem(false));
         menu.addItem(commands_.renameInScope().createMenuItem(false));
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
         menu.addSeparator();
         menu.addItem(commands_.profileCode().createMenuItem(false));
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
      if (canSourceOnSave && fileType.isJS()) 
         canSourceOnSave = (extendedType_.equals(SourceDocument.XT_JS_PREVIEWABLE));
      if (canSourceOnSave && fileType.isSql()) 
         canSourceOnSave = (extendedType_.equals(SourceDocument.XT_SQL_PREVIEWABLE));
      boolean canExecuteCode = fileType.canExecuteCode();
      boolean canExecuteChunks = fileType.canExecuteChunks();
      boolean isPlainMarkdown = fileType.isPlainMarkdown();
      boolean isCpp = fileType.isCpp();
      boolean isScript = fileType.isScript();
      boolean isRMarkdown2 = extendedType_ == "rmarkdown";
      boolean canPreviewFromR = fileType.canPreviewFromR();
      boolean terminalAllowed = session_.getSessionInfo().getAllowShell();
      
      if (isScript && !terminalAllowed)
      {
         commands_.executeCode().setEnabled(false);
         commands_.executeCodeWithoutFocus().setEnabled(false);
      }
      
      // don't show the run buttons for cpp files, or R files in Shiny/Tests/Plumber
      runButton_.setVisible(canExecuteCode && !canExecuteChunks && !isCpp && 
            !(isShinyFile() || isTestFile() || isPlumberFile()) && !(isScript && !terminalAllowed));
      runLastButton_.setVisible(runButton_.isVisible() && !canExecuteChunks && !isScript);
      
      // show insertion options for various knitr engines in rmarkdown v2
      insertChunkMenu_.setVisible(isRMarkdown2);
      
      // otherwise just show the regular insert chunk button
      insertChunkButton_.setVisible(canExecuteChunks && !isRMarkdown2);

      goToPrevButton_.setVisible(fileType.canGoNextPrevSection());
      goToNextButton_.setVisible(fileType.canGoNextPrevSection());
      
      sourceOnSave_.setVisible(canSourceOnSave);
      srcOnSaveLabel_.setVisible(canSourceOnSave);
      if (fileType.isRd() || fileType.isJS() || canPreviewFromR || fileType.isSql())
         srcOnSaveLabel_.setText(fileType.getPreviewButtonText() + " on Save");
      else if (hasCustomSource())
         srcOnSaveLabel_.setText("Custom Action on Save");
      else
         srcOnSaveLabel_.setText("Source on Save");
      codeTransform_.setVisible(
            (canExecuteCode && !isScript && !fileType.canAuthorContent()) ||
            fileType.isC() || fileType.isStan());   
     
      previewJsButton_.setVisible(fileType.isJS() && extendedType_.equals(SourceDocument.XT_JS_PREVIEWABLE));
      previewSqlButton_.setVisible(fileType.isSql() && extendedType_.equals(SourceDocument.XT_SQL_PREVIEWABLE));

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
      
      setRmdFormatButtonVisible(isRMarkdown2);
      rmdOptionsButton_.setVisible(isRMarkdown2);
      rmdOptionsButton_.setEnabled(isRMarkdown2);
     
      if (isShinyFile() || isTestFile() || isPlumberFile())
      {
         sourceOnSave_.setVisible(false);
         srcOnSaveLabel_.setVisible(false);
         runButton_.setVisible(false);
         sourceMenuButton_.setVisible(false);
         chunksButton_.setVisible(false);
      }

      testShinyButton_.setVisible(false);
      testThatButton_.setVisible(false);
      compareTestButton_.setVisible(false);
      if (isShinyFile())
      {
         shinyLaunchButton_.setVisible(true);
         plumberLaunchButton_.setVisible(false);
         setSourceButtonFromShinyState();
      }
      else if (isTestThatFile())
      {
         shinyLaunchButton_.setVisible(false);
         plumberLaunchButton_.setVisible(false);
         sourceButton_.setVisible(false);
         testThatButton_.setVisible(true);
      }
      else if (isShinyTestFile())
      {
         shinyLaunchButton_.setVisible(false);
         plumberLaunchButton_.setVisible(false);
         sourceButton_.setVisible(false);
         testShinyButton_.setVisible(true);
         compareTestButton_.setVisible(true);
      }
      else if (isPlumberFile())
      {
         plumberLaunchButton_.setVisible(true);
         setSourceButtonFromPlumberState();
      }
      else
      {
         shinyLaunchButton_.setVisible(false);
         plumberLaunchButton_.setVisible(false);
         setSourceButtonFromScriptState(fileType, 
                                        canPreviewFromR,
                                        fileType.getPreviewButtonText());
      }
      
      // set the content type based on the extended type
      setPublishPath(extendedType_, publishPath_);
      
      // make toggle outline visible if we have a scope tree
      toggleDocOutlineButton_.setVisible(fileType.canShowScopeTree());
      if (!fileType.canShowScopeTree())
      {
         editorPanel_.setWidgetSize(docOutlineWidget_, 0);
         toggleDocOutlineButton_.setLatched(false);
      }
      
      toolbar_.invalidateSeparators();
   }
   
   private boolean isShinyFile()
   {
      return extendedType_ != null &&
             extendedType_.startsWith(SourceDocument.XT_SHINY_PREFIX);
   }

   private boolean isPlumberFile()
   {
      return SourceDocument.isPlumberFile(extendedType_);
   }

   private boolean isTestFile()
   {
      return extendedType_ != null &&
             extendedType_.startsWith(SourceDocument.XT_TEST_PREFIX);
   }

   private boolean isTestThatFile()
   {
      return extendedType_ != null &&
             extendedType_.startsWith(SourceDocument.XT_TEST_TESTTHAT);
   }

   private boolean isShinyTestFile()
   {
      return extendedType_ != null &&
             extendedType_.startsWith(SourceDocument.XT_TEST_SHINYTEST);
   }

   private boolean hasCustomSource()
   {
      return SourceDocument.hasCustomSource(extendedType_);
   }
   
   @Override
   public void setNotebookUIVisible(boolean visible)
   {
      runSetupChunkOptionMenu_.setVisible(visible);
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
      ResizeEvent.fire(this, getOffsetWidth(), getOffsetHeight());
     
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
      
      if (editor_.getFileType().isRd() || editor_.getFileType().isJS() || 
          editor_.getFileType().isSql())
      {
         String preview = editor_.getFileType().getPreviewButtonText();
         srcOnSaveLabel_.setText(width < 450 ? preview : preview + " on Save");
      }
      else if (hasCustomSource())
      {
         srcOnSaveLabel_.setText(width < 450 ? "Custom" : "Custom Action on Save");
      }
      else
      {
         srcOnSaveLabel_.setText(width < 450 ? "Source" : "Source on Save");
      }

      sourceButton_.setText(width < 400 ? "" : sourceCommandText_);
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
      if (docUpdateSentinel_.hasProperty("disableDependencyDiscovery"))
      {
         String disableDependencyDiscovery = docUpdateSentinel_.getProperty("disableDependencyDiscovery");
         if (StringUtil.equals(disableDependencyDiscovery, "1"))
         {
            return;
         }
      }
      
      Command onInstall = () -> {
         StringBuilder builder = new StringBuilder();
         if (packages.size() == 1)
         {
            builder.append("install.packages(\"")
                   .append(packages.get(0))
                   .append("\")");
         }
         else
         {
            builder.append("install.packages(c(\"")
                   .append(StringUtil.join(packages, "\", \""))
                   .append("\"))");
         }
         events_.fireEvent(new SendToConsoleEvent(builder.toString(), true));
         hideWarningBar();
      };
      
      Command onDismiss = () -> {
         docUpdateSentinel_.setProperty("disableDependencyDiscovery", "1");
         hideWarningBar();
      };
      
      showWarningImpl(() -> {
         warningBar_.showRequiredPackagesMissingWarning(packages, onInstall, onDismiss);
      });
   }
   
   @Override
   public void showWarningBar(final String warning)
   {
      showWarningImpl(() -> warningBar_.setText(warning));
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
               if (s == ".")
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
   
   // Called by the owning TextEditingTarget to notify the widget that the 
   // Plumber API associated with this widget has changed state.
   @Override
   public void onPlumberAPIStateChanged(String state)
   {
      plumberAPIState_ = state;
      setSourceButtonFromPlumberState();
   }
    
   @Override
   public void setFormatOptions(TextFileType fileType,
                                boolean showRmdFormatMenu,
                                boolean canEditFormatOptions,
                                List<String> options, 
                                List<String> values, 
                                List<String> extensions, 
                                String selectedOption)
   { 
      if (!canEditFormatOptions)
      {
         setFormatText("");
      }
      
      setRmdFormatButtonVisible(showRmdFormatMenu);
      rmdFormatButton_.setEnabled(showRmdFormatMenu);
      rmdFormatButton_.clearMenu();
      
      int parenPos = selectedOption.indexOf('(');
      if (parenPos != -1)
          selectedOption = selectedOption.substring(0, parenPos).trim();
 
      // don't show format text (but leave the code in for now in case
      // we change our mind)
      // setFormatText(selectedOption);
      setFormatText("");
      
      String prefix = fileType.isPlainMarkdown() ? "Preview " : "Knit to ";
      
      for (int i = 0; i < Math.min(options.size(), values.size()); i++)
      {
         String ext = extensions.get(i);
         ImageResource img = ext != null ? 
               fileTypeRegistry_.getIconForFilename("output." + ext) :
               fileTypeRegistry_.getIconForFilename("Makefile");
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
         String text = ext == ".nb.html" ? "Preview Notebook" :
            prefix + options.get(i);
            
         MenuItem item = ImageMenuItem.create(img, text, cmd, 2);
         rmdFormatButton_.addMenuItem(item, values.get(i));
      }
      
      if (session_.getSessionInfo().getKnitParamsAvailable())
      {
         final AppCommand knitWithParams = commands_.knitWithParameters();
         rmdFormatButton_.addSeparator();
         ScheduledCommand cmd = new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               knitWithParams.execute();
            }
         };
         MenuItem item = new MenuItem(knitWithParams.getMenuHTML(false),
                                      true,
                                      cmd); 
         rmdFormatButton_.addMenuItem(item, knitWithParams.getMenuLabel(false));
      }
      
      if (session_.getSessionInfo().getKnitWorkingDirAvailable())
      {
         MenuBar knitDirMenu = new MenuBar(true);
         DocPropMenuItem knitInDocDir = new DocShadowPropMenuItem(
               "Document Directory", 
               docUpdateSentinel_, 
               uiPrefs_.knitWorkingDir(), 
               RenderRmdEvent.WORKING_DIR_PROP,
               UIPrefsAccessor.KNIT_DIR_DEFAULT);
         knitDirMenu.addItem(knitInDocDir);
         DocPropMenuItem knitInProjectDir = new DocShadowPropMenuItem(
               "Project Directory", 
               docUpdateSentinel_, 
               uiPrefs_.knitWorkingDir(), 
               RenderRmdEvent.WORKING_DIR_PROP,
               UIPrefsAccessor.KNIT_DIR_PROJECT);
         knitDirMenu.addItem(knitInProjectDir);
         DocPropMenuItem knitInCurrentDir = new DocShadowPropMenuItem(
               "Current Working Directory", 
               docUpdateSentinel_, 
               uiPrefs_.knitWorkingDir(), 
               RenderRmdEvent.WORKING_DIR_PROP,
               UIPrefsAccessor.KNIT_DIR_CURRENT);
         knitDirMenu.addItem(knitInCurrentDir);

         rmdFormatButton_.addSeparator();
         rmdFormatButton_.addMenuItem(knitDirMenu, "Knit Directory");
      }
      
      addClearKnitrCacheMenu(rmdFormatButton_);
          
      showRmdViewerMenuItems(true, canEditFormatOptions, fileType.isRmd(), 
            RmdOutput.TYPE_STATIC);
     
      if (publishButton_ != null)
         publishButton_.setIsStatic(true);
   }
   
   private void addClearKnitrCacheMenu(ToolbarPopupMenuButton menuButton)
   {
      final AppCommand clearKnitrCache = commands_.clearKnitrCache();
      menuButton.addSeparator();
      ScheduledCommand cmd = new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            clearKnitrCache.execute();
         }
      };
      MenuItem item = new MenuItem(clearKnitrCache.getMenuHTML(false),
                                   true,
                                   cmd); 
      menuButton.addMenuItem(item, clearKnitrCache.getMenuLabel(false));
   }
   
   @Override
   public void setIsShinyFormat(boolean showOutputOptions, 
                                boolean isPresentation,
                                boolean isShinyPrerendered)
   {
      setRmdFormatButtonVisible(false);
      
      showRmdViewerMenuItems(!isPresentation, showOutputOptions, true, 
            RmdOutput.TYPE_SHINY);
   
      String docType = isPresentation ? "Presentation" : "Document";

      if (!isPresentation && !isShinyPrerendered) {
         shinyLaunchButton_.setVisible(true);
      }
      
      knitCommandText_ = "Run " + docType;
      knitDocumentButton_.setTitle("View the current " + docType.toLowerCase() + 
            " with Shiny (" +
            DomUtils.htmlToText(
                  commands_.knitDocument().getShortcutPrettyHtml()) + ")");
      knitDocumentButton_.setText(knitCommandText_);
      knitDocumentButton_.setLeftImage(new ImageResource2x(StandardIcons.INSTANCE.run2x()));
      
      runDocumentMenuButton_.setVisible(isShinyPrerendered);
      setKnitDocumentMenuVisible(isShinyPrerendered);
      
      if (publishButton_ != null)
         publishButton_.setIsStatic(false);
   }
   
   @Override
   public void setIsNotShinyFormat()
   {
      runDocumentMenuButton_.setVisible(false);
   }
   
   @Override
   public void setIsNotebookFormat()
   {
      knitCommandText_ = "Preview";
      knitDocumentButton_.setTitle("Preview the notebook " +
            DomUtils.htmlToText(
                  commands_.knitDocument().getShortcutPrettyHtml()) + ")");
      knitDocumentButton_.setText(knitCommandText_);
      knitDocumentButton_.setLeftImage(
            commands_.newRNotebook().getImageResource());
      setRmdFormatButtonVisible(true);
      showRmdViewerMenuItems(true, true, true, RmdOutput.TYPE_NOTEBOOK);
   }
   
   private void setRmdFormatButtonVisible(boolean visible)
   {
      rmdFormatButton_.setVisible(visible);
      setKnitDocumentMenuVisible(visible);
   }
   
   private void setKnitDocumentMenuVisible(boolean visible)
   {
      knitDocumentButton_.getElement().getStyle().setMarginRight(
            visible ? 0 : 8, Unit.PX);
   }
   
   @Override
   public void setPublishPath(String type, String publishPath)
   {
      publishPath_ = publishPath;
      if (publishButton_ != null)
      {
         if (type == SourceDocument.XT_SHINY_DIR)
         {
            publishButton_.setContentPath(publishPath, "");
            publishButton_.setContentType(RSConnect.CONTENT_TYPE_APP);
         }
         else if (type == SourceDocument.XT_SHINY_SINGLE_FILE)
         {
            publishButton_.setContentPath(publishPath, "");
            publishButton_.setContentType(RSConnect.CONTENT_TYPE_APP_SINGLE);
         }
         else if (type == SourceDocument.XT_RMARKDOWN)
         {
            // don't publish markdown docs as static
            publishButton_.setRmd(publishPath, 
                  false // not static
                  );
         }
         else if (type == SourceDocument.XT_PLUMBER_API)
         {
            publishButton_.setContentPath(publishPath, "");
            publishButton_.setContentType(RSConnect.CONTENT_TYPE_PLUMBER_API);
         }
         else 
         {
            publishButton_.setContentType(RSConnect.CONTENT_TYPE_NONE);
         }
      }
   }

   @Override
   public void invokePublish()
   {
      if (publishButton_ == null)
      {
         // shouldn't happen in practice (we hide the publish button and 
         // disable the command when a non-publishable item is showing in the
         // widget) but in case it does let the user know why nothing's 
         // happening.
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               "Content not publishable", 
               "This item cannot be published.");
      }
      else
      {
         publishButton_.invokePublish();
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
   
   private void setSourceButtonFromScriptState(TextFileType fileType,
                                               boolean canPreviewFromR,
                                               String previewButtonText)
   {
      sourceCommandText_ = commands_.sourceActiveDocument().getButtonLabel();
      String sourceCommandDesc = commands_.sourceActiveDocument().getDesc();
      if (fileType.isPython())
      {
         sourceCommandText_ = "Source Script";
         sourceCommandDesc = "Save changes and source the current script";
         sourceButton_.setLeftImage(
                           commands_.debugContinue().getImageResource());
      }
      else if (fileType.isScript())
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
      sourceButton_.setLeftImage(
            commands_.sourceActiveDocument().getImageResource());
   }

   public void setSourceButtonFromShinyState()
   {
      sourceCommandText_ = commands_.sourceActiveDocument().getButtonLabel();
      String sourceCommandDesc = commands_.sourceActiveDocument().getDesc();
      if (isShinyFile())
      {
         if (shinyAppState_ == ShinyApplicationParams.STATE_STARTED) 
         {
            sourceCommandText_ = "Reload App";
            sourceCommandDesc = "Save changes and reload the Shiny application";
            sourceButton_.setLeftImage(
                  commands_.reloadShinyApp().getImageResource());
         }
         else if (shinyAppState_ == ShinyApplicationParams.STATE_STOPPED)
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

   public void setSourceButtonFromPlumberState()
   {
      sourceCommandText_ = commands_.sourceActiveDocument().getButtonLabel();
      String sourceCommandDesc = commands_.sourceActiveDocument().getDesc();
      if (isPlumberFile())
      {
         if (plumberAPIState_ == PlumberAPIParams.STATE_STARTED) 
         {
            sourceCommandText_ = "Reload API";
            sourceCommandDesc = "Save changes and reload the Plumber API";
            sourceButton_.setLeftImage(commands_.reloadPlumberAPI().getImageResource());
         }
         else if (plumberAPIState_ == PlumberAPIParams.STATE_STOPPED)
         {
            sourceCommandText_ = "Run API";
            sourceCommandDesc = "Run the Plumber API";
            sourceButton_.setLeftImage(commands_.debugContinue().getImageResource());
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

   @Override
   public HandlerRegistration addResizeHandler(ResizeHandler handler)
   {
      return addHandler(handler, ResizeEvent.getType());
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
   
   private void showRmdViewerMenuItems(boolean show, boolean showOutputOptions, 
         boolean isRmd, int type)
   {
      if (rmdViewerPaneMenuItem_ == null)
         rmdViewerPaneMenuItem_ = new UIPrefMenuItem<Integer>(
               uiPrefs_.rmdViewerType(),
               RmdOutput.RMD_VIEWER_TYPE_PANE, 
               "Preview in Viewer Pane", uiPrefs_);
      if (rmdViewerWindowMenuItem_ == null)
         rmdViewerWindowMenuItem_ = new UIPrefMenuItem<Integer>(
               uiPrefs_.rmdViewerType(),
               RmdOutput.RMD_VIEWER_TYPE_WINDOW, 
               "Preview in Window", uiPrefs_);
      if (rmdViewerNoPreviewMenuItem_ == null)
         rmdViewerNoPreviewMenuItem_ = new UIPrefMenuItem<Integer>(
               uiPrefs_.rmdViewerType(),
               RmdOutput.RMD_VIEWER_TYPE_NONE,
               "(No Preview)", uiPrefs_);
      
      
      ToolbarPopupMenu menu = rmdOptionsButton_.getMenu();
      menu.clearItems();
      if (show)
      {
         menu.addItem(rmdViewerWindowMenuItem_);
         menu.addItem(rmdViewerPaneMenuItem_);
         menu.addItem(rmdViewerNoPreviewMenuItem_);
         menu.addSeparator();
      }
      
      menu.addSeparator();

      String pref = uiPrefs_.showLatexPreviewOnCursorIdle().getValue();
      menu.addItem(new DocPropMenuItem(
            "Preview Images and Equations", docUpdateSentinel_, 
            docUpdateSentinel_.getBoolProperty(
               TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED, 
               pref != UIPrefsAccessor.LATEX_PREVIEW_SHOW_NEVER),
            TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED, 
            DocUpdateSentinel.PROPERTY_TRUE));
      menu.addItem(new DocPropMenuItem(
            "Show Previews Inline", docUpdateSentinel_, 
            docUpdateSentinel_.getBoolProperty(
               TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE, 
                 pref == UIPrefsAccessor.LATEX_PREVIEW_SHOW_ALWAYS),
            TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE, 
            DocUpdateSentinel.PROPERTY_TRUE));
      menu.addSeparator();
      
      if (type != RmdOutput.TYPE_SHINY)
      {
        boolean inline = uiPrefs_.showRmdChunkOutputInline().getValue();
        menu.addItem(new DocPropMenuItem(
              "Chunk Output Inline", docUpdateSentinel_,
              inline,
              TextEditingTargetNotebook.CHUNK_OUTPUT_TYPE,
              TextEditingTargetNotebook.CHUNK_OUTPUT_INLINE));
        menu.addItem(new DocPropMenuItem(
              "Chunk Output in Console", docUpdateSentinel_,
              !inline,
              TextEditingTargetNotebook.CHUNK_OUTPUT_TYPE,
              TextEditingTargetNotebook.CHUNK_OUTPUT_CONSOLE));
         
         menu.addSeparator();

         menu.addItem(commands_.notebookExpandAllOutput().createMenuItem(false));
         menu.addItem(commands_.notebookCollapseAllOutput().createMenuItem(false));
         menu.addSeparator();
         menu.addItem(commands_.notebookClearOutput().createMenuItem(false));
         menu.addItem(commands_.notebookClearAllOutput().createMenuItem(false));
         menu.addSeparator();
      }
      
      menu.addSeparator();
           
      if (showOutputOptions)
         menu.addItem(commands_.editRmdFormatOptions().createMenuItem(false));
   }
   
   private final TextEditingTarget target_;
   private final DocUpdateSentinel docUpdateSentinel_;
   private final Commands commands_;
   private final EventBus events_;
   private final UIPrefs uiPrefs_;
   private final Session session_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final DocDisplay editor_;
   private final ShinyViewerTypePopupMenu shinyViewerMenu_;
   private final ShinyTestPopupMenu shinyTestMenu_;
   private final PlumberViewerTypePopupMenu plumberViewerMenu_;
   private String extendedType_;
   private String publishPath_;
   private CheckBox sourceOnSave_;
   private DockLayoutPanel editorPanel_;
   private DocumentOutlineWidget docOutlineWidget_;
   private PanelWithToolbars panel_;
   private Toolbar toolbar_;
   private InfoBar warningBar_;
   private final TextEditingTargetFindReplace findReplace_;
   private ToolbarButton codeTransform_;
   private ToolbarButton compilePdfButton_;
   private ToolbarButton previewHTMLButton_;
   private ToolbarButton knitDocumentButton_;
   private ToolbarButton insertChunkMenu_;
   private ToolbarButton insertChunkButton_;
   private ToolbarButton goToPrevButton_;
   private ToolbarButton goToNextButton_;
   private ToolbarButton runButton_;
   private ToolbarButton runLastButton_;
   private ToolbarButton sourceButton_;
   private ToolbarButton previewJsButton_;
   private ToolbarButton previewSqlButton_;
   private ToolbarButton testThatButton_;
   private ToolbarButton testShinyButton_;
   private ToolbarButton compareTestButton_;
   private ToolbarButton sourceMenuButton_;
   private UIPrefMenuItem<Boolean> runSetupChunkOptionMenu_;
   private ToolbarButton chunksButton_;
   private ToolbarButton shinyLaunchButton_;
   private ToolbarButton plumberLaunchButton_;
   private ToolbarButton rmdOptionsButton_;
   private LatchingToolbarButton toggleDocOutlineButton_;
   private CheckBox showWhitespaceCharactersCheckbox_;
   private ToolbarPopupMenuButton rmdFormatButton_;
   private ToolbarPopupMenuButton runDocumentMenuButton_;
   private RSConnectPublishButton publishButton_;
   private MenuItem rmdViewerPaneMenuItem_;
   private MenuItem rmdViewerWindowMenuItem_;
   private MenuItem rmdViewerNoPreviewMenuItem_;
   private HandlerManager handlerManager_;
   
   private Widget texSeparatorWidget_;
   private ToolbarButton texToolbarButton_;
   private Widget notebookSeparatorWidget_;
   private ToolbarButton notebookToolbarButton_;
   private Label srcOnSaveLabel_;

   private String shinyAppState_ = ShinyApplicationParams.STATE_STOPPED;
   private String plumberAPIState_ = PlumberAPIParams.STATE_STOPPED;
   private String sourceCommandText_ = "Source";
   private String knitCommandText_ = "Knit";
   private String previewCommandText_ = "Preview";
}
