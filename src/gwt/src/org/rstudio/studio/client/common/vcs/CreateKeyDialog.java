package org.rstudio.studio.client.common.vcs;

import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ShowContentDialog;
import org.rstudio.studio.client.common.crypto.RSAEncrypt;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class CreateKeyDialog extends ModalDialog<CreateKeyOptions>
{
   public CreateKeyDialog(final VCSServerOperations server,
                          final OperationWithInput<String> onSuccess)
   {
      super("Create SSH Key", new ProgressOperationWithInput<CreateKeyOptions>() {
         @Override
         public void execute(final CreateKeyOptions input, 
                             final ProgressIndicator indicator)
         {
            RSAEncrypt.encrypt_ServerOnly(
               server, 
               input.getPassphrase(), 
               new RSAEncrypt.ResponseCallback() {
                  @Override
                  public void onSuccess(String encryptedData)
                  {
                     // substitute encrypted data
                     CreateKeyOptions options = CreateKeyOptions.create(
                                                            input.getPath(),
                                                            input.getType(),
                                                            encryptedData,
                                                            input.getComment());
                     
                     // call server to create the key
                     server.vcsCreateSshKey(
                        options, 
                        new ServerRequestCallback<CreateKeyResult>() {
   
                           @Override
                           public void onResponseReceived(CreateKeyResult res)
                           {
                              // check for failure due to the key existing
                              if (res.getFailedKeyExists())
                              {
                                 indicator.clearProgress();
                                 showKeyExistsError(input.getPath());
                              }
                              else
                              {
                                 // close the dialog
                                 indicator.onCompleted();
                              
                                 // update the key path if we succeeded
                                 if (res.getExitStatus() == 0)
                                    onSuccess.execute(input.getPath());
                                 
                                 // show the output
                                 new ShowContentDialog(
                                             "Create SSH Key",
                                             res.getOutput()).showModal();
                              }
                           }
                           
                           @Override
                           public void onError(ServerError error)
                           {
                              indicator.onError(error.getUserMessage());    
                           }
                           
                        });
                  }
                  
                  @Override
                  public void onFailure(ServerError error)
                  {
                     indicator.onError(error.getUserMessage());
                  }
               }
            );
         }
      });
      
      setOkButtonCaption("Create");
   }
   
   private static void showKeyExistsError(String path)
   {
      HTML msgHTML = new HTML(
         "<p>The SSH key could not be created because the key file '" + path +
         "' (or its corresponding public key) already exists.</p>" +
         "<p>Please delete or rename the existing key file(s) before " +
         "proceeding.</p>");
      msgHTML.getElement().getStyle().setMarginLeft(10, Unit.PX);
      msgHTML.setWidth("300px");    
      
      MessageDialog dlg = new MessageDialog(MessageDialog.ERROR, 
                                            "Key Already Exists", 
                                            msgHTML);
      dlg.addButton("OK", (Operation)null, true, false);
      dlg.showModal();
        
   }

   @Override
   protected CreateKeyOptions collectInput()
   {
      return CreateKeyOptions.create("~/.ssh/id_dsa", "dsa", "", "jj");
   }

   @Override
   protected boolean validate(CreateKeyOptions input)
   {
      
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return new Label("Create Key Dialog");
   }

}
