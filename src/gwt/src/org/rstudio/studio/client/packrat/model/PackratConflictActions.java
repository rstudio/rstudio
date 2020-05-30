/*
 * PackratConflictActions.java
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
package org.rstudio.studio.client.packrat.model;


import com.google.gwt.core.client.JavaScriptObject;

public class PackratConflictActions extends JavaScriptObject
{
   public final static native PackratConflictActions create(
                                          String packageName,
                                          String snapshotAction,
                                          String libraryAction) /*-{
        
      return {
         package_name: packageName,
         snapshot_action: snapshotAction,
         library_action: libraryAction
      };                                        
   }-*/;
   
   protected PackratConflictActions()
   {
   }
   
   public final native String getPackage() /*-{
      return this.package_name;
   }-*/;
 
   public final native String getSnapshotAction() /*-{
      return this.snapshot_action;
   }-*/;
   
   public final native String getLibraryAction() /*-{
      return this.library_action;
   }-*/;
   
   
   
   
}
