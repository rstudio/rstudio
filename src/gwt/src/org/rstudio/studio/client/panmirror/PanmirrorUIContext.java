/*
 * PanmirrorUIContext.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

package org.rstudio.studio.client.panmirror;


import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIContext
{
   public GetResourceDir getResourceDir;
   public MapResourcePath mapResourcePath;
   public TranslateText translateText;

   @JsFunction
   public interface GetResourceDir
   {
      String getResourceDir();
   }
   
   @JsFunction
   public interface MapResourcePath
   {
      String mapResourcePath(String path);
   }
   
   @JsFunction
   public interface TranslateText
   {
      String translateText(String path);
   }
}


