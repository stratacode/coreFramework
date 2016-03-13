import sc.lang.StatementProcessor;
import sc.lang.AbstractInterpreter;
import sc.lang.java.IdentifierExpression;

import java.awt.EventQueue;
import javax.swing.SwingUtilities;

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

   public synchronized void processStatement(final AbstractInterpreter interp, final Object statement) {
      if (isInlineStatement(statement) || EventQueue.isDispatchThread()) {
         try {
            super.processStatement(interp, statement);
         }
         finally {
            notify();
         }
      }
      else {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               processStatement(interp, statement);
            }
         });
         // Don't return until the other thread has processed the statement
         try {
            wait();
         }
         catch (InterruptedException exc) {
            System.err.println("*** interrupted: " + exc);
         }
      }
   }
}
