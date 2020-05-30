/*
 * CompileNotebookv2Prefs.java
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

package org.rstudio.studio.client.notebookv2;


import com.google.gwt.core.client.JavaScriptObject;

public class CompileNotebookv2Prefs extends JavaScriptObject
{  
   protected CompileNotebookv2Prefs() {}
   
   public static final CompileNotebookv2Prefs createDefault()
   {
      return create(CompileNotebookv2Options.FORMAT_DEFAULT);
   }
   
   public static final native CompileNotebookv2Prefs create(String format)
         
   /*-{
      var prefs = new Object();
      prefs.format = format;
      return prefs;
   }-*/;
   
   public native final String getFormat() /*-{
      return this.format;
   }-*/;
  
   public static native boolean areEqual(CompileNotebookv2Prefs a, 
                                         CompileNotebookv2Prefs b) /*-{
      if (a === null ^ b === null)
         return false;
      if (a === null)
         return true;
      return a.format === b.format;    
   }-*/;
   
}
