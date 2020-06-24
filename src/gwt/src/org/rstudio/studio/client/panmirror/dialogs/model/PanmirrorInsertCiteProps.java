package org.rstudio.studio.client.panmirror.dialogs.model;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorInsertCiteProps {
	public String suggestedId;
	public String[] bibliographyFiles;
	public PanMirrorInsertCitePreviewPair[] previewPairs;
}
