/*
 * RevealSlide.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

package org.rstudio.studio.client.workbench.views.presentation2.model;

import com.google.gwt.core.client.JavaScriptObject;


public class RevealSlide extends JavaScriptObject
{
   protected RevealSlide() {}

   
   public final boolean hasSameIndices(RevealSlide other)
   {
      return getHIndex() == other.getHIndex() &&
             getVIndex() == other.getVIndex() &&
             getFIndex() == other.getFIndex();
   }

   public native final String getId() /*-{
      return this.id;
   }-*/;

   public native final String getTitle() /*-{
      return this.title;
   }-*/;
   
   public native final int getHIndex() /*-{
      return this.h;
   }-*/;
   
   public native final int getVIndex() /*-{
      return this.v;
   }-*/;
   
   public native final int getFIndex() /*-{
      return this.f;
   }-*/;
}
