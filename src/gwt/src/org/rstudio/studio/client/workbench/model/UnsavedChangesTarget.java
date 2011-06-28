package org.rstudio.studio.client.workbench.model;

import com.google.gwt.resources.client.ImageResource;

public interface UnsavedChangesTarget
{
   String getId();
   ImageResource getIcon();
   String getTitle();
   String getPath();

}
