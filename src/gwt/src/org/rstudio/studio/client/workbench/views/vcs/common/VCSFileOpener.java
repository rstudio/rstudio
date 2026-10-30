/*
 * VCSFileOpener.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.common;

import java.util.ArrayList;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
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
   
   // Opens a file at a (1-based) line. From the main window only: the Git
   // pane's changes table, which offers it, is not in a satellite. A file the
   // editor can't take as text (an .rds, an image) has no line to go to, and
   // opens as it would from the pane's Open command; editFile() would force
   // it into the editor, which then refuses it as binary.
   public void openFileAtLine(StatusAndPath item, int line)
   {
      FileSystemItem file = FileSystemItem.createFile(resolvePath(item));
      if (fileTypeRegistry_.getTypeForFile(file) instanceof TextFileType)
         fileTypeRegistry_.editFile(file, FilePosition.create(line, 0));
      else
         fileTypeRegistry_.openFile(file);
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
         String path = resolvePath(item);
         if (item.isDirectory())
            jsItems.push(FileSystemItem.createDir(path));
         else
            jsItems.push(FileSystemItem.createFile(path));
      }
      return jsItems;
   }
   
   // The path of the file as it is in the working tree.
   private String resolvePath(StatusAndPath item)
   {
      // NOTE: renames need some extra processing: a rename's raw path is
      // "<old path> -> <new path>", where only the old path is absolute; the
      // new one is relative to the repository root, as both are in getPath()
      String path = item.getRawPath();
      if (StringUtil.equals(item.getStatus(), "R "))
      {
         String[] parts = path.split(" -> ");
         String[] relativeParts = item.getPath().split(" -> ");
         if (parts.length == 2 &&
             relativeParts.length == 2 &&
             parts[0].endsWith(relativeParts[0]))
         {
            String oldPath = parts[0], newPath = parts[1];
            String root = oldPath.substring(0, oldPath.length() - relativeParts[0].length());
            path = root + newPath;
         }
      }
      return path;
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
