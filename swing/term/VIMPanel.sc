import java.io.IOException;
import java.io.File;

class VIMPanel extends CommandTerminal implements TextComponentStyle {
   List<String> fileNames;

   int rows = -1, cols = -1;

   ArrayList<String> pendingCommands = null;

   int caretPosition = -1;

   // Byte offset to start out in.  Add 1 because VIM is 1 based
   caretPosition =: updateCaret();

   void updateCaret() {
      sendVIMCommand("goto " + String.valueOf(caretPosition + 1), true, false); // goto byte offset
      sendVIMCommand("zz", false, false); // center the window
   }

   void addInitArgs() {
      commandArgs = new ArrayList<String>();
      commandArgs.add("vim");
      if (fileNames != null) {
         for (String file:fileNames) {
            commandArgs.add(file);
         }
      }
      if (emulation.getRows() > 1 && emulation.getColumns() > 0) {
         commandArgs.add("--cmd");
         commandArgs.add("se lines=" + (rows = emulation.getRows()) + " columns=" + (cols = emulation.getColumns()));
      }

      //commandArgs.add("--startuptime");
      //commandArgs.add("/tmp/SCVIMDBG.txt");

/*
      commandArgs.add("-s");
      commandArgs.add("/tmp/xxx1");
*/

      // ARG!  If we do not add a dummy script to the command line options, VIM will stall for 2 seconds telling us that the input is not a terminal.  Not sure how to set
      // the TTY mode on the input stream, short of setting up a dummy pseudo terminal thing so this is a real hack workaround
      try {
         commandArgs.add("-s");
         File tmp = File.createTempFile("layerCakeVIMTemp", ".vimtmp");
         tmp.deleteOnExit();
         commandArgs.add(tmp.getAbsolutePath());
      }
      catch (IOException exc) {
         System.err.println("*** error creating temp file for VIM warning delay workaround");
      }

      if (pendingCommands != null) {
         // Run these after loading the file
         for (String cmd:pendingCommands) {
            commandArgs.add("-c");
            commandArgs.add(cmd);
         }
         pendingCommands = null;
      }
   }


   void sendVIMCommand(String cmd, boolean useColon, boolean queue) {
      try {
         //System.out.println("*** sending VIM command: " + cmd);
         if (output != null) {
            StringBuilder sb = new StringBuilder();
            appendEscape(sb);
            if (useColon)
               sb.append(":");
            sb.append(cmd);
            if (useColon)
               sb.append('\n');
            long start = System.currentTimeMillis();
            String str = sb.toString();
            output.write(str.getBytes());
            if (debug)
               System.err.println("VIM command: \"" + str + "\" (" + (System.currentTimeMillis() - start) + "ms)");
            output.flush();
         }
         else if (queue) {
            if (pendingCommands == null)
               pendingCommands = new ArrayList<String>();
            pendingCommands.add(cmd);
         }
      }
      catch (IOException exc) {
         System.err.println("*** error writng VIM command: " + cmd);
      }
   }

   void updateTerminalSize(int c, int r) {
      // Don't send an update before the window's size has been set or if it is set to 1 or the same
      if (c <= 1 || r <= 1 || (c == cols && r == rows))
         return;

      StringBuilder sb = new StringBuilder();
      sb.append("se lines=" + r);
      sb.append(" columns=" + c);

      rows = r;
      cols = c;

      sendVIMCommand(sb.toString(), true, false);
   }

   // An escape that does not beep if you are not in insert mode
   void appendEscape(StringBuilder sb) {
     sb.append((char) 27); // Escape
     //sb.append((char) 0x1c);  // ctrl \
     //sb.append((char) 0x0e); // ctrl n
   }

   void writeAndQuit() {
      sendVIMCommand("xa", true, false);
   }

   void removeEditor() {
     //System.out.println("*** in remove editor for VIM panel: " + this);
      writeAndQuit();
      waitForProcessToExit();
      // Don't want to be trying to communicate with this process anymore so remove any bindings
      sc.bind.Bind.removeBindings(this);
   }

   void addSplitPanes(int[] lineNums) {
      // From bottom to top because default is to put them on the top and move focus to the new one.  So this leaves
      // the focus on the first one.
      sendVIMCommand("" + lineNums[lineNums.length-1], true,  true);
      for (int i = lineNums.length-2; i >= 0; i--) {
         sendVIMCommand("sp+" + lineNums[i], true, true);
      }
   }

   void requestFocus() {
      terminal.requestFocus();
   }

   void showCursor(boolean val) {
      emulation.showCursor(val);
   }

   boolean hasFocus() {
      return terminal.hasFocus();
   }
}
