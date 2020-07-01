package org.rstudio.studio.client.panmirror.dialogs.model;


import elemental2.core.JsObject;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorInsertCiteProps {
	public String doi;
	public String[] existingIds;
	public String[] bibliographyFiles;
	public JsObject csl;
	public PanmirrorInsertCiteUI citeUI;
}
