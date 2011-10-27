package org.rstudio.studio.client.projects.model;

public class NewProjectResult
{
   public NewProjectResult(String projectFile, 
                           String newDefaultProjectLocation,
                           String gitRepoUrl)
   {
      projectFile_ = projectFile;
      newDefaultProjectLocation_ = newDefaultProjectLocation;
      gitRepoUrl_ = gitRepoUrl;
   }
   
   public String getProjectFile()
   {
      return projectFile_;
   }
   
   public String getNewDefaultProjectLocation()
   {
      return newDefaultProjectLocation_;
   }

   public String getGitRepoUrl()
   {
      return gitRepoUrl_;
   }

   private final String projectFile_;
   private final String newDefaultProjectLocation_;
   private final String gitRepoUrl_;
}
