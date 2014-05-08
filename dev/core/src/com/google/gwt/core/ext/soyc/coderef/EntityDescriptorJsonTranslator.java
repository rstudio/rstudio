/*
 * Copyright 2013 Google Inc.
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

package com.google.gwt.core.ext.soyc.coderef;

import com.google.gwt.core.ext.soyc.coderef.EntityDescriptor.Fragment;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Serialize/Deserialize EntityDescriptor instances to/from json.
 *
 */
public class EntityDescriptorJsonTranslator {

  private static class Deserializer {

    private final Map<Integer, MethodDescriptor> mapMethods = Maps.newHashMap();
    private final Map<MethodDescriptor, JSONArray> mapDependants = Maps.newIdentityHashMap();

    private PackageDescriptor readJson(JSONObject jsonObject) throws JSONException {
      String packageName = jsonObject.getString(ENTITY_NAME);
      PackageDescriptor packageDescriptor = readJsonPackage(jsonObject, packageName,
          packageName.equals(PackageDescriptor.DEFAULT_PKG) ? "" : packageName);
      setMethodDependencies();
      return packageDescriptor;
    }

    private void setMethodDependencies() throws JSONException {
      for (MethodDescriptor method : mapDependants.keySet()) {
        JSONArray dependants = mapDependants.get(method);
        for (int i = 0; i < dependants.length(); i++) {
          method.addDependant(mapMethods.get(dependants.getInt(i)));
        }
      }
    }

    private PackageDescriptor readJsonPackage(JSONObject jsonObject, String name,
        String longName) throws JSONException {
      PackageDescriptor descriptor = new PackageDescriptor(name, longName);
      JSONArray clss = jsonObject.getJSONArray(CLASSES);
      for (int i = 0; i < clss.length(); i++) {
        descriptor.addClass(readJsonClass(clss.getJSONObject(i), longName));
      }
      JSONArray packages = jsonObject.getJSONArray(PACKAGES);
      for (int i = 0; i < packages.length(); i++) {
        JSONObject subPackage = packages.getJSONObject(i);
        String packageName = subPackage.getString(ENTITY_NAME);
        descriptor.addPackage(readJsonPackage(subPackage, packageName,
            longName + (longName.isEmpty() ? "" : ".") + packageName));
      }
      return descriptor;
    }

    private ClassDescriptor readJsonClass(JSONObject jsonObject, String packageName)
        throws JSONException {
      ClassDescriptor descriptor = new ClassDescriptor(jsonObject.getString("name"), packageName);
      updateEntity(descriptor, jsonObject);
      JSONArray fields = jsonObject.getJSONArray(FIELDS);
      for (int i = 0; i < fields.length(); i++) {
        descriptor.addField(readJsonField(fields.getJSONObject(i), descriptor));
      }
      JSONArray methods = jsonObject.getJSONArray(METHODS);
      for (int i = 0; i < methods.length(); i++) {
        descriptor.addMethod(readJsonMethod(methods.getJSONObject(i), descriptor));
      }
      return descriptor;
    }

    private MethodDescriptor readJsonMethod(JSONObject jsonObject, ClassDescriptor classDescriptor)
        throws JSONException {
      MethodDescriptor method = new MethodDescriptor(classDescriptor,
          jsonObject.getString(ENTITY_NAME));
      updateEntity(method, jsonObject);
      method.setUniqueId(jsonObject.getInt(METHOD_ID));

      mapMethods.put(method.getUniqueId(), method);
      mapDependants.put(method, jsonObject.getJSONArray(METHOD_DEPENDENTS));

      return method;
    }

    private FieldDescriptor readJsonField(JSONObject jsonObject, ClassDescriptor classDescriptor)
        throws JSONException {
      String[] fullName = jsonObject.getString(ENTITY_NAME).split(":");
      FieldDescriptor fieldDescriptor = new FieldDescriptor(classDescriptor, fullName[0], fullName[1]);
      updateEntity(fieldDescriptor, jsonObject);
      return fieldDescriptor;
    }

    private void updateEntity(EntityDescriptor entity, JSONObject jsonObject) throws JSONException {
      JSONArray jsNames = jsonObject.getJSONArray(ENTITY_JS);
      for (int i = 0; i < jsNames.length(); i++) {
        entity.addObfuscatedName(jsNames.getString(i));
      }
      JSONArray frags = jsonObject.getJSONArray(EntityRecorder.FRAGMENTS);
      for (int i = 0; i < frags.length(); i++) {
        JSONObject frag = frags.getJSONObject(i);
        entity.addFragment(
            new Fragment(frag.getInt(EntityRecorder.FRAGMENT_ID),
                frag.getInt(EntityRecorder.FRAGMENT_SIZE)));
      }
    }
  }

  public static final String ENTITY_JS         = "js";
  public static final String ENTITY_NAME       = "name";
  public static final String FIELDS            = "fields";
  public static final String METHOD_ID         = "id";
  public static final String METHOD_DEPENDENTS = "dependents";
  public static final String METHODS           = "methods";
  public static final String CLASSES           = "classes";
  public static final String PACKAGES          = "packages";

  private static JSONObject writeJsonFromEntity(EntityDescriptor entity) throws JSONException {
    JSONObject json = new JSONObject();
    JSONArray fragments = new JSONArray();
    for (EntityDescriptor.Fragment frg : entity.getFragments()) {
      JSONObject frag = new JSONObject();
      frag.put(EntityRecorder.FRAGMENT_ID, frg.getId());
      frag.put(EntityRecorder.FRAGMENT_SIZE, frg.getSize());

      fragments.put(frag);
    }
    json.put(EntityRecorder.FRAGMENTS, fragments);
    json.put(ENTITY_JS, new JSONArray(entity.getObfuscatedNames()));
    return json;
  }

  private static JSONObject writeJsonFromMember(MemberDescriptor entity) throws JSONException {
    JSONObject json = writeJsonFromEntity(entity);
    json.put(ENTITY_NAME, entity.getJsniSignature());
    return json;
  }

  public static JSONObject writeJson(PackageDescriptor pkg) throws JSONException {
    JSONObject json = new JSONObject();
    json.put(ENTITY_NAME, pkg.getName());
    // classes
    JSONArray classes = new JSONArray();
    for (ClassDescriptor classDescriptor : pkg.getClasses()) {
      JSONObject jsonClass = writeJsonFromEntity(classDescriptor);
      jsonClass.put(ENTITY_NAME, classDescriptor.getName());
      // fields
      JSONArray fields = new JSONArray();
      for (FieldDescriptor fieldDescriptor : classDescriptor.getFields()) {
        fields.put(writeJsonFromMember(fieldDescriptor));
      }
      jsonClass.put(FIELDS, fields);
      // methods
      JSONArray methods = new JSONArray();
      for (MethodDescriptor methodDescriptor : classDescriptor.getMethods()) {
        JSONObject jsonMethod = writeJsonFromMember(methodDescriptor);
        jsonMethod.put(METHOD_ID, methodDescriptor.getUniqueId());
        jsonMethod.put(METHOD_DEPENDENTS, new JSONArray(methodDescriptor.getDependentPointers()));
        methods.put(jsonMethod);
      }
      jsonClass.put(METHODS, methods);

      classes.put(jsonClass);
    }
    json.put(CLASSES, classes);
    // packages
    JSONArray packages = new JSONArray();
    for (PackageDescriptor packageDescriptor : pkg.getPackages()) {
      packages.put(writeJson(packageDescriptor));
    }
    json.put(PACKAGES, packages);

    return json;
  }

  public static PackageDescriptor readJson(JSONObject jsonObject) throws JSONException {
    return new Deserializer().readJson(jsonObject);
  }

  private EntityDescriptorJsonTranslator() { }
}
