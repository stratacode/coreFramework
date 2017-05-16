import sc.js.JSSettings;
import sc.js.JSMethodSettings;

@JSSettings(jsLibFiles="js/javasys.js", prefixAlias="jv_")
String {
   override @JSMethodSettings(replaceWith="_length") length();
   override @JSMethodSettings(replaceWith="_valueOf") valueOf(Object c);
   override @JSMethodSettings(replaceWith="_valueOf") valueOf(int c);
   override @JSMethodSettings(replaceWith="_valueOf") valueOf(double c);
   override @JSMethodSettings(replaceWith="_valueOf") valueOf(float c);
   override @JSMethodSettings(replaceWith="_valueOf")  valueOf(char data[]);
   override @JSMethodSettings(replaceWith="_valueOf")  valueOf(boolean b);
   override @JSMethodSettings(replaceWith="_valueOf")  valueOf(char c);
   override @JSMethodSettings(replaceWith="substring") subSequence(int start, int end);

   // The normal char to int conversion done in the JS needs to be suppressed here because in JS, the 'ch' parameters are really strings
   override @JSMethodSettings(parameterTypes="char") indexOf(int ch);
   override @JSMethodSettings(parameterTypes="char,int") indexOf(int ch, int start);
}
