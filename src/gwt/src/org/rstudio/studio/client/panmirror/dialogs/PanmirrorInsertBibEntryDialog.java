package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertBibEntryProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertBibEntryResult;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class PanmirrorInsertBibEntryDialog extends ModalDialog<PanmirrorInsertBibEntryResult> {

   public PanmirrorInsertBibEntryDialog(PanmirrorInsertBibEntryProps bibEntry, 
		   OperationWithInput<PanmirrorInsertBibEntryResult> operation) {
      super("Insert Bibliography Entry", Roles.getDialogRole(), operation, () -> {
          operation.execute(null);
       });
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
      // TODO: Allow user selection of Bibliography
      bibEntryId_.setText(bibEntry.suggestedId);     
	}
   
   @Override
   public void focusInitialControl()
   {
	  super.focusInitialControl();
	  bibEntryId_.selectAll();
   }

	@Override
	protected PanmirrorInsertBibEntryResult collectInput() {
		PanmirrorInsertBibEntryResult result = new PanmirrorInsertBibEntryResult();
		result.id = bibEntryId_.getText();
		// TODO: allow user selection of the bibliography file and return it
		result.bibliographyFile = "";
		return result;
	}

	@Override
	protected Widget createMainWidget() {
		return mainWidget_;
	}
	
	@UiField TextBox bibEntryId_;
	
	interface Binder extends UiBinder<Widget, PanmirrorInsertBibEntryDialog> {}

	private Widget mainWidget_;

}
