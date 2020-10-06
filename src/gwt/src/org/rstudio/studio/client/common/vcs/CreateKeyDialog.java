/*
 * CreateKeyDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CaptionWithHelp;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ShowContentDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.crypto.RSAEncrypt;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CreateKeyDialog extends ModalDialog<CreateKeyOptions>
{
   public CreateKeyDialog(String rsaSshKeyPath,
                          final VCSServerOperations server,
                          final OperationWithInput<String> onCompleted)
   {
      super("Create RSA Key", Roles.getDialogRole(), new ProgressOperationWithInput<CreateKeyOptions>() {

         @Override
         public void execute(final CreateKeyOptions input, 
                             final ProgressIndicator indicator)
         {
            final ProgressOperationWithInput<CreateKeyOptions> 
                                                      thisOperation = this;

            indicator.onProgress("Creating RSA Key...");

            RSAEncrypt.encrypt_ServerOnly(
               server,
               input.getPassphrase(),
               new RSAEncrypt.ResponseCallback() {
                  @Override
                  public void onSuccess(final String encryptedData)
                  {
                     // substitute encrypted data
                     final CreateKeyOptions options = CreateKeyOptions.create(
                                                         input.getPath(),
                                                         input.getType(),
                                                         encryptedData,
                                                         input.getOverwrite());

                     // call server to create the key
                     server.createSshKey(
                        options, 
                        new ServerRequestCallback<CreateKeyResult>() {

                           @Override
                           public void onResponseReceived(CreateKeyResult res)
                           {
                              if (res.getFailedKeyExists())
                              {
                                 indicator.clearProgress();

                                 confirmOverwriteKey(
                                    input.getPath(),
                                    () ->
                                    {
                                       // re-execute with overwrite == true
                                       thisOperation.execute(
                                          CreateKeyOptions.create(
                                                options.getPath(),
                                                options.getType(),
                                                input.getPassphrase(),
                                                true),
                                          indicator);
                                    });

                              }
                              else
                              {
                                 // close the dialog
                                 indicator.onCompleted();

                                 // update the key path 
                                 if (res.getExitStatus() == 0)
                                    onCompleted.execute(input.getPath());

                                 else if (input.getOverwrite())
                                    onCompleted.execute(null);

                                 // show the output
                                 new ShowContentDialog(
                                                "Create RSA Key",
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

      rsaSshKeyPath_ = FileSystemItem.createDir(rsaSshKeyPath);

      setOkButtonCaption("Create");
   }

   @Override
   protected CreateKeyOptions collectInput()
   {  
      if (getPassphrase() != getConfirmPassphrase())
         return null;
      else
         return CreateKeyOptions.create(rsaSshKeyPath_.getPath(),
                                        "rsa",
                                        getPassphrase(),
                                        false);
   }

   @Override
   protected boolean validate(CreateKeyOptions input)
   {
      if (input != null)
      {
         return true;
      }
      else
      {
         GlobalDisplay display = RStudioGinjector.INSTANCE.getGlobalDisplay();

         if (getPassphrase() != getConfirmPassphrase())
         {
            display.showErrorMessage(
                  "Non-Matching Passphrases", 
                  "The passphrase and passphrase confirmation do not match.",
                  txtConfirmPassphrase_);
         }

         return false;
      }
   }

   @Override
   protected Widget createMainWidget()
   {
      Styles styles = RESOURCES.styles();

      VerticalPanel panel = new VerticalPanel();
      panel.addStyleName(styles.mainWidget());

      VerticalPanel namePanel = new VerticalPanel();
      namePanel.setWidth("100%");

      // path
      TextBox txtKeyPath = new TextBox();
      txtKeyPath.addStyleName(styles.keyPathTextBox());
      txtKeyPath.setReadOnly(true);
      txtKeyPath.setText(rsaSshKeyPath_.getPath());
      txtKeyPath.setWidth("100%");
      CaptionWithHelp pathCaption = new CaptionWithHelp(
                                 "The RSA key will be created at:",
                                 "SSH/RSA key management",
                                 "rsa_key_help",
                                 txtKeyPath);
      pathCaption.setIncludeVersionInfo(false);
      pathCaption.setWidth("100%");
      namePanel.add(pathCaption);
      namePanel.add(txtKeyPath);

      panel.add(namePanel);

      HorizontalPanel passphrasePanel = new HorizontalPanel();
      passphrasePanel.addStyleName(styles.newSection());

      VerticalPanel passphrasePanel1 = new VerticalPanel();
      txtPassphrase_ = new PasswordTextBox();
      txtPassphrase_.addStyleName(styles.passphrase());
      FormLabel passphraseLabel1 = new FormLabel("Passphrase (optional):", txtPassphrase_);
      passphraseLabel1.addStyleName(styles.entryLabel());
      passphrasePanel1.add(passphraseLabel1);

      passphrasePanel1.add(txtPassphrase_);
      passphrasePanel.add(passphrasePanel1);

      VerticalPanel passphrasePanel2 = new VerticalPanel();
      passphrasePanel2.addStyleName(styles.lastSection());
      txtConfirmPassphrase_ = new PasswordTextBox();
      txtConfirmPassphrase_.addStyleName(styles.passphraseConfirm());
      FormLabel passphraseLabel2 = new FormLabel("Confirm:", txtConfirmPassphrase_);
      passphraseLabel2.addStyleName(styles.entryLabel());
      passphrasePanel2.add(passphraseLabel2);
      passphrasePanel2.add(txtConfirmPassphrase_);
      passphrasePanel.add(passphrasePanel2);

      panel.add(passphrasePanel);

      return panel;
   }

   @Override
   protected void focusInitialControl()
   {
      FocusHelper.setFocusDeferred(txtPassphrase_);
   }

   private static void confirmOverwriteKey(String path, Operation onConfirmed)
   {
      RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
            MessageDialog.WARNING, 
            "Key Already Exists", 
            "An RSA key already exists at " + path + ". " +
            "Do you want to overwrite the existing key?", 
            onConfirmed,
            false);
   }

   private String getPassphrase()
   {
      return txtPassphrase_.getText().trim();
   }

   private String getConfirmPassphrase()
   {
      return txtConfirmPassphrase_.getText().trim();
   }

   interface Styles extends CssResource
   {
      String entryLabel();
      String keyPathTextBox();
      String mainWidget();
      String newSection();
      String lastSection();
      String passphrase();
      String passphraseConfirm();
   }

   interface Resources extends ClientBundle
   {
      @Source("CreateKeyDialog.css")
      Styles styles();
   }

   static final Resources RESOURCES = GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }

   private TextBox txtPassphrase_;
   private TextBox txtConfirmPassphrase_;
   
   private final FileSystemItem rsaSshKeyPath_;
}
