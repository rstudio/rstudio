/*
 * PanmirrorFind.java
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

package org.rstudio.studio.client.panmirror.findreplace;

import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class PanmirrorFindReplace
{
   public native boolean find(String term, PanmirrorFindOptions options);
   public native int matches();
   public native boolean selectCurrent();
   public native boolean selectFirst();
   public native boolean selectNext();
   public native boolean selectPrevious();
   public native boolean replace(String term);
   public native boolean replaceAll(String term);
   public native boolean clear();
}
