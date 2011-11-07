package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.studio.client.projects.model.RProjectConfig;

public abstract class ProjectPreferencesPane 
                     extends PreferencesDialogPaneBase<RProjectConfig>
{

   
   protected static final ProjectPreferencesDialogResources RESOURCES =
                           ProjectPreferencesDialogResources.INSTANCE;
}
