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

import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.user.client.Window;
import com.google.gwt.valuestore.shared.DeltaValueStore;
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

  private final RequestFactory requests;
  private final boolean creating;
  private final RecordEditView<R> view;

  private String id;
  private String futureId;
  private DeltaValueStore deltas;
  private Display display;

  public AbstractRecordEditActivity(RecordEditView<R> view, String id,
      RequestFactory requests) {
    this.view = view;
    this.creating = "".equals(id);
    this.id = id;
    this.requests = requests;
    this.deltas = requests.getValueStore().spawnDeltaView();
  }

  public void cancelClicked() {
    if (willStop()) {
      deltas = null; // silence the next willStop() call when place changes
      if (creating) {
        display.showActivityWidget(null);
      } else {
        exit();
      }
    }
  }

  public void onCancel() {
    onStop();
  }

  public void onStop() {
    this.display = null;
  }

  public void saveClicked() {
    if (deltas.isChanged()) {
      view.setEnabled(false);

      final DeltaValueStore toCommit = deltas;
      deltas = null;

      Receiver<Set<SyncResult>> receiver = new Receiver<Set<SyncResult>>() {
        public void onSuccess(Set<SyncResult> response) {
          if (display == null) {
            return;
          }
          boolean hasViolations = false;
          for (SyncResult syncResult : response) {
            Record syncRecord = syncResult.getRecord();
            if (creating) {
              if (futureId == null
                  || !futureId.equals(syncResult.getFutureId())) {
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
            deltas = toCommit;
            deltas.clearUsed();
            view.setEnabled(true);
            deltas.clearUsed();
          }
        }

      };
      requests.syncRequest(toCommit).to(receiver).fire();
    }
  }

  @SuppressWarnings("unchecked")
  public void start(Display display) {
    this.display = display;
    
    view.setDelegate(this);
    view.setDeltaValueStore(deltas);
    view.setCreating(creating);

    if (creating) {
      // TODO shouldn't have to cast like this. Let's get something better than
      // a string token
      R tempRecord = (R) deltas.create(getRecordToken());
      futureId = tempRecord.getId();
      doStart(display, tempRecord);
    } else {
      fireFindRequest(Value.of(id), new Receiver<R>() {
        public void onSuccess(R record) {
          if (AbstractRecordEditActivity.this.display != null) {
            doStart(AbstractRecordEditActivity.this.display, record);
          }
        }
      });
    }
  }

  public boolean willStop() {
    return deltas == null || !deltas.isChanged()
        || Window.confirm("Are you sure you want to abandon your changes?");
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
  protected abstract String getRecordToken();

  private void doStart(final Display display, R record) {
    view.setEnabled(true);
    view.setValue(record);
    view.showErrors(null);
    display.showActivityWidget(view);
  }
}
