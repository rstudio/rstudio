/*
 * VCSFileOpener.java
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
package org.rstudio.studio.client.workbench.views.vcs.common;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class VCSFileOpener
{
   @Inject
   public VCSFileOpener(Commands commands,
                   EventBus eventBus,
                   FileTypeRegistry fileTypeRegistry,
                   Satellite satellite)
   {
      commands_ = commands;
      eventBus_ = eventBus;
      fileTypeRegistry_ = fileTypeRegistry;
      satellite_ = satellite;
      
      if (!Satellite.isCurrentWindowSatellite())
         exportOpenFilesCallback();
   }
   
   public void openFiles(final ArrayList<StatusAndPath> items)
   {
      if (Satellite.isCurrentWindowSatellite())
      {
         satellite_.focusMainWindow();
         callSatelliteOpenFiles(toJsArray(items));
      }
      else
      {
         doOpenFiles(toJsArray(items));
      }
   }
   
   private void satelliteOpenFiles(JavaScriptObject items)
   {
      JsArray<FileSystemItem> itemsArray = items.cast();
      doOpenFiles(itemsArray);
   }

   private void doOpenFiles(JsArray<FileSystemItem> items)
   {
      for (int i=0; i<items.length(); i++)
      {
         FileSystemItem item = items.get(i);
         if (!item.isDirectory())
         {
            fileTypeRegistry_.openFile(item);
         }
         else 
         { 
            commands_.activateFiles().execute();
            eventBus_.fireEvent(new DirectoryNavigateEvent(item));
         }
      }
   }
   
   private JsArray<FileSystemItem> toJsArray(ArrayList<StatusAndPath> items)
   {
      JsArray<FileSystemItem> jsItems = JavaScriptObject.createArray().cast();
      for (StatusAndPath item : items)
      {
         // NOTE: renames need some extra processing
         String path = item.getRawPath();
         if (StringUtil.equals(item.getStatus(), "R "))
         {
            String[] parts = path.split(" -> ");
            if (parts.length == 2)
            {
               String oldPath = parts[0], newPath = parts[1];
               path = oldPath.substring(0, oldPath.lastIndexOf('/')) + "/" + newPath;
            }
         }
         
         if (item.isDirectory())
            jsItems.push(FileSystemItem.createDir(path));
         else
            jsItems.push(FileSystemItem.createFile(path));
      }
      return jsItems;
   }
   
   private final native void exportOpenFilesCallback()/*-{
      var vcsUtils = this;     
      $wnd.vcsOpenFilesFromRStudioSatellite = $entry(
         function(items) {
            vcsUtils.@org.rstudio.studio.client.workbench.views.vcs.common.VCSFileOpener::satelliteOpenFiles(Lcom/google/gwt/core/client/JavaScriptObject;)(items);
         }
      ); 
   }-*/;
   
   private final native void callSatelliteOpenFiles(JavaScriptObject items)/*-{
      $wnd.opener.vcsOpenFilesFromRStudioSatellite(items);
   }-*/;
  
   
   private final Commands commands_;
   private final EventBus eventBus_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Satellite satellite_;
}
