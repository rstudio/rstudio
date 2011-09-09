push-gwt.sh packages and deploys GWT to a local or remote Maven
repository
 
To build GWT from trunk and deploy to your local repo:

> cd trunk
> ant clean dist-dev
> maven/push-gwt.sh

Follow the prompts to set the GWT version # and path to the
distribution ZIP (it will automatically find it in build/dist if
you've just built it). By default, the script deploys to your local
Maven repo.

To deploy to a remote repo:

Set GWT_MAVEN_REPO_URL and GWT_MAVEN_REPO_ID as noted in the
script. If the remote repo requires a username and password, define
the repo in your ~/.m2/settings.xml as below and set GWT_MAVEN_REPO_ID
= server id. In this example, GWT_MAVEN_REPO_ID would be "sonatype".

> cd trunk
> ant clean dist # must be dist, not dist-dev, to generate Javadocs
> maven/push-gwt.sh

~/.m2/settings.xml:
<settings>
  <localRepository>${env.M2_REPO}</localRepository>
  <servers>
    <server>
      <id>sonatype</id>
      <username>sonatype_username</username>
      <password>sonatype_password</password>
    </server>
  </servers>
</settings>

If the remote repo requires jar signing as does Sonatype (Maven
Central), set up GPG on your local machine then enter the passphrase
when prompted. When deploying locally, you can enter a passphrase to
sign the jars for testing or press Enter to skip signing altogether.
