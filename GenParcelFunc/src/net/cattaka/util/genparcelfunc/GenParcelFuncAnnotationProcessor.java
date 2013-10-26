
package net.cattaka.util.genparcelfunc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.cattaka.util.genparcelfunc.GenParcelFunc.NamingConventions;
import net.cattaka.util.genparcelfunc.ParcelFuncAttr.FieldType;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.apt.Messager;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.ArrayType;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.EnumType;
import com.sun.mirror.type.MirroredTypeException;
import com.sun.mirror.type.PrimitiveType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.SimpleTypeVisitor;

public class GenParcelFuncAnnotationProcessor implements AnnotationProcessor {

    static class EnvironmentBundle {
        Map<String, FieldEntry> fieldEntryMap;

        List<FieldEntry> fieldEntries;
    }

    static class FindEntriesPerVersion implements Comparable<FindEntriesPerVersion> {
        long version;

        List<FieldEntry> fieldEntries;

        @Override
        public int compareTo(FindEntriesPerVersion o) {
            return (int)(this.version - o.version);
        }
    }

    static class FieldEntry {
        boolean persistent = true;

        String name = null;

        String columnName = null;

        String constantsColumnName = null;

        FieldType fieldType = FieldType.STRING;

        String fieldClass;

        String customParser;

        FieldType customDataType = FieldType.STRING;
    }

    static class OrderByEntry extends FieldEntry {
        boolean desc;

        public OrderByEntry(FieldEntry fe, boolean desc) {
            super();
            this.persistent = fe.persistent;
            this.name = fe.name;
            this.columnName = fe.columnName;
            this.fieldType = fe.fieldType;
            this.fieldClass = fe.fieldClass;
            this.customParser = fe.customParser;
            this.customDataType = fe.customDataType;
            this.desc = desc;
        }
    }

    public GenParcelFuncAnnotationProcessor(AnnotationProcessorEnvironment env) {
        _env = env;
    }

    public void process() {
        Messager messager = _env.getMessager();
        Collection<TypeDeclaration> tds = this.getEnvironment().getTypeDeclarations();
        for (TypeDeclaration td : tds) {
            GenParcelFunc genDbHandler = td.getAnnotation(GenParcelFunc.class);
            if (genDbHandler == null) {
                continue;
            }
            String packageName = td.getPackage().getQualifiedName();
            String className = td.getSimpleName();
            String cprPackageName = packageName + ".pf";
            String cprClassName = className + "Func";

            EnvironmentBundle bundle = new EnvironmentBundle();
            {
                bundle.fieldEntries = pickFieldDeclaration(td, messager, genDbHandler);
                { // fieldEntryMap:name->fieldEntyrの作成
                    Map<String, FieldEntry> feMap = new HashMap<String, FieldEntry>();
                    for (FieldEntry fe : bundle.fieldEntries) {
                        feMap.put(fe.name, fe);
                    }
                    bundle.fieldEntryMap = feMap;
                }
            }
            try {
                Filer f = getEnvironment().getFiler();
                PrintWriter pw = f.createSourceFile(cprPackageName + "." + cprClassName);
                pw.println("package " + cprPackageName + ";");
                pw.println("import " + packageName + "." + className + ";");
                pw.println("import android.os.Parcel;");
                pw.println("import android.os.Parcelable;");
                pw.println();
                pw.println("public class " + cprClassName + " {");
                pw.println("    public static final Parcelable.Creator<" + className
                        + "> CREATOR = new Parcelable.Creator<" + className + ">() {");
                pw.println("        @Override");
                pw.println("        public " + className + " createFromParcel(Parcel in) {");
                pw.println("            " + className + " dest = new " + className + "();");
                pw.println("            readFromParcel(dest, in);");
                pw.println("            return dest;");
                pw.println("        }");
                pw.println("        @Override");
                pw.println("        public " + className + "[] newArray(int size) {");
                pw.println("        	return new " + className + "[size];");
                pw.println("        }");
                pw.println("    };");

                pw.println("    public static void readFromParcel(" + className
                        + " dest, Parcel in) {");
                for (FieldEntry fe : bundle.fieldEntries) {
                    String ex1 = null;
                    String ex2 = null;
                    if (fe.fieldType == FieldType.ENUM) {
                        ex1 = fe.fieldClass;
                    } else if (fe.fieldType == FieldType.CUSTOM) {
                        ex1 = fe.customParser;
                        ex2 = (fe.customDataType != null) ? fe.customDataType.parcelReadFragment
                                : "String";
                    }
                    pw.println("        "
                            + String.format(Locale.ROOT, fe.fieldType.readFormat, "dest",
                                    convertCap(fe.name, true), ex1, ex2));
                }
                pw.println("    }");

                pw.println("    public static void writeToParcel(" + className
                        + " src, Parcel out, int flags) {");
                for (FieldEntry fe : bundle.fieldEntries) {
                    String ex1 = null;
                    String ex2 = null;
                    if (fe.fieldType == FieldType.ENUM) {
                        ex1 = fe.fieldClass;
                    } else if (fe.fieldType == FieldType.CUSTOM) {
                        ex1 = fe.customParser;
                        ex2 = (fe.customDataType != null) ? String.format(Locale.ROOT,
                                fe.customDataType.parcelWriteFragment, "out",
                                "%3$s.encode(%1$s.get%2$s())") : "String";
                        ex2 = String
                                .format(Locale.ROOT, ex2, "src", convertCap(fe.name, true), ex1);
                    }
                    pw.println("        "
                            + String.format(Locale.ROOT, fe.fieldType.writeFormat, "src",
                                    convertCap(fe.name, true), ex1, ex2));
                }
                pw.println("    }");
                pw.println("    private static byte[] readByteArray(Parcel in) {");
                pw.println("        int r = in.readInt();");
                pw.println("        byte[] bs = new byte[r];");
                pw.println("        in.readByteArray(bs);");
                pw.println("        return bs;");
                pw.println("    }");

                pw.println("    private static void writeByteArray(Parcel out, byte[] bs) {");
                pw.println("        out.writeInt(bs.length);");
                pw.println("        out.writeByteArray(bs);");
                pw.println("    }");
                pw.println("}");
                pw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public AnnotationProcessorEnvironment getEnvironment() {
        return _env;
    }

    AnnotationProcessorEnvironment _env;

    private static List<FieldEntry> pickFieldDeclaration(TypeDeclaration td, Messager messager,
            GenParcelFunc genDbHandler) {
        List<FieldEntry> fes = new ArrayList<FieldEntry>();
        List<FieldDeclaration> fds = new ArrayList<FieldDeclaration>(td.getFields());
        Collections.sort(fds, new Comparator<FieldDeclaration>() {
            @Override
            public int compare(FieldDeclaration o1, FieldDeclaration o2) {
                int r = o1.getPosition().line() - o2.getPosition().line();
                return (r != 0) ? r : (o1.getPosition().column() - o2.getPosition().column());
            }
        });

        for (FieldDeclaration fd : fds) {
            FieldEntry fe = new FieldEntry();
            if (fd.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            {
                ParcelFuncAttr attribute = fd.getAnnotation(ParcelFuncAttr.class);
                if (attribute != null) {
                    fe.persistent = attribute.persistent();
                    fe.customDataType = attribute.customDataType();
                    try {
                        fe.customParser = attribute.customCoder().getName();
                    } catch (MirroredTypeException mte) {
                        fe.customParser = mte.getTypeMirror().toString();
                    }
                }
            }
            {
                class MyTypeVisitor extends SimpleTypeVisitor {
                    private String qualifiedName;

                    private boolean primitiveFlag = false;

                    private boolean arrayFlag = false;

                    private boolean enumFlag = false;

                    @Override
                    public void visitDeclaredType(DeclaredType t) {
                        super.visitDeclaredType(t);
                        qualifiedName = (t != null && t.getDeclaration() != null) ? t
                                .getDeclaration().getQualifiedName() : null;
                    }

                    @Override
                    public void visitEnumType(EnumType t) {
                        super.visitEnumType(t);
                        this.enumFlag = true;
                    }

                    @Override
                    public void visitArrayType(ArrayType t) {
                        super.visitArrayType(t);
                        TypeMirror type = t.getComponentType();
                        arrayFlag = true;
                        if (type instanceof PrimitiveType) {
                            primitiveFlag = true;
                            qualifiedName = getPrimitiveTypeClassName((PrimitiveType)type);
                        } else if (type instanceof DeclaredType) {
                            qualifiedName = ((DeclaredType)type).getDeclaration()
                                    .getQualifiedName();
                        }
                    }

                    @Override
                    public void visitPrimitiveType(PrimitiveType t) {
                        super.visitPrimitiveType(t);
                        primitiveFlag = true;
                        qualifiedName = getPrimitiveTypeClassName(t);
                    }
                }
                MyTypeVisitor myTypeVisitor = new MyTypeVisitor();
                TypeMirror typeMirror = fd.getType();
                typeMirror.accept(myTypeVisitor);
                if (fe.customParser != null && !Object.class.getName().equals(fe.customParser)) {
                    fe.fieldType = FieldType.CUSTOM;
                } else if (myTypeVisitor.arrayFlag) {
                    if (byte.class.getCanonicalName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.BLOB;
                    } else {
                        // not supported
                        messager.printError(fd.getPosition(),
                                "Array type is not supported. set persistent=false, or use @customCoder and @customDataType");
                    }
                } else if (myTypeVisitor.primitiveFlag) {
                    // not supported
                    messager.printError(fd.getPosition(),
                            "Primitive type is not supported. set persistent=false, or use @customCoder and @customDataType");
                } else {
                    if (myTypeVisitor.qualifiedName == null) {
                        fe.fieldType = FieldType.STRING;
                        messager.printError(fd.getPosition(), "Unknown data type.");
                    } else if (Integer.class.getName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.INTEGER;
                    } else if (Short.class.getName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.SHORT;
                    } else if (Long.class.getName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.LONG;
                    } else if (Float.class.getName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.FLOAT;
                    } else if (Double.class.getName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.DOUBLE;
                    } else if (String.class.getName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.STRING;
                    } else if (byte[].class.getName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.BLOB;
                    } else if (Date.class.getName().equals(myTypeVisitor.qualifiedName)) {
                        fe.fieldType = FieldType.DATE;
                    } else if (myTypeVisitor.enumFlag) {
                        fe.fieldType = FieldType.ENUM;
                    } else {
                        fe.fieldType = FieldType.STRING;
                        if (fe.persistent) {
                            messager.printError(fd.getPosition(),
                                    "Data type is not supported. set persistent=false, or use @customCoder and @customDataType");
                        }
                    }
                }
                fe.fieldClass = myTypeVisitor.qualifiedName;
            }
            {
                fe.name = fd.getSimpleName();
                fe.columnName = convertName(genDbHandler.fieldNamingConventions(), fe.name);
                fe.constantsColumnName = convertName(NamingConventions.UPPER_COMPOSITE, fe.name);
            }
            if (fe.persistent) {
                fes.add(fe);
            } else {
                // ignored
            }
        }
        return fes;
    }

    private static String getPrimitiveTypeClassName(PrimitiveType t) {
        if (t != null) {
            switch (t.getKind()) {
                case BOOLEAN:
                    return boolean.class.getCanonicalName();
                case BYTE:
                    return byte.class.getCanonicalName();
                case SHORT:
                    return short.class.getCanonicalName();
                case INT:
                    return int.class.getCanonicalName();
                case LONG:
                    return long.class.getCanonicalName();
                case CHAR:
                    return char.class.getCanonicalName();
                case FLOAT:
                    return float.class.getCanonicalName();
                case DOUBLE:
                    return double.class.getCanonicalName();
            }
        }
        return null;
    }

    private static String convertName(NamingConventions namingConventions, String src) {
        if (src == null) {
            return null;
        }
        switch (namingConventions) {
            case LOWER_CAMEL_CASE:
                return convertCap(src, false);
            case UPPER_CAMEL_CASE:
                return convertCap(src, true);
            case LOWER_COMPOSITE:
                return camelToComposite(src, false);
            case UPPER_COMPOSITE:
                return camelToComposite(src, true);
        }
        return src;
    }

    private static String camelToComposite(String camel, boolean upperCase) {
        if (camel == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char ch = camel.charAt(i);
            if (i > 0 && Character.isUpperCase(ch)) {
                sb.append('_');
                sb.append(ch);
            } else {
                sb.append(Character.toUpperCase(ch));
            }
        }
        if (upperCase) {
            return sb.toString().toUpperCase();
        } else {
            return sb.toString().toLowerCase();
        }
    }

    private static String convertCap(String name, boolean upperCase) {
        if (name == null) {
            return null;
        }
        if (name.length() == 0) {
            return name;
        }
        if (upperCase) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        } else {
            return name.substring(0, 1).toLowerCase() + name.substring(1);
        }
    }
}
