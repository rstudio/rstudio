/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.dynatablerf.client.widgets;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory.ScheduleRequest;
import com.google.gwt.sample.dynatablerf.shared.TimeSlotProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.shared.Receiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Edits a list of time slots.
 */
public class TimeSlotListWidget extends Composite implements ValueAwareEditor<List<TimeSlotProxy>> {

  interface TableResources extends CellTable.Resources {
    @Override
    @Source(value = {CellTable.Style.DEFAULT_CSS, "CellTablePatch.css"})
    CellTable.Style cellTableStyle();
  }

  interface TimeSlotListWidgetUiBinder extends UiBinder<Widget, TimeSlotListWidget> {
  }
  
  private class ScheduleRow {
    int hour;
    
    ScheduleRow(int hour) {
      this.hour = hour;
    }

    public int getHour() {
      return hour;
    }

    public boolean isInUse(WeekDay day) {
      return currentSchedule.contains(new TimeSlotKey(day, hour));
    }
    
    public void toggleInUse(WeekDay day) {
      final TimeSlotKey key = new TimeSlotKey(day, hour);
      if (currentSchedule.contains(key)) {
        currentSchedule.remove(key);
        table.redraw();
      } else if (!existingSlots.containsKey(key)) {
        acceptClicks = false;
        ScheduleRequest context = factory.scheduleRequest();
        context.createTimeSlot(day.ordinal(), hour * 60, hour * 60 + 50).fire(
            new Receiver<TimeSlotProxy>() {
              @Override
              public void onSuccess(TimeSlotProxy slot) {
                existingSlots.put(key, slot);
                backing.add(slot);
                currentSchedule.add(key);
                table.redraw();
                acceptClicks = true;
              }
        });
      } else {
        currentSchedule.add(key);        
        table.redraw();
      }
    }
  }

  private static class TimeSlotKey {
    private int hour;
    private WeekDay day;
    
    TimeSlotKey(WeekDay day, int hour) {
      this.day = day;
      this.hour = hour;
    }
    
    TimeSlotKey(TimeSlotProxy slot) {
      day = WeekDay.fromInt(slot.getDayOfWeek());
      hour = slot.getStartMinutes() / 60;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      TimeSlotKey other = (TimeSlotKey) obj;
      if (day == null) {
        if (other.day != null) {
          return false;
        }
      } else if (!day.equals(other.day)) {
        return false;
      }
      if (hour != other.hour) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return 31 * (31 + ((day == null) ? 0 : day.hashCode())) + hour;
    }
  }

  private class WeekDayColumn extends Column<ScheduleRow, String> {
    private WeekDay day;

    public WeekDayColumn(WeekDay day) {
      super(new ClickableTextCell());
      this.day = day;
    }

    @Override
    public String getValue(ScheduleRow row) {
      if (day == null) {
        int hour = row.getHour();
        return Integer.toString(hour <= 12 ? hour : hour - 12) + ":00" 
            + ((hour < 12) ? "AM" : "PM");
      }
      return row.isInUse(day) ? "X" : ".";
    }
  }
  
  private static final int ROWS_IN_A_DAY = 9;
  private static final int FIRST_HOUR = 8;
  
  private static TimeSlotListWidgetUiBinder uiBinder = GWT.create(
      TimeSlotListWidgetUiBinder.class);

  @UiField(provided = true)
  CellTable<ScheduleRow> table;
  
  private enum WeekDay {
    SUNDAY("Su"), MONDAY("Mo"), TUESDAY("Tu"), WEDNESDAY("We"), 
    THURSDAY("Th"), FRIDAY("Fr"), SATURDAY("Sa");
  
    public static WeekDay fromInt(int ordinal) {
      return values()[ordinal];
    }

    private String shortName;
    
    WeekDay(String shortName) {
      this.shortName = shortName;
    }
  
    public String getShortName() {
      return shortName;
    }
  }
  
  private List<TimeSlotProxy> backing;
  private HashSet<TimeSlotKey> currentSchedule;
  private HashMap<TimeSlotKey, TimeSlotProxy> existingSlots;
  private DynaTableRequestFactory factory;
  private HashSet<TimeSlotKey> initialSchedule;
  private boolean acceptClicks = true;
  
  public TimeSlotListWidget(DynaTableRequestFactory factory) {
    this.factory = factory;
    table = new CellTable<TimeSlotListWidget.ScheduleRow>(ROWS_IN_A_DAY,
        GWT.<TableResources> create(TableResources.class));
    table.addColumn(new WeekDayColumn(null), "Hour");      
    for (WeekDay day : WeekDay.values()) {
      WeekDayColumn col = new WeekDayColumn(day);
      
      class Updater implements FieldUpdater<ScheduleRow, String> {
        private WeekDay columnDay;
        
        public Updater(WeekDay day) {
          columnDay = day;
        }
        
        @Override
        public void update(int index, ScheduleRow row, String value) {
          if (acceptClicks) {
            row.toggleInUse(columnDay);
          }
        }
      }
      
      FieldUpdater<ScheduleRow, String> fieldUpdater = new Updater(day);
      col.setFieldUpdater(fieldUpdater);
      table.addColumn(col, day.getShortName());      
    }
    
    table.setRowCount(ROWS_IN_A_DAY, false);
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    initWidget(uiBinder.createAndBindUi(this));
  }
  
  @Override
  public void flush() {
    HashMap<TimeSlotProxy, TimeSlotKey> index = new HashMap<TimeSlotProxy, TimeSlotKey>();
    
    for (TimeSlotProxy slot : backing) {
      index.put(slot, new TimeSlotKey(slot));
    }

    // Compute slots that need to be removed from the backing
    initialSchedule.removeAll(currentSchedule);
    
    for (Iterator<TimeSlotProxy> iterator = backing.iterator(); iterator.hasNext();) {
      TimeSlotProxy slot = iterator.next();
      TimeSlotKey key = index.get(slot);
      if (initialSchedule.contains(key)) {
        iterator.remove();
      }
    }    
  }

  @Override
  public void onPropertyChange(String... paths) {
  }

  @Override
  public void setDelegate(EditorDelegate<List<TimeSlotProxy>> delegate) {
  }

  @Override
  public void setValue(List<TimeSlotProxy> value) {
    backing = value;
    currentSchedule = new HashSet<TimeSlotKey>();
    existingSlots = new HashMap<TimeSlotKey, TimeSlotProxy>();
    
    initialSchedule = new HashSet<TimeSlotKey>();
    
    for (TimeSlotProxy slot : backing) {
      TimeSlotKey key = new TimeSlotKey(slot);
      currentSchedule.add(key);
      existingSlots.put(key, slot);
      initialSchedule.add(new TimeSlotKey(slot));
    }
    
    ArrayList<ScheduleRow> rows = new ArrayList<ScheduleRow>(ROWS_IN_A_DAY);
    for (int i = 0; i < ROWS_IN_A_DAY; i++) {
      rows.add(new ScheduleRow(FIRST_HOUR + i));
    }
    table.setRowData(rows);
  }

}
