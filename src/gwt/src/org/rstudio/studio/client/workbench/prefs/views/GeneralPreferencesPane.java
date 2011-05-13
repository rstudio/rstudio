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
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.cran.DefaultCRANMirror;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.GeneralPrefs;
import org.rstudio.studio.client.workbench.prefs.model.HistoryPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.common.cran.model.CRANMirror;

/**
 * TODO: Apply new settings
 * TODO: Make sure onApply only does non-desktop settings if in web mode
 */
public class GeneralPreferencesPane extends PreferencesPane
{
   @Inject
   public GeneralPreferencesPane(PreferencesDialogResources res,
                                 WorkbenchServerOperations server,
                                 RemoteFileSystemContext fsContext,
                                 FileDialogs fileDialogs,
                                 final DefaultCRANMirror defaultCRANMirror)
   {
      res_ = res;
      server_ = server;
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

      cranMirrorTextBox_ = new TextBoxWithButton(
         "Default CRAN mirror:",
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
      cranMirrorTextBox_.setWidth("100%");
      cranMirrorTextBox_.setText("");
      cranMirrorTextBox_.addStyleName(res.styles().extraSpaced());
      add(cranMirrorTextBox_);
      

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
            "Always save .Rhistory (even when not saving .RData)");
      alwaysSaveHistory_.addStyleName(res.styles().extraSpaced());
      add(alwaysSaveHistory_);
      
      useGlobalHistory_ = new CheckBox(
            "Use global .Rhistory (rather than per-working directory)");
      
      // only allow tweaking of global vs. non-global history in desktop
      // mode (in server mode there is no way to start in a non-standard
      // working directory so saving history in working directoriees rather
      // than globally will basically break history)
      if (Desktop.isDesktop())
         add(useGlobalHistory_);
      
     
      saveWorkspace_.setEnabled(false);
      loadRData_.setEnabled(false);
      dirChooser_.setEnabled(false);
      cranMirrorTextBox_.setEnabled(false);
      alwaysSaveHistory_.setEnabled(false);
      useGlobalHistory_.setEnabled(false);
      server_.getRPrefs(new SimpleRequestCallback<RPrefs>()
      {
         @Override
         public void onResponseReceived(RPrefs response)
         {
          
            // general prefs
            GeneralPrefs generalPrefs = response.getGeneralPrefs();
            
            saveWorkspace_.setEnabled(true);
            loadRData_.setEnabled(true);
            dirChooser_.setEnabled(true);
            cranMirrorTextBox_.setEnabled(true);
            
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
            
            if (!generalPrefs.getCRANMirror().isEmpty())
            {
               cranMirror_ = generalPrefs.getCRANMirror();
               cranMirrorTextBox_.setText(cranMirror_.getDisplay());
            }
            
            // history prefs
            HistoryPrefs historyPrefs = response.getHistoryPrefs();
            
            alwaysSaveHistory_.setEnabled(true);
            useGlobalHistory_.setEnabled(true);
            
            alwaysSaveHistory_.setValue(historyPrefs.getAlwaysSave());
            useGlobalHistory_.setValue(historyPrefs.getUseGlobal());
         }
      });
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconR();
   }

   @Override
   public void onApply()
   {
      super.onApply();

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

         server_.setGeneralPrefs(saveAction,
                           loadRData_.getValue(),
                           dirChooser_.getText(),
                           cranMirror_,
                           new SimpleRequestCallback<Void>());
         
         server_.setHistoryPrefs(alwaysSaveHistory_.getValue(),
                                 useGlobalHistory_.getValue(),
                                 new SimpleRequestCallback<Void>());
      }
   }

   @Override
   public String getName()
   {
      return "General";
   }

   private final PreferencesDialogResources res_;
   private final WorkbenchServerOperations server_;
   private final FileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private CRANMirror cranMirror_ = CRANMirror.empty();
   private TextBoxWithButton cranMirrorTextBox_;
   private TextBoxWithButton dirChooser_;
   private CheckBox loadRData_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox useGlobalHistory_;
}
