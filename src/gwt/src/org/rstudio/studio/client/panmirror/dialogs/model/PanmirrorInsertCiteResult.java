package org.rstudio.studio.client.panmirror.dialogs.model;

import elemental2.core.JsObject;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorInsertCiteResult
{
   public String id;
   public String bibliographyFile;
   public JsObject csl;
}
