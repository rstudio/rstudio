/*
 * WorkbenchContext.java
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
package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedHandler;
import org.rstudio.studio.client.workbench.prefs.model.PrefsServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class WorkbenchContext implements UiPrefsChangedHandler
{

   @Inject
   public WorkbenchContext(PrefsServerOperations server,
                           Session session, 
                           EventBus eventBus,
                           Provider<UIPrefs> uiPrefs)
   {
      server_ = server;
      session_ = session;
      uiPrefs_ = uiPrefs;
      
      // track current working dir
      currentWorkingDir_ = FileSystemItem.home();
      defaultFileDialogDir_ = FileSystemItem.home();
      eventBus.addHandler(WorkingDirChangedEvent.TYPE, 
                          new WorkingDirChangedHandler() {
         @Override
         public void onWorkingDirChanged(WorkingDirChangedEvent event)
         {
            currentWorkingDir_ = FileSystemItem.createDir(event.getPath());
            defaultFileDialogDir_ = FileSystemItem.createDir(event.getPath());;
         }      
      }); 
      
      eventBus.addHandler(UiPrefsChangedEvent.TYPE, this);
   }
   
  
   public FileSystemItem getCurrentWorkingDir()
   {
      return currentWorkingDir_;
   }
   
   public FileSystemItem getDefaultFileDialogDir()
   {
      return defaultFileDialogDir_;
   }
   
   public void setDefaultFileDialogDir(FileSystemItem dir)
   {
      defaultFileDialogDir_ = dir;
   }
   
   // NOTE: mirrors behavior of rEnvironmentDir in SessionMain.cpp
   public String getREnvironmentPath()
   {
      SessionInfo sessionInfo = session_.getSessionInfo();
      if (sessionInfo != null)
      {
         FileSystemItem rEnvDir = null;

         if (getActiveProjectDir() != null)
         {
            rEnvDir = getActiveProjectDir();
         }
         if (sessionInfo.getMode().equals(SessionInfo.DESKTOP_MODE))
         {
            rEnvDir = currentWorkingDir_;
         }
         else
         {
            rEnvDir = FileSystemItem.createDir(
                                       sessionInfo.getInitialWorkingDir());
         }
         return rEnvDir.completePath(".RData");
      }
      else
      {
         return FileSystemItem.home().completePath(".RData");
      }
   }
   
   public FileSystemItem getActiveProjectDir()
   {
      if (activeProjectDir_ == null)
      {
         SessionInfo sessionInfo = session_.getSessionInfo();
         if (sessionInfo != null &&
             sessionInfo.getActiveProjectFile() != null)
         {
            activeProjectDir_ = FileSystemItem.createFile(
                           sessionInfo.getActiveProjectFile()).getParentPath();
         }
      }
      return activeProjectDir_;
   }
   
   public boolean isProjectActive()
   {
      SessionInfo sessionInfo = session_.getSessionInfo();
      return sessionInfo != null && sessionInfo.getActiveProjectFile() != null;
   }
   
   
   
   public void updateUIPrefs()
   {
      server_.setUiPrefs(
         session_.getSessionInfo().getUiPrefs(),
         new VoidServerRequestCallback());
   }
   
   @Override
   public void onUiPrefsChanged(UiPrefsChangedEvent e)
   {      
      // get references to new and existing UI prefs
      UIPrefs uiPrefs = uiPrefs_.get();
      
      if (e.getType().equals(UiPrefsChangedEvent.GLOBAL_TYPE))
      {
         // get prefs accessor
         UIPrefsAccessor newUiPrefs = new UIPrefsAccessor(
                                                   e.getUIPrefs(), 
                                                   JsObject.createJsObject());
         
         // show line numbers
         uiPrefs.showLineNumbers().setGlobalValue(
                                 newUiPrefs.showLineNumbers().getGlobalValue());
         
         // highlight selected word
         uiPrefs.highlightSelectedWord().setGlobalValue(
                           newUiPrefs.highlightSelectedWord().getGlobalValue());
         
         // highlight selected line
         uiPrefs.highlightSelectedLine().setGlobalValue(
                          newUiPrefs.highlightSelectedLine().getGlobalValue());
       
         // pane config
         if (!newUiPrefs.paneConfig().getGlobalValue().isEqualTo(
                                 uiPrefs.paneConfig().getGlobalValue()))
         {
            uiPrefs.paneConfig().setGlobalValue(
                              newUiPrefs.paneConfig().getGlobalValue());
         }
         
         // use spaces for tab
         uiPrefs.useSpacesForTab().setGlobalValue(
                          newUiPrefs.useSpacesForTab().getGlobalValue());
           
         // num spacers for tab
         uiPrefs.numSpacesForTab().setGlobalValue(
               newUiPrefs.numSpacesForTab().getGlobalValue());
   
         // show margin
         uiPrefs.showMargin().setGlobalValue(
                                 newUiPrefs.showMargin().getGlobalValue());
         
         // print margin column
         uiPrefs.printMarginColumn().setGlobalValue(
                              newUiPrefs.printMarginColumn().getGlobalValue());
      
         // insert matching
         uiPrefs.insertMatching().setGlobalValue(
                                 newUiPrefs.insertMatching().getGlobalValue());
      
         // soft wrap R files
         uiPrefs.softWrapRFiles().setGlobalValue(
                                 newUiPrefs.softWrapRFiles().getGlobalValue());
         
         // syntax color console
         uiPrefs.syntaxColorConsole().setGlobalValue(
                             newUiPrefs.syntaxColorConsole().getGlobalValue());
      
         // font size
         uiPrefs.fontSize().setGlobalValue(
                             newUiPrefs.fontSize().getGlobalValue());
      
         // theme
         uiPrefs.theme().setGlobalValue(newUiPrefs.theme().getGlobalValue());
      
         // default encoding
         uiPrefs.defaultEncoding().setGlobalValue(
                                 newUiPrefs.defaultEncoding().getGlobalValue());
         
         // default project location
         uiPrefs.defaultProjectLocation().setGlobalValue(
                        newUiPrefs.defaultProjectLocation().getGlobalValue());
      
         // toolbar visible
         uiPrefs.toolbarVisible().setGlobalValue(
                                 newUiPrefs.toolbarVisible().getGlobalValue());
         
         // source with echo
         uiPrefs.sourceWithEcho().setGlobalValue(
                                 newUiPrefs.sourceWithEcho().getGlobalValue());
         
         
         // export plot options
         if (!ExportPlotOptions.areEqual(
               newUiPrefs.exportPlotOptions().getGlobalValue(),
               uiPrefs.exportPlotOptions().getGlobalValue()))
         {
            uiPrefs.exportPlotOptions().setGlobalValue(
                              newUiPrefs.exportPlotOptions().getGlobalValue());
         }
         
         // save plot as pdf options
         if (!SavePlotAsPdfOptions.areEqual(
               newUiPrefs.savePlotAsPdfOptions().getGlobalValue(),
               uiPrefs.savePlotAsPdfOptions().getGlobalValue()))
         {
            uiPrefs.savePlotAsPdfOptions().setGlobalValue(
                         newUiPrefs.savePlotAsPdfOptions().getGlobalValue());
         }
      }
      else if (e.getType().equals(UiPrefsChangedEvent.PROJECT_TYPE))
      {
         // get prefs accessor
         UIPrefsAccessor newUiPrefs = new UIPrefsAccessor(
                                                   JsObject.createJsObject(),
                                                   e.getUIPrefs());
         
         // use spaces for tab
         uiPrefs.useSpacesForTab().setProjectValue(
                          newUiPrefs.useSpacesForTab().getValue());
           
         // num spacers for tab
         uiPrefs.numSpacesForTab().setProjectValue(
               newUiPrefs.numSpacesForTab().getValue());
   
         // default encoding
         uiPrefs.defaultEncoding().setProjectValue(
                                 newUiPrefs.defaultEncoding().getValue());
 
      }
      else
      {
         Debug.log("Unexpected uiPrefs type: " + e.getType());
      }
   }
   
   
   FileSystemItem currentWorkingDir_ = FileSystemItem.home();
   FileSystemItem defaultFileDialogDir_ = FileSystemItem.home();
   FileSystemItem activeProjectDir_ = null;
   Session session_;
   private final PrefsServerOperations server_;
   private final Provider<UIPrefs> uiPrefs_;
   
}
