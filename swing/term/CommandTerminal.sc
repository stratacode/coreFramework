import de.mud.terminal.SwingTerminal;
import de.mud.terminal.VDUBuffer;
import de.mud.terminal.vt320;

import sc.util.FileUtil;

import java.awt.Graphics;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

class CommandTerminal extends JPanel implements FocusListener {
   boolean debug = false;
   String encoding = "UTF-8";
   List<String> commandArgs;

   protected Process process;
   protected BufferedOutputStream output;

   void init() {
      add(terminal);

      terminal.size = size;

      terminal.addFocusListener(this);

      // Any terminal specific args such as VIM's adding lines/columns for startup size
      addInitArgs();

      if (commandArgs == null || commandArgs.size() == 0) {
         System.err.println("*** No command set for command terminal");
         return;
      }
   }

   /** Called to kick off the command */
   void startCommand() {
      long start = System.currentTimeMillis();
      process = FileUtil.startCommand(commandArgs);
      if (process == null) {
         return;
      }

      output = new BufferedOutputStream(process.getOutputStream());
      reader.start();
      if (debug)
         System.err.println("start: \"" + process + "\" (" + (System.currentTimeMillis() - start) + "ms)");
   }

   /** Override in subclasses to add the command and other args */
   void addInitArgs() {
   }

   // Reads from the program and writes to the terminal
   object reader extends Thread {
      void run() {
         BufferedInputStream bis = new BufferedInputStream(process.getInputStream());
         byte [] buf = new byte[1024];
         int len;

         try {
            while ((len = bis.read(buf, 0, buf.length)) != -1) {
               if (len > 0) {
                  String str = new String(buf, 0, len, encoding);

                  long start = System.currentTimeMillis();
                  emulation.putString(str);

                  if (debug)
                     System.err.println("read: \"" + str + "\" (" + (System.currentTimeMillis() - start) + "ms)");
               }
            }
         }
         catch (IOException e) {
           System.err.println("*** failed to read from command: " + e);
           // TODO: close window
         }

         if (output != null) {
            try {
               output.close();
               output = null;
            }
            catch (IOException exc) {}
         }
      }
   }

   class Emulator extends vt320 {
      void write(byte[] buf) {
        try {

           long start = System.currentTimeMillis();
           output.write(buf);
           if (debug)
             System.err.println("write: \"" + new String(buf) + "\" (" + (System.currentTimeMillis() - start) + "ms)");
           output.flush();
        }
        catch (IOException e) {
          System.err.println("*** failed to write to command: " + e);
        }
      }
      void setWindowSize(int c, int r) {
         updateTerminalSize(c, r);
      }
   }

   /** 
    * Called when the user updates the size.  If you need to send a command to the program
    * override this method. 
    */
   void updateTerminalSize(int c, int r) {
   }

   /** These are good programmer font colors, close to the originals to make it easy to set them up. */
   Color colors[] = {Color.black,
                           new Color(0xcc, 0x22, 0x22),
                           new Color(0x3A, 0x87, 0x6D),
                           Color.yellow,
                           new Color(0x00, 0x18, 0x93),
                           new Color(0xcc, 0x00, 0xcc),
                           Color.cyan,
                           Color.white,
                           null,
                           null,
  };
   String fontName = "Courier";
   int fontStyle = Font.PLAIN;
   int fontSize = 12;
   font := new Font(fontName, fontStyle, fontSize);
   int fontWidth := getFontMetrics(font).charWidth('@');
   int fontHeight := getFontMetrics(font).getHeight();
   Emulator emulation = new Emulator();
   SwingTerminal terminal = new SwingTerminal(emulation, font);
   {
      terminal.setResizeStrategy(SwingTerminal.RESIZE_SCREEN);
      terminal.setCursorColors(Color.BLACK, Color.WHITE);
      terminal.setForeground(Color.BLACK);
      terminal.setBackground(Color.WHITE);
      terminal.setColorSet(colors);
   }
   size =: resizeTerminal();

   boolean sizeUpdatePending = false;
   void resizeTerminal() {
      if (sizeUpdatePending) {
         return;
      }
      sizeUpdatePending = true;
      // Need to do this later to avoid double-resizing because of width and height changing.  Really should get rid of that
      // by doing a two pass batch sendEvent... first mark stuff invalid, then evaluate.  Otherwise, method bindings either always
      // have to evaluate or get done more than once on a change like this.
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
             sizeUpdatePending = false;
             terminal.size = size;
         }
      });
   }

   // This helps the terminal refresh apparently by eliminating some background clear done in 
   // the default implementation.
   void update(Graphics g) {
      paint(g);
   }

   void refreshWindow() {
      terminal.redraw();
   }

   int waitForProcessToExit() {
      try {
         return process.waitFor();
      }
      catch (InterruptedException exc) {
         return -1;
      }
   }

   void focusGained(FocusEvent e) {
      emulation.showCursor(true);
      terminal.redraw();
   }

   void focusLost(FocusEvent e) {
      emulation.showCursor(false);
      terminal.redraw();
   }
}
