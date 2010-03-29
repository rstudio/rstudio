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
package com.google.gwt.sample.bikeshed.mail.client;

import com.google.gwt.bikeshed.cells.client.CheckboxCell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.Header;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.shared.ListListModel;
import com.google.gwt.bikeshed.list.shared.SelectionModel;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A demo of selection features.
 */
public class MailSample implements EntryPoint {

  class Message {
    int id;
    boolean isRead;
    boolean isSelected;
    String sender;
    String subject;

    public Message(int id, String sender, String subject) {
      super();
      this.id = id;
      this.sender = sender;
      this.subject = subject;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Message)) {
        return false;
      }
      return id == ((Message) obj).id;
    }

    public int getId() {
      return id;
    }

    public String getSender() {
      return sender;
    }

    public String getSubject() {
      return subject;
    }

    @Override
    public int hashCode() {
      return id;
    }

    public boolean isRead() {
      return isRead;
    }

    public boolean isSelected() {
      return isSelected;
    }

    @Override
    public String toString() {
      return "Message [id=" + id + ", isSelected=" + isSelected + ", sender="
          + sender + ", subject=" + subject + ", read=" + isRead + "]";
    }
  }

  class MailSelectionModel implements SelectionModel<Message> {
    private Set<Integer> plusExceptions = new HashSet<Integer>();
    private Set<Integer> minusExceptions = new HashSet<Integer>();

    private boolean isDefaultSelected(Message object) {
      return object.isRead();
    }

    public boolean isSelected(Message object) {
      return (isDefaultSelected(object) || plusExceptions.contains(object.id))
          && !minusExceptions.contains(object.id);
    }

    public void setSelected(Message object, boolean selected) {
      if (!selected) {
        minusExceptions.add(object.id);
        plusExceptions.remove(object.id);
      } else {
        minusExceptions.remove(object.id);
        plusExceptions.add(object.id);
      }
    }
  }

  private static final String[] senders = {
      "test@example.com", "spam1@spam.com", "gwt@google.com"};

  private static final String[] subjects = {
      "GWT rocks", "What's a widget?", "Money in Nigeria"};

  public void onModuleLoad() {
    TextCell textCell = new TextCell();

    ListListModel<Message> listModel = new ListListModel<Message>();
    List<Message> messages = listModel.getList();
    Random rand = new Random();
    for (int i = 0; i < 1000; i++) {
      Message message = new Message(i, senders[rand.nextInt(senders.length)],
          subjects[rand.nextInt(subjects.length)]);
      message.isRead = rand.nextBoolean();
      messages.add(message);
    }

    final SelectionModel<Message> selectionModel = new MailSelectionModel();
    
    final PagingTableListView<Message> table =
      new PagingTableListView<Message>(listModel, 10);

    Column<Message, Boolean, Void> selectedColumn = new Column<Message, Boolean, Void>(
        new CheckboxCell()) {
      @Override
      public Boolean getValue(Message object) {
        return selectionModel.isSelected(object);
      }
    };
    selectedColumn.setFieldUpdater(new FieldUpdater<Message, Boolean, Void>() {
      public void update(int index, Message object, Boolean value, Void viewData) {
        selectionModel.setSelected(object, value);
        table.refresh(); // TODO - remove
      }
    });
    Header<String> selectedHeader = new Header<String>(textCell);
    selectedHeader.setValue("Selected");
    table.addColumn(selectedColumn, selectedHeader);

    Column<Message, String, Void> isReadColumn =
      new Column<Message, String, Void>(textCell) {
      @Override
      public String getValue(Message object) {
        return object.isRead ? "read" : "unread";
      }
    };
    Header<String> isReadHeader = new Header<String>(textCell);
    isReadHeader.setValue("Read");
    table.addColumn(isReadColumn, isReadHeader);

    Column<Message, String, Void> senderColumn = new Column<Message, String, Void>(
        new TextCell()) {
      @Override
      public String getValue(Message object) {
        return object.getSender();
      }
    };
    Header<String> senderHeader = new Header<String>(textCell);
    senderHeader.setValue("Sender");
    table.addColumn(senderColumn, senderHeader);

    Column<Message, String, Void> subjectColumn = new Column<Message, String, Void>(
        textCell) {
      @Override
      public String getValue(Message object) {
        return object.getSubject();
      }
    };
    Header<String> subjectHeader = new Header<String>(textCell);
    subjectHeader.setValue("Subject");
    table.addColumn(subjectColumn, subjectHeader);

    table.setSelectionModel(selectionModel);

    RootPanel.get().add(table);
  }
}
