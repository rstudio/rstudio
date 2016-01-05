package org.rstudio.studio.client.workbench.views.environment.dataimport;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class DataImport extends Composite {

	private static DataImportUiBinder uiBinder = GWT.create(DataImportUiBinder.class);

	interface DataImportUiBinder extends UiBinder<Widget, DataImport> {
	}

	public DataImport() {
		initWidget(uiBinder.createAndBindUi(this));
	}

}
