/*
 * RBuildAvailability.java
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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Whether rig has an installer for a version of R, for one build of R.
 */
public class RBuildAvailability extends JavaScriptObject
{
   protected RBuildAvailability()
   {
   }

   // "arm64" or "x86_64"; "" where the machine has no choice of build
   public final native String getArchitecture() /*-{ return this.architecture; }-*/;

   // "available", "unavailable", or "unknown" when rig couldn't be asked
   public final native String getStatus()       /*-{ return this.status;       }-*/;
}
