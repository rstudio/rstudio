/*
 * ScopeManager.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ScopeManager extends JavaScriptObject
{
   protected ScopeManager()
   {
   }
   
   public static final native ScopeManager create()
   /*-{
      var module = $wnd.require("mode/r_scope_tree");
      var ScopeManager = module.ScopeManager;
      var ScopeNode    = module.ScopeNode;
      return new ScopeManager(ScopeNode);
   }-*/;
   
   public final native Position getParsePosition()
   /*-{
      return this.parsePos;
   }-*/;
   
   public final native void setParsePosition(Position position)
   /*-{
      this.parsePos = position;
   }-*/;
   
   public final native void onSectionStart(String label, Position position)
   /*-{
      this.onSectionHead(label, position);
   }-*/;
   
   public final native void onSectionEnd(Position position)
   /*-{
      this.onSectionEnd(position);
   }-*/;
   
   public final native void onChunkStart(String chunkLabel,
                                         String label,
                                         Position chunkStartPos,
                                         Position chunkPos)
   /*-{
      this.onChunkStart(chunkLabel, label, chunkStartPos, chunkPos);
   }-*/;
   
   public final native void onChunkEnd(Position position)
   /*-{
      this.onChunkEnd(position);
   }-*/;
   
   public final native void onNamedScopeStart(String label, Position position)
   /*-{
      this.onNamedScopeStart(label, position);
   }-*/;
   
   public final native void onScopeStart(Position position)
   /*-{
      this.onScopeStart(position);
   }-*/;
   
   public final native void onScopeEnd(Position position)
   /*-{
      this.onScopeEnd(position);
   }-*/;
   
   public final native JsArray<Scope> getActiveScopes(Position position)
   /*-{
      return this.getActiveScopes(position);
   }-*/;
   
   public final native JsArray<Scope> getScopeList()
   /*-{
      return this.getScopeList();
   }-*/;
   
   public final native void invalidateFrom(Position position)
   /*-{
      this.invalidateFrom(position);
   }-*/;
   
   public final Scope getScopeAt(Position position)
   {
      JsArray<Scope> scopes = getActiveScopes(position);
      return scopes.get(scopes.length() - 1);
   }
}
