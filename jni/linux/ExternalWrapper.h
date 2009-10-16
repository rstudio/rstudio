/*
 * Copyright 2006 Google Inc.
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

#ifndef EXTERNALWRAPPER_H_
#define EXTERNALWRAPPER_H_

class ExternalWrapper : public nsIScriptObjectOwner {
public:
  NS_DECL_ISUPPORTS
  NS_IMETHOD GetScriptObject(nsIScriptContext *aContext, void** aScriptObject);
  NS_IMETHOD SetScriptObject(void* aScriptObject);
  ExternalWrapper(): jsWindowExternalObject(0) { }
private:
  ~ExternalWrapper() { }
  void *jsWindowExternalObject;
};

class nsRpExternalFactory : public nsIFactory {
public:
  NS_DECL_ISUPPORTS
  NS_DECL_NSIFACTORY
  nsRpExternalFactory() { }
private:
  ~nsRpExternalFactory() { }
};

#define GWT_EXTERNAL_FACTORY_CID \
{ 0xF56E23F8, 0x5D06, 0x47F9, \
{ 0x88, 0x5A, 0xD9, 0xCA, 0xC3, 0x38, 0x41, 0x7F } }
#define GWT_EXTERNAL_CONTRACTID "@com.google/GWT/external;1"

#endif /*EXTERNALWRAPPER_H_*/
