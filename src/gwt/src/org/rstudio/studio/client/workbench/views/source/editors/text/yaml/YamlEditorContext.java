/*
 * YamlEditorContext.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.source.editors.text.yaml;

import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.DocumentMode.Mode;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.EditorBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import com.google.gwt.core.client.JavaScriptObject;

public class YamlEditorContext extends JavaScriptObject
{
   public static final String FILETYPE_YAML = "yaml";
   public static final String FILETYPE_MARKDOWN = "markdown";
   public static final String FILETYPE_SCRIPT = "script";
   
   protected YamlEditorContext()
   {
   }
   
   public static YamlEditorContext create(boolean explicit,
                                          CompletionContext context,
                                          DocDisplay docDisplay)
   {
      // determine file type
      String filetype = null;
      
      // yaml source file
      if (docDisplay.getFileType().isYaml())
      {
         filetype = FILETYPE_YAML;
      }
      // code chunk embedded in visual editor is either yaml or a script (i.e.
      // R or Python code that might have yaml in its comments)
      else if (docDisplay.getEditorBehavior() == EditorBehavior.AceBehaviorEmbedded)
      {
         if (DocumentMode.getModeForCursorPosition(docDisplay) == Mode.YAML)
            filetype = FILETYPE_YAML;
         else
            filetype = FILETYPE_SCRIPT;
      }
      // otherwise we consider this markdown (i.e. a mixed mode document that 
      // may have embedded yaml front matter and embedded code chunks
      else 
      {
         filetype = FILETYPE_MARKDOWN;
      }
      
      return create(
        context.getPath(),
        filetype,
        docDisplay.getEditorBehavior() == EditorBehavior.AceBehaviorEmbedded,
        context.getQuartoFormats(),
        context.getQuartoProjectFormats(),
        context.getQuartoEngine(),
        docDisplay.getCurrentLineUpToCursor(),
        docDisplay.getCode(),
        docDisplay.getCursorPosition(),
        explicit
      );
   }
   
   private static native YamlEditorContext create(
     String path, String filetype, boolean embedded,
     String[] formats, String[] projectFormats, String engine,
     String line, String code, Position position, boolean explicit) /*-{
     return { 
        path: path,
        filetype: filetype,
        embedded: embedded,
        formats: formats,
        project_formats: projectFormats,
        engine: engine,
        line: line, 
        code: code,
        position: position,
        explicit: explicit,
        client: "rstudio"
     };
  }-*/;
   
   public native final String getPath() /*-{
      return this.path;
   }-*/;
   
   public native final String getFiletype() /*-{
      return this.filetype;
   }-*/;
   
   public native final boolean getEmbedded() /*-{
      return this.embedded;
   }-*/;
   
   public native final String getLine() /*-{
      return this.line;
   }-*/;
   
   public native final String getCode() /*-{
      return this.code;
   }-*/;
   
   public native final Position getPosition() /*-{
      return this.position;
   }-*/;
   
   public native final boolean getExplicit() /*-{
      return this.explicit;
   }-*/;
   
}
