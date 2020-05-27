/*
 * PackageUpdate.java
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
package org.rstudio.studio.client.workbench.views.packages.model;

import org.rstudio.studio.client.packrat.model.PackratPackageAction;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class PackratActions extends JavaScriptObject
{
   protected PackratActions()
   {
   }

   public final native JsArray<PackratPackageAction> getRestoreActions() /*-{
      return this.restore_actions ? this.restore_actions : [];
   }-*/;

   public final native JsArray<PackratPackageAction> getSnapshotActions() /*-{
      return this.snapshot_actions ? this.snapshot_actions : [];
   }-*/;
}
