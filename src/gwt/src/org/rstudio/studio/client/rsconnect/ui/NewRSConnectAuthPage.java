package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.RSConnectAuthenticator;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectAuthUser;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

public class NewRSConnectAuthPage 
   extends WizardPage<NewRSConnectAccountInput, 
                      NewRSConnectAccountResult>
{
   public NewRSConnectAuthPage()
   {
      super("", "", "Verifying Account", 
            RSConnectResources.INSTANCE.localAccountIcon(), 
            RSConnectResources.INSTANCE.localAccountIconLarge());

   }

   @Override
   public void focus()
   {
   }

   @Override
   public void setIntermediateResult(NewRSConnectAccountResult result) 
   {
      result_ = result;
   }
   
   @Override
   public void onActivate(final ProgressIndicator indicator) 
   {
      authenticator_ = new RSConnectAuthenticator(
            server_, 
            result_.getServerInfo(),
            contents_);
      authenticator_.authenticate(new OperationWithInput<RSConnectAuthUser>()
      {
         @Override
         public void execute(RSConnectAuthUser user)
         {
            indicator.onCompleted();
            result_.setAuthUser(user);
            onUserAuthVerified();
         }
      });
   }

   @Override
   public void onWizardClosing()
   {
      if (authenticator_ != null)
         authenticator_.stopPolling();
   }

   @Override
   protected Widget createWidget()
   {
      contents_ = new RSConnectAuthWait();
      contents_.setOnTryAgain(new Command()
      {
         @Override
         public void execute()
         {
            onActivate(null);
         }
      });
      return contents_;
   }
   
   @Override
   protected void initialize(NewRSConnectAccountInput initData)
   {
      server_ = initData.getServer();
   }

   @Override
   protected NewRSConnectAccountResult collectInput()
   {
      return result_;
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return input != null && input.getAuthUser() != null;
   }

   
   private void onUserAuthVerified()
   {
      // set the account nickname if we didn't already have one
      if (result_.getAccountNickname().length() == 0)
      {
         if (result_.getAuthUser().getUsername().length() > 0) 
         {
            // if we have a username already, just use it 
            result_.setAccountNickname(
                  result_.getAuthUser().getUsername());
         }
         else
         {
            // if we don't have any username, guess one based on user's given name
            // on the server
            result_.setAccountNickname(
                  result_.getAuthUser().getFirstName().substring(0, 1) + 
                  result_.getAuthUser().getLastName().toLowerCase());
         }
      }
      
      contents_.showSuccess(result_.getServerName(), 
            result_.getAccountNickname());
   }

   private NewRSConnectAccountResult result_;
   private RSConnectServerOperations server_;
   private RSConnectAuthWait contents_;
   private RSConnectAuthenticator authenticator_;
}
