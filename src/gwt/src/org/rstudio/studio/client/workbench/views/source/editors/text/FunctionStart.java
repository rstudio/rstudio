/*
 * FunctionStart.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class FunctionStart extends JavaScriptObject
{
   protected FunctionStart()
   {}

   public native final String getLabel() /*-{
      return this.label;
   }-*/;
   
   public final boolean isTopLevel()
   {
      return getLabel().equals("(Top Level)");
   }
   
   public final boolean isChunk()
   {
      return getLabel().startsWith("Chunk ");
   }

   public native final Position getPreamble() /*-{
      return this.preamble;
   }-*/;

   public native final Position getEnd() /*-{
      return this.end;
   }-*/;

   public native final JsArray<FunctionStart> getChildren() /*-{
      return this.children;
   }-*/;

}
