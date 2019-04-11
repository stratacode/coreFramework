import sc.lang.StatementProcessor;
import sc.lang.AbstractInterpreter;
import sc.lang.java.IdentifierExpression;

import java.awt.EventQueue;
import javax.swing.SwingUtilities;
import sc.dyn.IScheduler;

public class SwingStatementProcessor  extends StatementProcessor {
   public SwingStatementProcessor() {
      AbstractInterpreter.statementProcessor = this;
   }

   private boolean isInlineStatement(Object statement) {
      if (statement instanceof List) {
         List statements = (List) statement;
         for (Object val:statements) {
            if (!isInlineStatement(val))
                return false;
         }
         return true;
      }
      if (statement instanceof IdentifierExpression) {
         IdentifierExpression ie = (IdentifierExpression) statement;
         if (ie.arguments != null && ie.identifiers.size() == 2 && ie.identifiers.get(0).equals("cmd") && ie.identifiers.get(1).equals("waitForUI"))
            return true;
      }
      return false;
   }

   boolean pendingStatement = false;

   public synchronized void processStatement(final AbstractInterpreter interp, final Object statement) {
      if (isInlineStatement(statement) || EventQueue.isDispatchThread()) {
         try {
            /*
            if (isInlineStatement(statement))
               System.out.println("*** processing inline statement: " + statement);
            else
               System.out.println("*** processing statement on event thread: " + statement);
             */
            super.processStatement(interp, statement);
         }
         finally {
            //System.out.println("*** Notifying for: " + statement + " on: " + sc.type.PTypeUtil.getThreadName());
            pendingStatement = false;
            notify();
         }
      }
      else {
         //System.out.println("*** Queuing statement for invokeLater: " + statement);
         pendingStatement = true;
         // Don't use the threadScheduler from the command interpreter - always go to the SwingScheduer
         DynUtil.frameworkScheduler.invokeLater(new Runnable() {
            public void run() {
               //System.out.println("*** Running statement from invokeLater: " + statement + " thread: " + sc.type.PTypeUtil.getThreadName());
               processStatement(interp, statement);
            }
         }, 0);
         // Don't return until the other thread has processed the statement
         try {
            if (pendingStatement) {
               //System.out.println("*** Waiting for being notified: " + statement + " on: " + sc.type.PTypeUtil.getThreadName());
               wait();
               //System.out.println("*** Waking up after being notified: " + statement + " on: " + sc.type.PTypeUtil.getThreadName());
            }
            //else
            //   System.out.println("*** Not waiting - process completed before we could wait " + statement + " on: " + sc.type.PTypeUtil.getThreadName());
         }
         catch (InterruptedException exc) {
            System.err.println("*** interrupted: " + exc);
         }
      }
   }
}
