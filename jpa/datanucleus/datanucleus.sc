import org.datanucleus.api.jpa.annotations.PersistenceAware;

// TODO: Depends on mysql driver here to force the order - otherwise the JDBC driver is not accessible from the datanucleus class loader due to how we chain
// parent and child classloaders..  We really should depend on a 'jdbc' layer which picks a default or adds a dependency onto an included layer
jpa.datanucleus extends basejpa, log4j.core, jdbc.mysql {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;

   object dataNucleusPkg extends MvnRepositoryPackage {
      //url = "mvn://org.datanucleus/datanucleus-rdbms/4.0.6";
      url = "mvn://org.datanucleus/datanucleus-accessplatform-jpa-rdbms/5.1.2";
      //url = "mvn://org.datanucleus/datanucleus-accessplatform-jpa-rdbms/4.0.6";
      includeProvided = true; // includes javax.persistence
   }

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      // If we generate the setX(int i, Object val) method data nucleus tries to enhance it as though it were a setX(Object val) method.  There's a verify error when that class is loaded.
      system.useIndexSetForArrays = false;

      // Currently only using the RDMS - we can break this into sub-layers and depend on different packages
/*
      String componentName = "datanucleus-accessplatform-rdbms-";
      String version = "4.0.6";
      sc.repos.RepositoryPackage pkg = addRepositoryPackage("dataNucleusRDBMS", "scp", "vsgit@stratacode.com:/home/vsgit/" + componentName + version + ".zip", true);
     
      if (pkg.installedRoot != null && !disabled) {
         String rootName = sc.util.FileUtil.concat(pkg.installedRoot, componentName + version);
         classPath = sc.util.FileUtil.listFiles(sc.util.FileUtil.concat(rootName, "deps"),".*\\.jar")
                     + sc.util.FileUtil.PATH_SEPARATOR + sc.util.FileUtil.listFiles(sc.util.FileUtil.concat(rootName, "lib"),".*\\.jar");
      }
*/

      // Turns on class load time enhancement via command line.  We use the postBuildCommand enhancer now instead.  Ideally we'd just generate the enhanced code at build time.
      //String jpaFile = getRelativeFile("./lib/datanucleus-core.jar");  
      //system.buildInfo.addVMParameter("jpaenhancer", "-javaagent:" + jpaFile + "=-api=JDO,mydomain.mypackage1,mydomain.mypackage2");

      // Registers a command, run after the compile in the process phase which enhances any JPA entities that have changed
      system.addPostBuildCommand("JPAEntity", this, sc.layer.BuildPhase.Process, "java",
                                 "-cp",
                                 "<%= layeredSystem.classPath %>",
                                 "org.datanucleus.enhancer.DataNucleusEnhancer",
                                 //"org.apache.openjpa.enhance.PCEnhancer",
                                 "-api", 
                                 "JPA",
                                 "[<% sc.layer.LayeredSystem system = getLayeredSystem(); " +
                                 "   java.util.List<sc.layer.TypeGroupMember> entities = system.buildInfo.getTypeGroupMembers(\"JPAEntity\"); " +
                                 "   boolean first = true; " +
                                 "   for (sc.layer.TypeGroupMember entity:entities) {" +
                                 "     if (entity.changed) { " +
                                 "        if (!first) { %> <% } " +
                                 "        first = false; " +
                                 "        %><%= entity.getClassFileName(getBuildClassesDir()) %><% " +
                                 "     }" +
                                 "   } %>]",
                                 // Writing this to a file because it messes with the auto-test verification and there seems to be no way to turn it off - -q may work in v5.0 but does not seem to work here
                                 "&>", "/tmp/dataNucleusEnhance.out");
   }
}
