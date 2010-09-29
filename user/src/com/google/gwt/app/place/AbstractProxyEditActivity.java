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
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestFactory;
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
public abstract class AbstractProxyEditActivity<P extends EntityProxy>
    implements Activity, ProxyEditView.Delegate {

  private final boolean creating;
  private final RequestFactory requests;
  private final PlaceController placeController;
  private RequestFactoryEditorDriver<P, ?> editorDriver;
  private final ProxyEditView<P, ?> view;

  private AcceptsOneWidget display;
  private EntityProxyId<P> proxyId;
  private Class<P> proxyClass;
  private boolean waiting = false;

  /**
   * Create an activity to edit or create proxy. Must provide either a proxyId
   * (to be edited) or a proxyClass (to create a new proxy). If proxyId is
   * provided, proxyClass will be ignored.
   */
  protected AbstractProxyEditActivity(EntityProxyId<P> proxyId,
      Class<P> proxyClass, RequestFactory requests,
      PlaceController placeController, ProxyEditView<P, ?> view) {

    if (proxyId == null && proxyClass == null) {
      throw new IllegalArgumentException(
          "Must provide either proxyId or proxyClass");
    }

    this.creating = proxyId == null;
    this.proxyClass = proxyClass;
    this.proxyId = proxyId;
    this.placeController = placeController;
    this.requests = requests;
    this.view = view;
    editorDriver = view.createEditorDriver(null, requests);
  }

  public void cancelClicked() {
    String unsavedChangesWarning = mayStop();
    if ((unsavedChangesWarning == null)
        || Window.confirm(unsavedChangesWarning)) {
      editorDriver = null;
      exit(false);
    }
  }

  public EntityProxyId<P> getEntityProxyId() {
    return proxyId;
  }

  public ProxyEditView<P, ?> getView() {
    return view;
  }

  public boolean isCreating() {
    return creating;
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
    this.display = null;
  }

  public void saveClicked() {
    assert editorDriver != null;
    Request<Object> request = editorDriver.flush();
    if (!request.isChanged()) {
      return;
    }

    setWaiting(true);
    request.fire(new Receiver<Object>() {
      // Do nothing if display is null, we were stopped in midflight

      @Override
      public void onFailure(ServerFailure error) {
        if (display != null) {
          setWaiting(false);
          super.onFailure(error);
        }
      }

      @Override
      public void onSuccess(Object ignore) {
        if (display != null) {
          // We want no warnings from mayStop, so:
          
          // Defeats isChanged check
          editorDriver = null;
          
          // Defeats call-in-flight check
          setWaiting(false);

          exit(true);
        }
      }

      @Override
      public void onViolation(Set<Violation> errors) {
        if (display != null) {
          setWaiting(false);
          editorDriver.setViolations(errors);
        }
      }
    });
  }

  public void start(AcceptsOneWidget startDisplay, EventBus eventBus) {

    this.display = startDisplay;

    view.setDelegate(this);
    view.setCreating(isCreating());
    /*
     * Lock ourselves until we actually have a proxy to edit
     */
    setWaiting(true);

    if (isCreating()) {
      P newRecord = requests.create(proxyClass);
      proxyId = getProxyId(newRecord);
      doStart(newRecord);
    } else {
      Request<P> findRequest = requests.find(getEntityProxyId());
      findRequest.with(editorDriver.getPaths()).fire(new Receiver<P>() {
        @Override
        public void onSuccess(P proxy) {
          if (display != null) {
            doStart(proxy);
          }
        }
      });
    }
  }

  /**
   * Called when the user cancels or has successfully saved. This default
   * implementation tells the {@link PlaceController} to show the details of the
   * edited record, or clears the display if a creation was canceled.
   * <p>
   * Call {@link getId()} for the id of the proxy that has been edited or
   * created.
   * 
   * @param saved true if changes were comitted
   */
  protected void exit(boolean saved) {
    if (!saved && isCreating()) {
      display.setWidget(null);
    } else {
      placeController.goTo(new ProxyPlace(proxyId, Operation.DETAILS));
    }
  }

  protected abstract Request<?> getPersistRequest(P proxy);

  private void doStart(P proxy) {
    setWaiting(false);

    Request<?> request = getPersistRequest(proxy);
    editorDriver.edit(proxy, request);

    display.setWidget(view);
  }

  @SuppressWarnings("unchecked")
  private EntityProxyId<P> getProxyId(P newRecord) {
    /*
     * We could make this cast go away if EntityProxy were typed to itself,
     * EntityProxy<P extends EntityProxy<P>>, but the ripples this causes
     * throughout the API are very, very unpleasant.
     */
    return (EntityProxyId<P>) newRecord.stableId();
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
