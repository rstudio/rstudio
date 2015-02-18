/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.gss;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssClassSelectorNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.VisitController;

import junit.framework.TestCase;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test for {@link ClassNamesCollector}.
 */
public class ClassNamesCollectorTest extends TestCase {
  private CssClassSelectorNode cssClassSelectorNode;
  private CssTree cssTree;
  private VisitController visitController;
  private ClassNamesCollector classNamesCollector;

  @Override
  protected void setUp() {
    classNamesCollector = new ClassNamesCollector();
    cssClassSelectorNode = mock(CssClassSelectorNode.class);
    cssTree = mock(CssTree.class);
    visitController = mock(VisitController.class);

    when(cssTree.getVisitController()).thenReturn(visitController);
  }

  public void testGetClassNames_noImport_collectAllClasses() {
    // Given
    List<CssClassSelectorNode> classSelectorNodes = createClassSelectorNodes("class1", "class2",
        "class3");
    whenVisitorControllerVisitTreeThenVisitAllNodes(classSelectorNodes);

    // When
    Set<String> classNames = classNamesCollector.getClassNames(cssTree, new HashSet<JClassType>());

    // Then
    assertEquals(3, classNames.size());
    assertTrue(classNames.contains("class1"));
    assertTrue(classNames.contains("class2"));
    assertTrue(classNames.contains("class3"));
  }

  public void testGetClassNames_withImport_dontCollectImportedClasses() {
    // Given
    List<CssClassSelectorNode> classSelectorNodes = createClassSelectorNodes("class1", "class2",
        "class3", "Imported-class", "ImportedFakeClass", "otherImport-class");
    whenVisitorControllerVisitTreeThenVisitAllNodes(classSelectorNodes);

    Set<JClassType> importedType = new HashSet<JClassType>();

    // mock a class without @ImportedWithPrefix annotation
    JClassType importedClassType = mock(JClassType.class);
    when(importedClassType.getSimpleSourceName()).thenReturn("Imported");
    importedType.add(importedClassType);

    // mock a class with a @ImportedWithPrefix annotation
    JClassType importedWithPrefixClassType = mock(JClassType.class);
    when(importedWithPrefixClassType.getSimpleSourceName()).thenReturn("ImportedWithPrefix");
    ImportedWithPrefix importedWithPrefixAnnotation = mock(ImportedWithPrefix.class);
    when(importedWithPrefixAnnotation.value()).thenReturn("otherImport");
    when(importedWithPrefixClassType.getAnnotation(ImportedWithPrefix.class))
        .thenReturn(importedWithPrefixAnnotation);
    importedType.add(importedWithPrefixClassType);

    // When
    Set<String> classNames = classNamesCollector.getClassNames(cssTree, importedType);

    // Then
    assertEquals(4, classNames.size());
    assertTrue(classNames.contains("class1"));
    assertTrue(classNames.contains("class2"));
    assertTrue(classNames.contains("class3"));
    assertTrue(classNames.contains("ImportedFakeClass"));
    assertFalse(classNames.contains("otherImport-class"));
    assertFalse(classNames.contains("Imported-class"));
  }

  private List<CssClassSelectorNode> createClassSelectorNodes(String... classNames) {
    List<CssClassSelectorNode> classSelectorNodes = new ArrayList<CssClassSelectorNode>(classNames
        .length);

    for (String className : classNames) {
      CssClassSelectorNode node = mock(CssClassSelectorNode.class);
      when(node.getRefinerName()).thenReturn(className);
      classSelectorNodes.add(node);
    }

    return classSelectorNodes;
  }

  private void whenVisitorControllerVisitTreeThenVisitAllNodes(final List<CssClassSelectorNode>
      nodes) {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        for (CssClassSelectorNode node : nodes) {
          classNamesCollector.enterClassSelector(node);
        }
        return null;
      }
    }).when(visitController).startVisit(classNamesCollector);
  }
}
