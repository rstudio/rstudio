/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.validation.example.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.editor.ui.client.ValueBoxEditorDecorator;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

/**
 * Tests for {@link Book}.
 */
public class BookTest extends GWTTestCase {

  private Author author;
  private Book book;
  private BookWidget bookWidget;
  private Driver driver;

  private Validator validator;

  @Override
  public String getModuleName() {
    return "com.google.gwt.validation.example.ValidationExample";
  }

  public void testValidate_emptyAuthorLastName() {
    initValidBook();
    author.setLastName("");
    Set<ConstraintViolation<Book>> violations = validator.validate(book);
    assertEquals(1, violations.size());
    ConstraintViolation<Book> violation = violations.iterator().next();
    
    assertEquals(author, violation.getLeafBean());
    assertEquals(book, violation.getRootBean());
    assertEquals("author.lastName", violation.getPropertyPath().toString());
  }

  public void testValidate_valid() {
    initValidBook();
    Set<ConstraintViolation<Book>> violations = validator.validate(book);
    assertTrue(violations.isEmpty());
  }
  
  public void testErrorDisplay_noErrors() {
    initValidBook();
    driver.initialize(bookWidget);
    driver.edit(book);
    assertEquals("Smith", bookWidget.author.lastName.asEditor().getValue());
    
    Set<ConstraintViolation<Book>> violations = validator.validate(book);
    assertTrue(violations.isEmpty());
    
    driver.setConstraintViolations(doHorribleCast(violations));
  }
  
  public void testErrorDisplay_lastNameError() {
    initValidBook();
    driver.initialize(bookWidget);
    driver.edit(book);
    
    bookWidget.author.lastName.asEditor().setValue("");
    driver.flush();
    assertEquals(null, author.getLastName());
    
    Set<ConstraintViolation<Book>> violations = validator.validate(book);
    assertEquals(1, violations.size());
    
    driver.setConstraintViolations(doHorribleCast(violations));
    List<EditorError> errors = bookWidget.author.errors;
    // Note: Is the fact that there are 2 errors here (rather than just one) also a bug?
    assertEquals(2, errors.size());
    
    EditorError error = errors.get(0);
    assertEquals(bookWidget.author.lastName.asEditor(), error.getEditor());
    // There is a bug here - error.getPath() throws a StringIndexOutOfBoundsException.
    // The pathPrefixLength is set in ErrorCollector line 63. It breaks the toString() method
    // of SimpleError as well.
    assertEquals("author.lastName", error.getAbsolutePath());
  }

  @Override
  protected final void gwtSetUp() throws Exception {
    super.gwtSetUp();
    author = new Author();
    book = new Book();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
    bookWidget = new BookWidget();
    driver = GWT.create(Driver.class);
  }

  protected void initValidBook() {
    author.setFirstName("John");
    author.setLastName("Smith");
    author.setCompany("Google");
    book.setAuthor(author);
    book.setTitle("JSR-303 Validation in GWT");
  }
  
  @SuppressWarnings("unchecked")
  private Iterable<ConstraintViolation<?>> doHorribleCast(Object o) {
    return (Iterable<ConstraintViolation<?>>) o;
  }
  
  interface Driver extends SimpleBeanEditorDriver<Book, BookWidget> { }
  
  class BookWidget extends Composite implements Editor<Book> {
    AuthorWidget author = new AuthorWidget();
  }
  
  class AuthorWidget extends Composite implements Editor<Author> {
    ValueBoxEditorDecorator<String> lastName;

    List<EditorError> errors;
    
    AuthorWidget() {
      lastName = new ValueBoxEditorDecorator<String>() {
        @Override
        public void showErrors(List<EditorError> errors) {
          super.showErrors(errors);
          AuthorWidget.this.errors = errors;
        }
      };
      lastName.setValueBox(new TextBox());
    }
  }
}
