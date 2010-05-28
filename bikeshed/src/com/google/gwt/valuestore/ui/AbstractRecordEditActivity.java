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
package com.google.gwt.valuestore.ui;

import com.google.gwt.app.place.Activity;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.user.client.Window;
import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.Value;

import java.util.Set;

/**
 * Abstract activity for editing a record.
 * 
 * @param <R> the type of Record being edited
 */
public abstract class AbstractRecordEditActivity<R extends Record> implements
    Activity, RecordEditView.Delegate {

  private final RecordEditView<R> view;
  private final String id;
  private DeltaValueStore deltas;
  private final RequestFactory requests;
  private boolean dead = false;

  public AbstractRecordEditActivity(RecordEditView<R> view, String id,
      RequestFactory requests) {
    this.view = view;
    this.id = id;
    this.requests = requests;
    this.deltas = requests.getValueStore().spawnDeltaView();
    view.setDelegate(this);
    view.setDeltaValueStore(deltas);
  }

  public void cancelClicked() {
    if (willStop()) {
      exit();
    }
  }

  public void onCancel() {
    this.dead = true;
  }

  public void onStop() {
    this.dead = true;
  }

  public void saveClicked() {
    if (deltas.isChanged()) {
      view.setEnabled(false);

      final DeltaValueStore toCommit = deltas;
      deltas = null;

      Receiver<Set<SyncResult>> receiver = new Receiver<Set<SyncResult>>() {
        public void onSuccess(Set<SyncResult> response) {
          if (dead) {
            return;
          }
          boolean hasViolations = false;
          for (SyncResult syncResult : response) {
            if (syncResult.getRecord().getId().equals(id)) {
              if (syncResult.hasViolations()) {
                hasViolations = true;
                view.showErrors(syncResult.getViolations());
              }
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

  public void start(final Display display) {
    Receiver<R> callback = new Receiver<R>() {
      public void onSuccess(R record) {
        if (dead) {
          return;
        }
        view.setEnabled(true);
        view.setValue(record);
        view.showErrors(null);
        display.showActivityWidget(view);
      }
    };

    fireFindRequest(Value.of(id), callback);
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

}
