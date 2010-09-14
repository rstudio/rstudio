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
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.ProxyRequest;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.Value;
import com.google.gwt.requestfactory.shared.Violation;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import java.util.HashMap;
import java.util.Map;
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

  private RequestObject<Void> requestObject;

  private final boolean creating;
  private final ProxyEditView<P> view;
  private final Class<P> proxyType;
  private final RequestFactory requests;
  private final PlaceController placeController;

  private AcceptsOneWidget display;
  private P record;
  private EntityProxyId stableId;

  public AbstractProxyEditActivity(ProxyEditView<P> view, P record,
      Class<P> proxyType, boolean creating, RequestFactory requests,
      PlaceController placeController) {

    this.view = view;
    this.record = record;
    this.proxyType = proxyType;
    this.placeController = placeController;
    this.creating = creating;
    this.requests = requests;
  }

  public void cancelClicked() {
    String unsavedChangesWarning = mayStop();
    if ((unsavedChangesWarning == null)
        || Window.confirm(unsavedChangesWarning)) {
      if (requestObject != null) {
        // silence the next mayStop() call when place changes
        requestObject.reset();
      }
      exit(false);
    }
  }

  public P getRecord() {
    return record;
  }

  public ProxyEditView<P> getView() {
    return view;
  }

  public String mayStop() {
    if (requestObject != null && requestObject.isChanged()) {
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
    assert requestObject != null;
    if (!requestObject.isChanged()) {
      return;
    }
    view.setEnabled(false);

    final RequestObject<Void> toCommit = requestObject;
    requestObject = null;

    Receiver<Void> receiver = new Receiver<Void>() {
      public void onSuccess(Void ignore, Set<SyncResult> response) {
        // TODO(rjrjr): This can be simplified with RequestFactory.refresh()
        if (display == null) {
          return;
        }

        for (SyncResult syncResult : response) {
          EntityProxy syncRecord = syncResult.getProxy();
          if (creating) {
            if (!stableId.equals(syncRecord.stableId())) {
              continue;
            }
            record = cast(syncRecord);
          } else {
            if (!syncRecord.getId().equals(record.getId())) {
              continue;
            }
          }
        }
        exit(true);
      }

      @Override
      public void onViolation(Set<Violation> errors) {
        Map<String, String> toShow = new HashMap<String, String>();
        for (Violation error : errors) {
          if (error.getProxyId().equals(stableId)) {
            toShow.put(error.getPath(), error.getMessage());
          }
        }
        view.showErrors(toShow);
        requestObject = toCommit;
        requestObject.clearUsed();
        view.setEnabled(true);
      }
    };
    toCommit.fire(receiver);
  }

  public void start(AcceptsOneWidget display, EventBus eventBus) {
    this.display = display;

    view.setDelegate(this);
    view.setCreating(creating);

    if (creating) {
      P tempRecord = requests.create(proxyType);
      stableId = tempRecord.stableId();
      doStart(display, tempRecord);
    } else {
      ProxyRequest<P> findRequest = getFindRequest(Value.of(getRecord().getId()));
      findRequest.with(getView().getPaths()).fire(new Receiver<P>() {
        public void onSuccess(P record, Set<SyncResult> syncResults) {
          if (AbstractProxyEditActivity.this.display != null) {
            doStart(AbstractProxyEditActivity.this.display, record);
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
   * If we're creating, a call to getRecord() from here will return a record
   * with the correct id. However, other properties may be stale or unset.
   * 
   * @param saved true if changes were comitted
   */
  protected void exit(boolean saved) {
    if (!saved && creating) {
      display.setWidget(null);
    } else {
      placeController.goTo(new ProxyPlace(getRecord(), Operation.DETAILS));
    }
  }

  /**
   * Called to fetch the details of the edited record.
   */
  protected abstract ProxyRequest<P> getFindRequest(Value<Long> id);

  protected abstract RequestObject<Void> getPersistRequest(P record);

  @SuppressWarnings("unchecked")
  private P cast(EntityProxy syncRecord) {
    return (P) syncRecord;
  }

  private void doStart(final AcceptsOneWidget display, P record) {
    requestObject = getPersistRequest(record);
    P editableRecord = requestObject.edit(record);
    view.setEnabled(true);
    view.setValue(editableRecord);
    view.showErrors(null);
    display.setWidget(view);
  }
}
