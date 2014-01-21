/*
 * ProfilerContents.java
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
package org.rstudio.studio.client.workbench.views.source.editors.profiler.model;

import java.util.HashMap;

import com.google.gwt.core.client.JavaScriptObject;

public class ProfilerContents extends JavaScriptObject
{
   protected ProfilerContents()
   {
   }

   public static final ProfilerContents createDefault()
   {
      return create(10, true);
   }
   
   public static final native ProfilerContents create(int propA,
                                                      boolean propB) /*-{
      var contents = new Object();
      contents.prop_a = propA.toString();
      contents.prop_b = propB.toString();
      return contents ;
   }-*/;
   
   
   public final int getPropA()
   {
      return Integer.parseInt(getPropAString());
   };
   
   public final boolean getPropB()
   {
      return Boolean.parseBoolean(getPropBString());
   };
   
   public final boolean equalTo(ProfilerContents other)
   {
      return getPropA() == other.getPropA() &&
             getPropB() == other.getPropB();
   }
   
   public final void fillProperties(HashMap<String, String> properties)
   {
      properties.put("prop_a", getPropAString());
      properties.put("prop_b", getPropBString());
   }
   
   private native final String getPropAString() /*-{
      return this.prop_a;
   }-*/;

   private native final String getPropBString() /*-{
      return this.prop_b;
   }-*/;
   
   
}
