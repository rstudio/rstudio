/*
 * CompileOutput.java
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

   public static native CompileOutput create(int type, String output) /*-{
      var compileOutput = new Object();
      compileOutput.type = type;
      compileOutput.output = output;
      return compileOutput;
   }-*/; 
   
   public native final int getType() /*-{
      return this.type;
   }-*/;

   public native final String getOutput() /*-{
      return this.output;
   }-*/;
}
