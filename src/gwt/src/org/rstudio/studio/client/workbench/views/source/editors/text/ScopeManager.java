/*
 * ScopeManager.java
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

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import com.google.gwt.core.client.JsArray;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "AceSupport")
public class ScopeManager
{
   public final native Position getParsePosition();
   public final native void setParsePosition(Position position);
   public final native void onSectionStart(String label, Position position);
   public final native void onSectionEnd(Position position);
   public final native void onChunkStart(String chunkLabel, String label, Position chunkStartPos, Position chunkPos);
   public final native void onChunkEnd(Position position);
   public final native void onNamedScopeStart(String label, Position position);
   public final native void onScopeStart(Position position);
   public final native void onScopeEnd(Position position);
   public final native JsArray<Scope> getActiveScopes(Position position);
   public final native JsArray<Scope> getScopeList();
   public final native Position invalidateFrom(Position position);
   
   @JsOverlay
   public final Scope getScopeAt(Position position)
   {
      JsArray<Scope> scopes = getActiveScopes(position);
      return scopes.get(scopes.length() - 1);
   }
}
