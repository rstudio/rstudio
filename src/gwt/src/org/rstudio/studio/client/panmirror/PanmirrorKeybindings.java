/*
 * PanmirrorKeybindings.java
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


package org.rstudio.studio.client.panmirror;

import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import jsinterop.base.Js;
import elemental2.core.JsArray;
import elemental2.core.JsString;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;

@JsType(name = "Object", namespace = JsPackage.GLOBAL, isNative = true)
public class PanmirrorKeybindings  {

    @JsConstructor
    public PanmirrorKeybindings()
    {
    }

    @JsOverlay
    public final void add(String commandId, String[] keys){
       JsArray<JsString> jsKeys = new JsArray<>();
       for (String key : keys)
          jsKeys.push(new JsString(key));
       Js.<Any>cast(this).asPropertyMap().set(commandId,  jsKeys);
    }
}