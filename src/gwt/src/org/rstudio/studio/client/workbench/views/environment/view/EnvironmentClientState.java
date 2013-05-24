/*
 * EnvironmentClientState.java
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

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import java.util.ArrayList;

public class EnvironmentClientState extends JavaScriptObject
{
   protected EnvironmentClientState()
   {
   }

   public static final native EnvironmentClientState create(
           int scrollPosition,
           String[] expandedObjects) /*-{
       var options = new Object();
       options.scrollPosition = scrollPosition;
       options.expandedObjects = expandedObjects;
       return options ;
   }-*/;


   public final native int getScrollPosition() /*-{
       return this.scrollPosition;
   }-*/;

   public final native JsArrayString getExpandedObjects() /*-{
       return this.expandedObjects;
   }-*/;

   public static native boolean areEqual(EnvironmentClientState a,
                                         EnvironmentClientState b) /*-{
       if (a === null ^ b === null)
           return false;
       if (a === null)
           return true;
       return (a.scrollPosition === b.scrollPosition
               && a.expandedObjects.toString() === b.expandedObjects.toString());
   }-*/;
}
