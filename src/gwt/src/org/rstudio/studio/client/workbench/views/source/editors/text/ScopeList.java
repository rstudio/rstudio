/*
 * ScopeList.java
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

import com.google.gwt.core.client.JsArray;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import java.util.ArrayList;

public class ScopeList
{
   public interface ScopePredicate
   {
      boolean test(Scope scope);
   }

   public static class ContainsPredicate implements ScopePredicate
   {
      public ContainsPredicate(Range range)
      {
         range_ = range;
      }

      @Override
      public boolean test(Scope scope)
      {
         return scope.getPreamble().isBeforeOrEqualTo(range_.getStart()) &&
                scope.getEnd().isAfterOrEqualTo(range_.getStart()) &&
                scope.getPreamble().isBeforeOrEqualTo(range_.getEnd()) &&
                scope.getEnd().isAfterOrEqualTo(range_.getEnd());
      }

      private final Range range_;
   }

   public static final ScopePredicate CHUNK = new ScopePredicate()
   {
      @Override
      public boolean test(Scope scope)
      {
         return scope.isChunk();
      }
   };

   public static final ScopePredicate FUNC = new ScopePredicate()
   {
      @Override
      public boolean test(Scope scope)
      {
         return scope.isBrace() && !StringUtil.isNullOrEmpty(scope.getLabel());
      }
   };

   public static final ScopePredicate ANON_BRACE = new ScopePredicate()
   {
      @Override
      public boolean test(Scope scope)
      {
         return scope.isBrace() && StringUtil.isNullOrEmpty(scope.getLabel());
      }
   };

   public ScopeList(DocDisplay docDisplay)
   {
      addScopes(docDisplay.getScopeTree());
   }

   public Scope[] getScopes()
   {
      return scopes_.toArray(new Scope[scopes_.size()]);
   }

   public void removeAll(ScopePredicate shouldRemove)
   {
      for (int i = 0; i < scopes_.size(); i++)
         if (shouldRemove.test(scopes_.get(i)))
            scopes_.remove(i--);
   }

   public void selectAll(ScopePredicate shouldRetain)
   {
      for (int i = 0; i < scopes_.size(); i++)
         if (!shouldRetain.test(scopes_.get(i)))
            scopes_.remove(i--);
   }

   public Scope findFirst(ScopePredicate predicate)
   {
      for (Scope scope : scopes_)
         if (predicate.test(scope))
            return scope;
      return null;
   }

   public Scope findLast(ScopePredicate predicate)
   {
      for (int i = scopes_.size() - 1; i >= 0; i--)
         if (predicate.test(scopes_.get(i)))
            return scopes_.get(i);
      return null;
   }

   private void addScopes(JsArray<Scope> scopes)
   {
      for (Scope scope : JsUtil.asIterable(scopes))
      {
         scopes_.add(scope);
         addScopes(scope.getChildren());
      }
   }

   private final ArrayList<Scope> scopes_ = new ArrayList<Scope>();
}
