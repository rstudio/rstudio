/*
 * ImmediatelyInvokedFunctionExpression.java
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
package org.rstudio.core.client;

// The Java analogue of an immediately invoked function expression.
// Useful when you require a small anonymous class that hides some state,
// but don't want to promote that to a named class outside of the caller's
// scope.
public abstract class ImmediatelyInvokedFunctionExpression
{
   protected abstract void invoke();
   
   public ImmediatelyInvokedFunctionExpression()
   {
      invoke();
   }
}
