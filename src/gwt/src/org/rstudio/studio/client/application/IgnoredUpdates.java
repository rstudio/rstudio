/*
 * IgnoredUpdates.java
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
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class IgnoredUpdates extends JavaScriptObject
{
   protected IgnoredUpdates() {}
   
   public static final native IgnoredUpdates create() /*-{
      return { updates: [] };
   }-*/;
   
   public final native JsArrayString getIgnoredUpdates() /*-{
      return this.updates;
   }-*/;
   
   public final native void setIgnoredUpdates(JsArrayString updates) /*-{
      this.updates = updates;
   }-*/;
   
   public final void addIgnoredUpdate(String update) 
   {
      JsArrayString newUpdateList = create().getIgnoredUpdates();
      JsArrayString existingUpdateList = getIgnoredUpdates();
      
      for (int i = 0; i < existingUpdateList.length(); i++)
      {
         // We want to discard any updates we're ignoring that are older than
         // the one we're ignoring now--i.e. if we're currently ignoring 
         // { 0.98.407, 0.99.440 }, and we were just asked to ignore 
         // 0.98.411, the new set should be { 0.98.411, 0.99.440 }. Do this by
         // only keeping updates in the list that are newer than the update 
         // we're about to add.
         if (ApplicationUtils.compareVersions(update, existingUpdateList.get(i)) < 0)
         {
            newUpdateList.push(existingUpdateList.get(i));
         }
      }
      newUpdateList.push(update);
      setIgnoredUpdates(newUpdateList);
   }
}
