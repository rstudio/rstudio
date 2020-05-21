/*
 * Completions.java
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
package org.rstudio.studio.client.common.codetools;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayBoolean;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;

public class Completions extends JavaScriptObject
{
   public static native Completions createCompletions(String token,
                                                      JsArrayString completions,
                                                      JsArrayString packages,
                                                      JsArrayBoolean quote,
                                                      JsArrayInteger type,
                                                      JsArrayString meta,
                                                      String fguess,
                                                      boolean excludeOtherCompletions,
                                                      boolean overrideInsertParens,
                                                      boolean cacheable,
                                                      String helpHandler,
                                                      String language)
   /*-{
      return {
         token: [token],
         results: completions,
         packages: packages,
         quote: quote,
         type: type,
         meta: meta,
         fguess: fguess ? [fguess] : null,
         excludeOtherCompletions: excludeOtherCompletions,
         overrideInsertParens: overrideInsertParens,
         cacheable: cacheable,
         helpHandler: helpHandler,
         language: language
      };
   }-*/;

   protected Completions()
   {
   }

   public final native String getToken() /*-{
      return this.token[0];
   }-*/;
   
   public final native JsArrayString getCompletions() /*-{
      return this.results;
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
         return null;
      return this.fguess[0];
   }-*/;

   public final native void setCacheable(boolean cacheable) /*-{
      this.cacheable = cacheable;
   }-*/;

   public final native boolean isCacheable() /*-{
      return !!this.cacheable;
   }-*/;

   public final native void setSuggestOnAccept(boolean suggestOnAccept) /*-{
      this.suggestOnAccept = suggestOnAccept;
   }-*/;
   
   public final native boolean shouldInsertParens() /*-{
      return this.insertParens;
   }-*/;
   
   public final native JsArrayBoolean getQuote() /*-{
      return this.quote;
   }-*/;
   
   public final native JsArrayInteger getType() /*-{
      return this.type;
   }-*/;
   
   public final native JsArrayString getMeta() /*-{
      return this.meta;
   }-*/;

   public final native boolean getSuggestOnAccept() /*-{
      return !!this.suggestOnAccept;
   }-*/;
   
   public final native boolean getExcludeOtherCompletions() /*-{
      return this.excludeOtherCompletions;
   }-*/;
   
   public final native boolean getOverrideInsertParens() /*-{
      return this.overrideInsertParens;
   }-*/;
   
   public final native String getHelpHandler() /*-{
      return this.helpHandler;
   }-*/;
   
   public final native String getLanguage() /*-{
      return this.language;
   }-*/;
   
   
}
