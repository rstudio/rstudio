/*
 * DragDropReceiver.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
package org.rstudio.core.client;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;

import elemental2.core.JsArray;
import elemental2.dom.DataTransfer;
import elemental2.dom.File;
import elemental2.dom.FileList;
import jsinterop.base.Js;

public abstract class DragDropReceiver
{
   public abstract void onDrop(NativeEvent event);
   
   public void onDragOver(NativeEvent event) {}
   public void onDragLeave(NativeEvent event) {}
   
   public DragDropReceiver(Widget host)
   {
      addDragDropHandlers(this, host.getElement());
   }
   
   public boolean handleDroppedFiles(NativeEvent event)
   {
      DataTransfer data = Js.cast(event.getDataTransfer());
      JsArray<String> types = data.types;
      if (types.length != 1)
         return false;
      
      String type = types.getAt(0);
      if (!StringUtil.equals(type, "Files"))
         return false;
      
      event.stopPropagation();
      event.preventDefault();
      
      FileTypeRegistry registry = RStudioGinjector.INSTANCE.getFileTypeRegistry();
      
      FileList fileList = data.files;
      for (int i = 0; i < fileList.length; i++)
      {
         File file = Js.cast(fileList.getAt(i));
         String path = Desktop.getFrame().getPathForFile(file);
         FileSystemItem item = FileSystemItem.createFile(path);
         FileType fileType = registry.getTypeForFile(item);
         if (fileType != null && fileType instanceof TextFileType)
            registry.editFile(item);
      }
      
      return true;
   }
   
   private static final native void addDragDropHandlers(DragDropReceiver self,
                                                        Element el)
   /*-{
      
      el.addEventListener("dragover", function(event) {
         event.preventDefault();
         self.@org.rstudio.core.client.DragDropReceiver::onDragOver(*)(event);
      });
      
      el.addEventListener("dragleave", function(event) {
         event.preventDefault();
         self.@org.rstudio.core.client.DragDropReceiver::onDragLeave(*)(event);
      });
      
      el.addEventListener("drop", function(event) {
         event.preventDefault();
         self.@org.rstudio.core.client.DragDropReceiver::onDrop(*)(event);
      });
   
   }-*/;
}
