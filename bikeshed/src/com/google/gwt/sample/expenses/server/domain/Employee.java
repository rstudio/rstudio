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
package com.google.gwt.sample.expenses.server.domain;

import org.datanucleus.jpa.annotations.Extension;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Version;

/**
 * The Employee domain object.
 */
@Entity
public class Employee {

  public static long countEmployees() {
    EntityManager em = entityManager();
    try {
      return ((Integer) em.createQuery("select count(o) from Employee o").getSingleResult()).intValue();
    } finally {
      em.close();
    }
  }

  public static final EntityManager entityManager() {
    return EMF.get().createEntityManager();
  }

  @SuppressWarnings("unchecked")
  public static List<Employee> findAllEmployees() {
    EntityManager em = entityManager();
    try {
      List<Employee> list = em.createQuery("select o from Employee o").getResultList();
      // force to get all the employees
      list.size();
      return list;
    } finally {
      em.close();
    }
  }

  public static Employee findEmployee(String id) {
    if (id == null) {
      return null;
    }
    EntityManager em = entityManager();
    try {
      return em.find(Employee.class, id);
    } finally {
      em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Employee> findEmployeeEntries(int firstResult,
      int maxResults) {
    EntityManager em = entityManager();
    try {
      return em.createQuery("select o from Employee o").setFirstResult(
          firstResult).setMaxResults(maxResults).getResultList();
    } finally {
      em.close();
    }
  }

  private String userName;

  private String displayName;

  private String password;

  @JoinColumn
  private String supervisorKey;

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Extension(vendorName = "datanucleus", key = "gae.encoded-pk", value = "true")
  private String id;

  @Version
  @Column(name = "version")
  private Long version;

  public String getDisplayName() {
    return this.displayName;
  }

  public String getId() {
    return this.id;
  }

  public String getPassword() {
    return this.password;
  }

  public String getSupervisor() {
    return supervisorKey;
  }

  public String getUserName() {
    return this.userName;
  }

  public Long getVersion() {
    return this.version;
  }

  public void persist() {
    EntityManager em = entityManager();
    try {
      em.persist(this);
    } finally {
      em.close();
    }
  }

  public void remove() {
    EntityManager em = entityManager();
    try {
      Employee attached = em.find(Employee.class, this.id);
      em.remove(attached);
    } finally {
      em.close();
    }
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setSupervisorKey(String supervisorKey) {
    this.supervisorKey = supervisorKey;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Id: ").append(getId()).append(", ");
    sb.append("Version: ").append(getVersion()).append(", ");
    sb.append("UserName: ").append(getUserName()).append(", ");
    sb.append("DisplayName: ").append(getDisplayName()).append(", ");
    sb.append("Password: ").append(getPassword()).append(", ");
    return sb.toString();
  }

}
