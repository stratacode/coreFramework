import sc.lang.html.Element;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Date;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Iterator;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

import sc.type.IBeanMapper;

import sc.util.StringUtil;
import sc.util.TextUtil;
import sc.util.PerfMon;
import sc.type.PTypeUtil;
import sc.dyn.DynUtil;
import sc.dyn.ITypeChangeListener;

import sc.lang.SCLanguage;
import sc.lang.js.JSRuntimeProcessor;
import sc.parser.Language;
import sc.lang.pattern.Pattern;
import sc.lang.java.ModelUtil;
import sc.parser.Parselet;
import sc.layer.LayeredSystem;

import sc.obj.ScopeContext;
import sc.obj.ScopeDefinition;
import sc.obj.ScopeEnvironment;
import sc.obj.RequestScopeDefinition;
import sc.obj.CurrentScopeContext;

import sc.lang.html.QueryParamProperty;

import sc.js.URLPath;

import sc.sync.SyncManager;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/** 
 * The PageDispatcher manages a registry of URL patterns to request handling objects.  For each incoming URL, the
 * set of matching objects generates the content for the response.
 * <p>
 * This servlet uses the sync framework to support flexible scoping and state management.  Page objects can be global, session, request or window scoped, or user 
 * defined scopes support the ability to change the lifecycle.  
 * <p>
 * For the sync framework, the resulting HTML includes the server version of the response page, and following that any 'initial sync state' from synchronized objects
 * referenced on the page.
 * <p>
 * The PageDispatcher runs as either a Servlet or a ServletFilter.  When it is a servlet,
 * it must handle the requests it's given or it's an error.  When it is a filter, it handles the request when it receives a matching URL
 * and forwards it otherwise.  
 * <p>
 * For synchronization, all page objects required for a URL are locked before the page request starts, so pages are run one at a time, only one
 * page per session if there are session-scoped page objects, etc.
 * <p>
 * For debugging, use the -vh (for a summary) or -vha (for all) HTML traffic.  Use -vs or -vsa for tracing synchronization.
 * <p>
 * TODO: we should have a way to support read-only locks for page objects shared between users that are only used in a read-only way.  A use case for this is a header template
 * with shared content that's changed infrequently that's used by lots of pages that are used concurrently.
 * <p>
 * TODO: when more than one HTML page object is found for a given request, the PageDispatcher will first call getHead().outputStart()/outputBody() on each
 * page object and concatenate the contents in the order in which the page objects are registered.  It will then do the same for the body.  So at runtime it
 * attempts to merge the page objects.  Ideally we'd recursively merge all matching tags with ids but I haven't found a use case for that yet.
 */
@sc.servlet.PathServletFilter(path="/*")
class PageDispatcher extends HttpServlet implements Filter, ITypeChangeListener {
   static LinkedHashMap<String,PageEntry> pages = new LinkedHashMap<String,PageEntry>();

   static Language language = sc.lang.pattern.URLPatternLanguage.getURLPatternLanguage();

   static public String indexPattern = "/index.html";

   static public boolean verbose = false, trace = false, traceLocks = false, testMode = false;

   public static long serverStartTime = System.currentTimeMillis();

   private FilterConfig filterConfig;

   public static final int PAGE_FLAG_URL = 1;
   public static final int PAGE_FLAG_SYNC = 2;

   static class PageEntry {
      String keyName;
      String pattern;
      Parselet patternParselet;
      Pattern urlPattern;
      Object pageType;
      boolean urlPage; // TODO: rename this to dynamic page or something?
      int priority;
      String lockScope; // The scope name to use for locking.  if null, use the type's scope as the scope for the lock.
      String mimeType;
      boolean doSync;
      boolean resource;

      // Stores the list of query parameters (if any) for the given page - created with @QueryParam
      List<QueryParamProperty> queryParamProps;

      public String toString() {
         if (pattern == null)
            return "<not initialized>";
         return pattern + (pageType == null ? " <no type>" : "(" + DynUtil.getTypeName(pageType, false) + ")");
      }
   }

   /**
    * This method is usually called from generated code, attached to an annotation via a mixin-template. It register 
    * a pattern with a page type of an object to handle the request or a class to be created to handle the
    * request.  The priority will typically be provided as the layer position, in case one type overrides another.
    * We are not guaranteed these get called in any order so need to use the priority to decide who gets to listen on that
    * pattern.
    */
   public static void addPage(String keyName, String pattern, Object pageType, boolean urlPage, boolean doSync, boolean isResource, int priority, String lockScope, List<QueryParamProperty> queryParamProps) {
      PageEntry ent = new PageEntry();
      ent.keyName = keyName;
      ent.pattern = pattern;
      Object patternRes = Pattern.initPattern(language, pageType, pattern);
      if (!(patternRes instanceof Pattern))
         throw new IllegalArgumentException("Invalid pattern: " + pattern + " error: " + patternRes);
      ent.urlPattern = (Pattern) patternRes;
      ent.patternParselet = (Parselet) ent.urlPattern.getParselet(language, pageType);
      ent.pageType = pageType;
      ent.priority = priority;
      ent.urlPage = urlPage;
      ent.resource = isResource;
      ent.doSync = doSync;
      ent.mimeType = getMimeType(pattern);
      ent.queryParamProps = queryParamProps;
      // Used to use the keyName here as the key but really can only have one per pattern anyway and need a precedence so sc.foo.index can override sc.bar.index.
      // TODO: now that we have multiple PageEntry's supporting each URL, should we have an option to support multiple handlers for the same pattern?  patternFilter=true?
      PageEntry oldEnt = pages.get(pattern);
      if (oldEnt == null || oldEnt.priority < priority)
         pages.put(pattern, ent);

      if (verbose)
         System.out.println("PageDispatcher: new page id: " + keyName + " url:" + pattern + " type: " + ModelUtil.getTypeName(pageType) + ")");

      if (pattern.equals(indexPattern)) {
         if (verbose)
            System.out.println("PageDispatcher: adding index page");
         addPage("_index_", "/", pageType, urlPage, doSync, isResource, priority, lockScope, queryParamProps);
      }
   }

   public void service(javax.servlet.http.HttpServletRequest request, 
                       javax.servlet.http.HttpServletResponse response) 
                           throws IOException, ServletException {
       if (!handleRequest(request, response)) {
          response.sendError(404, "Page not found");
       }
   }

   public List<PageEntry> getPageEntries(String uri, TreeMap<String,String> queryParams) {
      ArrayList<PageEntry> matchedEnts = null;
      for (PageEntry pageEnt:pages.values()) {
         // TODO: performance optimization where we compute the prefix of each pattern, use that as an index to retrieve the list of
         // patterns, similar to how IndexedChoice works for parselets.  Maybe we should build an IndexedChoice of the
         // page parselets and use that logic?  This could be part of the URLPatternLanguage.  It could start out as just an orderedChoice of
         // the registered URL pattern to pageEntry mappings.
         if (language.matchString(uri, pageEnt.patternParselet)) {
            if (pageEnt.queryParamProps != null) {
               boolean allReqFound = true;
               for (QueryParamProperty prop:pageEnt.queryParamProps) {
                  if (prop.required) {
                     if (queryParams == null || queryParams.get(prop.paramName) == null) {
                        if (verbose)
                           System.out.println("Page - URL pattern: " + pageEnt.pattern + " matches type: " + pageEnt + " - but missing required query parameter:" + prop.paramName);
                        allReqFound = false;
                        break;
                     }
                  }
               }
               if (!allReqFound)
                  continue;
            }
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

   public static ScopeDefinition getScopeDefForPageType(Object pageType) {
      String scopeName = ModelUtil.getInheritedScopeName(null, pageType);
      if (scopeName != null && scopeName.length() > 0) {
         ScopeDefinition scopeDef = ScopeDefinition.getScopeByName(scopeName);
         if (scopeDef == null) {
            System.err.println("*** Missing ScopeDefinition for scope: " + scopeName);
         }
         return scopeDef;
      }
      return null;
   }

   public CurrentScopeContext initPageContext(Context ctx, String uri, List<PageEntry> pageEnts, HttpSession session, LayeredSystem sys) {
      String scopeName = null;
      int scopeId = -1;
      ScopeContext scopeCtx = null;
      int sz = pageEnts.size();
      List<String> scopeNames = new ArrayList<String>(sz);
      List<ScopeContext> scopeCtxs = new ArrayList<ScopeContext>(sz);
      List<Object> locks = new ArrayList<Object>(); // These are right now always Lock objects but not putting that into the api so it's more portable to other platforms like JS
      String sysLockInfo = null;
      boolean windowAdded = false;

// first we'll loop through all page objects and figure out which scopes and locks are needed for this request
// that way we can acquire locks "all or none" to avoid deadlocks.
      for (PageEntry pageEnt:pageEnts) {
         if (pageEnt.urlPage) {

            Object pageType = pageEnt.pageType;

            ScopeDefinition scopeDef = getScopeDefForPageType(pageType);
            if (scopeDef != null) {
               scopeCtx = scopeDef.getScopeContext(true);
            }
            else {
               if (verbose)
                  System.out.println("*** No scope defined for page type: " + pageType + " matched by url: " + uri + " - using request scope");
               scopeDef = RequestScopeDefinition.getRequestScopeDefinition();
            }
            scopeName = scopeDef.name;
            if (scopeName == null)
               scopeName = scopeDef.aliases.get(0); // "global"

            if (locks != null && !scopeNames.contains(scopeName)) {
               // For now all dyn types are synchronized globally because we do not have proper synchronization around loading new types (but we should just like class loader)
               // Also, if the command interpreter is enabled, we'll also lock just the dyn global lock so that we can update things from the command line.  We might only
               // need a read-only lock though need to consider that refreshing code changes will need a write
               boolean isDyn = ModelUtil.isDynamicType(pageType) || (sys != null && sys.commandLineEnabled());
               String lockScope = pageEnt.lockScope;
               if (isDyn) {
                  // TODO: can we make this the read lock - unless we are going to refresh because some source was changed
                  Lock dynLock = sys.getDynWriteLock();
                  if (!locks.contains(dynLock)) {
                     locks.add(dynLock);
                     sysLockInfo = appendLogStr(sysLockInfo, "system dyn lock");
                  }
               }
               if (lockScope == null) {
                  lockScope = scopeName;
                  if (lockScope == null) {
                     System.err.println("Warning: no lock scope defined for: " + uri + " defaulting to global.");
                     lockScope = "global";
                     sysLockInfo = appendLogStr(sysLockInfo, "global");
                  }
               }

               // Skip locking only if explicitly specified - otherwise we lock based on
               if (!lockScope.equals("none")) {
                  ScopeDefinition lockScopeDef = ScopeDefinition.getScopeByName(lockScope);
                  // Temporary scopes - like request don't have to be locked because they are only used by one thread at a time.
                  ScopeContext lockScopeCtx = lockScopeDef.getScopeContext(true);

                  ReentrantReadWriteLock rwLock = (ReentrantReadWriteLock) lockScopeCtx.getValue("_lock");
                  if (rwLock == null) {
                     synchronized (lockScopeCtx) {
                        rwLock = (ReentrantReadWriteLock) lockScopeCtx.getValue("_lock");
                        if (rwLock == null) {
                           rwLock = new ReentrantReadWriteLock();
                           if (traceLocks)
                              System.out.println("Page: new lock created for scope: " + lockScope + getTraceInfo(session));
                           lockScopeCtx.setValue("_lock", rwLock);
                        }
                     }
                  }

                  sysLockInfo = appendLogStr(sysLockInfo, lockScope);

                  // TODO: provide some way to specify this request is a read-only request so we only acquire a read lock
                  // Build this into the scope definitions - so there's a default of read only for how we are using teh scope and override it per page?
                  Lock lock = rwLock.writeLock();
                  locks.add(lock);
               }
            }

            scopeNames.add(scopeName);
            scopeCtxs.add(scopeCtx);
            if (scopeName != null && scopeName.equals("window"))
               windowAdded = true;
         }
         if (!windowAdded && ctx != null) {
            scopeNames.add("window");
            scopeCtxs.add(ctx.getWindowScopeContext(true));
            // presumably we've already locked at the session, app, global, etc level so no need to lock the lower-level window
         }
      }

      if (scopeCtxs.size() > 0) {
         CurrentScopeContext curScopeCtx = new CurrentScopeContext(scopeCtxs, locks);
         if (verbose)
            curScopeCtx.traceInfo = sysLockInfo + ": for" + getCtxTraceInfo(session);
         // Associates this set of locks and scopes with this thread so binding operation know how to get back here if there's a cross-scope binding.
         CurrentScopeContext.pushCurrentScopeContext(curScopeCtx, true);
         return curScopeCtx;
      }
      // Static page request or something which doesn't need the scope system or it's locking mechanism
      return null;
   }

   public Object getCurrentPageInstance(PageEntry pageEnt) {
      ScopeEnvironment.setAppId(pageEnt.keyName);

      return DynUtil.resolveName(ModelUtil.getTypeName(pageEnt.pageType), false);
   }

   private static String appendLogStr(String orig, String opt) {
      return orig == null || orig.length() == 0 ? " " + opt : orig + ", " + opt;
   }

   public List<Object> initPageObjects(Context ctx, String uri, List<PageEntry> pageEnts, HttpSession session, 
                                       CurrentScopeContext curScopeCtx, boolean reset, boolean initial,
                                       boolean resetSync, LayeredSystem sys) {
      if (pageEnts == null)
         return null;

      int sz = pageEnts.size();
      List<Object> insts = new ArrayList<Object>(sz);
      boolean hasInst = false;

      int i = 0;
      for (PageEntry pageEnt:pageEnts) {
         if (pageEnt.urlPage) {
            // Enable sync for the page if it needsSync, or if sync is enabled and we are in test mode (so we can control the client with test scripts).  If the client has no js.sync layer (syncEnabled=false), it won't have loaded the syncManager and so we can't do the client/server/sync
            boolean doSync = pageEnt.doSync || (testMode && sys != null && sys.syncEnabled);

            Object pageType = pageEnt.pageType;
            boolean isObject = ModelUtil.isObjectType(pageType);

            if (initial) {
               // Mark this as the initial sync mode
               SyncManager.setInitialSync("jsHttp", uri, RequestScopeDefinition.getRequestScopeDefinition().scopeId, true);
            }
            else {
               // When we get the page for a sync reset operation, we do not record the changes and it's not the "initial sync" layer
               if (resetSync)
                  SyncManager.setSyncState(SyncManager.SyncState.ApplyingChanges);
               else
                  SyncManager.setSyncState(SyncManager.SyncState.RecordingChanges);
            }

            if (doSync) {
               if (reset) { // When we are loading an initial page, the client state is gone so need to reset the window context state.
                  reset = false;
                  SyncManager.resetContext(WindowScopeDefinition.scopeId);
               }

               SyncManager.beginSyncQueue();

            }
            // Doing this even when doSync is false because we need this to process even basic RPC requests.
            // TODO: should we have a "pageEnt.doRPC" flag?  We could then break out just the sync code required to
            // support RPC and put it into a separate module - js.rpc or something like that?
            markSyncSession(session, uri, reset, initial);

            Object inst = null;

            if (!isObject) {
               String typeName = ModelUtil.getTypeName(pageType);
               ScopeDefinition scopeDef = getScopeDefForPageType(pageType);
               if (scopeDef != null) {
                  ScopeContext scopeCtx = scopeDef.getScopeContext(true);
                  if (scopeCtx != null) {
                     inst = scopeCtx.getValue(typeName);
                     if (inst == null) {
                        inst = ModelUtil.getObjectInstance(pageType);
                        scopeCtx.setValue(typeName, inst);
                        // Register this instance by name but don't initialize it.
                        SyncManager.registerSyncInst(inst, typeName, scopeDef.scopeId, false);
                     }
                  }
               }
            }
            if (inst == null) {
               inst = ModelUtil.getAndRegisterGlobalObjectInstance(pageType);
            }
            insts.add(inst);

            if (verbose) {
               String pageTypeStr = (initial ? "Page" : (reset ? "Sync: reset session" : "Sync"));
               System.out.println(pageTypeStr + " start: " + uri + getTraceInfo(session));
            }

            if (doSync)
               SyncManager.flushSyncQueue();

            if (inst != null) {
               hasInst = true;
               // If necessary, parse the URI again but this time set properties as necessary in inst.
               String svClassName = pageEnt.patternParselet.getSemanticValueClassName();
               Object svClass = sc.dyn.DynUtil.findType(svClassName);
               if (svClass != null && ModelUtil.isInstance(svClass, inst))
                  language.parseIntoInstance(uri, pageEnt.patternParselet, inst);

               if (pageEnt.queryParamProps != null) {
                  for (QueryParamProperty qpp:pageEnt.queryParamProps) {
                     Object paramValue = ctx.queryParams == null ? null : ctx.queryParams.get(qpp.paramName);
                     try {
                        IBeanMapper mapper = qpp.mapper;
                        if (mapper == null) {
                           throw new IllegalArgumentException("*** No mapping for query parameter: " + qpp);
                        }
                        else {
                           if (paramValue == null) {
                              Object propType = mapper.getPropertyType();
                              if (propType instanceof Class && PTypeUtil.isPrimitive((Class) propType))
                                 continue;
                           }
                           mapper.setPropertyValue(inst, paramValue);
                        }
                     }
                     catch (RuntimeException exc) {
                        System.err.println("*** Error setting property: " +  qpp.propName + " for: " + inst);
                        try {
                           ctx.response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter: " + qpp.paramName);
                        }
                        catch (IOException ioexc) {
                        }
                        ctx.requestComplete = true;
                     }
                  }
               }
            }

            // This runs any code triggered by 'do later' jobs during the page-init phase.  It makes sure the page content is in sync before we start rendering.
            ctx.execLaterJobs();
         }

         i++;
      }
      return insts;
   }

   static String getRuntimeString(long startTime) {
      return TextUtil.format("#.##", (((System.currentTimeMillis() - startTime))/1000.0)) + " secs.";
   }

   static String getSessionTraceInfo(HttpSession session) {
      return " session: " + (session == null ? "<none>" : DynUtil.getTraceObjId(session.getId())); 
   }

   static String getCtxTraceInfo(HttpSession session) {
      return getSessionTraceInfo(session) + " thread: " + DynUtil.getCurrentThreadString();
   }

   static String getTraceInfo(HttpSession session) {
      return getCtxTraceInfo(session) + " at " + getTimeString();
   }

   // Here we print the time since the PageDispatcher started since that's perhaps the easiest basic way to follow "elapsed time" in the context of a server process.
   // I could imagine having an option to show this relative to start-session or start-thread time or even displaying multiple time spaces in the same log to diagnose different scenarios
   static String getTimeString() {
      if (serverStartTime == 0)
         return "<server not yet started!>";
      long now = System.currentTimeMillis();
      StringBuilder sb = new StringBuilder();
      long elapsed = now - serverStartTime;
      sb.append("+");
      boolean remainder = false;
      if (elapsed > 60*60*1000) {
         long hrs = elapsed / (60*60*1000);
         elapsed -= hrs * 60*60*1000;
         if (hrs < 10)
            sb.append("0");
         sb.append(hrs);
         sb.append(":");
         remainder = true;
      }
      if (elapsed > 60*1000 || remainder) {
         long mins = elapsed / (60*1000);
         elapsed -= mins * 60*1000;
         if (mins < 10)
            sb.append("0");
         sb.append(mins);
         sb.append(":");
      }
      if (elapsed > 1000 || remainder) {
         long secs = elapsed / 1000;
         elapsed -= secs * 1000;
         if (secs < 10)
            sb.append("0");
         sb.append(secs);
         sb.append(".");
      }
      if (elapsed > 1000)
         System.err.println("*** bad algorithm!");
      sb.append(elapsed);
      return sb.toString();
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
         if (inst instanceof Element && pageEnt.urlPage) {
            needsDyn = true;

            if (pageEnt.doSync || testMode)
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

      int pageBodySize = sb == null ? 0 : sb.length();
      int initSyncSize = 0;

      if (needsDyn && needsInitialSync && !resetSync) {
         /*
         if (jsFiles.size() > 0) {
            sb.append("\n");
            for (String jsFile:jsFiles) {
               sb.append("<script type='text/javascript' src='" + jsFile + "'></script>\n");
            }
         }
         */
         // Gets the contents of the 'initial sync layer' - i.e. the data to populate the current state for this page.
         // Using the jsHttp destination but force the output to javascript
         sb.append("\n\n<!-- Init SC JS -->\n");
         sb.append("<script type='text/javascript'>\n");
         if (sc.bind.Bind.trace) {
            sb.append("sc_Bind_c.trace = true;\n");
         }
         sb.append("   var sc_windowId = " + ctx.getWindowId() + ";\n");
         if (doSync) {
            CharSequence initSync = SyncManager.getInitialSync("jsHttp", WindowScopeDefinition.scopeId, resetSync, "js");
               // Here are in injecting code into the generated script for debugging - if you enable logging on the server, it's on in the client automatically
            if (SyncManager.trace) {
               sb.append("sc_SyncManager_c.trace = true;\n");
            }
            if (SyncManager.verbose) {
               sb.append("sc_SyncManager_c.verbose = true;\n");
            }
            if (SyncManager.traceAll) {
               sb.append("sc_SyncManager_c.traceAll = true;\n");
            }
            // Propagate this option when we load up the page so the client has the same defaultLanguage that we do (if it's not the default)
            if (!SyncManager.defaultLanguage.equals("json"))
               sb.append("sc_SyncManager_c.defaultLanguage = \"" + SyncManager.defaultLanguage + "\";\n");
            if (initSync != null && (initSyncSize = initSync.length()) > 0) {
               sb.append(JSRuntimeProcessor.SyncBeginCode);
               sb.append(initSync);
               sb.append(JSRuntimeProcessor.SyncEndCode);
            }
         }
         if (trace || Element.trace) {
            sb.append("js_Element_c.trace = true;\n");
         }
         sb.append("</script>");
      }
      String pageOutput = sb == null ? "" : sb.toString();

      if (traceBuffer != null) {
         if (SyncManager.traceAll) {
            traceBuffer.append(" url=" + uri + " size: " + pageOutput.length() + " -----:\n");
            traceBuffer.append(trace ? pageOutput : StringUtil.ellipsis(pageOutput, SyncManager.logSize, false));
            traceBuffer.append("----- \n");
         }
         else if (verbose) {
            traceBuffer.append(" url=" + uri + " pageSize: " + pageBodySize + " initSyncSize: " + initSyncSize);
         }
      }

      return sb;
   }

   public boolean handleRequest(javax.servlet.http.HttpServletRequest request, 
                                javax.servlet.http.HttpServletResponse response) 
                           throws IOException, ServletException {
      Context ctx = null;  
      String uri = request.getRequestURI();
      TreeMap<String,String> queryParams = Context.initQueryParams(request);
      HttpSession session = null;

      CurrentScopeContext curScopeCtx = null;

      try {
         boolean isUrlPage = false;
         List<PageEntry> pageEnts = getPageEntries(uri, queryParams);

         if (pageEnts != null && pageEnts.size() > 0) {
            int sz = pageEnts.size();
            if (verbose)
               System.out.println("Page request: " + uri + " matched: " + sz + " objects" + getTraceInfo(session));

            List<Object> insts = null;

            PageEntry pageEnt = pageEnts.get(0);

            isUrlPage = pageEnt.urlPage;

            // Must be set before we call Context.initContext
            if (isUrlPage)
               ScopeEnvironment.setAppId(pageEnt.keyName);

            ctx = Context.initContext(request, response, queryParams);

            LayeredSystem sys = LayeredSystem.getCurrent();
            if (sys != null && sys.options.autoRefresh) {
               sys.rebuild();
            }

            long startTime = 0;
            if (verbose)
               startTime = System.currentTimeMillis();

            try {
               // TODO: check if we need a session for this request before creating it
               session = request.getSession(true);

               // Acquires the locks for the page and gets info
               curScopeCtx = initPageContext(ctx, uri, pageEnts, session, sys);

               // Make sure the page object is initialized for this request
               insts = initPageObjects(ctx, uri, pageEnts, session, curScopeCtx, true, true, false, sys);

               // Something in creating the object rejected, redirected or whatever
               if (ctx != null && ctx.requestComplete) {
                  if (trace) {
                     System.out.println("PageDispatcher request handled after page init - aborting processing: " + uri);
                  }
                  return true;
               }

               StringBuilder traceBuffer = new StringBuilder();
               StringBuilder pageOutput = getInitialSync(insts, ctx, uri, pageEnts, traceBuffer);

               if (ctx != null && ctx.requestComplete) {
                  if (verbose) {
                     System.out.println("PageDispatcher request handled after page init - aborting processing: " + traceBuffer);
                  }
                  return true;
               }

               if (pageOutput != null) {
                  ctx.write(pageOutput.toString());
               }

               if (verbose)
                  System.out.println("Page complete: session: " + getTraceInfo(session) + traceBuffer + " for " + getRuntimeString(startTime));

               // We don't want to register a command context for the '.css' page - i.e. pageEnt.resource = true
               if (sys != null && isUrlPage && (pageEnt.doSync || (testMode && !pageEnt.resource))) {
                  // In test mode only we accept the scopeAlias parameter, so we can attach to a specific request's scope context from the test script
                  String scopeContextName = !sys.options.testMode ? null : request.getParameter("scopeContextName");
                  // If the command line interpreter is enabled, use a scopeContextName so the command line is sync'd up to the scope of the page page we rendered
                  if (scopeContextName == null && sys.commandLineEnabled())
                     scopeContextName = "defaultCmdContext";
                  if (scopeContextName != null) {
                     CurrentScopeContext currentCtx = CurrentScopeContext.getCurrentScopeContext();
                     CurrentScopeContext.register(scopeContextName, currentCtx);
                     ctx.windowCtx.setValue("scopeContextName", scopeContextName);
                  }
               }
            }
            finally {
               try {
                  if (pageEnt.doSync) {
                     // This clears the initial sync flag in case we called setInitialSync(..., true) in initPageObjects.  It also clears the SyncState for the other initPageObjects cases.
                     SyncManager.setInitialSync("jsHttp", uri, WindowScopeDefinition.scopeId, false);
                  }
               }
               catch (RuntimeException exc) {
                  System.err.println("*** Application error processing initial sync for request: " + uri + ": " + exc);
                  exc.printStackTrace();
               }
            }
         }
         return pageEnts != null && pageEnts.size() > 0 && isUrlPage;
      }
      finally {
         try {
            if (ctx != null) {
               ctx.execLaterJobs();
               Context.clearContext();
            }
         }
         catch (RuntimeException exc) {
            System.err.println("*** Application error clearing context request: " + uri + ": " + exc);
            exc.printStackTrace();
         }
         finally {
            if (curScopeCtx != null)
               CurrentScopeContext.popCurrentScopeContext(true);
            ScopeEnvironment.setAppId(null);
         }
      }
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
      // Create the global scopes first, before the session scope and other application defined scopes are defined.
      sc.obj.GlobalScopeDefinition.getGlobalScopeDefinition();
      sc.obj.AppGlobalScopeDefinition.getAppGlobalScopeDefinition();

      // If any clients want to synchronize the layered system, this needs to be initialized first.  This has to be run after we've initialized the
      // destinations, hence after initTypes.
      LayeredSystem sys = LayeredSystem.getCurrent();
      if (sys != null) {
         // If we've included the js.layer layer, we'll need to sync the layered system
         if (sys.getLayerByDirName("sys.layeredSystem") != null)
            sys.initSync();

         if (sys.options.testMode)
            testMode = true;

         if (Element.trace) {
            Context.trace = trace = true;
         }

         if (Element.verbose) {
            Context.verbose = verbose = true;
         }

         if (trace || SyncManager.trace)
            Context.verbose = verbose = true;

         if (sys.options.verboseLocks)
            traceLocks = true;

         // This enables us to keep track of changes to types that might be registered with this servlet
         sys.registerTypeChangeListener(this);
      }

      this.filterConfig = filterConfig;

      if (pageDispatcher != null)
         System.err.println("*** Warning - replacing existing static page dispatcher");
      pageDispatcher = this;
   }

   public List<PageDispatcher.PageEntry> getPageEntriesOrError(Context ctx, String url) {
      List<PageDispatcher.PageEntry> pageEnts = getPageEntries(url, ctx.queryParams);
      if (pageEnts == null) {
         try {
            ctx.response.sendError(HttpServletResponse.SC_NOT_FOUND, "No page found for url: " + url);
         }
         catch (IOException exc) {}
         return null;
      }
      return pageEnts;
   }

   public StringBuilder getPageOutput(Context ctx, String url, List<PageDispatcher.PageEntry> pageEnts, CurrentScopeContext curScopeCtx,
                                      boolean initSync, boolean resetSync, LayeredSystem sys, StringBuilder traceBuffer) {
      StringBuilder pageOutput = null;
      try {
         List<Object> insts = pageDispatcher.initPageObjects(ctx, url, pageEnts, ctx.session, curScopeCtx, false, initSync, resetSync, sys);

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

   public void updateType(Object oldType, Object newType) {
      typeRemoved(oldType);
      typeCreated(newType);
   }

   public void typeCreated(Object newType) {
      Object urlAnnot = DynUtil.getAnnotation(newType, "sc.html.URL");
      if (urlAnnot != null) {
         String newTypeName = DynUtil.getTypeName(newType, false);
         Boolean isPageObj = (Boolean) DynUtil.getAnnotationValue(newType, "sc.html.URL", "page");
         Boolean isResourceObj = (Boolean) DynUtil.getAnnotationValue(newType, "sc.html.URL", "resource");
         boolean isURLPage = isPageObj == null || isPageObj;
         boolean isResource = isResourceObj != null && isResourceObj;
         boolean needsSync = DynUtil.needsSync(newType);
          String pattern = (String) DynUtil.getAnnotationValue(newType, "sc.html.URL", "pattern");
          String lockScope = (String) DynUtil.getAnnotationValue(newType, "sc.html.URL", "lockScope");
          String resultSuffix = (String) DynUtil.getInheritedAnnotationValue(newType, "sc.obj.ResultSuffix", "value");
          if (pattern == null) {
             pattern = "/" + sc.type.CTypeUtil.getClassName(newTypeName) + (resultSuffix == null ? "" : "." + resultSuffix);
          }
          System.out.println("*** Adding page type: " + newType);
          addPage(newTypeName, pattern, newType, isURLPage, needsSync, isResource,
                  DynUtil.getLayerPosition(newType), lockScope, QueryParamProperty.getQueryParamProperties(newType));
      }
   }

   public static String getMimeType(String pattern) {
      if (pattern.endsWith(".css"))
         return "text/css";
      return "text/html";
   }

   public void typeRemoved(Object oldType) {
      for (Iterator<Map.Entry<String,PageEntry>> it = pages.entrySet().iterator(); it.hasNext(); ) {
         Map.Entry<String,PageEntry> ent = it.next();
         if (ent.getValue().pageType == oldType) {
            System.out.println("*** Removing page type: " + oldType);
            it.remove();
         }
      }
   }

   public Collection<PageEntry> getAllPageEntries() {
      ArrayList<PageEntry> res = new ArrayList<PageEntry>();
      for (PageEntry ent:pages.values()) {
         if (!ent.keyName.equals("_index_"))
           res.add(ent);
      }
      return res;
   }

}
