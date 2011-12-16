package org.rstudio.core.client.widget;

import com.google.gwt.resources.client.ImageResource;

public interface WizardPageInfo
{
   String getTitle();
   String getSubTitle();
   String getPageCaption();
   ImageResource getImage();
   ImageResource getLargeImage();
}
