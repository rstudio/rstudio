/*
 * FunctionDefinition.java
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
package org.rstudio.studio.client.workbench.codesearch.model;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;

public class FunctionDefinition extends JavaScriptObject
{
   protected FunctionDefinition()
   {
      
   }

   /*
    *  Name of function -- can be null if no function token could be
    *  ascertained from the line and pos passed to the server
    */
   public final native String getFunctionName() /*-{
      return this.function_name;
   }-*/;
  
   /*
    *  File position where the definition of the function is. Can be null
    *  if no indexed file defining the function was found
    */
   public final native FileSystemItem getFile() /*-{
      return this.file;
   }-*/;
   public final native FilePosition getPosition()/*-{
      return this.position;
   }-*/;
   
   /*
    *  A definition of the function from the current search path. Can be null
    *  if the function was found in an indexed file
    */
   public final native SearchPathFunctionDefinition 
                                 getSearchPathFunctionDefinition() /*-{
      return this.search_path_definition;
   }-*/;
   
}
