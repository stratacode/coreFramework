
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Version;
import javax.persistence.Basic;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.persistence.Lob;

jpa.basejpa extends util {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;

   public void init() {
      // Exclude the javascript runtime.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses JS
      excludeRuntimes("js", "android", "gwt");

      // JPA only works in the Java runtime
      addRuntime(null);
   }

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      // Adds handler for the @Entity annotation - build a global list of entity types for use in generating persistence.xml and compile time class enhancement
      sc.lang.DefaultAnnotationProcessor entityProc = new sc.lang.DefaultAnnotationProcessor();
      entityProc.typeGroupName = "JPAEntity";
      entityProc.validOnField = false;
      entityProc.validOnMethod = false;
      entityProc.compiledOnly = true; // These types must be compiled, even if put into a dynamic layer as the JPA integration has no dynamic support
      registerAnnotationProcessor("javax.persistence.Entity", entityProc);


      // Layers web files in the "doc" folder of any downstream layers
      sc.layer.LayerFileProcessor persistenceXML = new sc.layer.LayerFileProcessor();

      // Only layers after this one will see this extension
      persistenceXML.definedInLayer = this;    
      persistenceXML.prependLayerPackage = false;
      persistenceXML.useSrcDir = false;
      // Put this into the classpath if that gets reconfigured
      persistenceXML.useClassesDir = true;

      // Copy this file into the top-level of the buildDir
      system.registerPatternFileProcessor("persistence\\.xml", persistenceXML);
   }
}
