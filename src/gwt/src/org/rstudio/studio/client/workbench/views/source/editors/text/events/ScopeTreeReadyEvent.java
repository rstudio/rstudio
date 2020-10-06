/*
 * ScopeTreeReadyEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ScopeTreeReadyEvent extends GwtEvent<ScopeTreeReadyEvent.Handler>
{
   public ScopeTreeReadyEvent(JsArray<Scope> scopeTree,
                              Scope currentScope)
   {
      scopeTree_ = scopeTree;
      currentScope_ = currentScope;
   }

   public JsArray<Scope> getScopeTree() { return scopeTree_; }
   public Scope getCurrentScope() { return currentScope_; }

   private final JsArray<Scope> scopeTree_;
   private final Scope currentScope_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onScopeTreeReady(ScopeTreeReadyEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onScopeTreeReady(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
