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
package com.google.gwt.app.place;

import com.google.gwt.app.place.ProxyPlace.Operation;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.Violation;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract activity for editing a record.
 * 
 * @param <P> the type of Proxy being edited
 */
public abstract class AbstractProxyEditActivity<P extends EntityProxy> implements Activity,
    ProxyEditView.Delegate {

  private final ProxyEditView<P, ?> view;
  private final PlaceController placeController;

  private RequestFactoryEditorDriver<P, ?> editorDriver;
  private boolean waiting;

  public AbstractProxyEditActivity(ProxyEditView<P, ?> view, PlaceController placeController) {
    this.view = view;
    this.placeController = placeController;
  }

  public void cancelClicked() {
    String unsavedChangesWarning = mayStop();
    if ((unsavedChangesWarning == null)
        || Window.confirm(unsavedChangesWarning)) {
      editorDriver = null;
      exit(false);
    }
  }

  public String mayStop() {
    if (isWaiting()
        || (editorDriver != null && editorDriver.flush().isChanged())) {
      return "Are you sure you want to abandon your changes?";
    }

    return null;
  }

  public void onCancel() {
    onStop();
  }

  public void onStop() {
    editorDriver = null;
  }

  public void saveClicked() {
    setWaiting(true);
    editorDriver.flush().fire(new Receiver<Void>() {
      /*
       * Callbacks do nothing if editorDriver is null, we were stopped in
       * midflight
       */
      @Override
      public void onFailure(ServerFailure error) {
        if (editorDriver != null) {
          setWaiting(false);
          super.onFailure(error);
        }
      }

      @Override
      public void onSuccess(Void ignore) {
        if (editorDriver != null) {
          // We want no warnings from mayStop, so:

          // Defeat isChanged check
          editorDriver = null;

          // Defeat call-in-flight check
          setWaiting(false);

          exit(true);
        }
      }

      @Override
      public void onViolation(Set<Violation> errors) {
        if (editorDriver != null) {
          setWaiting(false);
          editorDriver.setViolations(errors);
        }
      }
    });
  }

  public void start(AcceptsOneWidget display, EventBus eventBus) {
    editorDriver = view.createEditorDriver();
    view.setDelegate(this);
    editorDriver.edit(getProxy(), createSaveRequest(getProxy()));
    display.setWidget(view);
  }

  /**
   * Called once to create the appropriate request to save
   * changes.
   * 
   * @return the request context to fire when the save button is clicked
   */
  protected abstract RequestContext createSaveRequest(P proxy);

  /**
   * Called when the user cancels or has successfully saved. This default
   * implementation tells the {@link PlaceController} to show the details of the
   * edited record.
   * 
   * @param saved true if changes were comitted, false if user canceled
   */
  protected void exit(@SuppressWarnings("unused") boolean saved) {
    placeController.goTo(new ProxyPlace(getProxyId(), Operation.DETAILS));
  }

  /**
   * Get the proxy to be edited. Must be mutable, typically via a call to
   * {@link RequestContext#edit(EntityProxy)}, or
   * {@link RequestContext#create(Class)}.
   */
  protected abstract P getProxy();

  @SuppressWarnings("unchecked")
  // id type always matches proxy type
  protected EntityProxyId<P> getProxyId() {
    return (EntityProxyId<P>) getProxy().stableId();
  }

  /**
   * @return true if we're waiting for an rpc response.
   */
  private boolean isWaiting() {
    return waiting;
  }

  /**
   * While we are waiting for a response, we cannot poke setters on the proxy
   * (that is, we cannot call editorDriver.flush). So we set the waiting flag to
   * warn ourselves not to, and to disable the view.
   */
  private void setWaiting(boolean wait) {
    this.waiting = wait;
    view.setEnabled(!wait);
  }
}
