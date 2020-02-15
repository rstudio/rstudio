package org.rstudio.studio.client.panmirror;

import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;

import jsinterop.annotations.JsType;

@JsType(isNative = true, name="UITools", namespace = "Panmirror")
public class PanmirrorUITools
{
   public native PanmirrorAttrEditInput attrPropsToInput(PanmirrorAttrProps attr);
   public native PanmirrorAttrProps attrInputToProps(PanmirrorAttrEditInput input);
}

