/*
 * CodeModel.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.workbench.views.source.editors.text.RFunction;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeFunction;

public class CodeModel extends JavaScriptObject
{
   protected CodeModel() {}

   public native final boolean hasScopes() /*-{
     if (this.getCurrentScope)
        return true;
     else
        return false;
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

   public native final ScopeFunction getCurrentFunction(Position position,
                                                        boolean allowAnonymous) /*-{
      if (!this.getCurrentScope)
         return null;
      return this.getCurrentScope(position, function(scope) {
         return scope.isBrace() && scope.label &&
                (allowAnonymous || scope.label.indexOf("<function>") === -1);
      });
   }-*/;

   public native final Scope getCurrentSection(Position position) /*-{
      if (!this.getCurrentScope)
         return null;
      return this.getCurrentScope(position, function(scope) {
         return scope.isSection();
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
   
   public native final Position findNextSignificantToken(Position pos) /*-{
      // Used to seek past whitespace and comments to find an expression for
      // breakpoint setting. Use the code model's findNextSignificantToken
      // method if available; if not, this is a no-op.
      if (this.findNextSignificantToken)
         return this.findNextSignificantToken(pos);
      else
         return pos;
   }-*/;
   
   public final TokenCursor getTokenCursor()
   {
      return TokenCursor.create(this);
   }
   
   public native final boolean tokenizeUpToRow(int row) /*-{
      this.$tokenizeUpToRow(row);
   }-*/;
   
   public native final JsArray<ScopeFunction> getAllFunctionScopes() /*-{
      return this.getAllFunctionScopes() || [];
   }-*/;
   
   public native final JsArray<ScopeFunction> getAllFunctionScopes(int row) /*-{
      return this.getAllFunctionScopes(row) || [];
   }-*/;
   
   public native final JsArray<RFunction> getFunctionsInScope(Position position) /*-{
      return this.getFunctionsInScope(position) || [];
   }-*/;
   
   public native final JsArray<RScopeObject> getVariablesInScope(Position position) /*-{
      return this.getVariablesInScope(position) || [];
   }-*/;
   
   public native final RInfixData getDataFromInfixChain(TokenCursor tokenCursor) /*-{
      return this.getDataFromInfixChain(tokenCursor);
   }-*/;
   
   public native final DplyrJoinContext getDplyrJoinContextFromInfixChain(TokenCursor tokenCursor) /*-{
      return this.getDplyrJoinContextFromInfixChain(tokenCursor);
   }-*/;
   
   public native final void insertRoxygenSkeleton() /*-{
      this.insertRoxygenSkeleton && this.insertRoxygenSkeleton();
   }-*/;
   
   public native final int buildScopeTreeUpToRow(int row) /*-{
      if (typeof this.$buildScopeTreeUpToRow !== "function")
         return 0;
      return this.$buildScopeTreeUpToRow(row);
   }-*/;
   
}
