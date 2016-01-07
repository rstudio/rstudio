package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;

import com.google.gwt.user.client.ui.Widget;

public class DataImportDialog extends ModalDialog<String>
{
   public DataImportDialog(String caption,
                           OperationWithInput<String> operation)
   {
      super(caption, operation);
      setOkButtonCaption("Import");
   }

   @Override
   protected String collectInput()
   {
      return null;
   }

   @Override
   protected Widget createMainWidget()
   {
      return new DataImport();
   }
}
