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