/*
 * ViewFilePanel.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import java.util.ArrayList;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FullscreenPopupPanel;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetFindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsHistoryEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ViewFilePanel extends Composite implements TextDisplay
{
   @Inject
   public ViewFilePanel(DocDisplay docDisplay,
                        FileTypeRegistry fileTypeRegistry,
                        UIPrefs uiPrefs,
                        EventBus events,
                        Commands commands,
                        FontSizeManager fontSizeManager,
                        FileDialogs fileDialogs,
                        RemoteFileSystemContext fileContext,
                        Session session,
                        GitServerOperations server)
   {
      fileTypeRegistry_ = fileTypeRegistry;
      commands_ = commands;
      fileDialogs_ = fileDialogs;
      fileContext_ = fileContext;
      session_ = session;
      server_ = server;
      docDisplay_ = docDisplay; 
      docDisplay_.setReadOnly(true);
      
      TextEditingTarget.registerPrefs(releaseOnDismiss_, 
            uiPrefs, 
            docDisplay_);

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
                  findReplace_.showFindReplace();
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
      
      
      initWidget(panel_);
   }
   
   HandlerRegistration addShowVcsHistoryHandler(
                                 ShowVcsHistoryEvent.Handler handler)
   {
      return addHandler(handler, ShowVcsHistoryEvent.TYPE);
   }
   
   public void showFile(FileSystemItem file, String commitId, String contents)
   {
      commitId_ = commitId;
      targetFile_ = file;
      
      docDisplay_.setCode(contents, false);  
      
      adaptToFileType(fileTypeRegistry_.getTextTypeForFile(file));
      
      // header widget has icon + label
      HorizontalPanel panel = new HorizontalPanel();
     
      Image imgFile = new Image(fileTypeRegistry_.getIconForFile(file));
      imgFile.addStyleName(RES.styles().captionIcon());
      panel.add(imgFile);
      
      Label lblCaption = new Label(file.getPath() + " @ " + commitId);
      lblCaption.addStyleName(RES.styles().captionLabel());
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
    
   private Toolbar createToolbar()
   {
      Toolbar toolbar = new ViewFileToolbar();
      
      toolbar.addLeftWidget(new ToolbarButton(
         "Save As", 
         commands_.saveSourceDoc().getImageResource(),
         new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               saveFileAs();
            }
            
         }));
      toolbar.addLeftSeparator();
      
      toolbar.addLeftWidget(new ToolbarButton(
         null,
         commands_.printSourceDoc().getImageResource(),
         new ClickHandler() {

            @Override
            public void onClick(ClickEvent event)
            {
               docDisplay_.print();
            }
            
         }));
      toolbar.addLeftSeparator();
      
      toolbar.addLeftWidget(findReplace_.createFindReplaceButton());
      
      
      toolbar.addRightWidget(new ToolbarButton(
           "Show History",
           commands_.goToWorkingDir().getImageResource(),
           new ClickHandler() {

            @Override
            public void onClick(ClickEvent event)
            {
               fireEvent(new ShowVcsHistoryEvent(targetFile_));
               popupPanel_.close();
            }
              
           }));
      
      return toolbar;
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
                  
                  server_.gitExportFile(
                        commitId_,
                        targetFile_.getPath(),
                        input.getPath(),
                        new VoidServerRequestCallback(indicator));

               }
               
            });
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("ViewFilePanel.css")
      Styles styles();
   }
   
   public interface Styles extends CssResource
   {
      String captionIcon();
      String captionLabel();
   }
   
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
   
   private class ViewFileToolbar extends Toolbar
   {
      @Override
      public int getHeight()
      {
         return 23;
      }
   }
  
   private final FileTypeRegistry fileTypeRegistry_;
   private final RemoteFileSystemContext fileContext_;
   private final FileDialogs fileDialogs_;
   private final Commands commands_;
   private final Session session_;
   private final GitServerOperations server_;
   private final DocDisplay docDisplay_;
   
   private final PanelWithToolbars panel_;
   private FullscreenPopupPanel popupPanel_;
   private final TextEditingTargetFindReplace findReplace_;
   
   private String commitId_ = null;
   private FileSystemItem targetFile_ = null;
   
   private static final Resources RES = GWT.<Resources>create(Resources.class);
   
   private final ArrayList<HandlerRegistration> releaseOnDismiss_ =
         new ArrayList<HandlerRegistration>();
}
