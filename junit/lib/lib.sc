package sc.junit;

import org.junit.*;
import org.junit.runner.*;

import static org.junit.Assert.*;

junit.lib {
   //classPath="./lib/junit-4.12.jar:./lib/hamcrest-core-1.3.jar";
   

   // These classes must be compiled so they can be used by the next layer up in the
   // chain.
   buildSeparate = true;
   compiledOnly = true;

   void init() {
      excludeRuntimes("js", "gwt", "android");
   }

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();
      // Add the maven package mvn://groupId/artifactId/version
      sc.repos.RepositoryPackage pkg = addRepositoryPackage("mvn://junit/junit/4.12");
      if (pkg.installedRoot != null && !disabled) {
         classPath=pkg.classPath;
      }
   }
}
