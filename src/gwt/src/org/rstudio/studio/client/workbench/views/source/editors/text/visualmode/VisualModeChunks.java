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
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunk;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunkFactory;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;

public class VisualModeChunks
{
   public PanmirrorUIChunkFactory uiChunkFactory()
   {
      PanmirrorUIChunkFactory factory = new PanmirrorUIChunkFactory();
      factory.createChunkEditor = () -> {

         PanmirrorUIChunk chunk = new PanmirrorUIChunk();

         // Create a new AceEditor instance and allow access to the underlying
         // native JavaScript object it represents (AceEditorNative)
         final AceEditor editor = new AceEditor();
         chunk.editor = editor.getWidget().getEditor();

         // Provide the editor's container element; in the future this will be a
         // host element which hosts chunk output
         DivElement ele = Document.get().createDivElement();
         ele.appendChild(chunk.editor.getContainer());
         chunk.element = ele;
         
         // Provide a callback to set the file's mode; this needs to happen in
         // GWT land since the editor accepts GWT-flavored Filetype objects
         chunk.setMode = (String mode) -> {
            setMode(editor, mode);
         };
         
         // Turn off line numbers as they're not helpful in chunks
         chunk.editor.getRenderer().setShowGutter(false);

         // Allow the editor's size to be determined by its content (these
         // settings trigger an auto-growing behavior), up to a max of 1000
         // lines.
         chunk.editor.setMaxLines(1000);
         chunk.editor.setMinLines(1);

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
      case "yaml-frontmatter":
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
      case "theorem":
      case "lemma":
      case "corollary":
      case "proposition":
      case "conjecture":
      case "definition":
      case "example":
      case "exercise":
         // These are Bookdown theorem types
         editor.setFileType(FileTypeRegistry.TEX);
      default:
         editor.setFileType(FileTypeRegistry.TEXT);
         break;
      }
   }
}
