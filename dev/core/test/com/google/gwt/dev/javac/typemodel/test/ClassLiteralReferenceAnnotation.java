/**
 * 
 */
package com.google.gwt.dev.javac.typemodel.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which references class literals.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD,
    ElementType.METHOD, ElementType.PACKAGE, ElementType.PARAMETER,
    ElementType.TYPE})
public @interface ClassLiteralReferenceAnnotation {
  /**
   * Class literals used with this annotation need to be a Foo or subtype
   * thereof.
   */
  public class Foo {
  }

  Class<? extends Foo> value();
}
