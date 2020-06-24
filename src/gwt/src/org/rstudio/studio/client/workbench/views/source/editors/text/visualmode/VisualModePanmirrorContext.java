/*
 * VisualModePanmirrorContext.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsinterop.JsVoidFunction;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.panmirror.PanmirrorContext;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIContext;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIDisplay;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.BlogdownConfig;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ImagePreviewer;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.inject.Inject;


public class VisualModePanmirrorContext
{
   
   public VisualModePanmirrorContext(DocUpdateSentinel docUpdateSentinel,
                                     TextEditingTarget target,
                                     VisualModeChunkExec exec,
                                     VisualModePanmirrorFormat format)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docUpdateSentinel_ = docUpdateSentinel;
      target_ = target;
      exec_ = exec;
      format_ = format;
   }
   
   @Inject
   void initialize(WorkbenchContext workbenchContext, Session session, EventBus events)
   {
      workbenchContext_ = workbenchContext;
      sessionInfo_ = session.getSessionInfo();
      
      // notify watchers of file changes
      events.addHandler(FileChangeEvent.TYPE, new FileChangeHandler() {
         @Override
         public void onFileChange(FileChangeEvent event)
         {
            fileWatchers_.forEach(fileWatcher -> {
               fileWatcher.onFileChanged(event.getFileChange().getFile());
            }); 
         }
      });
   }
   
   public PanmirrorContext createContext(PanmirrorUIDisplay.ShowContextMenu showContextMenu)
   {  
      return new PanmirrorContext(
         uiContext(), 
         uiDisplay(showContextMenu), 
         exec_.uiExecute(),
         target_
      );
   }
   
   private PanmirrorUIContext uiContext()
   {
      PanmirrorUIContext uiContext = new PanmirrorUIContext();
      
      uiContext.getDocumentPath = () -> {
        return docUpdateSentinel_.getPath(); 
      };
      
      uiContext.getDefaultResourceDir = () -> {  
         if (docUpdateSentinel_.getPath() != null)
            return FileSystemItem.createDir(docUpdateSentinel_.getPath()).getParentPathString();
         else
            return workbenchContext_.getCurrentWorkingDir().getPath();
      };
      
      uiContext.mapPathToResource = path -> {
         FileSystemItem resourceDir = FileSystemItem.createDir(uiContext.getDefaultResourceDir.get());
         FileSystemItem file = FileSystemItem.createFile(path);
         String resourcePath = file.getPathRelativeTo(resourceDir);
         if (resourcePath != null)
         {
            return resourcePath;
         }
         else
         {
            // try for hugo asset
            return pathToHugoAsset(path);
         }
      };
      
      uiContext.mapResourceToURL = path -> { 
         path = resolvePath(path);  
         FileSystemItem resourceDir = FileSystemItem.createDir(uiContext.getDefaultResourceDir.get());
         return ImagePreviewer.imgSrcPathFromHref(resourceDir.getPath(), path);
      };
      
      uiContext.watchResource = (path, notify) -> {
         String resourcePath = resolvePath(path);
         if (FilePathUtils.pathIsRelative(resourcePath))
         {
            FileSystemItem resourceDir = FileSystemItem.createDir(uiContext.getDefaultResourceDir.get());
            resourcePath = resourceDir.completePath(resourcePath);
         }
         FileWatcher watcher = new FileWatcher(FileSystemItem.createFile(resourcePath), notify);
         fileWatchers_.add(watcher);   
         return () -> {
            fileWatchers_.remove(watcher);
         };
      };
      
      
      uiContext.translateText = text -> {
         return text;
      
      
      };
      return uiContext;
   }
   
   private PanmirrorUIDisplay uiDisplay(PanmirrorUIDisplay.ShowContextMenu showContextMenu)
   {
      PanmirrorUIDisplay uiDisplay = new PanmirrorUIDisplay();
      uiDisplay.showContextMenu = showContextMenu;
      return uiDisplay;
   }
   
   private String pathToHugoAsset(String path)
   {
      if (format_.isHugoProjectDocument())
      {
         FileSystemItem file = FileSystemItem.createFile(path);
         for (FileSystemItem dir : hugoStaticDirs())
         {
            String assetPath = file.getPathRelativeTo(dir);
            if (assetPath != null)
               return "/" + assetPath;
         }
         
         return null;
      }
      else
      {
         return null;
      }
   }
   
   private String resolvePath(String path)
   {
      String hugoPath = hugoAssetPath(path);
      if (hugoPath != null)
         return hugoPath;
      else
         return path;
   }

   
   // TODO: currently can only serve image preview out of main static dir
   // (to resolve we'd need to create a server-side handler that presents
   // a union view of the various static dirs, much as hugo does internally)
   private String hugoAssetPath(String asset)
   {
      if (format_.isHugoProjectDocument() && asset.startsWith("/"))
      {
         return hugoStaticDirs().get(0).completePath(asset.substring(1));
      }
      else
      {
         return null;
      }
   }
   
   
   private List<FileSystemItem> hugoStaticDirs()
   {
      FileSystemItem siteDir = getBlogdownConfig().site_dir;
      List<FileSystemItem> staticDirs = new ArrayList<FileSystemItem>();
      for (String dir : getBlogdownConfig().static_dirs)
         staticDirs.add(FileSystemItem.createDir(siteDir.completePath(dir)));
      return staticDirs;
    
   }
   

   private BlogdownConfig getBlogdownConfig()
   {
      return sessionInfo_.getBlogdownConfig();
   }
   
   
   
   private class FileWatcher
   {
      public FileWatcher(FileSystemItem file, JsVoidFunction notify)
      {
         file_ = file;
         notify_ = notify;
      }
      
      public void onFileChanged(FileSystemItem file)
      {
         if (file.equalTo(file_))
            notify_.call();
      }
      
      private FileSystemItem file_;
      private JsVoidFunction notify_;
   }
   private HashSet<FileWatcher> fileWatchers_ = new HashSet<FileWatcher>();
   
  

   private final DocUpdateSentinel docUpdateSentinel_;
   private final TextEditingTarget target_;
   
   private final VisualModeChunkExec exec_;
   private final VisualModePanmirrorFormat format_;
   
   private WorkbenchContext workbenchContext_;
   private SessionInfo sessionInfo_;
   
}
