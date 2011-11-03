package org.rstudio.studio.client.common.vcs;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FocusHelper;
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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CreateKeyDialog extends ModalDialog<CreateKeyOptions>
{
   public CreateKeyDialog(String defaultSshKeyPath,
                          final VCSServerOperations server,
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
                                                            encryptedData);
                     
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
      
      defaultSshKeyPath_ = FileSystemItem.createDir(defaultSshKeyPath);
      
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
      if (getPath().length() == 0)
         return null;
      else if (!getPassphrase().equals(getConfirmPassphrase()))
         return null;
      else
         return CreateKeyOptions.create(getPath(), getType(), getPassphrase());
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
         
         if (getPath().length() == 0)
         {
            display.showErrorMessage(
                  "Missing Key Path", 
                  "You must provide a destination path for the key.",
                  txtName_);
         }
         else if (!getPassphrase().equals(getConfirmPassphrase()))
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
      
      HorizontalPanel nameAndTypePanel = new HorizontalPanel();
      nameAndTypePanel.setWidth("100%");
      
      VerticalPanel typePanel = new VerticalPanel();
      Label typeLabel = new Label("Type:");
      typeLabel.addStyleName(styles.entryLabel());
      typePanel.add(typeLabel);
   
      typeSelector_ = new ListBox();
      typeSelector_.addStyleName(styles.keyTypeSelector());
      typeSelector_.addItem(RSA);
      typeSelector_.addItem(DSA);
      typeSelector_.addItem(RSA1);
      typeSelector_.setSelectedIndex(0);
      typeSelector_.addChangeHandler(new ChangeHandler() {

         @Override
         public void onChange(ChangeEvent event)
         {
            String type = getType();
            if (type.equals(RSA))
               txtName_.setValue(defaultSshKeyPath_.completePath("id_rsa"));
            else if (type.equals(DSA))
               txtName_.setValue(defaultSshKeyPath_.completePath("id_dsa"));
            else if (type.equals(RSA1))
               txtName_.setValue(defaultSshKeyPath_.completePath("identity")); 
         }
         
      });
      typePanel.add(typeSelector_);
      nameAndTypePanel.add(typePanel);
      
      VerticalPanel namePanel = new VerticalPanel();
      namePanel.setWidth("270px");
      Label nameLabel = new Label("Path:");
      nameLabel.addStyleName(styles.entryLabel());
      namePanel.add(nameLabel);
      txtName_ = new TextBox();
      txtName_.setText(defaultSshKeyPath_.completePath("id_rsa"));
      txtName_.setWidth("100%");
      namePanel.add(txtName_);
      nameAndTypePanel.add(namePanel);
      
      panel.add(nameAndTypePanel);
      
      HorizontalPanel passphrasePanel = new HorizontalPanel();
      passphrasePanel.addStyleName(styles.newSection());
      
      VerticalPanel passphrasePanel1 = new VerticalPanel();
      Label passphraseLabel1 = new Label("Passphrase (optional):");
      passphraseLabel1.addStyleName(styles.entryLabel());
      passphrasePanel1.add(passphraseLabel1);
      txtPassphrase_ = new PasswordTextBox();
      txtPassphrase_.addStyleName(styles.passphrase());
      
      passphrasePanel1.add(txtPassphrase_);
      passphrasePanel.add(passphrasePanel1);
      
      VerticalPanel passphrasePanel2 = new VerticalPanel();
      passphrasePanel2.addStyleName(styles.lastSection());
      Label passphraseLabel2 = new Label("Confirm:");
      passphraseLabel2.addStyleName(styles.entryLabel());
      passphrasePanel2.add(passphraseLabel2);
      txtConfirmPassphrase_ = new PasswordTextBox();
      txtConfirmPassphrase_.addStyleName(styles.passphraseConfirm());
      passphrasePanel2.add(txtConfirmPassphrase_);
      passphrasePanel.add(passphrasePanel2);
      
      panel.add(passphrasePanel);
      
          
      return panel;
   }
   
   @Override
   protected void onLoad()
   {
      FocusHelper.setFocusDeferred(txtPassphrase_);
   }
   
   private String getPath()
   {
      return txtName_.getText().trim();
   }
   
   private String getType()
   {
      return typeSelector_.getItemText(typeSelector_.getSelectedIndex());
   }
   
   private String getPassphrase()
   {
      return txtPassphrase_.getText().trim();
   }
   
   private String getConfirmPassphrase()
   {
      return txtConfirmPassphrase_.getText().trim();
   }
   
   private final String RSA = "rsa";
   private final String DSA = "dsa";
   private final String RSA1 = "rsa1";
   
   static interface Styles extends CssResource
   {
      String entryLabel();
      String keyTypeSelector();
      String mainWidget();
      String newSection();
      String lastSection();
      String passphrase();
      String passphraseConfirm();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("CreateKeyDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private ListBox typeSelector_;
   private TextBox txtName_;
   private TextBox txtPassphrase_;
   private TextBox txtConfirmPassphrase_;
   
   private final FileSystemItem defaultSshKeyPath_;
}
