/*
 * Scope.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class Scope extends JavaScriptObject
{
   protected Scope()
   {}
   
   public static final native Scope createRScopeNode(String label,
                                                     Position start,
                                                     Position end,
                                                     int scopeType)
   /*-{
      var ScopeNode = $wnd.require("mode/r_scope_tree").ScopeNode;
      var node = new ScopeNode(label, start, start, scopeType);
      node.end = end;
      return node;
   }-*/;

   public native final String getLabel() /*-{
      return this.label;
   }-*/;

   public native final boolean isTopLevel() /*-{
      return this.isRoot();
   }-*/;

   public native final boolean isBrace() /*-{
      return this.isBrace();
   }-*/;

   public native final boolean isChunk() /*-{
      return this.isChunk();
   }-*/;

   public native final boolean isSection() /*-{
      return this.isSection();
   }-*/;

   public native final boolean isFunction() /*-{
      return this.isFunction();
   }-*/;

   public native final Scope getParentScope() /*-{
      return this.parentScope;
   }-*/;

   /**
    * For named functions, the preamble points to the beginning of the function
    * declaration, including function name. For chunks, it points to the
    * beginning of the chunk itself. For other scopes, it just points to the
    * opening brace (same as getBodyStart).
    */
   public native final Position getPreamble() /*-{
      return this.preamble;
   }-*/;

   /**
    * Points to the start of the body of the scope. Note that for named
    * functions, chunks, and sections, this is different than the preamble.
    */
   public native final Position getBodyStart() /*-{
      return this.start;
   }-*/;

   /**
    * Points to the part of a scope where a fold would begin.
    */
   public final Position getFoldStart()
   {
      if (isFunction())
         return getBodyStart();
      else
         return getPreamble();

   }

   public native final Position getEnd() /*-{
      return this.end;
   }-*/;

   public native final JsArray<Scope> getChildren() /*-{
      return this.$children;
   }-*/;

   public native final String getChunkLabel() /*-{
      return this.chunkLabel;
   }-*/;
   
   public native final boolean isClass() /*-{
      return typeof this.isClass !== "undefined" && this.isClass();
   }-*/;
   
   public native final boolean isNamespace() /*-{
      return typeof this.isNamespace !== "undefined" && this.isNamespace();
   }-*/;
   
   public native final boolean isLambda() /*-{
      return typeof this.isLambda !== "undefined" && this.isLambda();
   }-*/;
   
   public native final boolean isAnon() /*-{
      return typeof this.isAnon !== "undefined" && this.isAnon();
   }-*/;
   
   public native final boolean isMarkdownHeader() /*-{
      return this.attributes && this.attributes.isMarkdown === true;
   }-*/;
   
   public native final boolean isYaml() /*-{
      return this.attributes && this.attributes.isYaml === true;
   }-*/;
   
   public native final int getDepth() /*-{
      return this.attributes && this.attributes.depth || 0;
   }-*/;

   public static final int SCOPE_TYPE_ROOT    = 1;
   public static final int SCOPE_TYPE_BRACE   = 2;
   public static final int SCOPE_TYPE_CHUNK   = 3;
   public static final int SCOPE_TYPE_SECTION = 4;
}
