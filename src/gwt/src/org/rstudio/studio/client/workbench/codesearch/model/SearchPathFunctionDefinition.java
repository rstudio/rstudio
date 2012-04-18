/*
 * SearchPathFunctionDefinition.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.codesearch.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;


public class SearchPathFunctionDefinition extends JavaScriptObject
{
   protected SearchPathFunctionDefinition()
   {
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getNamespace() /*-{
      return this.namespace;
   }-*/;
   
   public final native String getCode() /*-{
      return this.code;
   }-*/;
   
   public final native JsArrayString getMethods() /*-{
      return this.methods;
   }-*/;
    
   public final native boolean isCodeFromSrcAttrib() /*-{
      return this.from_src_attrib;
   }-*/;
}
