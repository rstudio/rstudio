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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
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
                                  EventBus events)
   {
      commands_ = commands;
      uiPrefs_ = uiPrefs;
      fileTypeRegistry_ = fileTypeRegistry;
      editor_ = editor;
      extendedType_ = extendedType;
      sourceOnSave_ = new CheckBox();
      srcOnSaveLabel_ =
                  new CheckboxLabel(sourceOnSave_, "Source on Save").getLabel();
      statusBar_ = new StatusBarWidget();
      
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
      
      panel_ = new PanelWithToolbars(createToolbar(fileType),
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
      toolbar.addLeftWidget(knitToHTMLButton_ = commands_.knitToHTML().createToolbarButton());
      toolbar.addLeftWidget(compilePdfButton_ = commands_.compilePDF().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.synctexSearch().createToolbarButton());

      toolbar.addRightWidget(runButton_ = commands_.executeCode().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.executeLastCode().createToolbarButton());
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
      sourceMenu.addSeparator();
      sourceMenu.addItem(commands_.compileNotebook().createMenuItem(false));
      
      sourceMenuButton_ = new ToolbarButton(sourceMenu, true);
      toolbar.addRightWidget(sourceMenuButton_);  

      toolbar.addRightSeparator();
     
      ToolbarPopupMenu chunksMenu = new ToolbarPopupMenu();
      chunksMenu.addItem(commands_.insertChunk().createMenuItem(false));
      chunksMenu.addSeparator();
      chunksMenu.addItem(commands_.jumpTo().createMenuItem(false));
      chunksMenu.addSeparator();
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
         menu.addSeparator();
         menu.addItem(commands_.extractFunction().createMenuItem(false));
         menu.addItem(commands_.reindent().createMenuItem(false));
         menu.addItem(commands_.reflowComment().createMenuItem(false));
         menu.addItem(commands_.commentUncomment().createMenuItem(false));
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
      boolean canSource = fileType.canSource();
      boolean canSourceWithEcho = fileType.canSourceWithEcho();
      boolean canSourceOnSave = fileType.canSourceOnSave();
      boolean canExecuteCode = fileType.canExecuteCode();
      boolean canExecuteChunks = fileType.canExecuteChunks();
      boolean isMarkdown = fileType.isMarkdown();
      boolean isRPresentation = fileType.isRpres();
      boolean isCpp = fileType.isCpp();
     
      sourceOnSave_.setVisible(canSourceOnSave);
      srcOnSaveLabel_.setVisible(canSourceOnSave);
      if (fileType.isRd())
         srcOnSaveLabel_.setText("Preview on Save");
      else
         srcOnSaveLabel_.setText("Source on Save");
      codeTransform_.setVisible(
            (canExecuteCode && !fileType.canAuthorContent()) ||
            fileType.isCpp());   
     
      sourceButton_.setVisible(canSource);
      sourceMenuButton_.setVisible(canSourceWithEcho);
   
      texSeparatorWidget_.setVisible(canCompilePdf);
      texToolbarButton_.setVisible(canCompilePdf);
      compilePdfButton_.setVisible(canCompilePdf);
      chunksButton_.setVisible(canExecuteChunks);
      
      helpMenuButton_.setVisible(isMarkdown || isRPresentation);
      rcppHelpButton_.setVisible(isCpp);
      
      if (isShinyFile())
      {
         sourceOnSave_.setVisible(false);
         srcOnSaveLabel_.setVisible(false);
         runButton_.setText("");
         sourceButton_.setVisible(false);
         sourceMenuButton_.setVisible(false);
      }
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
      runButton_.setText(((width < 480) || isShinyFile()) ? "Launch" : "Run");
      compilePdfButton_.setText(width < 450 ? "" : "Compile PDF");
      previewHTMLButton_.setText(width < 450 ? "" : "Preview");                                                       
      knitToHTMLButton_.setText(width < 450 ? "" : "Knit HTML");
      
      if (editor_.getFileType().isRd())
         srcOnSaveLabel_.setText(width < 450 ? "Preview" : "Preview on Save");
      else
         srcOnSaveLabel_.setText(width < 450 ? "Source" : "Source on Save");
      sourceButton_.setText(width < 400 ? "" : "Source");
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
   public void findPrevious()
   {
      findReplace_.findPrevious();
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

   private final Commands commands_;
   private final UIPrefs uiPrefs_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final DocDisplay editor_;
   private String extendedType_;
   private CheckBox sourceOnSave_;
   private PanelWithToolbars panel_;
   private InfoBar warningBar_;
   private final TextEditingTargetFindReplace findReplace_;
   private ToolbarButton codeTransform_;
   private ToolbarButton compilePdfButton_;
   private ToolbarButton previewHTMLButton_;
   private ToolbarButton knitToHTMLButton_;
   private ToolbarButton runButton_;
   private ToolbarButton sourceButton_;
   private ToolbarButton sourceMenuButton_;
   private ToolbarButton chunksButton_;
   private ToolbarButton helpMenuButton_;
   private ToolbarButton rcppHelpButton_;
   
   private Widget texSeparatorWidget_;
   private ToolbarButton texToolbarButton_;
   private Label srcOnSaveLabel_;

}
