/*
 * CodeModel.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;

public class CodeModel extends JavaScriptObject
{
   protected CodeModel() {}

   public native final String getLanguageMode(Position position) /*-{
      if (!this.getLanguageMode)
         return null;
      return this.getLanguageMode(position);
   }-*/;

   public native final Scope getCurrentScope(Position position) /*-{
      if (!this.getCurrentScope)
         return null;
      return this.getCurrentScope(position);
   }-*/;

   public native final Scope getCurrentChunk(Position position) /*-{
      if (!this.getCurrentScope)
         return null;
      return this.getCurrentScope(position, function(scope) {
         return scope.isChunk();
      });
   }-*/;

   public native final Scope getCurrentFunction(Position position) /*-{
      if (!this.getCurrentScope)
         return null;
      return this.getCurrentScope(position, function(scope) {
         return scope.isFunction() && scope.label;
      });
   }-*/;

   public native final JsArray<Scope> getScopeTree() /*-{
      return this.getScopeTree ? this.getScopeTree() : [];
   }-*/;

   public native final Scope findFunctionDefinitionFromUsage(
         Position usagePos, String functionName) /*-{
      if (this.findFunctionDefinitionFromUsage != null)
         return this.findFunctionDefinitionFromUsage(usagePos, functionName);
      else
         return null;
   }-*/;

}
