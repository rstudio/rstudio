/*
 * HistoryPrefs.java
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;

public class HistoryPrefs extends JavaScriptObject
{
   protected HistoryPrefs() {}

   public static final native HistoryPrefs create(boolean alwaysSave, 
                                                  boolean removeDuplicates) /*-{
      var prefs = new Object();
      prefs.always_save = alwaysSave;
      prefs.remove_duplicates = removeDuplicates;
      return prefs ;
   }-*/;

   
   public native final boolean getAlwaysSave() /*-{
      return this.always_save;
   }-*/;
   
   public native final boolean getRemoveDuplicates() /*-{
      if (this.remove_duplicates === undefined)
         return false;
      else
         return this.remove_duplicates;
   }-*/;
}
