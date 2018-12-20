package org.rstudio.studio.client.common.spelling;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Typo")
public class TypoNative
{
   TypoNative(String dictionary) {}

   public native boolean check(String word);
   public native String[] suggest(String word);
}
