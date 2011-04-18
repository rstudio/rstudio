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
package com.google.web.bindery.autobean.gwt.rebind.model;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Describes an AutoBean.
 */
public class AutoBeanType {

  /**
   * Builder.
   */
  public static class Builder {
    private boolean affectedByCategories;
    private String beanSimpleSourceName;
    private String categorySuffix;
    private AutoBeanType toReturn = new AutoBeanType();

    public AutoBeanType build() {
      // Different implementations necessary for category-affected impls
      toReturn.simpleSourceName = beanSimpleSourceName
          + (affectedByCategories ? categorySuffix : "");
      try {
        return toReturn;
      } finally {
        toReturn = null;
      }
    }

    public void setInterceptor(JMethod interceptor) {
      affectedByCategories = interceptor != null;
      toReturn.interceptor = interceptor;
    }

    public void setMethods(List<AutoBeanMethod> methods) {
      toReturn.methods = new ArrayList<AutoBeanMethod>(methods);
      Collections.sort(toReturn.methods, new Comparator<AutoBeanMethod>() {
        public int compare(AutoBeanMethod o1, AutoBeanMethod o2) {
          int c = o1.getAction().compareTo(o2.getAction());
          if (c != 0) {
            return c;
          }
          // Name alone would cause overload conflicts
          return o1.getMethod().getReadableDeclaration().compareTo(
              o2.getMethod().getReadableDeclaration());
        }
      });
      toReturn.methods = Collections.unmodifiableList(toReturn.methods);

      toReturn.simpleBean = true;
      for (AutoBeanMethod method : methods) {
        if (method.getAction().equals(JBeanMethod.CALL)) {
          if (method.getStaticImpl() == null) {
            toReturn.simpleBean = false;
          } else {
            affectedByCategories = true;
          }
        }
      }
    }

    public void setNoWrap(boolean noWrap) {
      toReturn.noWrap = noWrap;
    }

    public void setOwnerFactory(AutoBeanFactoryModel autoBeanFactoryModel) {
      if (autoBeanFactoryModel.getCategoryTypes() == null) {
        return;
      }
      StringBuilder sb = new StringBuilder();
      for (JClassType category : autoBeanFactoryModel.getCategoryTypes()) {
        sb.append("_").append(
            category.getQualifiedSourceName().replace('.', '_'));
      }
      categorySuffix = sb.toString();
    }

    public void setPeerType(JClassType type) {
      assert type.isParameterized() == null && type.isRawType() == null;
      toReturn.peerType = type;
      String packageName = type.getPackage().getName();
      if (packageName.startsWith("java")) {
        packageName = "emul." + packageName;
      }
      toReturn.packageName = packageName;
      beanSimpleSourceName = type.getName().replace('.', '_') + "AutoBean";
    }
  }

  private JMethod interceptor;
  private List<AutoBeanMethod> methods;
  private boolean noWrap;
  private String packageName;
  private JClassType peerType;
  private boolean simpleBean;
  private String simpleSourceName;

  private AutoBeanType() {
  }

  /**
   * A method that is allowed to intercept and modify return values from
   * getters.
   */
  public JMethod getInterceptor() {
    return interceptor;
  }

  public List<AutoBeanMethod> getMethods() {
    return methods;
  }

  public String getPackageNome() {
    return packageName;
  }

  public JClassType getPeerType() {
    return peerType;
  }

  public String getQualifiedSourceName() {
    return getPackageNome() + "." + getSimpleSourceName();
  }

  public String getSimpleSourceName() {
    return simpleSourceName;
  }

  public boolean isNoWrap() {
    return noWrap;
  }

  /**
   * A simple bean has only getters and setters.
   */
  public boolean isSimpleBean() {
    return simpleBean;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return peerType.toString();
  }
}
