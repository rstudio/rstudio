/**
 * 
 */
package org.rstudio.studio.client.workbench.copilot.ui;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ElementPanel;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.common.HelpLink;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class CopilotInstallDialog extends ModalDialogBase
{

   private static CopilotInstallDialogUiBinder uiBinder = GWT
         .create(CopilotInstallDialogUiBinder.class);

   interface CopilotInstallDialogUiBinder extends UiBinder<Element, CopilotInstallDialog>
   {
   }

   public CopilotInstallDialog(ClickHandler handler)
   {
      super(Roles.getDialogRole());
      ui_ = uiBinder.createAndBindUi(this);
      
      setTitle("GitHub Copilot: Install Agent");
      setText("GitHub Copilot: Install Agent");
      setWidth("400px");
      
      ThemedButton okButton = new ThemedButton("Install", handler);
      addOkButton(okButton);
      
      ThemedButton cancelButton = new ThemedButton("Cancel");
      cancelButton.addClickHandler((event) -> {
         CopilotInstallDialog.this.closeDialog();
      });
      addCancelButton(cancelButton);
      
      HelpLink tosLink = new HelpLink(
            "Terms of Service",
            GITHUB_TOS_LINK,
            false,
            false);
      addLeftWidget(tosLink);
   }

   @Override
   protected Widget createMainWidget()
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.add(new Image(new ImageResource2x(MessageDialogImages.INSTANCE.dialog_info2x())));
      panel.add(new ElementPanel(ui_));
      return panel;
   }
   
   private final Element ui_;
   
   private static final String GITHUB_TOS_LINK =
         "https://docs.github.com/en/site-policy/github-terms/github-terms-for-additional-products-and-features#github-copilot";
}
