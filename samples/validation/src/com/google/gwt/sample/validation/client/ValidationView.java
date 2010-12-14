/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.sample.validation.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.sample.validation.shared.ClientGroup;
import com.google.gwt.sample.validation.shared.Person;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;

/**
 * Display the Validation sample.
 */
public class ValidationView extends Composite {
  interface MyStyle extends CssResource {
    String error();
  }

  interface ValidationViewUiBinder extends UiBinder<Widget, ValidationView> {
  }

  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  private static ValidationViewUiBinder uiBinder = GWT.create(ValidationViewUiBinder.class);

  @UiField
  Button closeButton;

  @UiField
  DialogBox dialogBox;

  @UiField
  Label errorLabel;

  @UiField
  TextBox nameField;

  @UiField
  Button sendButton;

  @UiField
  HTML serverResponse;

  @UiField
  MyStyle style;

  @UiField
  Label textToServer;

  private final GreetingServiceAsync greetingService;

  private final Person person;

  public ValidationView(Person person, GreetingServiceAsync greetingService) {

    this.person = person;

    this.greetingService = greetingService;
    initWidget(uiBinder.createAndBindUi(this));

    nameField.setText(person.getName());
  }

  @UiHandler("closeButton")
  public void doClick(ClickEvent e) {
    dialogBox.hide();
    sendButton.setEnabled(true);
    sendButton.setFocus(true);
  }

  @UiHandler("sendButton")
  void onClick(ClickEvent e) {
    sendPersonToServer();
  }

  @UiHandler("sendButton")
  void onKeyPress(KeyUpEvent e) {
    if (e.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
      sendPersonToServer();
    }
  }

  /**
   * Send the person from the nameField to the server and wait for a response.
   */
  private void sendPersonToServer() {
    errorLabel.setText("");
    person.setName(nameField.getText());

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Person>> violations = validator.validate(person,
        Default.class, ClientGroup.class);
    if (!violations.isEmpty()) {
      StringBuffer errorMessage = new StringBuffer();
      for (ConstraintViolation<Person> constraintViolation : violations) {
        if (errorMessage.length() == 0) {
          errorMessage.append('\n');
        }
        errorMessage.append(constraintViolation.getMessage());
      }
      errorLabel.setText(errorMessage.toString());
      return;
    }
    sendButton.setEnabled(false);
    textToServer.setText(person.getName());
    serverResponse.setText("");
    greetingService.greetServer(person, new AsyncCallback<SafeHtml>() {
      public void onFailure(Throwable caught) {
        if (caught instanceof ConstraintViolationException) {
          ConstraintViolationException violationException = (ConstraintViolationException) caught;
          Set<ConstraintViolation<?>> violations = violationException.getConstraintViolations();
          StringBuffer sb = new StringBuffer();
          for (ConstraintViolation<?> constraintViolation : violations) {
            sb.append(constraintViolation.getPropertyPath().toString()) //
            .append(":") //
            .append(constraintViolation.getMessage()) //
            .append("\n");
          }
          errorLabel.setText(sb.toString());
          sendButton.setEnabled(true);
          sendButton.setFocus(true);
          return;
        }

        // Show the RPC error message to the user
        dialogBox.setText("Remote Procedure Call - Failure");
        serverResponse.addStyleName(style.error());
        serverResponse.setHTML(SERVER_ERROR);
        dialogBox.center();
        closeButton.setFocus(true);
      }

      public void onSuccess(SafeHtml result) {
        dialogBox.setText("Remote Procedure Call");
        serverResponse.removeStyleName(style.error());
        serverResponse.setHTML(result);
        dialogBox.center();
        closeButton.setFocus(true);
      }
    });
  }
}
