/*
 * AceDocumentChangeEventNative.java
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

import com.google.gwt.core.client.JsArrayString;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class AceDocumentChangeEventNative
{
   public String action; // "insert" or "remove"
   public Position start;
   public Position end;
   public JsArrayString lines;

   @JsOverlay
   public final boolean isInsertion()
   {
      return ACTION_INSERT.equals(action);
   }

   @JsOverlay
   public final boolean isRemoval()
   {
      return ACTION_REMOVE.equals(action);
   }
   
   @JsOverlay
   public final Range getRange()
   {
      return Range.fromPoints(start, end);
   }
   
   @JsOverlay
   public final String getAction()
   {
      return action;
   }

   @JsOverlay public static final String ACTION_INSERT = "insert";
   @JsOverlay public static final String ACTION_REMOVE = "remove";
}
