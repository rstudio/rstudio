/*
 * PanmirrorZoteroCollectionSpec.java
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


package org.rstudio.studio.client.panmirror.server;

import com.google.gwt.core.client.JavaScriptObject;


public class PanmirrorZoteroCollectionSpec extends JavaScriptObject
{
   protected PanmirrorZoteroCollectionSpec() {}
   
   public static native PanmirrorZoteroCollectionSpec create(String name, int version) /*-{
      return {name: name, version: version};
   }-*/;

   public native final String getName() /*-{
      return this.name;
   }-*/;
   
   public native final int getVersion() /*-{
      return this.version;
   }-*/;
   
   public native final String getKey() /*-{
      return this.key;
   }-*/;
   
   public native final String getParentKey() /*-{
      return this.parentKey;
   }-*/;

}

