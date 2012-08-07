/*
 * PackagesPrefs.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.studio.client.common.mirrors.model.BioconductorMirror;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;

import com.google.gwt.core.client.JavaScriptObject;

public class PackagesPrefs extends JavaScriptObject
{
   protected PackagesPrefs() {}

   public static final native PackagesPrefs create(
                                  CRANMirror cranMirror,
                                  BioconductorMirror bioconductorMirror,
                                  boolean cleanupAfterCheck) /*-{
      var prefs = new Object();
      prefs.cran_mirror = cranMirror;
      prefs.bioconductor_mirror = bioconductorMirror;
      prefs.cleanup_after_check = cleanupAfterCheck;
      return prefs ;
   }-*/;

   public native final CRANMirror getCRANMirror() /*-{
      return this.cran_mirror;
   }-*/;
   
   public native final BioconductorMirror getBioconductorMirror() /*-{
      return this.bioconductor_mirror;
   }-*/;
   
   public native final boolean getCleanupAfterCMDCheck() /*-{
      return this.cleanup_after_check;
   }-*/;
}
