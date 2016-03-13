import sc.lang.html.Element;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Date;
import java.util.Calendar;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;


import sc.util.StringUtil;
import sc.util.TextUtil;
import sc.util.PerfMon;
import sc.dyn.DynUtil;

import sc.lang.SCLanguage;
import sc.lang.js.JSRuntimeProcessor;
import sc.parser.Language;
import sc.lang.pattern.Pattern;
import sc.lang.java.ModelUtil;
import sc.parser.Parselet;
import sc.layer.LayeredSystem;

import sc.obj.ScopeContext;
import sc.obj.ScopeDefinition;

import sc.sync.SyncManager;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/** 
  * The PageDispatcher manages a registry of URL patterns which are matched against incoming request URIs
  * and then farmed to the appropriate object to handle the request.
  * <p>
  * The PageDispatcher can be configured as either a Servlet or a ServletFilter.  When it is a servlet
  * it must handle the request and when it is a filter, it handles the request when it receives a matching URL
  * and forwards it otherwise.  
  */
@sc.servlet.PathServletFilter(path="/*")
class PageDispatcher extends InitServlet implements Filter {
   static LinkedHashMap<String,PageEntry> pages = new LinkedHashMap<String,PageEntry>();

   static Language language = SCLanguage.getSCLanguage();

   static public String indexPattern = "/index.html";

   static public boolean trace = false, traceLocks = false;

   private FilterConfig filterConfig;

   static class PageEntry {
      String pattern;
      Parselet patternParselet;
      Object pageType;
      boolean urlOnly;
      int priority;
      String lockScope; // The scope name to use for locking.  if null, use the type's scope as the scope for the lock.
      String mimeType;
      boolean doSync;

      public String toString() {
         if (pattern == null)
            return "<not initialized>";
         return pattern + (pageType == null ? " <no type>" : "(" + DynUtil.getTypeName(pageType, false) + ")");
      }
   }

   /** 
    * This method is called from generated code, attached to an annotation via a mixin-template. It register 
    * a pattern with a page type of an object to handle the request or a class to be created to handle the
    * request.  The priority will typically be provided as the layer position, in case one type overrides another.
    * We are not guaranteed these get called in any order so need to use the priority to decide who gets to listen on that
    * pattern.
    */
   public static void addPage(String keyName, String pattern, Object pageType, boolean urlOnly, int priority, String lockScope) {
      PageEntry ent = new PageEntry();
      ent.pattern = pattern;
      Object patternRes = Pattern.initPattern(language, pageType, pattern);
      if (!(patternRes instanceof Parselet))
         throw new IllegalArgumentException("Invalid pattern: " + pattern + " error: " + patternRes);
      ent.patternParselet = (Parselet) patternRes;
      ent.pageType = pageType;
      ent.priority = priority;
      ent.urlOnly = urlOnly;
      if (pattern.endsWith(".css")) {
         ent.doSync = false;
         ent.mimeType = "text/css";
      }
      else
         ent.doSync = !urlOnly;
      // Used to use the keyName here as the key but really can only have one per pattern anyway and need a precednce so sc.foo.index can override sc.bar.index.
      // TODO: now that we have multiple PageEntry's supporting each URL, should we have an option to support multiple handlers for the same pattern?  patternFilter=true?
      PageEntry oldEnt = pages.get(pattern);
      if (oldEnt == null || oldEnt.priority < priority)
         pages.put(pattern, ent);

      if (trace)
         System.out.println("PageDispatcher: new page id: " + keyName + " url:" + pattern + " type: " + ModelUtil.getTypeName(pageType) + ")");

      if (pattern.equals(indexPattern)) {
         if (trace)
            System.out.println("PageDispatcher: adding index page");
         addPage("_index_", "/", pageType, urlOnly, priority, lockScope);
      }
   }

   public void service(javax.servlet.http.HttpServletRequest request, 
                       javax.servlet.http.HttpServletResponse response) 
                           throws IOException, ServletException {
       if (!handleRequest(request, response)) {
          response.sendError(404, "Page not found");
       }
   }

   public List<PageEntry> getPageEntries(String uri) {
      ArrayList<PageEntry> matchedEnts = null;
      for (PageEntry pageEnt:pages.values()) {
         // TODO: optimization where we compute the prefix of each pattern, use that as an index to retrive the list of
         // patterns, similar to how IndexedChoice works for parselets.  Maybe we should build an IndexedChoice of the
         // page parselets and use that logic?
         if (language.matchString(uri, pageEnt.patternParselet)) {
            if (matchedEnts != null) {
               int i;
               for (i = 0; i < matchedEnts.size(); i++) {
                  PageEntry prevEnt = matchedEnts.get(i);
                  if (prevEnt.priority > pageEnt.priority)
                     break;
               }
               if (i == matchedEnts.size())
                  matchedEnts.add(pageEnt);
               else
                  matchedEnts.add(i, pageEnt);
               return matchedEnts;
            }
            else {
               matchedEnts = new ArrayList<PageEntry>();
               matchedEnts.add(pageEnt);
            }
         }
      }
      return matchedEnts;
   }

   public List<Object> initPageObjects(Context ctx, String uri, List<PageEntry> pageEnts, HttpSession session, boolean reset, boolean initial,
                                       boolean resetSync, ArrayList<Lock> locks, LayeredSystem sys) {
      if (pageEnts == null)
         return null;

      int sz = pageEnts.size();
      List<Object> insts = new ArrayList<Object>(sz);
      boolean hasInst = false;

      List<Integer> scopeIds = new ArrayList<Integer>(sz);
      List<String> scopeNames = new ArrayList<String>(sz);
      List<String> lockScopeNames = new ArrayList<String>(sz);
      List<ScopeContext> scopeCtxs = new ArrayList<ScopeContext>(sz);

      String scopeName;
      int scopeId = -1;
      ScopeContext scopeCtx = null;

      // first we'll loop through all page objects and figure out which scopes and locks are needed for this request
      // that way we can acquire locks "all or none" to avoid deadlocks.
      for (PageEntry pageEnt:pageEnts) {
         if (!pageEnt.urlOnly) {
            Object pageType = pageEnt.pageType;
            boolean isObject = ModelUtil.isObjectType(pageType);

            scopeName = null;
            scopeCtx = null;

            // If this is a class we handle the scopes here.  Otherwise, it is handled in the dyn child manager or generated
            // getX method for the class.
            if (!isObject) {
               if (isSessionScopeType(pageType)) {
                  scopeName = "session";
                  scopeId = SessionScopeDefinition.scopeId;
               }
               else if (isWindowScopeType(pageType)) {
                  scopeName = "window";
                  scopeId = WindowScopeDefinition.scopeId;
                  scopeCtx = ctx.getWindowScopeContext();
               }
            }
            if (scopeName == null) {
               scopeName = ModelUtil.getInheritedScopeName(null, pageType);
               if (scopeName != null && scopeName.length() > 0) {
                  ScopeDefinition scopeDef = ScopeDefinition.getScopeByName(scopeName);
                  if (scopeDef == null) {
                     System.err.println("*** Missing ScopeDefinition for scope: " + scopeName);
                  }
                  else {
                     scopeId = scopeDef.scopeId;
                     scopeCtx = scopeDef.getScopeContext();
                  }
               }
            }
            if (scopeCtx == null) {
               ScopeDefinition scopeDef = ScopeDefinition.getScopeByName(scopeName);
               if (scopeDef == null) {
                  System.err.println("*** Missing ScopeDefinition for scope: " + scopeName);
               }
               else
                  scopeCtx = scopeDef.getScopeContext();
            }

            if (locks != null && !scopeNames.contains(scopeName)) {
               // TODO: For now all dyn types are synchronized globally.  We do not have proper synchronization around loading new types (but we should just like class loader)
               boolean isDyn = ModelUtil.isDynamicType(pageType);
               String lockScope = pageEnt.lockScope;
               if (isDyn) {
                  Lock dynLock = sys.getDynWriteLock();
                  if (!locks.contains(dynLock)) {
                     locks.add(dynLock);
                     lockScopeNames.add("<dyn global lock>");
                  }
               }
               if (lockScope == null) {
                  lockScope = scopeName;
                  if (lockScope == null) {
                     System.err.println("Warning: no lock scope defined for: " + uri + " defaulting to global.");
                     lockScope = "global";
                  }
               }

               // Skip locking only if explicitly specified - otherwise we lock based on
               if (!lockScope.equals("none")) {
                  ScopeDefinition lockScopeDef = ScopeDefinition.getScopeByName(lockScope);
                  ScopeContext lockScopeCtx = lockScopeDef.getScopeContext();

                  ReentrantReadWriteLock rwLock = (ReentrantReadWriteLock) lockScopeCtx.getValue("_lock");
                  if (rwLock == null) {
                     synchronized (lockScopeCtx) {
                        rwLock = (ReentrantReadWriteLock) lockScopeCtx.getValue("_lock");
                        if (rwLock == null) {
                           rwLock = new ReentrantReadWriteLock();
                           if (traceLocks)
                              System.out.println("Page: new lock created for scope: " + lockScope + " " + rwLock + " session: " + DynUtil.getTraceObjId(session.getId()) + " thread: " + getCurrentThreadString());
                           lockScopeCtx.setValue("_lock", rwLock);
                        }
                     }
                  }

                  // TODO: provide some way to specify this request is a read-only request so we only acquire a read lock
                  Lock lock = rwLock.writeLock();

                  locks.add(lock);
                  lockScopeNames.add(lockScope);
               }
            }

            scopeNames.add(scopeName);
            scopeIds.add(scopeId);
            scopeCtxs.add(scopeCtx);
         }
      }

      if (locks != null) {
         if (traceLocks)
            System.out.println("Page - acquiring locks: " + lockScopeNames + " session: " + DynUtil.getTraceObjId(session.getId()) + " thread: " + getCurrentThreadString());

         acquireLocks(locks, uri);

         if (traceLocks)
            System.out.println("Page - locks acquired: " + lockScopeNames + locks + " session: " + DynUtil.getTraceObjId(session.getId()) + " thread: " + getCurrentThreadString());
      }

      int i = 0;
      for (PageEntry pageEnt:pageEnts) {
         if (!pageEnt.urlOnly) {
            scopeName = scopeNames.get(i);
            scopeId = scopeIds.get(i);
            scopeCtx = scopeCtxs.get(i);

            Object pageType = pageEnt.pageType;
            boolean isObject = ModelUtil.isObjectType(pageType);

            if (initial) {
               // Mark this as the initial sync mode
               SyncManager.setInitialSync("jsHttp", uri, WindowScopeDefinition.scopeId, true);
            }
            else {
               SyncManager.setSyncAppId(uri);
               // When we get the page for a sync reset operation, we do not record the changes and it's not the "initial sync" layer
               if (resetSync)
                  SyncManager.setSyncState(SyncManager.SyncState.ApplyingChanges);
               else
                  SyncManager.setSyncState(SyncManager.SyncState.RecordingChanges);
            }

            if (pageEnt.doSync) {
               if (reset) { // When we are loading an initial page, the client state is gone so need to reset the window context state.
                  reset = false;
                  SyncManager.resetContext(WindowScopeDefinition.scopeId);
               }

               SyncManager.beginSyncQueue();

               // Mark the session as a sync session so we know in the sync servlet we can use it without a reset.
               markSyncSession(session, uri, reset, initial);
            }

            String typeName = ModelUtil.getTypeName(pageType);
            Object inst = null;

            if (!isObject) {
               if (scopeId == SessionScopeDefinition.scopeId) {
                  inst = session.getAttribute(typeName);
                  if (inst == null) {
                     inst = ModelUtil.getObjectInstance(pageType);
                     session.setAttribute(typeName, inst);
                     // Register this instance by name but don't initialize it.
                     SyncManager.registerSyncInst(inst, typeName, SessionScopeDefinition.scopeId, false);
                  }
               }
               else {
                  inst = scopeCtx.getValue(typeName);
                  if (inst == null) {
                     inst = ModelUtil.getObjectInstance(pageType);
                     scopeCtx.setValue(typeName, inst);
                     // Register this instance by name but don't initialize it.
                     SyncManager.registerSyncInst(inst, typeName, scopeId, false);
                  }
               }
            }
            if (inst == null) {
               inst = ModelUtil.getAndRegisterGlobalObjectInstance(pageType);
            }
            insts.add(inst);

            if (trace) {
               String pageTypeStr = (initial ? "Page" : (reset ? "Sync: reset session" : "Sync"));
               System.out.println(pageTypeStr + " start: " + uri + " type:" + typeName + " uri:" + uri + " scope: " + scopeName + " session: " + DynUtil.getTraceObjId(session.getId()) + " thread: " + getCurrentThreadString() + " " + getTimeString() + " locks: " + lockScopeNames);
            }

            if (pageEnt.doSync)
               SyncManager.flushSyncQueue();

            if (inst != null) {
               hasInst = true;
               // If necessary, parse the URI again but this time set properties as necessary in inst.
               String svClassName = pageEnt.patternParselet.getSemanticValueClassName();
               Object svClass = sc.dyn.DynUtil.findType(svClassName);
               if (svClass != null && ModelUtil.isInstance(svClass, inst))
                  language.parseIntoInstance(uri, pageEnt.patternParselet, inst);
            }

            ctx.execLaterJobs();
         }

         i++;
      }
      return insts;
   }

   static void acquireLocks(List<Lock> locks, String uri) {
      if (locks.size() == 0)
         return;
      // Wait as normal to get the first lock
      locks.get(0).lock();
      int fetchFrom = 1;
      int fetchTo = locks.size();
      int repeatTo = -1;
      boolean repeat;
      do {
         repeat = false;
         for (int i = fetchFrom; i < fetchTo; i++) {
            Lock lock = locks.get(i);
            // If we can't immediately get the next lock
            if (!lock.tryLock()) {
               releaseLocks(locks, 0, i);
               if (trace || traceLocks)
                  System.out.println("Waiting for locks held: " + uri + " thread: " + getCurrentThreadString() + ": " + getTimeString());
               // Wait now to get the contended lock to avoid a busy loop but we'll just immediately release it just to make the code simpler
               lock.lock();

               // We're going to finish this iteration of the loop to acquire
               repeat = true;
               fetchFrom = 0;
               repeatTo = i;
            }
         }
         // We'll either exit this loop with all of the locks (repeat=false) or repeat=true, from repeatTo=>size locks.  In the latter case we need to acquire then 0-repeatTo locks and repeat the loop once.
         if (repeat)
            fetchTo = repeatTo;
      } while (repeat);
   }

   static String getRuntimeString(long startTime) {
      return TextUtil.format("#.##", (((System.currentTimeMillis() - startTime))/1000.0)) + " secs.";
   }

   /** Don't put the ugly thread ids into the logs - normalize them with an incremending integer */
   static String getCurrentThreadString() {
      return DynUtil.getTraceObjId(Thread.currentThread());
   }

   static String getTimeString() {
      Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "." + cal.get(Calendar.MILLISECOND);
   }

   static void releaseLocks(List<Lock> locks, HttpSession session) {
      if (traceLocks)
         System.out.println("Page - releasing " + locks.size() + " locks: " + locks + " session: " + (session == null ? "<none>" : DynUtil.getTraceObjId(session.getId())) + " thread: " + getCurrentThreadString());
      releaseLocks(locks, 0, locks.size());
   }

   static void releaseLocks(List<Lock> locks, int from, int to) {
      for (int i = from; i < to; i++) {
         locks.get(i).unlock();
      }
   }

   public StringBuilder getInitialSync(List<Object> insts, Context ctx, String uri, List<PageEntry> pageEnts, StringBuilder traceBuffer) {
      try {
         PerfMon.start("getInitialSync");
         return getOutputFromPages(insts, ctx, uri, pageEnts, true, false, traceBuffer);
      }
      finally {
         PerfMon.end("getInitialSync");
      }
   }

   public StringBuilder getOutputFromPages(List<Object> insts, Context ctx, String uri, List<PageEntry> pageEnts, boolean needsInitialSync, boolean resetSync, StringBuilder traceBuffer) {
      // If this is really a page, render it.
      StringBuilder sb = null, headSB = null, bodySB = null;

      int i = 0;
      boolean needsDyn = false;
      Element mainPage = null, mainHead = null, mainBody = null;
      boolean doSync = false;
      //LinkedHashSet<String> jsFiles = new LinkedHashSet<String>();
      for (PageEntry pageEnt:pageEnts) {
         Object inst = insts.get(i);
         if (inst instanceof Element && !pageEnt.urlOnly) {
            needsDyn = true;

            if (pageEnt.doSync)
               doSync = true;

            if (pageEnt.mimeType != null)
               ctx.mimeType = pageEnt.mimeType;

            Element page = (Element) inst;

            if (pageEnts.size() < 2)
               sb = page.output();
            else {
               Element headElem = (Element) DynUtil.getProperty(page, "head");
               Element bodyElem = (Element) DynUtil.getProperty(page, "body");
               if (i == 0) {
                  headSB = new StringBuilder();
                  bodySB = new StringBuilder();
                  sb = new StringBuilder();

                  page.outputStartTag(sb);
                  if (headElem != null) {
                     headElem.outputStartTag(headSB);
                     headElem.outputBody(headSB);
                  }
                  if (bodyElem != null) {
                     bodyElem.outputStartTag(bodySB);
                     bodyElem.outputBody(bodySB);
                  }
                  mainPage = page;
                  mainHead = headElem;
                  mainBody = bodyElem;
               }
               else {
                  if (headElem != null)
                     headElem.outputBody(headSB);
                  if (bodyElem != null)
                     bodyElem.outputBody(bodySB);
               }
            }

            /*
            List<String> pageFiles = page.getJSFiles();
            if (pageFiles != null) {
               for (int jsix = 0; jsix < pageFiles.size(); jsix++) {
                  String pageFile = pageFiles.get(jsix);
                  jsFiles.add(pageFile);
               }
            }
            */

            ctx.execLaterJobs();

            // Rendering the page rejected or redirected it
            if (ctx.requestComplete)
               return null;
        }
        i++;
      }

      if (mainPage != null) {
         mainHead.outputEndTag(headSB);
         mainBody.outputEndTag(bodySB);
         sb.append(headSB);
         sb.append(bodySB);
         mainPage.outputEndTag(sb);
      }

      int pageBodySize = sb.length();
      int initSyncSize = 0;

      if (needsDyn && needsInitialSync && !resetSync && doSync) {
         /*
         if (jsFiles.size() > 0) {
            sb.append("\n");
            for (String jsFile:jsFiles) {
               sb.append("<script type='text/javascript' src='" + jsFile + "'></script>\n");
            }
         }
         */
         CharSequence initSync = SyncManager.getInitialSync("jsHttp", WindowScopeDefinition.scopeId, resetSync);
         sb.append("\n\n<!-- Init SC JS -->\n");
         sb.append("<script type='text/javascript'>\n");
         if (SyncManager.trace) {
            sb.append("sc_SyncManager_c.trace = true;\n");
         }
         if (SyncManager.traceAll) {
            sb.append("sc_SyncManager_c.traceAll = true;\n");
         }
         if (trace || Element.trace) {
            sb.append("js_Element_c.trace = true;\n");
         }
         sb.append("   var sc_windowId = " + ctx.getWindowId() + ";\n");
         if (initSync != null && (initSyncSize = initSync.length()) > 0) {
            sb.append(JSRuntimeProcessor.SyncBeginCode);
            sb.append(initSync);
            sb.append(JSRuntimeProcessor.SyncEndCode);
         }
         sb.append("</script>");
      }
      String pageOutput = sb.toString();

      if (SyncManager.traceAll) {
         traceBuffer.append(" url=" + uri + " size: " + pageOutput.length() + " -----:\n");
         traceBuffer.append(SyncManager.verbose ? pageOutput : StringUtil.ellipsis(pageOutput, SyncManager.logSize, false));
         traceBuffer.append("----- \n");
      }
      else if (trace) {
         traceBuffer.append(" url=" + uri + " pageSize: " + pageBodySize + " initSyncSize: " + initSyncSize);
      }

      return sb;
   }

   public boolean handleRequest(javax.servlet.http.HttpServletRequest request, 
                                javax.servlet.http.HttpServletResponse response) 
                           throws IOException, ServletException {
      Context ctx = null;  
      try {
         boolean urlOnly = true;
         String uri = request.getRequestURI();

         List<PageEntry> pageEnts = getPageEntries(uri);;
         ArrayList<Lock> locks = new ArrayList<Lock>();

         if (pageEnts != null && pageEnts.size() > 0) {
            List<Object> insts = null;

            if (trace)
               System.out.println("Page init: " + uri + " matched: " + pageEnts + " thread: " + getCurrentThreadString());

            urlOnly = pageEnts.get(0).urlOnly && urlOnly;

            if (!urlOnly && ctx == null)
               ctx = Context.initContext(request, response);

            LayeredSystem sys = LayeredSystem.getCurrent();
            if (sys != null && sys.options.autoRefresh) {
               sys.rebuild();
            }

            long startTime = 0;
            if (trace)
               startTime = System.currentTimeMillis();

            HttpSession session = null;
            try {
               // TODO: check if we need a session for this request before creating it
               session = request.getSession(true);

               // Make sure the page object is initialized for this request
               insts = initPageObjects(ctx, uri, pageEnts, session, true, true, false, locks, sys);

               // Something in creating the object rejected, redirected or whatever
               if (ctx.requestComplete) {
                  if (trace) {
                     System.out.println("PageDispatcher request handled after page init - aborting processing: " + uri);
                  }
                  return true;
               }

               StringBuilder traceBuffer = new StringBuilder();
               StringBuilder pageOutput = getInitialSync(insts, ctx, uri, pageEnts, traceBuffer);

               if (ctx.requestComplete) {
                  if (trace) {
                     System.out.println("PageDispatcher request handled after page init - aborting processing: " + traceBuffer);
                  }
                  return true;
               }

               if (pageOutput != null) {
                  ctx.write(pageOutput.toString());
               }

               if (trace)
                  System.out.println("Page complete: session: " + DynUtil.getTraceObjId(session.getId()) + " thread: " + getCurrentThreadString() + traceBuffer + ": " + getRuntimeString(startTime));
            }
            finally {
               // This clears the initial sync flag in case we called setInitialSync(..., true) in initPageObjects.  It also clears the SyncState for the other initPageObjects cases.
               SyncManager.setInitialSync("jsHttp", uri, WindowScopeDefinition.scopeId, false);

               releaseLocks(locks, session);
            }
         }
         return pageEnts != null && pageEnts.size() > 0 && !urlOnly;
      }
      finally {
         if (ctx != null) {
            ctx.execLaterJobs();
            Context.clearContext();
         }
      }
   }

   public boolean isWindowScopeType(Object typeObj) {
      String scopeName = ModelUtil.getInheritedScopeName(null, typeObj);
      if (scopeName != null && scopeName.equals("window"))
         return true;

      if (ModelUtil.getInheritedAnnotation(null, typeObj, "sc.servlet.WindowScope") != null)
         return true;

      return false;
   }

   public boolean isSessionScopeType(Object typeObj) {
      String scopeName = ModelUtil.getInheritedScopeName(null, typeObj);
      if (scopeName != null && scopeName.equals("session"))
         return true;

      if (ModelUtil.getInheritedAnnotation(null, typeObj, "sc.servlet.SessionScope") != null)
         return true;

      return false;
   }

   public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain) 
                         throws IOException, ServletException {
      if (!handleRequest((HttpServletRequest) request, (HttpServletResponse) response)) {
         chain.doFilter(request, response);
      }
   }

   public FilterConfig getFilterConfig() {
      return this.filterConfig;
   }

   static PageDispatcher pageDispatcher;

   public static PageDispatcher getPageDispatcher() {
      return pageDispatcher;
   }

   static class SyncSession {
      String uri;
      long lastSyncTime = -1;
   }

   public static boolean syncInitialized(HttpSession session, String uri) {
      return getSyncSession(session, uri, false) != null;
   }

   public static SyncSession getSyncSession(HttpSession session, String uri, boolean create) {
       HashMap<String,SyncSession> syncSessions = (HashMap<String,SyncSession>) session.getAttribute("syncSessions");
       if (syncSessions == null) {
          syncSessions = new HashMap<String,SyncSession>();
          session.setAttribute("syncSessions", syncSessions);
       }
       SyncSession sess = syncSessions.get(uri);
       if (sess == null && create) {
          sess = new SyncSession();
          sess.uri = uri;
          syncSessions.put(uri, sess);
       }
       return sess;
   }

   /**
    * The sync session stores the last time we either fetched the initial page or handled a sync request.
    * On each sync, the servlet can append any Javascript generated by the UpdateInstanceInfo for any system
    * refreshes or rebuilds etc.  We also set this when the initial page is loaded.  It's assumed that you will
    * have rebuilt on each page refresh and so get the current JS at that time.
    *
    * If we ever do not do the rebuild, we should append all JS system updates since the last rebuild on the
    * initial page request.
    */
   public static void markSyncSession(HttpSession session, String uri, boolean reset, boolean initial) {
      SyncSession sess = getSyncSession(session, uri, false);
      if (sess == null) {
         // On the first request, leave the lastSyncTime as -1 so we know it's a new session
         sess = getSyncSession(session, uri, true);
      }
      // Set this even on the first request because it's used to gather up system updates.  We want to load any
      // system updates after the initial page load.
      if (initial && sess != null)
         sess.lastSyncTime = System.currentTimeMillis();
   }

   public void init(FilterConfig filterConfig) {
      // Registers a scheduler to handle the invokeLater
      ServletScheduler.init();
      // Create the global scope first, before the session scope and other application defined scopes are defined.
      sc.obj.GlobalScopeDefinition.getGlobalScopeDefinition();

      // This method gets generated via the InitTypesMixin template.  It will init all initOnStartup and createAtStartup objects for the servlet layer.
      initTypes();

      // If any clients want to synchronize the layered system, this needs to be initialized first.  This has to be run after we've initialized the
      // destinations, hence after initTypes.
      LayeredSystem sys = LayeredSystem.getCurrent();
      if (sys != null) {
         // If we've included the js.layer layer, we'll need to sync the layered system
         if (sys.getLayerByDirName("sys.layeredSystem") != null)
            sys.initSync();

         if (Element.trace)
            trace = true;
      }

      this.filterConfig = filterConfig;

      if (pageDispatcher != null)
         System.err.println("*** Warning - replacing existing static page dispatcher");
      pageDispatcher = this;
   }

   public StringBuilder getPageOutput(Context ctx, String url, boolean initSync, boolean resetSync, ArrayList<Lock> locks, LayeredSystem sys, StringBuilder traceBuffer) {
      StringBuilder pageOutput = null;
      List<PageDispatcher.PageEntry> pageEnts = pageDispatcher.getPageEntries(url);
      if (pageEnts == null) {
         try {
            ctx.response.sendError(HttpServletResponse.SC_NOT_FOUND, "No page found for url: " + url);
         }
         catch (IOException exc) {}
         return null;
      }
      try {
         List<Object> insts = pageDispatcher.initPageObjects(ctx, url, pageEnts, ctx.session, false, initSync, resetSync, locks, sys);

         if (insts != null) {
         /*
          * TODO: do we need this anymore or is it handled in getInitialSync
          * If we do this, even invisible pages get initialized, like the iconified editor.  I think it's best of rendering the page
          * pulls in just the objects needed for that context but maybe we need to make this more configurable, so a page specific init
          * method is run.
             for (Object inst:insts) {
                if (inst != null) {
                   SyncManager.initChildren(inst);
                }
             }
             ctx.execLaterJobs();
         */

            // Just need to do the initial sync to get things set up for this new client.
            // TODO: optimize this process so we init the object graph but don't bother generating the response
            // TODO: add an argument to outputStartTag and outputBody called PageContext.  In there put a flag - render for server?  Or maybe it's in the Context?  Have this pull back a Javascript string that
            // updates any serverContent nodes (those with serverContent explicitly or exec="server" which sets serverContent).  Take that JS script and append it to the script going back.
            // e.g. sc_DynUtil_c.updateHTML("idName", escaped HTML string).  We can build that by walking down the object tree, calling the is-valid, outputStart and body methods manually as needed?

            PerfMon.start("getOutputFromPages");
            pageOutput = pageDispatcher.getOutputFromPages(insts, ctx, url, pageEnts, initSync, resetSync, traceBuffer);
            PerfMon.end("getOutputFromPages");
         }
      }
      finally {
         SyncManager.setInitialSync("jsHttp", url, WindowScopeDefinition.scopeId, false);
      }
      return pageOutput;
   }

}
