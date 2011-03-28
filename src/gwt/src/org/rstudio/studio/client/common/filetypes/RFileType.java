package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.resources.client.ImageResource;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.reditor.EditorLanguage;

public class RFileType extends TextFileType
{
   RFileType(String id,
             String label,
             EditorLanguage editorLanguage,
             String defaultExtension,
             ImageResource defaultIcon,
             boolean canSourceOnSave,
             boolean canExecuteCode,
             boolean canExecuteAllCode,
             boolean canCompilePDF)
   {
      super(id,
            label,
            editorLanguage,
            defaultExtension,
            defaultIcon,
            false,
            canSourceOnSave,
            canExecuteCode,
            canExecuteAllCode,
            canCompilePDF);
   }

   @Override
   public boolean getWordWrap()
   {
      return RStudioGinjector.INSTANCE.getUIPrefs().softWrapRFiles().getValue();
   }
}
