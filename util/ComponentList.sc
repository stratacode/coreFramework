@Component
@CompilerSettings(objectTemplate="sc.util.ComponentListInit", newTemplate="sc.util.ComponentListNew",
                  childTypeParameter="E",dynChildManager="sc.util.ListDynChildManager")
public class ComponentList<E> extends ArrayList<E> {
}
