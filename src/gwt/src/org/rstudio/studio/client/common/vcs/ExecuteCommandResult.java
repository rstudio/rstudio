/*
 * ExecuteCommandResult.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class ExecuteCommandResult extends JavaScriptObject
{
   protected ExecuteCommandResult()
   {}

   public native final String getOutput() /*-{
      return this.output;
   }-*/;

   public final boolean isError()
   {
      return isErrorNative() != 0;
   }

   private native int isErrorNative() /*-{
      return this.error ? 1 : 0;
   }-*/;
}
