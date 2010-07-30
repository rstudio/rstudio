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

import com.google.gwt.requestfactory.shared.DeltaValueStore;
import com.google.gwt.requestfactory.shared.Receiver;
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

  private final boolean creating;
  private final RecordEditView<R> view;

  private String id;
  private String futureId;
  private Display display;
  private RequestObject<Void> requestObject;

  public AbstractRecordEditActivity(RecordEditView<R> view, String id,
      RequestObject<Void> requestObject) {
    this.view = view;
    this.creating = "".equals(id);
    this.id = id;
    this.requestObject = requestObject;
  }

  public void cancelClicked() {
    String unsavedChangesWarning = mayStop();
    if ((unsavedChangesWarning == null)
        || Window.confirm(unsavedChangesWarning)) {
      requestObject.reset(); // silence the next mayStop() call when place
                             // changes
      if (creating) {
        display.showActivityWidget(null);
      } else {
        exit();
      }
    }
  }

  public String mayStop() {
    if (requestObject != null && requestObject.getDeltaValueStore().isChanged()) {
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
    DeltaValueStore deltas = requestObject.getDeltaValueStore();
    if (!deltas.isChanged()) {
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
        for (SyncResult syncResult : response) {
          Record syncRecord = syncResult.getRecord();
          if (creating) {
            if (futureId == null || !futureId.equals(syncResult.getFutureId())) {
              continue;
            }
            id = syncRecord.getId();
          } else {
            if (!syncRecord.getId().equals(id)) {
              continue;
            }
          }
          if (syncResult.hasViolations()) {
            hasViolations = true;
            view.showErrors(syncResult.getViolations());
          }
        }
        if (!hasViolations) {
          exit();
        } else {
          requestObject = toCommit;
          requestObject.getDeltaValueStore().clearUsed();
          view.setEnabled(true);
        }
      }

    };
    toCommit.fire(receiver);
  }

  @SuppressWarnings("unchecked")
  public void start(Display display) {
    this.display = display;

    view.setDelegate(this);
    view.setDeltaValueStore(requestObject.getDeltaValueStore());
    view.setCreating(creating);

    if (creating) {
      R tempRecord = (R) requestObject.getDeltaValueStore().create(getRecordClass());
      futureId = tempRecord.getId();
      doStart(display, tempRecord);
    } else {
      fireFindRequest(Value.of(id), new Receiver<R>() {
        public void onSuccess(R record, Set<SyncResult> syncResults) {
          if (AbstractRecordEditActivity.this.display != null) {
            doStart(AbstractRecordEditActivity.this.display, record);
          }
        }
      });
    }
  }

  /**
   * Called when the user has clicked Cancel or has successfully saved.
   */
  protected abstract void exit();

  /**
   * Called to fetch the details of the edited record.
   */
  protected abstract void fireFindRequest(Value<String> id, Receiver<R> callback);

  protected String getId() {
    return id;
  }

  /**
   * Called to fetch the string token needed to get a new record via
   * {@link DeltaValueStore#create}.
   */
  protected abstract Class<? extends Record> getRecordClass();

  private void doStart(final Display display, R record) {
    view.setEnabled(true);
    view.setValue(record);
    view.showErrors(null);
    display.showActivityWidget(view);
  }
}
