/*
 * RetinaStyleInjector.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.dom.client.StyleInjector;

public class RetinaStyleInjector
{
   public static void injectAtEnd(String css, boolean immediate)
   {
      StyleInjector.injectAtEnd(asRetina(css), immediate);
   }
   
   public static void injectAtEnd(String css)
   {
      StyleInjector.injectAtEnd(asRetina(css));
   }
   
   private static String asRetina(String css)
   {
      return
            "@media " +
            "(-webkit-min-device-pixel-ratio: 1.5), " +
            "(-moz-min-device-pixel-ratio: 1.5), " +
            "(-o-min-device-pixel-ratio: 3/2), " +
            "(min-device-pixel-ratio: 1.5) {" +
            css +
            "}";
   }
}
