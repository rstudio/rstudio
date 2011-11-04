package org.rstudio.studio.client.common.vcs;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SshKeyChooser extends Composite
{
   
   
   public SshKeyChooser(VCSServerOperations server,
                        final String defaultSshKeyDir,
                        String textWidth,
                        ProgressIndicator progressIndicator)
   {
      server_ = server;
      progressIndicator_ = progressIndicator;
      
      FlowPanel panel = new FlowPanel();
           
      // caption panel
      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.setWidth(textWidth);
      Label sshKeyPathLabel = new Label("SSH key path:");
      captionPanel.add(sshKeyPathLabel);
      captionPanel.setCellHorizontalAlignment(
                                          sshKeyPathLabel,
                                          HasHorizontalAlignment.ALIGN_LEFT);
   
      HorizontalPanel linkPanel = new HorizontalPanel();
      publicKeyLink_ = new HyperlinkLabel("View public key");
      publicKeyLink_.addStyleName(RES.styles().viewPublicKeyLink());
      publicKeyLink_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            viewPublicKey();
         }    
      });    
      linkPanel.add(publicKeyLink_);
      captionPanel.add(publicKeyLink_);
      captionPanel.setCellHorizontalAlignment(
                                        publicKeyLink_, 
                                        HasHorizontalAlignment.ALIGN_RIGHT);
      panel.add(captionPanel);
      
      
      // chooser
      sshKeyPathChooser_ = new FileChooserTextBox(
           null,
           "(Not Found)",
           null,
           new Command() {
            @Override
            public void execute()
            {
               publicKeyLink_.setVisible(true); 
            }    
      });
      sshKeyPathChooser_.setTextWidth(textWidth);
      setSshKey("");
      panel.add(sshKeyPathChooser_);  
      
    
      // ssh key path action buttons
      HorizontalPanel sshButtonPanel = new HorizontalPanel();
      sshButtonPanel.addStyleName(RES.styles().sshButtonPanel());
      createKeyButton_ = new SmallButton();
      createKeyButton_.setText("Create New Key...");
      createKeyButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            new CreateKeyDialog(defaultSshKeyDir,
                                server_,
                                new OperationWithInput<String>() {
               @Override
               public void execute(String keyPath)
               {
                  setSshKey(keyPath);             
               }
            }).showModal();    
         }
      });
      // only add the create-key button in server mode
      sshButtonPanel.add(createKeyButton_);
      panel.add(sshButtonPanel);
      
      
      initWidget(panel);
   }
   
   
   public void setSshKey(String keyPath)
   {
      sshKeyPathChooser_.setText(keyPath);
      publicKeyLink_.setVisible(getSshKey().length() > 0);
   }
   
   public String getSshKey()
   {
      return sshKeyPathChooser_.getText().trim();
   }
   
   public void setAllowKeyCreation(boolean allowKeyCreation)
   {
      createKeyButton_.setVisible(allowKeyCreation);
   }
   
   private void viewPublicKey()
   {
      progressIndicator_.onProgress("Reading public key...");
      
      // compute path to public key
      FileSystemItem privKey = 
               FileSystemItem.createFile(sshKeyPathChooser_.getText());
      FileSystemItem keyDir = privKey.getParentPath();
      final String keyPath = keyDir.completePath(privKey.getStem() + ".pub");
      
      server_.vcsSshPublicKey(keyPath,
                              new ServerRequestCallback<String> () {
         
         @Override
         public void onResponseReceived(String publicKeyContents)
         {
            progressIndicator_.onCompleted();
            
            // transform contents into displayable form
            SafeHtmlBuilder htmlBuilder = new SafeHtmlBuilder();
            SafeHtmlUtil.appendDiv(htmlBuilder,
                                   RES.styles().viewPublicKeyContent(),
                                   publicKeyContents);
            
            new ShowPublicKeyDialog(publicKeyContents).showModal();
         }

         @Override
         public void onError(ServerError error)
         {
            String msg = "Error attempting to read key '" + keyPath + "' (" +
                         error.getUserMessage() + ")";
            progressIndicator_.onError(msg);
         } 
      }); 
   }
   
   
   private class ShowPublicKeyDialog extends ModalDialogBase
   {
      public ShowPublicKeyDialog(String publicKey)
      {
         publicKey_ = publicKey;
         
         setText("Public Key");
         
         setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);
         
         ThemedButton closeButton = new ThemedButton("Close",
                                                     new ClickHandler() {
            public void onClick(ClickEvent event) {
               closeDialog();
            }
         });
         addOkButton(closeButton); 
      }
      
      @Override
      protected Widget createMainWidget()
      {
         VerticalPanel panel = new VerticalPanel();
         
         int mod = BrowseCap.hasMetaKey() ? KeyboardShortcut.META : 
                                            KeyboardShortcut.CTRL;
         String cmdText = new KeyboardShortcut(mod, 'C').toString(true);
         HTML label = new HTML("Press " + cmdText + 
                               " to copy the key to the clipboard");
         label.addStyleName(RES.styles().viewPublicKeyLabel());
         panel.add(label);
         
         textArea_ = new TextArea();
         textArea_.setText(publicKey_);
         textArea_.addStyleName(RES.styles().viewPublicKeyContent());
         textArea_.setSize("400px", "250px");
         textArea_.getElement().setAttribute("spellcheck", "false");
         FontSizer.applyNormalFontSize(textArea_.getElement());
         
         panel.add(textArea_);
         
         return panel;
      }
      
      @Override
      protected void onLoad()
      {
         super.onLoad();
        
         textArea_.selectAll();
         FocusHelper.setFocusDeferred(textArea_);
      }
      
      private final String publicKey_;
      private TextArea textArea_;
   }

   
   static interface Styles extends CssResource
   {
      String viewPublicKeyLink();
      String viewPublicKeyContent();
      String viewPublicKeyLabel();
      String sshButtonPanel();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("SshKeyChooser.css")
      Styles styles();
   }
   
   static Resources RES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
   
   private HyperlinkLabel publicKeyLink_;
   private TextBoxWithButton sshKeyPathChooser_;
   private SmallButton createKeyButton_;
   
   private final VCSServerOperations server_;
   private final ProgressIndicator progressIndicator_;
}
