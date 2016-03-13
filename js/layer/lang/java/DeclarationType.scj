package sc.lang.java;

import sc.obj.Constant;

public enum DeclarationType {
   CLASS("class"), OBJECT("object"), INTERFACE("interface"), ENUM("enum"), ANNOTATION("@interface"), ENUMCONSTANT("<enumConstant>"), TEMPLATE("<template>"), UNKNOWN("<modify>");
   DeclarationType(String nm) {
      name = nm;
      keyword = nm.startsWith("<") ? "" : nm;
   }
   @Constant
   public String name;
   @Constant
   public String keyword;
}
