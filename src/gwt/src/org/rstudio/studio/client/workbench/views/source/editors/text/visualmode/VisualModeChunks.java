/*
 * VisualModeChunks.java
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

import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.RFileType;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunk;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunkFactory;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class VisualModeChunks
{
   public PanmirrorUIChunkFactory uiChunkFactory()
   {
      PanmirrorUIChunkFactory factory = new PanmirrorUIChunkFactory();
      factory.createChunkEditor = () -> {

         PanmirrorUIChunk chunk = new PanmirrorUIChunk();

         final AceEditor editor = new AceEditor();
         chunk.editor = editor.getWidget().getEditor();
         chunk.element = chunk.editor.getContainer();
         
         chunk.setMode = (String mode) -> {
            setMode(editor, mode);
         };
         
         chunk.editor.setMaxLines(1000);
         chunk.editor.setMinLines(3);

         return chunk;
      };
      return factory;
   }
   
   private void setMode(AceEditor editor, String mode)
   {
      switch(mode)
      {
      case "r":
         editor.setFileType(FileTypeRegistry.R);
         break;
      case "python":
         editor.setFileType(FileTypeRegistry.PYTHON);
         break;
      case "js":
      case "javascript":
         editor.setFileType(FileTypeRegistry.JS);
         break;
      case "tex":
      case "latex":
         editor.setFileType(FileTypeRegistry.TEX);
         break;
      case "c":
         editor.setFileType(FileTypeRegistry.C);
         break;
      case "cpp":
         editor.setFileType(FileTypeRegistry.CPP);
         break;
      case "sql":
         editor.setFileType(FileTypeRegistry.SQL);
         break;
      case "yaml":
         editor.setFileType(FileTypeRegistry.YAML);
         break;
      case "java":
         editor.setFileType(FileTypeRegistry.JAVA);
         break;
      case "html":
         editor.setFileType(FileTypeRegistry.HTML);
         break;
      case "shell":
      case "bash":
         editor.setFileType(FileTypeRegistry.SH);
         break;
      default:
         editor.setFileType(FileTypeRegistry.TEXT);
         break;
      }
   }
}
