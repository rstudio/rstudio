package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface ManipulatorResources extends ClientBundle
{
   public static final ManipulatorResources INSTANCE = GWT.create(ManipulatorResources.class);

   @Source("ManipulatorStyles.css")
   ManipulatorStyles manipulatorStyles();
   
   ImageResource manipulateButton();
   ImageResource manipulateProgress();
}
