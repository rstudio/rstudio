/*
 * CompileNotebookPrefs.java
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

package org.rstudio.studio.client.notebook;


import com.google.gwt.core.client.JavaScriptObject;

public class CompileNotebookPrefs extends JavaScriptObject
{  
   protected CompileNotebookPrefs() {}
   
   public static final CompileNotebookPrefs createDefault()
   {
      return create("", CompileNotebookOptions.TYPE_DEFAULT);
   }
   
   public static final native CompileNotebookPrefs create(String author,
                                                          String type)
         
   /*-{
      var prefs = new Object();
      prefs.author = author;
      prefs.type = type;
      return prefs;
   }-*/;
   
   public native final String getAuthor() /*-{
      return this.author;
   }-*/;
 
   public native final String getType() /*-{
      return this.type;
   }-*/;
  
   public static native boolean areEqual(CompileNotebookPrefs a, 
                                         CompileNotebookPrefs b) /*-{
      if (a === null ^ b === null)
         return false;
      if (a === null)
         return true;
      return a.author === b.author &&
             a.type === b.type;    
   }-*/;
   
}
