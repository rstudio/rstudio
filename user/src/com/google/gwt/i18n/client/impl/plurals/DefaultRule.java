/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.i18n.client.impl.plurals;

import com.google.gwt.i18n.client.Localizable;
import com.google.gwt.i18n.client.PluralRule;

/**
 * Default implementation of plural rules.  The i18n generator will
 * substitute subclasses of this class, which may reside anywhere in
 * the package hierarchy, based on the locale.  See Localizable
 * for more details on this process.
 * 
 * The default implementation here always selects the default form.
 * 
 * To define a new language, simply declare a subclass of this class
 * that is named with the locale you want to specify, such as
 * DefaultRule_en_uk for English spoken in the UK (note the lower-cased
 * tag).
 * 
 * This is an implementation of the language pluralization rules described
 * at http://translate.sourceforge.net/wiki/l10n/pluralforms
 * 
 * Eventually, all these rules will be machine generated from Unicode's
 * CLDR, perhaps with some additional data that isn't kept there if
 * necessary.  The current subclasses are defined just to get reasonable
 * plural support for most of the common languages -- in particular, you
 * should not rely on particular keywords for the plural forms of a
 * given language.
 */
public class DefaultRule implements Localizable, PluralRule {

  public PluralForm[] pluralForms() {
    return new PluralForm[] {
        new PluralForm("other", "Default plural form"),
    };
  }

  public int select(int n) {
    return 0;
  }

}
