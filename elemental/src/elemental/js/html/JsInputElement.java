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
package elemental.js.html;
import elemental.html.ValidityState;
import elemental.js.dom.JsElement;
import elemental.html.InputElement;
import elemental.dom.Element;
import elemental.html.FileList;
import elemental.html.FormElement;
import elemental.js.dom.JsNodeList;
import elemental.dom.NodeList;
import elemental.js.events.JsEventListener;
import elemental.events.EventListener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsInputElement extends JsElement  implements InputElement {
  protected JsInputElement() {}

  public final native String getAccept() /*-{
    return this.accept;
  }-*/;

  public final native void setAccept(String param_accept) /*-{
    this.accept = param_accept;
  }-*/;

  public final native String getAlign() /*-{
    return this.align;
  }-*/;

  public final native void setAlign(String param_align) /*-{
    this.align = param_align;
  }-*/;

  public final native String getAlt() /*-{
    return this.alt;
  }-*/;

  public final native void setAlt(String param_alt) /*-{
    this.alt = param_alt;
  }-*/;

  public final native String getAutocomplete() /*-{
    return this.autocomplete;
  }-*/;

  public final native void setAutocomplete(String param_autocomplete) /*-{
    this.autocomplete = param_autocomplete;
  }-*/;

  public final native boolean isAutofocus() /*-{
    return this.autofocus;
  }-*/;

  public final native void setAutofocus(boolean param_autofocus) /*-{
    this.autofocus = param_autofocus;
  }-*/;

  public final native boolean isChecked() /*-{
    return this.checked;
  }-*/;

  public final native void setChecked(boolean param_checked) /*-{
    this.checked = param_checked;
  }-*/;

  public final native boolean isDefaultChecked() /*-{
    return this.defaultChecked;
  }-*/;

  public final native void setDefaultChecked(boolean param_defaultChecked) /*-{
    this.defaultChecked = param_defaultChecked;
  }-*/;

  public final native String getDefaultValue() /*-{
    return this.defaultValue;
  }-*/;

  public final native void setDefaultValue(String param_defaultValue) /*-{
    this.defaultValue = param_defaultValue;
  }-*/;

  public final native String getDirName() /*-{
    return this.dirName;
  }-*/;

  public final native void setDirName(String param_dirName) /*-{
    this.dirName = param_dirName;
  }-*/;

  public final native boolean isDisabled() /*-{
    return this.disabled;
  }-*/;

  public final native void setDisabled(boolean param_disabled) /*-{
    this.disabled = param_disabled;
  }-*/;

  public final native JsFileList getFiles() /*-{
    return this.files;
  }-*/;

  public final native void setFiles(FileList param_files) /*-{
    this.files = param_files;
  }-*/;

  public final native JsFormElement getForm() /*-{
    return this.form;
  }-*/;

  public final native String getFormAction() /*-{
    return this.formAction;
  }-*/;

  public final native void setFormAction(String param_formAction) /*-{
    this.formAction = param_formAction;
  }-*/;

  public final native String getFormEnctype() /*-{
    return this.formEnctype;
  }-*/;

  public final native void setFormEnctype(String param_formEnctype) /*-{
    this.formEnctype = param_formEnctype;
  }-*/;

  public final native String getFormMethod() /*-{
    return this.formMethod;
  }-*/;

  public final native void setFormMethod(String param_formMethod) /*-{
    this.formMethod = param_formMethod;
  }-*/;

  public final native boolean isFormNoValidate() /*-{
    return this.formNoValidate;
  }-*/;

  public final native void setFormNoValidate(boolean param_formNoValidate) /*-{
    this.formNoValidate = param_formNoValidate;
  }-*/;

  public final native String getFormTarget() /*-{
    return this.formTarget;
  }-*/;

  public final native void setFormTarget(String param_formTarget) /*-{
    this.formTarget = param_formTarget;
  }-*/;

  public final native int getHeight() /*-{
    return this.height;
  }-*/;

  public final native void setHeight(int param_height) /*-{
    this.height = param_height;
  }-*/;

  public final native boolean isIncremental() /*-{
    return this.incremental;
  }-*/;

  public final native void setIncremental(boolean param_incremental) /*-{
    this.incremental = param_incremental;
  }-*/;

  public final native boolean isIndeterminate() /*-{
    return this.indeterminate;
  }-*/;

  public final native void setIndeterminate(boolean param_indeterminate) /*-{
    this.indeterminate = param_indeterminate;
  }-*/;

  public final native JsNodeList getLabels() /*-{
    return this.labels;
  }-*/;

  public final native String getMax() /*-{
    return this.max;
  }-*/;

  public final native void setMax(String param_max) /*-{
    this.max = param_max;
  }-*/;

  public final native int getMaxLength() /*-{
    return this.maxLength;
  }-*/;

  public final native void setMaxLength(int param_maxLength) /*-{
    this.maxLength = param_maxLength;
  }-*/;

  public final native String getMin() /*-{
    return this.min;
  }-*/;

  public final native void setMin(String param_min) /*-{
    this.min = param_min;
  }-*/;

  public final native boolean isMultiple() /*-{
    return this.multiple;
  }-*/;

  public final native void setMultiple(boolean param_multiple) /*-{
    this.multiple = param_multiple;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native EventListener getOnwebkitspeechchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitspeechchange);
  }-*/;

  public final native void setOnwebkitspeechchange(EventListener listener) /*-{
    this.onwebkitspeechchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native String getPattern() /*-{
    return this.pattern;
  }-*/;

  public final native void setPattern(String param_pattern) /*-{
    this.pattern = param_pattern;
  }-*/;

  public final native String getPlaceholder() /*-{
    return this.placeholder;
  }-*/;

  public final native void setPlaceholder(String param_placeholder) /*-{
    this.placeholder = param_placeholder;
  }-*/;

  public final native boolean isReadOnly() /*-{
    return this.readOnly;
  }-*/;

  public final native void setReadOnly(boolean param_readOnly) /*-{
    this.readOnly = param_readOnly;
  }-*/;

  public final native boolean isRequired() /*-{
    return this.required;
  }-*/;

  public final native void setRequired(boolean param_required) /*-{
    this.required = param_required;
  }-*/;

  public final native String getSelectionDirection() /*-{
    return this.selectionDirection;
  }-*/;

  public final native void setSelectionDirection(String param_selectionDirection) /*-{
    this.selectionDirection = param_selectionDirection;
  }-*/;

  public final native int getSelectionEnd() /*-{
    return this.selectionEnd;
  }-*/;

  public final native void setSelectionEnd(int param_selectionEnd) /*-{
    this.selectionEnd = param_selectionEnd;
  }-*/;

  public final native int getSelectionStart() /*-{
    return this.selectionStart;
  }-*/;

  public final native void setSelectionStart(int param_selectionStart) /*-{
    this.selectionStart = param_selectionStart;
  }-*/;

  public final native int getSize() /*-{
    return this.size;
  }-*/;

  public final native void setSize(int param_size) /*-{
    this.size = param_size;
  }-*/;

  public final native String getSrc() /*-{
    return this.src;
  }-*/;

  public final native void setSrc(String param_src) /*-{
    this.src = param_src;
  }-*/;

  public final native String getStep() /*-{
    return this.step;
  }-*/;

  public final native void setStep(String param_step) /*-{
    this.step = param_step;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native void setType(String param_type) /*-{
    this.type = param_type;
  }-*/;

  public final native String getUseMap() /*-{
    return this.useMap;
  }-*/;

  public final native void setUseMap(String param_useMap) /*-{
    this.useMap = param_useMap;
  }-*/;

  public final native String getValidationMessage() /*-{
    return this.validationMessage;
  }-*/;

  public final native JsValidityState getValidity() /*-{
    return this.validity;
  }-*/;

  public final native String getValue() /*-{
    return this.value;
  }-*/;

  public final native void setValue(String param_value) /*-{
    this.value = param_value;
  }-*/;

  public final native Date getValueAsDate() /*-{
    return this.valueAsDate;
  }-*/;

  public final native void setValueAsDate(Date param_valueAsDate) /*-{
    this.valueAsDate = param_valueAsDate;
  }-*/;

  public final native double getValueAsNumber() /*-{
    return this.valueAsNumber;
  }-*/;

  public final native void setValueAsNumber(double param_valueAsNumber) /*-{
    this.valueAsNumber = param_valueAsNumber;
  }-*/;

  public final native boolean isWebkitGrammar() /*-{
    return this.webkitGrammar;
  }-*/;

  public final native void setWebkitGrammar(boolean param_webkitGrammar) /*-{
    this.webkitGrammar = param_webkitGrammar;
  }-*/;

  public final native boolean isWebkitSpeech() /*-{
    return this.webkitSpeech;
  }-*/;

  public final native void setWebkitSpeech(boolean param_webkitSpeech) /*-{
    this.webkitSpeech = param_webkitSpeech;
  }-*/;

  public final native boolean isWebkitdirectory() /*-{
    return this.webkitdirectory;
  }-*/;

  public final native void setWebkitdirectory(boolean param_webkitdirectory) /*-{
    this.webkitdirectory = param_webkitdirectory;
  }-*/;

  public final native int getWidth() /*-{
    return this.width;
  }-*/;

  public final native void setWidth(int param_width) /*-{
    this.width = param_width;
  }-*/;

  public final native boolean isWillValidate() /*-{
    return this.willValidate;
  }-*/;

  public final native boolean checkValidity() /*-{
    return this.checkValidity();
  }-*/;

  public final native void select() /*-{
    this.select();
  }-*/;

  public final native void setCustomValidity(String error) /*-{
    this.setCustomValidity(error);
  }-*/;

  public final native void setSelectionRange(int start, int end) /*-{
    this.setSelectionRange(start, end);
  }-*/;

  public final native void setSelectionRange(int start, int end, String direction) /*-{
    this.setSelectionRange(start, end, direction);
  }-*/;

  public final native void stepDown() /*-{
    this.stepDown();
  }-*/;

  public final native void stepDown(int n) /*-{
    this.stepDown(n);
  }-*/;

  public final native void stepUp() /*-{
    this.stepUp();
  }-*/;

  public final native void stepUp(int n) /*-{
    this.stepUp(n);
  }-*/;
}
