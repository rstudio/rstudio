package org.rstudio.studio.client.panmirror.dialogs;


import org.rstudio.core.client.widget.FormListBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCitePreviewPair;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteWork;
import org.rstudio.studio.client.panmirror.server.PanmirrorCrossrefServerOperations;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsCite;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import jsinterop.base.Js;

public class PanmirrorInsertCiteDialog extends ModalDialog<PanmirrorInsertCiteResult> {

	public PanmirrorInsertCiteDialog(PanmirrorInsertCiteProps citeProps,
			OperationWithInput<PanmirrorInsertCiteResult> operation) {
		super("Insert Citation", Roles.getDialogRole(), operation, () -> {
			operation.execute(null);
		});
		
		RStudioGinjector.INSTANCE.injectMembers(this);
		mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);	
		
		// Populate the bibliographies
		setBibliographies(citeProps.bibliographyFiles);
				

		if (citeProps.previewPairs != null && citeProps.previewPairs.length > 0) {
		    // We were passed a completed DOI, just populate the UI
		   citationId_.setText(citeProps.suggestedId);
		   displayPreview(citeProps.previewPairs);
		   
		} else {
		    // We were given an incomplete DOI, we need to look it up
         ProgressIndicator indicator = addProgressIndicator(false);
         indicator.onProgress("Looking Up Citation..", () -> {
            // on cancel unload the dialog?
            GWT.log("Cancel");
         });
         
         displayPlaceholderPreview();
         server_.crossrefDoi(citeProps.doi, new ServerRequestCallback<JavaScriptObject>() {
            @Override
            public void onResponseReceived(JavaScriptObject response)
            {
               GWT.log("Response Received");
               indicator.onCompleted();
               
               PanmirrorInsertCiteWork work = Js.uncheckedCast(response);
               PanmirrorUIToolsCite citeTools = new PanmirrorUITools().cite;
               String suggestedId = citeTools.suggestCiteId(
                     citeProps.existingIds, 
                     work.author[0].family, 
                     2012); //TODO: work.issued);
               
               PanmirrorInsertCitePreviewPair[] previewPairs = citeTools.previewPairs(work);
               displayPreview(previewPairs);
                              
               citationId_.setText(suggestedId);      
            }
            
            @Override
            public void onError(ServerError error) {
               GWT.log(error.getMessage());
               GWT.log(error.toString());
               indicator.onError(error.getUserMessage());
            }
            
         });
		}
	}
	
	@Inject
   void initialize(PanmirrorCrossrefServerOperations server)
	{
		server_ = server;
	}
	
	private int addPreviewRow(String label, String value, int row) {
		if (value != null && value.length() > 0) {
			previewTable_.setText(row, 0, label);
			previewTable_.getFlexCellFormatter().addStyleName(row, 0, PanmirrorDialogsResources.INSTANCE.styles().flexTablePreviewName());
			previewTable_.setText(row, 1, value);
			previewTable_.getFlexCellFormatter().addStyleName(row, 1, PanmirrorDialogsResources.INSTANCE.styles().flexTablePreviewValue());
			return ++row;
		}
		return row;
	}

	@Override
	public void focusInitialControl() {
		super.focusInitialControl();
		citationId_.selectAll();
	}

	@Override
	protected PanmirrorInsertCiteResult collectInput() {
		PanmirrorInsertCiteResult result = new PanmirrorInsertCiteResult();
		result.id = citationId_.getText();
		result.bibliographyFile = bibliographies_.getValue(bibliographies_.getSelectedIndex());
		return result;
	}

	@Override
	protected Widget createMainWidget() {
		return mainWidget_;
	}
	
	private void setBibliographies(String[] bibliographyFiles) {
      if (bibliographyFiles.length == 0) {
         bibliographies_.addItem("New bibliography (bibliography.bib)", "bibliography.bib");
      } else {
         for (String file : bibliographyFiles) {
            bibliographies_.addItem(file);
         }
      }
	}
	
	private void displayPreview(PanmirrorInsertCitePreviewPair[] previewPairs) {
	   previewTable_.clear();
	   // Display a preview
      int row = 0;
      for (PanmirrorInsertCitePreviewPair pair: previewPairs) {
         row = addPreviewRow(pair.name, pair.value, row);   
      }           
	}
	
	private void displayPlaceholderPreview() {
	   previewTable_.clear();
	   for (int i=0; i < 14; i++) {
	      addPreviewRow(" ", " ", i);
	   }
	}

	@UiField
	TextBox citationId_;
	@UiField
	FormListBox bibliographies_;
	@UiField
	FlexTable previewTable_;

	interface Binder extends UiBinder<Widget, PanmirrorInsertCiteDialog> {
	}

	private Widget mainWidget_;
	private PanmirrorCrossrefServerOperations server_;

}
