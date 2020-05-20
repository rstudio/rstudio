/*
 * RmdChunkOptions.java
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
package org.rstudio.studio.client.rmarkdown.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RmdChunkOptions extends JavaScriptObject
{
   protected RmdChunkOptions()
   {
   }
   
   public final static native RmdChunkOptions create() /*-{
      return {};
   }-*/;
   
   public final native boolean eval() /*-{
      if (typeof(this.eval) !== "undefined")
        return !!this.eval;
      return true;
   }-*/;
   
   public final native boolean error() /*-{
      if (typeof(this.error) !== "undefined")
        return !!this.error;
      return false;
   }-*/;

   public final native boolean include() /*-{
      if (typeof(this.include) !== "undefined")
        return !!this.include;
      return true;
   }-*/;
   
   public final native void setInclude(boolean include) /*-{
      this.include = include;
   }-*/;
   
   public final boolean equalTo(RmdChunkOptions other)
   {
      return eval()    == other.eval() &&
             error()   == other.error() &&
             include() == other.include();
   }
}
