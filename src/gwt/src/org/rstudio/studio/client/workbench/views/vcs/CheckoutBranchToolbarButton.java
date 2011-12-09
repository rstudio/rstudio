package org.rstudio.studio.client.workbench.views.vcs;

import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.inject.Inject;

public class CheckoutBranchToolbarButton extends BranchToolbarButton
{
   @Inject
   public CheckoutBranchToolbarButton(final GitState vcsState,
                                      final GitServerOperations server)
   {
      super(vcsState);
      
      vcsState.bindRefreshHandler(this, new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            setBranchCaption(vcsState.getBranchInfo().getActiveBranch());
         }
      });
      
      addValueChangeHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            final String branch = event.getValue();
            server.gitCheckout(
                  branch,
                  new SimpleRequestCallback<ConsoleProcess>()
                  {
                     @Override
                     public void onResponseReceived(ConsoleProcess proc)
                     {
                        new ConsoleProgressDialog("Checkout " + branch,
                                                  proc,
                                                  server).showModal();
                     }
                  });
            
         }
         
      });
      
   }

}
