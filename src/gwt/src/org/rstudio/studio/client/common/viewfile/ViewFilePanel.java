/*
 * ViewFilePanel.java
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
package org.rstudio.studio.client.common.viewfile;

import java.util.ArrayList;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.FullscreenPopupPanel;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetFindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetPrefsHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ViewFilePanel extends Composite implements TextDisplay
{
   public interface SaveFileAsHandler
   {
      void onSaveFileAs(FileSystemItem source,
                        FileSystemItem destination,
                        ProgressIndicator indicator);
   }
   
   @Inject
   public ViewFilePanel(GlobalDisplay globalDisplay,
                        DocDisplay docDisplay,
                        FileTypeRegistry fileTypeRegistry,
                        UserPrefs uiPrefs,
                        EventBus events,
                        Commands commands,
                        FilesServerOperations server,
                        FontSizeManager fontSizeManager,
                        FileDialogs fileDialogs,
                        RemoteFileSystemContext fileContext,
                        Session session)
   {
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      commands_ = commands;
      fileDialogs_ = fileDialogs;
      fileContext_ = fileContext;
      session_ = session;
      server_ = server;
      docDisplay_ = docDisplay; 
      docDisplay_.setReadOnly(true);
      
      TextEditingTargetPrefsHelper.registerPrefs(releaseOnDismiss_, 
            uiPrefs,
            null,
            docDisplay_,
            new TextEditingTargetPrefsHelper.PrefsContext()
            {
               @Override
               public FileSystemItem getActiveFile()
               {
                  return targetFile_;
               }
            },
            TextEditingTargetPrefsHelper.PrefsSet.Full);

      TextEditingTarget.syncFontSize(releaseOnDismiss_, 
           events, 
           this, 
           fontSizeManager); 
      
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
                                     null,
                                     docDisplay_.asWidget(),
                                     null);
      panel_.setSize("100%", "100%");
      
      releaseOnDismiss_.add(docDisplay_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            NativeEvent ne = event.getNativeEvent();
            int mod = KeyboardShortcut.getModifierValue(ne);
            if ((mod == KeyboardShortcut.META || 
                (mod == KeyboardShortcut.CTRL && !BrowseCap.hasMetaKey())))
            {
               if (ne.getKeyCode() == 'F')
               {
                  event.preventDefault();
                  event.stopPropagation();
                  findReplace_.showFindReplace(true);
               }
               else if (ne.getKeyCode() == 'S')
               {
                  event.preventDefault();
                  event.stopPropagation();
                  saveFileAs();
               }
            }
            else if (mod == KeyboardShortcut.NONE &&
                     ne.getKeyCode() == KeyCodes.KEY_ESCAPE)
            {
               if (findReplace_.isShowing())
                  findReplace_.hideFindReplace();
               else
                  popupPanel_.close();
            }
            
         }
      }));
        
      saveFileAsHandler_ = new SaveFileAsHandler() {

         @Override
         public void onSaveFileAs(FileSystemItem source,
                                  FileSystemItem destination, 
                                  ProgressIndicator indicator)
         {
            server_.copyFile(source, 
                             destination, 
                             true, 
                             new VoidServerRequestCallback(indicator));
         }
         
      };
      
      initWidget(panel_);
   }
   
   public Toolbar getToolbar()
   {
      return toolbar_;
   }
    
   
   public void setSaveFileAsHandler(SaveFileAsHandler handler)
   {
      saveFileAsHandler_ = handler;
   }
   
   public void showFile(final FileSystemItem file, String encoding)
   {
      final ProgressIndicator indicator = new GlobalProgressDelayer(
            globalDisplay_, 300, "Loading file contents").getIndicator();
                                               
      
      server_.getFileContents(file.getPath(), 
                              encoding,
                              new ServerRequestCallback<String>() {

         @Override
         public void onResponseReceived(String contents)
         {
            indicator.onCompleted();
            showFile(file.getPath(), file, contents);
         }

         @Override
         public void onError(ServerError error)
         {
            indicator.onError(error.getUserMessage());
         }
      });
   }
   
   public void showFile(String caption, FileSystemItem file, String contents)
   {
      targetFile_ = file;
      
      docDisplay_.setCode(contents, false);  
      
      adaptToFileType(fileTypeRegistry_.getTextTypeForFile(file));
     
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      
      // header widget has icon + label
      HorizontalPanel panel = new HorizontalPanel();
     
      FileIcon icon = fileTypeRegistry_.getIconForFile(file);
      Image imgFile = new Image(icon.getImageResource());
      imgFile.addStyleName(styles.fullscreenCaptionIcon());
      imgFile.setAltText(icon.getDescription());
      panel.add(imgFile);
      
      Label lblCaption = new Label(caption);
      lblCaption.addStyleName(styles.fullscreenCaptionLabel());
      panel.add(lblCaption);
      
      popupPanel_ = new FullscreenPopupPanel(panel,asWidget(), false);
      popupPanel_.center();
      
      // set focus to the doc display after 100ms
      Timer timer = new Timer() {
         public void run() {
            docDisplay_.focus();
         }
      };
      timer.schedule(100); 
   }
   
   public FileSystemItem getTargetFile()
   {
      return targetFile_;
   }
   
   public void close()
   {
      popupPanel_.close();
   }
    
   private Toolbar createToolbar()
   {
      toolbar_ = new ViewFileToolbar();
      
      toolbar_.addLeftWidget(new ToolbarButton(
         "Save As",
         ToolbarButton.NoTitle,
         commands_.saveSourceDoc().getImageResource(),
         new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               saveFileAs();
            }
            
         }));
      toolbar_.addLeftSeparator();
      
      toolbar_.addLeftWidget(new ToolbarButton(
         ToolbarButton.NoText,
         commands_.printSourceDoc().getTooltip(),
         commands_.printSourceDoc().getImageResource(),
         new ClickHandler() {

            @Override
            public void onClick(ClickEvent event)
            {
               docDisplay_.print();
            }
            
         }));
      toolbar_.addLeftSeparator();
      
      toolbar_.addLeftWidget(findReplace_.createFindReplaceButton());
           
      return toolbar_;
   }  
   
   @Override
   public void onActivate()
   {
      docDisplay_.onActivate();
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
   public Widget asWidget()
   {
      return this;
   }
   
   @Override
   public void onUnload()
   {
      super.onUnload();
      
      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();
   }
   
   private void saveFileAs()
   {
      fileDialogs_.saveFile(
            "Save File - " + targetFile_.getName(), 
            fileContext_, 
            FileSystemItem.createFile(
                session_.getSessionInfo().getActiveProjectDir()
                         .completePath(targetFile_.getName())), 
            targetFile_.getExtension(), 
            false, 
            new ProgressOperationWithInput<FileSystemItem> () {

               @Override
               public void execute(FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  if (input == null)
                  {
                     indicator.onCompleted();
                     return;
                  }
                  
                  indicator.onProgress("Saving file...");
                  
                  saveFileAsHandler_.onSaveFileAs(targetFile_,
                                                  input,
                                                  indicator);
               }
               
            });
   }
   
   private class ViewFileToolbar extends Toolbar
   {
      public ViewFileToolbar()
      {
         super("View File Tab");
      }
      
      @Override
      public int getHeight()
      {
         return 23;
      }
   }
  
   private final GlobalDisplay globalDisplay_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final RemoteFileSystemContext fileContext_;
   private final FileDialogs fileDialogs_;
   private final Commands commands_;
   private final Session session_;
   private final DocDisplay docDisplay_;
   private final FilesServerOperations server_;
   
   private Toolbar toolbar_;
  
   private final PanelWithToolbars panel_;
   private FullscreenPopupPanel popupPanel_;
   private final TextEditingTargetFindReplace findReplace_;
   
   private FileSystemItem targetFile_ = null;
   
   private SaveFileAsHandler saveFileAsHandler_ = null;
   
   private final ArrayList<HandlerRegistration> releaseOnDismiss_ =
         new ArrayList<HandlerRegistration>();
}
