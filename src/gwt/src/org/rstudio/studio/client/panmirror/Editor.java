package org.rstudio.studio.client.panmirror;



import jsinterop.annotations.JsType;

import elemental2.promise.Promise;



@JsType(isNative = true, namespace = "Panmirror")
public class Editor
{  
   
   public native Promise<Editor> create();
 
}
