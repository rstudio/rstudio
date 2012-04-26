/*
 * RnwWeave.java
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
package org.rstudio.studio.client.common.rnw;

import com.google.gwt.core.client.JavaScriptObject;

public class RnwWeave extends JavaScriptObject
{
   protected RnwWeave()
   {
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getPackageName() /*-{
      return this.package_name;
   }-*/;
   
   public final native boolean getInjectConcordance() /*-{
      return this.inject_concordance;
   }-*/;

   public final native boolean usesCodeForOptions() /*-{
      return this.uses_code_for_options;
   }-*/;

   public final native boolean forceEchoOnExec() /*-{
      return this.force_echo_on_exec;
   }-*/;
}
