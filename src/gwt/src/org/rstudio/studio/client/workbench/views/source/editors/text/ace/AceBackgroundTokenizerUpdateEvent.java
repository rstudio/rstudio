/*
 * AceBackgroundTokenizerUpdateEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Event data from Ace's background tokenizer update event.
 * Contains the range of rows that were re-tokenized.
 */
public class AceBackgroundTokenizerUpdateEvent extends JavaScriptObject
{
   protected AceBackgroundTokenizerUpdateEvent() {}

   public native final int getFirstRow() /*-{
      return this.first;
   }-*/;

   public native final int getLastRow() /*-{
      return this.last;
   }-*/;

   /**
    * Returns true if the given row is within the tokenized range.
    */
   public final boolean includesRow(int row)
   {
      return row >= getFirstRow() && row <= getLastRow();
   }
}
