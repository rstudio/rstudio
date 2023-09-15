/**
 * 
 */
package org.rstudio.studio.client.workbench.copilot.ui;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ElementPanel;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.common.HelpLink;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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

   public CopilotInstallDialog(boolean isAlreadyInstalled)
   {
      super(Roles.getDialogRole());
      ui_ = uiBinder.createAndBindUi(this);
      
      String alreadyInstalledMessage = isAlreadyInstalled
            ? "The GitHub Copilot agent is already installed. Would you like to re-install it?"
            : "The GitHub Copilot agent is not currently installed. Would you like to install it?";
      
      alreadyInstalledMessage_.setInnerText(alreadyInstalledMessage);
      
      progress_ = addProgressIndicator();
      
      setTitle("GitHub Copilot: Install Agent");
      setText("GitHub Copilot: Install Agent");
      setWidth("400px");
      
      okButton_ = new ThemedButton("Install");
      addOkButton(okButton_);
      
      cancelButton_ = new ThemedButton("Cancel");
      addCancelButton(cancelButton_);
      
      HelpLink tosLink = new HelpLink(
            "Terms of Service",
            "github-copilot-terms-of-service",
            false);
      addLeftWidget(tosLink);
   }
   
   public ProgressIndicator getProgressIndicator()
   {
      return progress_;
   }
   
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return okButton_.addClickHandler(handler);
   }
   
   public HandlerRegistration addCancelHandler(ClickHandler handler)
   {
      return cancelButton_.addClickHandler(handler);
   }
   
   @Override
   protected Widget createMainWidget()
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.add(new Image(new ImageResource2x(MessageDialogImages.INSTANCE.dialog_info2x())));
      panel.add(new ElementPanel(ui_));
      return panel;
   }
   
   @UiField ParagraphElement alreadyInstalledMessage_;
   
   private final Element ui_;
   private final ProgressIndicator progress_;
   private final ThemedButton okButton_;
   private final ThemedButton cancelButton_;
}
