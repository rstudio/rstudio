package org.rstudio.studio.client.panmirror.dialogs.model;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorInsertCiteProps {
	public String doi;
	public String[] existingIds;
	public String[] bibliographyFiles;
	public String suggestedId;
	public PanmirrorInsertCitePreviewPair[] previewPairs;
	
}
