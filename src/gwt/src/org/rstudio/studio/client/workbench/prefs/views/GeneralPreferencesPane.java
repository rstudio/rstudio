/*
 * GeneralPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.prefs.model.GeneralPrefs;
import org.rstudio.studio.client.workbench.prefs.model.HistoryPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;

/**
 * TODO: Apply new settings
 * TODO: Make sure onApply only does non-desktop settings if in web mode
 */
public class GeneralPreferencesPane extends PreferencesPane
{
   @Inject
   public GeneralPreferencesPane(PreferencesDialogResources res,
                                 RemoteFileSystemContext fsContext,
                                 FileDialogs fileDialogs)
   {
      res_ = res;
      fsContext_ = fsContext;
      fileDialogs_ = fileDialogs;

      if (Desktop.isDesktop())
      {
         if (Desktop.getFrame().canChooseRVersion())
         {
            rVersion_ = new TextBoxWithButton(
                  "R version:",
                  "Change...",
                  new ClickHandler()
                  {
                     public void onClick(ClickEvent event)
                     {
                        String ver = Desktop.getFrame().chooseRVersion();
                        if (!StringUtil.isNullOrEmpty(ver))
                           rVersion_.setText(ver);
                     }
                  });
            rVersion_.setWidth("100%");
            rVersion_.setText(Desktop.getFrame().getRVersion());
            add(rVersion_);
         }
      }

      add(tight(new Label("Initial working directory:")));
      add(dirChooser_ = new TextBoxWithButton(null, "Browse...", new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            fileDialogs_.chooseFolder(
                  "Choose Directory",
                  fsContext_,
                  FileSystemItem.createDir(dirChooser_.getText()),
                  new ProgressOperationWithInput<FileSystemItem>()
                  {
                     public void execute(FileSystemItem input,
                                         ProgressIndicator indicator)
                     {
                        if (input == null)
                           return;

                        dirChooser_.setText(input.getPath());
                        indicator.onCompleted();
                     }
                  });
         }
      }));
      dirChooser_.setWidth("80%");

      saveWorkspace_ = new SelectWidget(
            "Save workspace to .RData on exit:",
            new String[] {
                  "Always",
                  "Never",
                  "Ask"
            });
      add(saveWorkspace_);
      
      add(loadRData_ = new CheckBox("Restore .RData into workspace at startup"));
   
      alwaysSaveHistory_ = new CheckBox(
            "Always save history (even when not saving .RData)");
      alwaysSaveHistory_.addStyleName(res.styles().extraSpaced());
      add(alwaysSaveHistory_);
      
      useGlobalHistory_ = new CheckBox(
            "Use single global history (rather than per-working directory)");
      useGlobalHistory_.addStyleName(res.styles().extraSpaced());
         
      // only allow tweaking of global vs. non-global history in desktop
      // mode (in server mode there is no way to start in a non-standard
      // working directory so saving history in working directoriees rather
      // than globally will basically break history)
      if (Desktop.isDesktop())
         add(useGlobalHistory_);
      
      removeHistoryDuplicates_ = new CheckBox(
                                 "Remove duplicate entries in history");
      removeHistoryDuplicates_.addStyleName(res.styles().extraSpaced());
      add(removeHistoryDuplicates_);

    
      
     
      saveWorkspace_.setEnabled(false);
      loadRData_.setEnabled(false);
      dirChooser_.setEnabled(false);
      alwaysSaveHistory_.setEnabled(false);
      useGlobalHistory_.setEnabled(false);
      removeHistoryDuplicates_.setEnabled(false);
   }
   
   @Override
   protected void initializeRPrefs(RPrefs rPrefs)
   {
      // general prefs
      GeneralPrefs generalPrefs = rPrefs.getGeneralPrefs();
      
      saveWorkspace_.setEnabled(true);
      loadRData_.setEnabled(true);
      dirChooser_.setEnabled(true);
      
      int saveWorkspaceIndex;
      switch (generalPrefs.getSaveAction())
      {
         case SaveAction.NOSAVE: 
            saveWorkspaceIndex = 1; 
            break;
         case SaveAction.SAVE: 
            saveWorkspaceIndex = 0; 
            break; 
         case SaveAction.SAVEASK:
         default: 
            saveWorkspaceIndex = 2; 
            break; 
      }
      saveWorkspace_.getListBox().setSelectedIndex(saveWorkspaceIndex);

      loadRData_.setValue(generalPrefs.getLoadRData());
      dirChooser_.setText(generalPrefs.getInitialWorkingDirectory());
        
      // history prefs
      HistoryPrefs historyPrefs = rPrefs.getHistoryPrefs();
      
      alwaysSaveHistory_.setEnabled(true);
      useGlobalHistory_.setEnabled(true);
      removeHistoryDuplicates_.setEnabled(true);
      
      alwaysSaveHistory_.setValue(historyPrefs.getAlwaysSave());
      useGlobalHistory_.setValue(historyPrefs.getUseGlobal());
      removeHistoryDuplicates_.setValue(historyPrefs.getRemoveDuplicates());
   }
   

   @Override
   public ImageResource getIcon()
   {
      return res_.iconR();
   }

   @Override
   public void onApply(RPrefs rPrefs)
   {
      super.onApply(rPrefs);

      if (saveWorkspace_.isEnabled())
      {
         int saveAction;
         switch (saveWorkspace_.getListBox().getSelectedIndex())
         {
            case 0: 
               saveAction = SaveAction.SAVE; 
               break; 
            case 1: 
               saveAction = SaveAction.NOSAVE; 
               break; 
            case 2:
            default: 
               saveAction = SaveAction.SAVEASK; 
               break; 
         }

         // set general prefs
         GeneralPrefs generalPrefs = GeneralPrefs.create(saveAction, 
                                                         loadRData_.getValue(),
                                                         dirChooser_.getText());
         rPrefs.setGeneralPrefs(generalPrefs);
         
         // set history prefs
         HistoryPrefs historyPrefs = HistoryPrefs.create(
                                          alwaysSaveHistory_.getValue(),
                                          useGlobalHistory_.getValue(),
                                          removeHistoryDuplicates_.getValue());
         rPrefs.setHistoryPrefs(historyPrefs);
      }
   }

   @Override
   public String getName()
   {
      return "General";
   }

   private final PreferencesDialogResources res_;
   private final FileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private TextBoxWithButton dirChooser_;
   private CheckBox loadRData_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox useGlobalHistory_;
   private final CheckBox removeHistoryDuplicates_;
}
