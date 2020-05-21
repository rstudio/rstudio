/*
 * TexCapabilities.java
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
package org.rstudio.studio.client.workbench.model;

import org.rstudio.studio.client.common.rnw.RnwWeave;

import com.google.gwt.core.client.JavaScriptObject;

public class TexCapabilities extends JavaScriptObject
{
   protected TexCapabilities()
   {
   }
   
   public final native boolean isTexInstalled() /*-{
      return this.tex_installed;
   }-*/;
   
   public final boolean isRnwWeaveAvailable(RnwWeave rnwWeave)
   {
      return isPackageInstalledNative(
                    rnwWeave.getPackageName().toLowerCase() + "_installed");
   }
   
   private final native boolean isPackageInstalledNative(String attrib) /*-{
      return this[attrib];
   }-*/;
}
