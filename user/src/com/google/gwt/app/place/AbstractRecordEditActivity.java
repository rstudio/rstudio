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
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.valuestore.shared.Value;

import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract activity for editing a record.
 * 
 * @param <R> the type of Record being edited
 */
public abstract class AbstractRecordEditActivity<R extends Record> implements
    Activity, RecordEditView.Delegate {

  private RequestObject<Void> requestObject;

  private final boolean creating;
  private final RecordEditView<R> view;
  private final Class<R> proxyType;
  private final RequestFactory requests;
  private final PlaceController placeController;

  private R record;
  @SuppressWarnings("unused")
  private Long futureId;
  private Display display;

  public AbstractRecordEditActivity(RecordEditView<R> view, R record,
      Class<R> proxyType, boolean creating, RequestFactory requests,
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
        requestObject.reset(); // silence the next mayStop() call when place
      }
      // changes
      if (creating) {
        display.showActivityWidget(null);
      } else {
        exit();
      }
    }
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
        if (display == null) {
          return;
        }
        boolean hasViolations = false;
        
        // TODO(amit) at the moment we only get one response, and futures are buggy. 
        // So forcing the issue for now, but the more code involved may have to come back
        // when bugs are fixed
        assert response.size() == 1;
        SyncResult syncResult = response.iterator().next();
        record = cast(syncResult.getRecord());
        if (syncResult.hasViolations()) {
          hasViolations = true;
          view.showErrors(syncResult.getViolations());
        }
//        for (SyncResult syncResult : response) {
//          Record syncRecord = syncResult.getRecord();
//          if (creating) {
//            if (futureId == null || !futureId.equals(syncResult.getFutureId())) {
//              continue;
//            }
//            record = cast(syncRecord);
//          } else {
//            if (!syncRecord.getId().equals(record.getId())) {
//              continue;
//            }
//          }
//          if (syncResult.hasViolations()) {
//            hasViolations = true;
//            view.showErrors(syncResult.getViolations());
//          }
//        }
        if (!hasViolations) {
          exit();
        } else {
          requestObject = toCommit;
          requestObject.clearUsed();
          view.setEnabled(true);
        }
      }
    };
    toCommit.fire(receiver);
  }

  @SuppressWarnings("unchecked")
  public void start(Display display, EventBus eventBus) {
    this.display = display;

    view.setDelegate(this);
    view.setCreating(creating);

    if (creating) {
      R tempRecord = (R) requests.create(proxyType);
      futureId = tempRecord.getId();
      doStart(display, tempRecord);
    } else {
      fireFindRequest(Value.of(record.getId()), new Receiver<R>() {
        public void onSuccess(R record, Set<SyncResult> syncResults) {
          if (AbstractRecordEditActivity.this.display != null) {
            doStart(AbstractRecordEditActivity.this.display, record);
          }
        }
      });
    }
  }

  /**
   * Called to fetch the details of the edited record.
   */
  protected abstract void fireFindRequest(Value<Long> id, Receiver<R> callback);

  protected abstract RequestObject<Void> getPersistRequest(R record);

  @SuppressWarnings("unchecked")
  private R cast(Record syncRecord) {
    return (R) syncRecord;
  }

  private void doStart(final Display display, R record) {
    requestObject = getPersistRequest(record);
    R editableRecord = requestObject.edit(record);
    view.setEnabled(true);
    view.setValue(editableRecord);
    view.showErrors(null);
    display.showActivityWidget(view);
  }

  /**
   * Called when the user has clicked Cancel or has successfully saved.
   */
  private void exit() {
    placeController.goTo(new ProxyPlace(record, Operation.DETAILS));
  }
}
