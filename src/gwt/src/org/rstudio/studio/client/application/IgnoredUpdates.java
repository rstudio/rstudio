/*
 * IgnoredUpdates.java
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
         if (compareVersions(update, existingUpdateList.get(i)) < 0)
         {
            newUpdateList.push(existingUpdateList.get(i));
         }
      }
      newUpdateList.push(update);
      setIgnoredUpdates(newUpdateList);
   }
   
   // Returns:
   // < 0 if version1 is earlier than version 2
   // 0 if version1 and version2 are the same 
   // > 0 if version1 is later than version 2
   private final int compareVersions(String version1, String version2)
   {
      String[] v1parts = version1.split(".");
      String[] v2parts = version2.split(".");
      int numParts = Math.min(v1parts.length, v2parts.length);
      for (int i = 0; i < numParts; i++)
      {
         int result = Integer.parseInt(v1parts[i]) - 
                      Integer.parseInt(v2parts[i]);
         if (result != 0)
            return result;
      }
      return 0;
   }
}
