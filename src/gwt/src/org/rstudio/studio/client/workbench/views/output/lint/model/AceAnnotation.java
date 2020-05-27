/*
 * AceAnnotation.java
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
package org.rstudio.studio.client.workbench.views.output.lint.model;

import com.google.gwt.core.client.JavaScriptObject;

public class AceAnnotation extends JavaScriptObject
{
   protected AceAnnotation() {}
   
   public static native AceAnnotation create(int row,
                                             int column,
                                             String text,
                                             String type) /*-{
      return {
         row: row,
         column: column,
         text: text,
         type: type
      }
   }-*/;
   
   public final native int row() /*-{ return this.row; }-*/;
   public final native int column() /*-{ return this.column; }-*/;
   public final native String text() /*-{ return this.text; }-*/;
   public final native String type() /*-{ return this.type; }-*/;
}
