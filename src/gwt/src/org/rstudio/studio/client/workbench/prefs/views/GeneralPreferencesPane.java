/*
 * GeneralPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.mirrors.DefaultCRANMirror;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.prefs.model.GeneralPrefs;
import org.rstudio.studio.client.workbench.prefs.model.HistoryPrefs;
import org.rstudio.studio.client.workbench.prefs.model.PackagesPrefs;
import org.rstudio.studio.client.workbench.prefs.model.ProjectsPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;

/**
 * TODO: Apply new settings
 * TODO: Make sure onApply only does non-desktop settings if in web mode
 */
public class GeneralPreferencesPane extends PreferencesPane
{
   @Inject
   public GeneralPreferencesPane(RemoteFileSystemContext fsContext,
                                 FileDialogs fileDialogs,
                                 final DefaultCRANMirror defaultCRANMirror)
   {
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
            spaced(rVersion_);
            add(rVersion_);
         }
      }

      Label defaultLabel = new Label("Default working directory (when not in a project):");
      nudgeRight(defaultLabel);
      add(tight(defaultLabel));
      add(dirChooser_ = new DirectoryChooserTextBox(null, 
                                                    null,
                                                    fileDialogs_, 
                                                    fsContext_));  
      extraSpaced(dirChooser_);
      nudgeRight(dirChooser_);
      textBoxWithChooser(dirChooser_);

      saveWorkspace_ = new SelectWidget(
            "Save workspace to .RData on exit:",
            new String[] {
                  "Always",
                  "Never",
                  "Ask"
            });
      add(saveWorkspace_);
      
      add(noSaveRDataForPackages_ = new CheckBox(
                    "Don't save workspace on exit for package projects"));
      spaced(noSaveRDataForPackages_);
      
      add(loadRData_ = new CheckBox("Restore .RData into workspace at startup"));
      spaced(loadRData_); 
      alwaysSaveHistory_ = new CheckBox(
            "Always save history (even when not saving .RData)");
      spaced(alwaysSaveHistory_);
      add(alwaysSaveHistory_);
      
      removeHistoryDuplicates_ = new CheckBox(
                                 "Remove duplicate entries in history");
      extraSpaced(removeHistoryDuplicates_);
      add(removeHistoryDuplicates_);

      cranMirrorTextBox_ = new TextBoxWithButton(
            "CRAN mirror:",
            "Change...",
            new ClickHandler()
            {
               public void onClick(ClickEvent event)
               {
                  defaultCRANMirror.choose(new OperationWithInput<CRANMirror>(){
                     @Override
                     public void execute(CRANMirror cranMirror)
                     {
                        cranMirror_ = cranMirror;
                        cranMirrorTextBox_.setText(cranMirror_.getDisplay());
                     }     
                  });
                 
               }
            });
      extraSpaced(cranMirrorTextBox_);
      nudgeRight(cranMirrorTextBox_);
      textBoxWithChooser(cranMirrorTextBox_);
      cranMirrorTextBox_.setText("");
      add(cranMirrorTextBox_);
      
      restoreLastProject_ = new CheckBox("Restore most recently opened project at startup");
      extraSpaced(restoreLastProject_);
      add(restoreLastProject_);
        
      saveWorkspace_.setEnabled(false);
      noSaveRDataForPackages_.setEnabled(false);
      loadRData_.setEnabled(false);
      dirChooser_.setEnabled(false);
      alwaysSaveHistory_.setEnabled(false);
      removeHistoryDuplicates_.setEnabled(false);
      cranMirrorTextBox_.setEnabled(false);
      restoreLastProject_.setEnabled(false);
   }
   
   @Override
   protected void initialize(RPrefs rPrefs)
   {
      // general prefs
      GeneralPrefs generalPrefs = rPrefs.getGeneralPrefs();
      
      saveWorkspace_.setEnabled(true);
      noSaveRDataForPackages_.setEnabled(true);
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
      noSaveRDataForPackages_.setValue(generalPrefs.getNoSaveRDataForPackages());
      loadRData_.setValue(generalPrefs.getLoadRData());
      dirChooser_.setText(generalPrefs.getInitialWorkingDirectory());
        
      // history prefs
      HistoryPrefs historyPrefs = rPrefs.getHistoryPrefs();
      
      alwaysSaveHistory_.setEnabled(true);
      removeHistoryDuplicates_.setEnabled(true);
      
      alwaysSaveHistory_.setValue(historyPrefs.getAlwaysSave());
      removeHistoryDuplicates_.setValue(historyPrefs.getRemoveDuplicates());
      
      // packages prefs
      PackagesPrefs packagesPrefs = rPrefs.getPackagesPrefs();
      cranMirrorTextBox_.setEnabled(true);
      if (!packagesPrefs.getCRANMirror().isEmpty())
      {
         cranMirror_ = packagesPrefs.getCRANMirror();
         cranMirrorTextBox_.setText(cranMirror_.getDisplay());
      }     
      
      // projects prefs
     ProjectsPrefs projectsPrefs = rPrefs.getProjectsPrefs();
     restoreLastProject_.setEnabled(true);
     restoreLastProject_.setValue(projectsPrefs.getRestoreLastProject());
   }
   

   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconR();
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);

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
         GeneralPrefs generalPrefs = GeneralPrefs.create(
                                          saveAction, 
                                          noSaveRDataForPackages_.getValue(),
                                          loadRData_.getValue(),
                                          dirChooser_.getText());
         rPrefs.setGeneralPrefs(generalPrefs);
         
         // set history prefs
         HistoryPrefs historyPrefs = HistoryPrefs.create(
                                          alwaysSaveHistory_.getValue(),
                                          removeHistoryDuplicates_.getValue());
         rPrefs.setHistoryPrefs(historyPrefs);
         
         // set packages prefs
         PackagesPrefs packagesPrefs = PackagesPrefs.create(cranMirror_, null);
         rPrefs.setPackagesPrefs(packagesPrefs);
         
         // set projects prefs
         ProjectsPrefs projectsPrefs = ProjectsPrefs.create(
                                             restoreLastProject_.getValue());
         rPrefs.setProjectsPrefs(projectsPrefs);
      }

      return restartRequired;
   }

   @Override
   public String getName()
   {
      return "General";
   }

   private final FileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private TextBoxWithButton dirChooser_;
   private CheckBox noSaveRDataForPackages_;
   private CheckBox loadRData_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox removeHistoryDuplicates_;
   private CRANMirror cranMirror_ = CRANMirror.empty();
   private TextBoxWithButton cranMirrorTextBox_;
   private CheckBox restoreLastProject_;
}
