/*
 * NotebookDoc.java
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
package org.rstudio.studio.client.rmarkdown.model;

import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class NotebookDoc extends JavaScriptObject
{
   protected NotebookDoc()
   {
   }
   
   public final native JsArray<ChunkDefinition> getChunkDefs() /*-{
      return this.chunk_definitions || [];
   }-*/;
   
   public final native RmdChunkOptions getDefaultOptions() /*-{
      return this.default_chunk_options || {};
   }-*/;
   
   public final native int getChunkRenderedWidth() /*-{
      return this.chunk_rendered_width || 0;
   }-*/;
   
   public final native int getDocWriteTime() /*-{
      return this.doc_write_time || 0;
   }-*/;
   
   // returns any manually specified working directory (i.e. from the knitr
   // root.dir option)
   public final native String getWorkingDir() /*-{
      return this.working_dir || "";
   }-*/;
   
   public final native void setChunkDefs(JsArray<ChunkDefinition> defs) /*-{
      this.chunk_definitions = defs;
   }-*/;

   public final static String CHUNK_RENDERED_WIDTH = "chunk_rendered_width";
}
