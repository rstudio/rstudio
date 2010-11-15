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
package com.google.gwt.sample.validationtck;

import com.google.gwt.validation.client.GwtValidation;

import org.hibernate.jsr303.tck.tests.constraints.application.SuperWoman;
import org.hibernate.jsr303.tck.tests.constraints.application.Woman;

import javax.validation.Validator;

/**
 * Top Level validator for the TCK tests
 */
@GwtValidation(value = {
    Woman.class,
    SuperWoman.class
    })
public interface TckValidator extends Validator {
}
