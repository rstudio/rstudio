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
package com.google.gwt.place.testplacemappers;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import com.google.gwt.place.testplaces.Place1;
import com.google.gwt.place.testplaces.Place6;
import com.google.gwt.place.testplaces.Tokenizer2;
import com.google.gwt.place.testplaces.Tokenizer3;
import com.google.gwt.place.testplaces.Tokenizer4;

/**
 * Used by tests of {@link com.google.gwt.place.rebind.PlaceHistoryMapperGenerator}.
 */
@WithTokenizers({
  Place1.Tokenizer.class, Tokenizer2.class, Tokenizer3.class,
  Tokenizer4.class, Place6.Tokenizer.class})
public interface NoFactory extends PlaceHistoryMapper {
}