package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.studio.client.projects.model.RProjectOptions;

public abstract class ProjectPreferencesPane 
                     extends PreferencesDialogPaneBase<RProjectOptions>
{

   
   protected static final ProjectPreferencesDialogResources RESOURCES =
                           ProjectPreferencesDialogResources.INSTANCE;
}
