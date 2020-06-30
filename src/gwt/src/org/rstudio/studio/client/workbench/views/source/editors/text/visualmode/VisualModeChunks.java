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

import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunk;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunkFactory;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;

public class VisualModeChunks
{
   public PanmirrorUIChunkFactory uiChunkFactory()
   {
      PanmirrorUIChunkFactory factory = new PanmirrorUIChunkFactory();
      factory.createChunkEditor = () -> {
         PanmirrorUIChunk chunk = new PanmirrorUIChunk();
         DivElement ele = Document.get().createDivElement(); 
         
         chunk.editor = AceEditorNative.createEditor(ele);
         chunk.element = ele;
         
         chunk.editor.setMaxLines(1000);
         chunk.editor.setMinLines(3);

         return chunk;
      };
      return factory;
   }
}
