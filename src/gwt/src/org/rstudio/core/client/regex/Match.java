/*
 * Match.java
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
package org.rstudio.core.client.regex;

import com.google.gwt.core.client.JavaScriptObject;

public class Match extends JavaScriptObject
{
   protected Match() {}
   
   public final native String getValue() /*-{
      return this.value;
   }-*/;

   public final native int getIndex() /*-{
      return this.index;
   }-*/;

   public final native Match nextMatch() /*-{
      return @org.rstudio.core.client.regex.Match::doNextMatch(Lorg/rstudio/core/client/regex/Pattern;Ljava/lang/String;I)(this.pattern, this.input, this.next);
   }-*/;
   
   public final native String getGroup(int number) /*-{
      return this.match[number];
   }-*/;

   public final native boolean hasGroup(int number) /*-{
      return typeof(this.match[number]) != 'undefined';
   }-*/;

   private static Match doNextMatch(Pattern pattern, String input, int index)
   {
      return pattern.match(input, index);
   }
}
