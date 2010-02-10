/**
 * 
 */
package com.google.gwt.list.client;

import com.google.gwt.cells.client.Cell;
import com.google.gwt.cells.client.Mutator;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

public abstract class Column<T, C> {
  private final Cell<C> cell;
  private Mutator<T, C> mutator;

  public Column(Cell<C> cell) {
    this.cell = cell;
  }

  public void onBrowserEvent(Element elem, final T object, NativeEvent event) {
    cell.onBrowserEvent(elem, getValue(object), event, new Mutator<C, C>() {
      public void mutate(C unused, C after) {
        mutator.mutate(object, after);
      }
    });
  }

  public void render(T object, StringBuilder sb) {
    cell.render(getValue(object), sb);
  }

  public void setMutator(Mutator<T, C> mutator) {
    this.mutator = mutator;
  }

  protected abstract C getValue(T object);

  protected Cell<C> getCell() {
    return cell;
  }
}