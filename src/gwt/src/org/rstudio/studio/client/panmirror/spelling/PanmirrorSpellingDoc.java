/*
 * PanmirrorSpellingDoc.java
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

package org.rstudio.studio.client.panmirror.spelling;

import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class PanmirrorSpellingDoc
{
   public native PanmirrorWordSource getWords(int start, int end);
   public native PanmirrorAnchor createAnchor(int position);
   
   public native boolean shouldCheck(PanmirrorWordRange wordRange);
   public native void setSelection(PanmirrorWordRange wordRange);
   public native String getText(PanmirrorWordRange wordRange);
   
   public native int getCursorPosition();
   public native void replaceSelection(String text);
   public native int getSelectionStart();
   public native int getSelectionEnd();
   
   public native PanmirrorRect getCursorBounds();
   public native void moveCursorNearTop();
   
   public native void dispose();
}
