package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.common.filetypes.TextFileType;

import com.google.gwt.user.client.ui.IsWidget;

public interface TextDisplay extends IsWidget
{
   void onActivate();
   void adaptToFileType(TextFileType fileType);
   void setFontSize(double size);
}
