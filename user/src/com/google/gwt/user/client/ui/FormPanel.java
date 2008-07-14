/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.FormPanelImpl;
import com.google.gwt.user.client.ui.impl.FormPanelImplHost;

/**
 * A panel that wraps its contents in an HTML &lt;FORM&gt; element.
 * 
 * <p>
 * This panel can be used to achieve interoperability with servers that accept
 * traditional HTML form encoding. The following widgets (those that implement
 * {@link com.google.gwt.user.client.ui.HasName}) will be submitted to the
 * server if they are contained within this panel:
 * <ul>
 * <li>{@link com.google.gwt.user.client.ui.TextBox}</li>
 * <li>{@link com.google.gwt.user.client.ui.PasswordTextBox}</li>
 * <li>{@link com.google.gwt.user.client.ui.RadioButton}</li>
 * <li>{@link com.google.gwt.user.client.ui.SimpleRadioButton}</li>
 * <li>{@link com.google.gwt.user.client.ui.CheckBox}</li>
 * <li>{@link com.google.gwt.user.client.ui.SimpleCheckBox}</li>
 * <li>{@link com.google.gwt.user.client.ui.TextArea}</li>
 * <li>{@link com.google.gwt.user.client.ui.ListBox}</li>
 * <li>{@link com.google.gwt.user.client.ui.FileUpload}</li>
 * <li>{@link com.google.gwt.user.client.ui.Hidden}</li>
 * </ul>
 * In particular, {@link com.google.gwt.user.client.ui.FileUpload} is <i>only</i>
 * useful when used within a FormPanel, because the browser will only upload
 * files using form submission.
 * </p>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.FormPanelExample}
 * </p>
 */
public class FormPanel extends SimplePanel implements FiresFormEvents,
    FormPanelImplHost {

  /**
   * Used with {@link #setEncoding(String)} to specify that the form will be
   * submitted using MIME encoding (necessary for {@link FileUpload} to work
   * properly).
   */
  public static final String ENCODING_MULTIPART = "multipart/form-data";

  /**
   * Used with {@link #setEncoding(String)} to specify that the form will be
   * submitted using traditional URL encoding.
   */
  public static final String ENCODING_URLENCODED = "application/x-www-form-urlencoded";

  /**
   * Used with {@link #setMethod(String)} to specify that the form will be
   * submitted using an HTTP GET request.
   */
  public static final String METHOD_GET = "get";

  /**
   * Used with {@link #setMethod(String)} to specify that the form will be
   * submitted using an HTTP POST request (necessary for {@link FileUpload} to
   * work properly).
   */
  public static final String METHOD_POST = "post";

  private static int formId = 0;
  private static FormPanelImpl impl = GWT.create(FormPanelImpl.class);

  /**
   * Creates a FormPanel that wraps an existing &lt;form&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * <p>
   * The specified form element's target attribute will not be set, and the
   * {@link FormSubmitCompleteEvent} will not be fired.
   * </p>
   * 
   * @param element the element to be wrapped
   */
  public static FormPanel wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    FormPanel formPanel = new FormPanel(element);

    // Mark it attached and remember it for cleanup.
    formPanel.onAttach();
    RootPanel.detachOnWindowClose(formPanel);

    return formPanel;
  }

  /**
   * Creates a FormPanel that wraps an existing &lt;form&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * <p>
   * If the createIFrame parameter is set to <code>true</code>, then the
   * wrapped form's target attribute will be set to a hidden iframe. If not, the
   * form's target will be left alone, and the FormSubmitComplete event will not
   * be fired.
   * </p>
   * 
   * @param element the element to be wrapped
   * @param createIFrame <code>true</code> to create an &lt;iframe&gt; element
   *          that will be targeted by this form
   */
  public static FormPanel wrap(Element element, boolean createIFrame) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    FormPanel formPanel = new FormPanel(element, createIFrame);

    // Mark it attached and remember it for cleanup.
    formPanel.onAttach();
    RootPanel.detachOnWindowClose(formPanel);

    return formPanel;
  }

  private FormHandlerCollection formHandlers;
  private String frameName;
  private Element synthesizedFrame;

  /**
   * Creates a new FormPanel. When created using this constructor, it will be
   * submitted to a hidden &lt;iframe&gt; element, and the results of the
   * submission made available via {@link FormHandler}.
   * 
   * <p>
   * The back-end server is expected to respond with a content-type of
   * 'text/html', meaning that the text returned will be treated as HTML. If any
   * other content-type is specified by the server, then the result html sent in
   * the onFormSubmit event will be unpredictable across browsers, and the
   * {@link FormHandler#onSubmitComplete(FormSubmitCompleteEvent)} event may not
   * fire at all.
   * </p>
   * 
   * @tip The initial implementation of FormPanel specified that the server
   *      respond with a content-type of 'text/plain'. This has been
   *      intentionally changed to specify 'text/html' because 'text/plain'
   *      cannot be made to work properly on all browsers.
   */
  public FormPanel() {
    this(Document.get().createFormElement(), true);
  }

  /**
   * Creates a FormPanel that targets a {@link NamedFrame}. The target frame is
   * not physically attached to the form, and must therefore still be added to a
   * panel elsewhere.
   * 
   * <p>
   * When the FormPanel targets an external frame in this way, it will not fire
   * the FormSubmitComplete event.
   * </p>
   * 
   * @param frameTarget the {@link NamedFrame} to be targetted
   */
  public FormPanel(NamedFrame frameTarget) {
    this(frameTarget.getName());
  }

  /**
   * Creates a new FormPanel. When created using this constructor, it will be
   * submitted either by replacing the current page, or to the named
   * &lt;iframe&gt;.
   * 
   * <p>
   * When the FormPanel targets an external frame in this way, it will not fire
   * the FormSubmitComplete event.
   * </p>
   * 
   * @param target the name of the &lt;iframe&gt; to receive the results of the
   *          submission, or <code>null</code> to specify that the current
   *          page be replaced
   */
  public FormPanel(String target) {
    super(Document.get().createFormElement());
    setTarget(target);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be a &lt;form&gt; element.
   * 
   * <p>
   * The specified form element's target attribute will not be set, and the
   * {@link FormSubmitCompleteEvent} will not be fired.
   * </p>
   * 
   * @param element the element to be used
   * @param createIFrame <code>true</code> to create an &lt;iframe&gt; element
   *          that will be targeted by this form
   */
  protected FormPanel(Element element) {
    this(element, false);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be a &lt;form&gt; element.
   * 
   * <p>
   * If the createIFrame parameter is set to <code>true</code>, then the
   * wrapped form's target attribute will be set to a hidden iframe. If not, the
   * form's target will be left alone, and the FormSubmitComplete event will not
   * be fired.
   * </p>
   * 
   * @param element the element to be used
   * @param createIFrame <code>true</code> to create an &lt;iframe&gt; element
   *          that will be targeted by this form
   */
  protected FormPanel(Element element, boolean createIFrame) {
    super(element);
    FormElement.as(element);

    if (createIFrame) {
      assert ((getTarget() == null) || (getTarget().trim().length() == 0)) : "Cannot create target iframe if the form's target is already set.";

      frameName = "FormPanel_" + (++formId);
      setTarget(frameName);

      sinkEvents(Event.ONLOAD);
    }
  }

  public void addFormHandler(FormHandler handler) {
    if (formHandlers == null) {
      formHandlers = new FormHandlerCollection();
    }
    formHandlers.add(handler);
  }

  /**
   * Gets the 'action' associated with this form. This is the URL to which it
   * will be submitted.
   * 
   * @return the form's action
   */
  public String getAction() {
    return getFormElement().getAction();
  }

  /**
   * Gets the encoding used for submitting this form. This should be either
   * {@link #ENCODING_MULTIPART} or {@link #ENCODING_URLENCODED}.
   * 
   * @return the form's encoding
   */
  public String getEncoding() {
    return impl.getEncoding(getElement());
  }

  /**
   * Gets the HTTP method used for submitting this form. This should be either
   * {@link #METHOD_GET} or {@link #METHOD_POST}.
   * 
   * @return the form's method
   */
  public String getMethod() {
    return getFormElement().getMethod();
  }

  /**
   * Gets the form's 'target'. This is the name of the {@link NamedFrame} that
   * will receive the results of submission, or <code>null</code> if none has
   * been specified.
   * 
   * @return the form's target.
   */
  public String getTarget() {
    return getFormElement().getTarget();
  }

  public boolean onFormSubmit() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      return onFormSubmitAndCatch(handler);
    } else {
      return onFormSubmitImpl();
    }
  }

  public void onFrameLoad() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      onFrameLoadAndCatch(handler);
    } else {
      onFrameLoadImpl();
    }
  }

  public void removeFormHandler(FormHandler handler) {
    if (formHandlers != null) {
      formHandlers.remove(handler);
    }
  }

  /**
   * Sets the 'action' associated with this form. This is the URL to which it
   * will be submitted.
   * 
   * @param url the form's action
   */
  public void setAction(String url) {
    getFormElement().setAction(url);
  }

  /**
   * Sets the encoding used for submitting this form. This should be either
   * {@link #ENCODING_MULTIPART} or {@link #ENCODING_URLENCODED}.
   * 
   * @param encodingType the form's encoding
   */
  public void setEncoding(String encodingType) {
    impl.setEncoding(getElement(), encodingType);
  }

  /**
   * Sets the HTTP method used for submitting this form. This should be either
   * {@link #METHOD_GET} or {@link #METHOD_POST}.
   * 
   * @param method the form's method
   */
  public void setMethod(String method) {
    getFormElement().setMethod(method);
  }

  /**
   * Submits the form.
   * 
   * <p>
   * The FormPanel must <em>not</em> be detached (i.e. removed from its parent
   * or otherwise disconnected from a {@link RootPanel}) until the submission
   * is complete. Otherwise, notification of submission will fail.
   * </p>
   */
  public void submit() {
    // Fire the onSubmit event, because javascript's form.submit() does not
    // fire the built-in onsubmit event.
    if (formHandlers != null) {
      if (formHandlers.fireOnSubmit(this)) {
        return;
      }
    }

    impl.submit(getElement(), synthesizedFrame);
  }

  @Override
  protected void onAttach() {
    super.onAttach();

    if (frameName != null) {
      // Create and attach a hidden iframe to the body element.
      createFrame();
      Document.get().getBody().appendChild(synthesizedFrame);

      // Hook up the underlying iframe's onLoad event when attached to the DOM.
      // Making this connection only when attached avoids memory-leak issues.
      // The FormPanel cannot use the built-in GWT event-handling mechanism
      // because there is no standard onLoad event on iframes that works across
      // browsers.
      impl.hookEvents(synthesizedFrame, getElement(), this);
    }
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    if (synthesizedFrame != null) {
      // Unhook the iframe's onLoad when detached.
      impl.unhookEvents(synthesizedFrame, getElement());

      // And remove it from the document.
      Document.get().getBody().removeChild(synthesizedFrame);
      synthesizedFrame = null;
    }
  }

  // For unit-tests.
  Element getSynthesizedIFrame() {
    return synthesizedFrame;
  }

  private void createFrame() {
    // Attach a hidden IFrame to the form. This is the target iframe to which
    // the form will be submitted. We have to create the iframe using innerHTML,
    // because setting an iframe's 'name' property dynamically doesn't work on
    // most browsers.
    Element dummy = Document.get().createDivElement();
    dummy.setInnerHTML("<iframe src=\"javascript:''\" name='" + frameName
        + "' style='position:absolute;width:0;height:0;border:0'>");

    synthesizedFrame = dummy.getFirstChildElement();
  }

  private FormElement getFormElement() {
    return getElement().cast();
  }

  private boolean onFormSubmitAndCatch(UncaughtExceptionHandler handler) {
    try {
      return onFormSubmitImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
      return false;
    }
  }

  private boolean onFormSubmitImpl() {
    if (formHandlers != null) {
      // fireOnSubmit() returns true if the submit should be cancelled
      return !formHandlers.fireOnSubmit(this);
    }

    return true;
  }

  private void onFrameLoadAndCatch(UncaughtExceptionHandler handler) {
    try {
      onFrameLoadImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private void onFrameLoadImpl() {
    if (formHandlers != null) {
      // Fire onComplete events in a deferred command. This is necessary
      // because clients that detach the form panel when submission is
      // complete can cause some browsers (i.e. Mozilla) to go into an
      // 'infinite loading' state. See issue 916.
      DeferredCommand.addCommand(new Command() {
        public void execute() {
          formHandlers.fireOnComplete(FormPanel.this,
              impl.getContents(synthesizedFrame));
        }
      });
    }
  }

  private void setTarget(String target) {
    getFormElement().setTarget(target);
  }
}
