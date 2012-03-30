/*
 * Completions.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.codetools;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class Completions extends JavaScriptObject
{
   public static native Completions createCompletions(String token,
                                                      JsArrayString completions,
                                                      JsArrayString packages,
                                                      String fguess) /*-{
      return {
         token: [token],
         results: completions,
         packages: packages,
         fguess: fguess ? [fguess] : null
      };
   }-*/;

   protected Completions()
   {
   }

   public final native String getToken() /*-{
      return this.token[0] ;
   }-*/;
   
   public final native JsArrayString getCompletions() /*-{
      return this.results ;
   }-*/;
   
   public final native JsArrayString getPackages() /*-{
      // Packages end up as arrays of arrays because I suck at R.
      //   results: [["base"], null, null, ["graphics"], null]
      //         => ["base", null, null, "graphics", null]
      
      return this.packages;
   }-*/;
   
   /**
    * If rcompgen thinks we're doing function args, this
    * returns the name of the function it thinks we're in
    */
   public final native String getGuessedFunctionName() /*-{
      if (!this.fguess)
         return null ;
      return this.fguess[0] ;
   }-*/;

   public final native void setCacheable(boolean cacheable) /*-{
      this.nocache = !cacheable;
   }-*/;

   public final native boolean isCacheable() /*-{
      return !this.nocache;
   }-*/;

   public final native void setSuggestOnAccept(boolean suggestOnAccept) /*-{
      this.suggestOnAccept = suggestOnAccept;
   }-*/;

   public final native boolean getSuggestOnAccept() /*-{
      return !!this.suggestOnAccept;
   }-*/;
}
