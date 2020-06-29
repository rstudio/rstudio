package org.rstudio.studio.client.panmirror.uitools;

import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCitePreviewPair;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteWork;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIToolsCite
{
   public native PanmirrorInsertCitePreviewPair[] previewPairs(PanmirrorInsertCiteWork work);
   public native String suggestCiteId(String[] existingIds, String authorLastName, int issuedYear);

}
