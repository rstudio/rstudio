package org.rstudio.studio.client.panmirror;


import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIContext
{
   
   public TranslateResourcePath translateResourcePath;

    
   @JsFunction
   public interface TranslateResourcePath
   {
      String translateResourcePath(String path);
   }
}


