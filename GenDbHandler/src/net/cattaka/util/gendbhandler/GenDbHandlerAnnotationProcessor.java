package net.cattaka.util.gendbhandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cattaka.util.gendbhandler.Attribute.FieldType;
import net.cattaka.util.gendbhandler.GenDbHandler.NamingConventions;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.apt.Messager;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.EnumType;
import com.sun.mirror.type.MirroredTypeException;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.SimpleTypeVisitor;
import com.sun.mirror.util.SourcePosition;

public class GenDbHandlerAnnotationProcessor implements AnnotationProcessor {
	
    static class EnvironmentBundle {
        Map<String, FieldEntry> fieldEntryMap;
        List<FieldEntry> fieldEntries;
           List<FindEntry> findList;
           List<UniqueEntry> uniqueList;
    }
    
    static class FieldEntry {
        boolean persistent = true;
        boolean primaryKey = false;
        String name = null;
        String columnName = null;
        FieldType fieldType = FieldType.STRING;
        String fieldClass;
    	String customParser;
    	FieldType customDataType = FieldType.STRING;
        long version = 1;
    }
    static class OrderByEntry extends FieldEntry {
    	boolean desc;
		public OrderByEntry(FieldEntry fe, boolean desc) {
			super();
			this.persistent = fe.persistent;
			this.primaryKey = fe.primaryKey;
			this.name = fe.name;
			this.columnName = fe.columnName;
			this.fieldType = fe.fieldType;
			this.fieldClass = fe.fieldClass;
	    	this.customParser = fe.customParser;
	    	this.customDataType = fe.customDataType;
			this.version = fe.version;
			this.desc = desc;
		}
    }
    static class FindEntry {
        List<FieldEntry> columns;
        List<OrderByEntry> orderBy;
    }
    static class UniqueEntry {
        List<FieldEntry> columns;
    }
    
    public GenDbHandlerAnnotationProcessor(AnnotationProcessorEnvironment env) {
        _env = env;
    }

    public void process() {
        Messager messager = _env.getMessager();
        Collection<TypeDeclaration> tds = this.getEnvironment().getTypeDeclarations();
        for (TypeDeclaration td : tds) {
            GenDbHandler genDbHandler = td.getAnnotation(GenDbHandler.class);
            String packageName = td.getPackage().getQualifiedName();
            String className = td.getSimpleName();
            String cprPackageName = packageName + ".handler";
            String cprClassName = className + "Handler";
            String tableName = convertName(genDbHandler.tableNamingConventions(), className);
            
            EnvironmentBundle bundle = new EnvironmentBundle();
            {
                bundle.fieldEntries = pickFieldDeclaration(td, messager, genDbHandler);
                {    // fieldEntryMap:name->fieldEntyrの作成
                    Map<String, FieldEntry> feMap = new HashMap<String, FieldEntry>();
                    for (FieldEntry fe : bundle.fieldEntries) {
                        feMap.put(fe.name, fe);
                    }
                    bundle.fieldEntryMap = feMap;
                }
                   bundle.findList = createFindEntries(td.getPosition(), genDbHandler.find(), bundle, messager);
                   bundle.uniqueList = createUniqueEntries(td.getPosition(), genDbHandler.unique(), bundle, messager);
            }
            FieldEntry keyFieldEntry = null;
            {    // Check existence of PRIMARY KEY (Only 1 key is supported.)
                int primaryKeyCount = 0;
                for (FieldEntry fe : bundle.fieldEntries) {
                    if (fe.primaryKey) {
                        primaryKeyCount++;
                        keyFieldEntry = fe;
                    }
                }
                if (primaryKeyCount == 0) {
                    messager.printError(td.getPosition(), "At least one primary key is required. put @Attribute(primaryKey=true) to field of key");
                } else if (primaryKeyCount > 1) {
                    messager.printError(td.getPosition(), "Only single primary key is supported.");
                }
            }
            try {
                Filer f = getEnvironment().getFiler();
                PrintWriter pw = f.createSourceFile(cprPackageName + "." + cprClassName);
                pw.println("package " + cprPackageName + ";");
                pw.println("import android.content.ContentValues;");
                pw.println("import android.database.Cursor;");
                pw.println("import android.database.sqlite.SQLiteDatabase;");
                pw.println("import " + packageName + "." + className + ";");
                pw.println();
                pw.println("public class " + cprClassName + " {");
                pw.println("    public static final String SQL_CREATE_TABLE = \"" + createCreateStatement(tableName, bundle) + "\";");
                pw.println("    public static final String TABLE_NAME = \"" + tableName + "\";");
                pw.println("    public static final String COLUMNS = \"" + createColumns(bundle) + "\";");
                pw.println("    public static final String[] COLUMNS_ARRAY = new String[] {" + createColumnsArray(bundle) + "};");
                
                // Insert
                pw.println("    public static long insert(SQLiteDatabase db, " + className + " model) {");
                pw.println("        ContentValues values = new ContentValues();");
                for (FieldEntry fe : bundle.fieldEntries) {
                    if (!fe.primaryKey) {
                        pw.println("        values.put(\"" + fe.columnName + "\", " + convertGetter("model", fe) + ");");
                    }
                }
                pw.println("        long key = db.insert(TABLE_NAME, null, values);");
                for (FieldEntry fe : bundle.fieldEntries) {
                    if (fe.primaryKey) {
                        pw.println("        "+convertSetter("model", fe, "key") + ";");
                    }
                }
                pw.println("        return key;");
                pw.println("    }");
                // Update
                pw.println("    public static long update(SQLiteDatabase db, " + className + " model) {");
                pw.println("        ContentValues values = new ContentValues();");
                pw.println("        String whereClause = \"" + keyFieldEntry.columnName + "=?\";");
                pw.println("        String[] whereArgs = new String[]{String.valueOf(" + convertGetter("model", keyFieldEntry) + ")};");
                   for (FieldEntry fe : bundle.fieldEntries) {
                       if (!fe.primaryKey) {
                           pw.println("        values.put(\"" + fe.columnName + "\", " + convertGetter("model", fe) + ");");
                       }
                   }
                pw.println("        return db.update(TABLE_NAME, values, whereClause, whereArgs);");
                pw.println("    }");
                // Delete
                pw.println("    public static long delete(SQLiteDatabase db, " + convertFieldType2Java(keyFieldEntry) + " key) {");
                pw.println("        String whereClause = \"" + keyFieldEntry.columnName + "=?\";");
                pw.println("        String[] whereArgs = new String[]{String.valueOf(key)};");
                pw.println("        return db.delete(TABLE_NAME, whereClause, whereArgs);");
                pw.println("    }");
                // Find
                for (FindEntry findEntry : bundle.findList) {
                    if (checkUnique(findEntry, bundle.uniqueList)) {
                        pw.println("    public static " + className + " " + createMethodName(findEntry) + "(SQLiteDatabase db" + createMethodArg(findEntry) + ") {");
                        pw.println("        String selection = \"" + createSelection(findEntry) + "\";");
                        pw.println("        String[] selectionArgs = new String[]{" + createSelectionArgs(findEntry) + "};");
                        pw.println("        Cursor cursor = db.query(TABLE_NAME, COLUMNS_ARRAY, selection, selectionArgs, null, null, null);");
                        pw.println("        if (cursor.moveToNext()) {");
                        pw.println("            return readCursorInner(cursor);");
                        pw.println("        } else {");
                        pw.println("            return null;");
                        pw.println("        }");
                        pw.println("    }");
                    } else {
                        String orderBy = createOrderBy(findEntry);
                        pw.println("    public static java.util.List<" + className + "> " + createMethodName(findEntry) + "(SQLiteDatabase db, int limit" + createMethodArg(findEntry) + ") {");
                        pw.println("        String selection = \"" + createSelection(findEntry) + "\";");
                        pw.println("        String[] selectionArgs = new String[]{" + createSelectionArgs(findEntry) + "};");
                        pw.println("        String limitStr = (limit > 0) ? String.valueOf(limit) : null;");
                        if (orderBy.length() > 0) {
                            pw.println("        String orderBy = \"" + createOrderBy(findEntry) + "\";");
                            pw.println("        Cursor cursor = db.query(TABLE_NAME, COLUMNS_ARRAY, selection, selectionArgs, null, null, orderBy, limitStr);");
                        } else {
                            pw.println("        Cursor cursor = db.query(TABLE_NAME, COLUMNS_ARRAY, selection, selectionArgs, null, null, null, limitStr);");
                        }
                        pw.println("        java.util.List<"+className+"> result = new java.util.ArrayList<"+className+">();");
                        pw.println("        while (cursor.moveToNext()) {");
                        pw.println("            result.add(readCursorInner(cursor));");
                        pw.println("        }");
                        pw.println("        return result;");
                        pw.println("    }");
                    }
                }
                pw.println("    private static "+className+" readCursorInner(Cursor cursor) {");
                pw.println("        "+className+" result = new "+className+"();");
                {
                    int index = 0;
                    for (FieldEntry fe : bundle.fieldEntries) {
                        pw.println("        result.set"+convertCap(fe.name,true)+"(!cursor.isNull("+index+") ? "+createCursorGetter("cursor", fe, String.valueOf(index))+" : null);");
                        index++;
                    }
                }
                pw.println("        return result;");
                pw.println("    }");
                pw.println("    public static "+className+" readCursor(Cursor cursor) {");
                pw.println("        "+className+" result = new "+className+"();");
                pw.println("        int idx;");
                {
                    int index = 0;
                    for (FieldEntry fe : bundle.fieldEntries) {
                        pw.println("        idx = cursor.getColumnIndex(\"" + fe.columnName + "\");");
                        pw.println("        result.set"+convertCap(fe.name,true)+"(idx>0 && !cursor.isNull(idx) ? "+createCursorGetter("cursor", fe, "idx")+" : null);");
                        index++;
                    }
                }
                pw.println("        return result;");
                pw.println("    }");
                pw.println("}");
                pw.close();
            } catch( IOException ioe ) {
                ioe.printStackTrace();
            }
        }
    }

    public AnnotationProcessorEnvironment getEnvironment()
    {
        return _env;
    }

    AnnotationProcessorEnvironment    _env;

    public String createCreateStatement(String tableName, EnvironmentBundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(tableName);
        sb.append("(");
        boolean firstFlag = true;
        for (FieldEntry fe : bundle.fieldEntries) {
            if (firstFlag) {
                firstFlag = false;
            } else {
                sb.append(",");
            }
            sb.append(fe.columnName);
            sb.append(" ");
            sb.append(convertFieldType2DbType(fe));
            if (fe.primaryKey) {
                sb.append(" PRIMARY KEY AUTOINCREMENT");
            }
        }
        for (UniqueEntry entries : bundle.uniqueList) {
            sb.append(",UNIQUE(");
            boolean firstFlag2 = true;
            for (FieldEntry entry : entries.columns) {
                if (firstFlag2) {
                    firstFlag2 = false;
                } else {
                    sb.append(",");
                }
                sb.append(entry.columnName);
            }
            sb.append(")");
        }
        sb.append(")");
        return sb.toString();
    }
    private static String createMethodName(FindEntry findEntry) {
        StringBuilder sb = new StringBuilder();
        for (FieldEntry fe : findEntry.columns) {
            if (sb.length() == 0) {
                sb.append("findBy");
            } else {
                sb.append("And");
            }
            sb.append(convertCap(fe.name, true));
        }
        if (findEntry.orderBy.size() > 0) {
            boolean firstFlag = true;
            for (OrderByEntry fe : findEntry.orderBy) {
                if (firstFlag) {
                    sb.append("OrderBy");
                    firstFlag = false;
                } else {
                    sb.append("And");
                }
                sb.append(convertCap(fe.name, true));
                sb.append((fe.desc) ? "Desc" : "Asc");
            }
        }
        return sb.toString();
    }
    private static String createMethodArg(FindEntry findEntry) {
        StringBuilder sb = new StringBuilder();
        for (FieldEntry fe : findEntry.columns) {
               sb.append(", ");
            sb.append(convertFieldType2Java(fe));
               sb.append(" ");
            sb.append(fe.name);
        }
        return sb.toString();
    }
    private static String createColumns(EnvironmentBundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (FieldEntry fe : bundle.fieldEntries) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(fe.columnName);
        }
        return sb.toString();
    }
    private static String createColumnsArray(EnvironmentBundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (FieldEntry fe : bundle.fieldEntries) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append('"'+fe.columnName+'"');
        }
        return sb.toString();
    }
    private static String createSelection(FindEntry findEntry) {
        StringBuilder sb = new StringBuilder();
        for (FieldEntry fe : findEntry.columns) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(fe.columnName);
               sb.append("=?");
        }
        return sb.toString();
    }
    private static String createSelectionArgs(FindEntry findEntry) {
        StringBuilder sb = new StringBuilder();
        for (FieldEntry fe : findEntry.columns) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(convertInnerType(fe.name, fe, true));
        }
        return sb.toString();
    }
    private static String createOrderBy(FindEntry findEntry) {
        StringBuilder sb = new StringBuilder();
        for (OrderByEntry fe : findEntry.orderBy) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(fe.columnName);
            sb.append((fe.desc) ? " desc" : " asc");
        }
        return sb.toString();
    }
    private static boolean checkUnique(FindEntry findEntry, List<UniqueEntry> uniqueEntries) {
        String targetStr = toSortedString(findEntry.columns);
        for (UniqueEntry uniqueEntry : uniqueEntries) {
            String uniqueStr = toSortedString(uniqueEntry.columns);
            if (targetStr.equals(uniqueStr)) {
                return true;
            }
        }
        {
        	for (FieldEntry fe : findEntry.columns) {
        		if (!fe.primaryKey) {
        			return false;
        		}
        	}
            return true;
        }
    }
    private static String toSortedString(List<FieldEntry> src) {
        ArrayList<String> tmp = new ArrayList<String>();
        for (FieldEntry fe : src) {
            tmp.add(fe.name);
        }
        Collections.sort(tmp);
        StringBuilder sb = new StringBuilder();
        for (String str : tmp) {
            sb.append(str);
            sb.append(',');
        }
        return sb.toString();
    }
    private static List<UniqueEntry> createUniqueEntries(SourcePosition sp, String[] srcs, EnvironmentBundle bundle, Messager messager) {
        List<UniqueEntry> uniqueEntries = new ArrayList<UniqueEntry>();
        for (String src : srcs) {
            UniqueEntry entry = new UniqueEntry();
            entry.columns = createFieldList(sp, src, bundle, messager);
            uniqueEntries.add(entry);
        }
        return uniqueEntries;
    }
    private static List<FindEntry> createFindEntries(SourcePosition sp, String[] srcs, EnvironmentBundle bundle, Messager messager) {
        List<FindEntry> findEntries = new ArrayList<FindEntry>();
        for (String src : srcs) {
            String[] src2 = src.split(":");
            FindEntry entry = new FindEntry();
            entry.columns = createFieldList(sp, src2[0], bundle, messager);
            entry.orderBy = (src2.length > 1) ? createOrderByList(sp, src2[1], bundle, messager) : new ArrayList<OrderByEntry>();
            findEntries.add(entry);
        }
        return findEntries;
    }
    private static List<FieldEntry> createFieldList(SourcePosition sp, String src, EnvironmentBundle bundle, Messager messager) {
        String[] names = src.split(",");
        List<FieldEntry> fieldEntries = new ArrayList<FieldEntry>();
        for (String name : names) {
            name = name.trim();
            FieldEntry fe = bundle.fieldEntryMap.get(name);
            if (fe != null) {
                fieldEntries.add(fe);
            } else {
                messager.printError(sp, "Field '" + name + "' in fields is not found.");
            }
        }
        return fieldEntries;
    }
    private static List<OrderByEntry> createOrderByList(SourcePosition sp, String src, EnvironmentBundle bundle, Messager messager) {
        String[] names = src.split(",");
        List<OrderByEntry> orderByEntries = new ArrayList<OrderByEntry>();
        for (String name : names) {
            name = name.trim();
            boolean desc = false;
            {
            	char ch = name.charAt(name.length() - 1);
	            if (ch == '+' || ch == '-') {
	            	name = name.substring(0, name.length() - 1);
	            	desc = (ch == '-');
	            }
            }
            FieldEntry fe = bundle.fieldEntryMap.get(name);
            if (fe != null) {
                orderByEntries.add(new OrderByEntry(fe, desc));
            } else {
                messager.printError(sp, "Field '" + name + "' in fields is not found.");
            }
        }
        return orderByEntries;
    }
    private static List<FieldEntry> pickFieldDeclaration(TypeDeclaration td, Messager messager, GenDbHandler genDbHandler) {
        List<FieldEntry> fes = new ArrayList<FieldEntry>();
        for (FieldDeclaration fd : td.getFields()) {
            FieldEntry fe = new FieldEntry();
            if (fd.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            {
                Attribute attribute = fd.getAnnotation(Attribute.class);
                if (attribute != null) {
                    fe.persistent = attribute.persistent();
                    fe.primaryKey = attribute.primaryKey();
                    fe.customDataType = attribute.customDataType();
                    fe.version = attribute.version();
                    try {
                    	fe.customParser = attribute.customCoder().getName();
                    } catch(MirroredTypeException mte) {
                    	fe.customParser = mte.getTypeMirror().toString();
                    }
                }
            }
            {
                class MyTypeVisitor extends SimpleTypeVisitor {
                    private String qualifiedName;
                    private boolean enumFlag = false;
                    @Override
                    public void visitDeclaredType(DeclaredType t) {
                        super.visitDeclaredType(t);
                        qualifiedName = t.getDeclaration().getQualifiedName();
                    }
                    @Override
                    public void visitEnumType(EnumType t) {
                    	super.visitEnumType(t);
                    	this.enumFlag = true;
                    }
                }
                MyTypeVisitor myTypeVisitor = new MyTypeVisitor();
                TypeMirror typeMirror = fd.getType();
                typeMirror.accept(myTypeVisitor);
                if (myTypeVisitor.qualifiedName == null) {
                    fe.fieldType = FieldType.STRING;
                    messager.printError(fd.getPosition(), "Unknown data type.");
                } else if (fe.customParser != null && !Object.class.getName().equals(fe.customParser)) {
                    fe.fieldType = FieldType.CUSTOM;
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
                    messager.printError(fd.getPosition(), "Data type is not supported. set persistent=false, or use @customCoder and @customDataType");
                }
                fe.fieldClass = myTypeVisitor.qualifiedName;
            }
            {
                if (fe.primaryKey && fe.fieldType != FieldType.LONG) {
                    messager.printError(fd.getPosition(), "Only Long is supported for primary key.");
                }
            }
            {
                fe.name = fd.getSimpleName();
                fe.columnName = convertName(genDbHandler.fieldNamingConventions(), fe.name);
            }
            if (fe.persistent) {
                fes.add(fe);
            } else {
                // ignored
            }
        }
        return fes;
    }
    private static String convertGetter(String varName, FieldEntry fieldEntry) {
        String getter = varName + ".get" + convertCap(fieldEntry.name, true) + "()";
        return convertInnerType(getter, fieldEntry, false);
    }
    private static String convertSetter(String varName, FieldEntry fieldEntry, String args) {
        String setter = varName + ".set" + convertCap(fieldEntry.name, true) + "("+convertInnerType(args, fieldEntry, false)+")";
        return setter;
    }
    private static String convertInnerType(String src, FieldEntry fieldEntry, boolean withToString) {
        switch (fieldEntry.fieldType) {
        case DATE:
            if (withToString) {
                return "(("+src+" != null) ? String.valueOf("+src+".getTime()) : null)";
            } else {
                return "(("+src+" != null) ? "+src+".getTime() : null)";
            }
        case ENUM:
            return "(("+src+" != null) ? "+src+".name() : null)";
        case CUSTOM:
            return "(("+src+" != null) ? " + fieldEntry.customParser + ".encode("+src+") : null)";
        case INTEGER:
        case SHORT:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case STRING:
        case BLOB:
        default:
            if (withToString) {
                return "String.valueOf("+src+")";
            } else {
                return src;
            }
        }
    }
    private static String convertFieldType2DbType(FieldEntry fieldEntry) {
    	if (fieldEntry.fieldType == FieldType.CUSTOM) {
    		return convertFieldType2DbType(fieldEntry.customDataType);
    	} else {
    		return convertFieldType2DbType(fieldEntry.fieldType);
    	}
    }
    private static String convertFieldType2DbType(FieldType fieldType) {
        switch (fieldType) {
        case INTEGER:
        case SHORT:
        case LONG:
            return "INTEGER";
        case FLOAT:
        case DOUBLE:
            return "REAL";
        case STRING:
            return "TEXT";
        case BLOB:
            return "BLOB";
        case DATE:
            return "LONG";
        case ENUM:
            return "TEXT";
        default:
            return "TEXT";
        }
    }
    private static String convertFieldType2Java(FieldEntry fieldEntry) {
    	switch (fieldEntry.fieldType) {
    	case ENUM:
            return fieldEntry.fieldClass;
    	case CUSTOM:
    		return convertFieldType2Java(fieldEntry.customDataType);
    	default:
    		return convertFieldType2Java(fieldEntry.fieldType);
    	}
    }
    private static String convertFieldType2Java(FieldType fieldType) {
        switch (fieldType) {
        case INTEGER:
            return "Integer";
        case SHORT:
            return "Short";
        case LONG:
            return "Long";
        case FLOAT:
            return "Float";
        case DOUBLE:
            return "Double";
        case STRING:
            return "String";
        case BLOB:
            return "byte[]";
        case DATE:
            return "java.util.Date";
        case ENUM:
        case CUSTOM:
        	throw new RuntimeException("Program Error");
        default:
            return "String";
        }
    }
    private static String createCursorGetter(String varName, FieldEntry fieldEntry, String index) {
        switch (fieldEntry.fieldType) {
        case ENUM:
            return fieldEntry.fieldClass + ".valueOf("+varName+".getString("+index+"))";
        case CUSTOM:
            return fieldEntry.customParser + ".decode("+createCursorGetter(varName, fieldEntry.customDataType, index)+")";
       	default:
       		return createCursorGetter(varName, fieldEntry.fieldType, index);
        }
    }
    private static String createCursorGetter(String varName, FieldType fieldType, String index) {
        switch (fieldType) {
        case INTEGER:
            return varName+".getInt("+index+")";
        case SHORT:
            return varName+".getShort("+index+")";
        case LONG:
            return varName+".getLong("+index+")";
        case FLOAT:
            return varName+".getFloat("+index+")";
        case DOUBLE:
            return varName+".getDouble("+index+")";
        case STRING:
            return varName+".getString("+index+")";
        case BLOB:
            return varName+".getBlob("+index+")";
        case DATE:
            return "new java.util.Date("+varName+".getLong("+index+"))";
        case ENUM:
        case CUSTOM:
        	throw new RuntimeException("Program Error");
        default:
            return "String";
        }
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
        for (int i=0;i<camel.length();i++) {
            char ch = camel.charAt(i);
            if (i>0 && Character.isUpperCase(ch)) {
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
            return name.substring(0,1).toUpperCase() + name.substring(1);
        } else {
            return name.substring(0,1).toLowerCase() + name.substring(1);
        }
    }
}