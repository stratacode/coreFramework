import sc.js.JSSettings;
import sc.js.JSMethodSettings;

@JSSettings(jsLibFiles="js/big.js", replaceWithNative="Big")
BigDecimal {
  override @JSMethodSettings(replaceWith="plus") add(BigDecimal bd);
  override @JSMethodSettings(replaceWith="times") multiply(BigDecimal bd);
  override @JSMethodSettings(replaceWith="div") divide(BigDecimal bd);
  override @JSMethodSettings(replaceWith="minus") subtract(BigDecimal bd);

  override @JSMethodSettings(replaceWith="cmp") compareTo(BigDecimal bd);
  override @JSMethodSettings(replaceWith="eq") equals(Object bd);
}
