/*
 * PanmirrorUIToolsImage.java
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


package org.rstudio.studio.client.panmirror.uitools;


import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIToolsImage
{
   public native String[] validUnits();
   public native String percentUnit();
   public native double unitToPixels(double value, String unit, double containerWidth);
   public native double pixelsToUnit(double pixels, String unit, double containerWidth);
   public native String roundUnit(double value, String unit);
}

