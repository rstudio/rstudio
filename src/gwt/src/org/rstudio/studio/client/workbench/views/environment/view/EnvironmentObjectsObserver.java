package org.rstudio.studio.client.workbench.views.environment.view;

public interface EnvironmentObjectsObserver
{
   void viewObject(String objectName);
   void setObjectExpanded(String objectName);
   void setObjectCollapsed(String objectName);
   void setPersistedScrollPosition(int scrollPosition);
   void changeContextDepth(int newDepth);
   void setViewDirty();
   boolean getHideInternalFunctions();
   void setHideInternalFunctions(boolean hide);
}