sys.options.noPropertyMappers {
   void start() {
      if (activated) 
         layeredSystem.usePropertyMappers = false;
   }
}
