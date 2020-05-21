/*
 * TypoNativeSettings.java
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
package org.rstudio.studio.client.common.spelling;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.function.Consumer;

class TypoNativeSettings extends JavaScriptObject
{
   protected TypoNativeSettings() {}

   /*
      Since we only care about Async and Async loader for now that's all we're taking.
      The Typo.js interface is a little opaque using JSNI so we are just using a
      Minimum Viable Interface at the moment.
    */
   public static native TypoNativeSettings create(Boolean isAsync, Consumer<TypoNative> asyncCallback)  /*-{
      return {asyncLoad: async, loadedCallback: asyncCallback};
   }-*/;
}
