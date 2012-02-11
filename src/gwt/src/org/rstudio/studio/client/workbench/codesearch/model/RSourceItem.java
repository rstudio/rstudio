/*
 * RSourceItem.java
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
package org.rstudio.studio.client.workbench.codesearch.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;


public class RSourceItem extends JavaScriptObject
{
   protected RSourceItem()
   {
   }
   
   public static final int NONE = 0;
   public static final int FUNCTION = 1;
   public static final int METHOD = 2;
   public static final int CLASS = 3;

   public final native int getType() /*-{
      return this.type;
   }-*/;
   
   public final native String getFunctionName() /*-{
      return this.name;
   }-*/;

   public final native JsArray<RS4MethodParam> getSignature() /*-{
      return this.signature;
   }-*/;

   // project-relative filename
   public final native String getContext() /*-{
      return this.context;
   }-*/;
   
   public final native int getLine() /*-{
      return this.line;
   }-*/;
   
   public final native int getColumn() /*-{
      return this.column;
   }-*/;

   public final CodeNavigationTarget toCodeNavigationTarget()
   {
      return new CodeNavigationTarget(getContext(),
                                      FilePosition.create(getLine(),
                                                          getColumn()));
   }
}
