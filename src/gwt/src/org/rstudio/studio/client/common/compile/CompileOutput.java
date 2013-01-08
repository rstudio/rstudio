/*
 * CompileOutput.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.common.compile;

import com.google.gwt.core.client.JavaScriptObject;

public class CompileOutput extends JavaScriptObject
{ 
   protected CompileOutput()
   {
   }
   
   public static final int kCommand = 0;
   public static final int kNormal = 1;
   public static final int kError = 2;

   public native final int getType() /*-{
      return this.type;
   }-*/;

   public native final String getOutput() /*-{
      return this.output;
   }-*/;
}
