/*
 * PanmirrorUIToolsAttr.java
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


package org.rstudio.studio.client.panmirror.uitools;

import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIToolsAttr
{
   public native PanmirrorAttrEditInput propsToInput(PanmirrorAttrProps attr);
   public native PanmirrorAttrProps inputToProps(PanmirrorAttrEditInput input);
   public native String pandocAutoIdentifier(String text);
}

