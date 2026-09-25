/*
 * RVersionInstall.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.renv.model;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * An installed version of R, as reported by rig.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class RVersionInstall
{
   public String version;

   // the installation root (e.g. the R.framework version directory)
   public String path;

   // the R executable
   public String binary;

   // R's home directory (as reported by 'R RHOME')
   public String home;

   // false for a macOS framework version that would run the framework's
   // default version of R instead of itself (see r-framework.ts)
   public boolean orthogonal;
}
