
// Deprecated - openjpa is no longer supported by anyone.  Using datanucleus
// now by default.
jpa.openjpa extends basejpa {
   codeType = sc.layer.CodeType.Framework;

   //classPath=sc.util.FileUtil.listFiles(getRelativeFile("./lib"),".*\\.jar");

   // Directory holding this file has a symbolic link to the android/platforms/android-xxx directory which contains android.jar
   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();
      /* This package is no longer available...
      sc.repos.RepositoryPackage pkg = addRepositoryPackage("openjpaLibs", "scp", "vsgit@stratacode.com:/home/vsgit/openjpaLibs", false);
      if (pkg.installedRoot != null && !disabled) {
         classPath=sc.util.FileUtil.listFiles(sc.util.FileUtil.concat(pkg.installedRoot, "lib"),".*\\.jar");
      }
      */

      // Turns on class load time enhancement via command line.  We use the postBuildCommand enhancer now instead.
      //String jpaFile = getRelativeFile("./lib/openjpa.jar");
      //system.buildInfo.addVMParameter("jpaenhancer", "-javaagent:" + jpaFile + "=addDefaultConstructor=false");

      // Registers a command, run after the compile in the process phase which enhances any JPA entities that have changed
      system.addPostBuildCommand("JPAEntity", this, sc.layer.BuildPhase.Process, "java",
                                 "-cp",
                                 "<%= layeredSystem.classPath %>",
                                 "org.apache.openjpa.enhance.PCEnhancer",
                                 "[<% sc.layer.LayeredSystem system = getLayeredSystem(); " +
                                 "   java.util.List<sc.layer.TypeGroupMember> entities = system.buildInfo.getTypeGroupMembers(\"JPAEntity\"); " +
                                 "   boolean first = true; " +
                                 "   for (sc.layer.TypeGroupMember entity:entities) {" +
                                 "     if (entity.changed) { " +
                                 "        if (!first) { %> <% } " +
                                 "        first = false; " +
                                 "        %><%= entity.getClassFileName(getBuildClassesDir()) %><% " +
                                 "     }" +
                                 "   } %>]");
   }
}
