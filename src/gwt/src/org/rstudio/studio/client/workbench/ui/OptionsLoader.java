package org.rstudio.studio.client.workbench.ui;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.AsyncShim;
import org.rstudio.studio.client.workbench.prefs.views.PreferencesDialog;

public class OptionsLoader
{
   public abstract static class Shim extends AsyncShim<OptionsLoader>
   {
      public abstract void showOptions();
   }


   @Inject
   OptionsLoader(Provider<PreferencesDialog> pPrefDialog)
   {
      pPrefDialog_ = pPrefDialog;
   }

   public void showOptions()
   {
      pPrefDialog_.get().showModal();
   }

   private final Provider<PreferencesDialog> pPrefDialog_;
}
