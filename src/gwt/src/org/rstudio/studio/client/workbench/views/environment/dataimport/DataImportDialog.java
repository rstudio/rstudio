package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.user.client.ui.Widget;

public class DataImportDialog extends ModalDialog<String>
{

   public DataImportDialog(String caption, OperationWithInput<String> operation)
   {
      super(caption, operation);
      super.setOkButtonCaption("Import");
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
