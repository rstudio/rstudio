/*
 * RInfixData.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class RInfixData extends JavaScriptObject
{
   protected RInfixData() {}
   
   public static native final RInfixData create() /*-{
      return {
         "name": "",
         "additionalArgs": [],
         "excludeArgs": [],
         "excludeArgsFromObject": false
      };
   }-*/;
   
   public native final String getDataName() /*-{
      return this.name;
   }-*/;
   
   public native final JsArrayString getAdditionalArgs() /*-{
      return this.additionalArgs;
   }-*/;
   
   public native final JsArrayString getExcludeArgs() /*-{
      return this.excludeArgs;
   }-*/;
   
   public native final boolean getExcludeArgsFromObject() /*-{
      return this.excludeArgsFromObject;
   }-*/;

}
