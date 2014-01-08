
package net.cattaka.util.gendbhandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.cattaka.util.gendbhandler.Attribute.FieldType;
import net.cattaka.util.gendbhandler.GenDbHandler.NamingConventions;

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
import com.sun.mirror.type.PrimitiveType.Kind;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.SimpleTypeVisitor;
import com.sun.mirror.util.SourcePosition;

public class GenDbHandlerAnnotationProcessor implements AnnotationProcessor {

    static class EnvironmentBundle {
        GenDbHandler genDbHandler;

        Map<String, FieldEntry> fieldEntryMap;

        List<FieldEntry> fieldEntries;

        List<FindEntry> findList;

        List<UniqueEntry> uniqueList;
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

        boolean primaryKey = false;

        String name = null;

        String columnName = null;

        String constantsColumnName = null;

        InnerFieldType fieldType = InnerFieldType.STRING;

        String fieldClass;

        String nullValue;

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

    static class MyTypeVisitor extends SimpleTypeVisitor {
        private DeclaredType declaredType;

        private EnumType enumType;

        private ArrayType arrayType;

        private PrimitiveType primitiveType;

        @Override
        public void visitDeclaredType(DeclaredType t) {
            super.visitDeclaredType(t);
            declaredType = t;
        }

        @Override
        public void visitEnumType(EnumType t) {
            super.visitEnumType(t);
            enumType = t;
        }

        @Override
        public void visitArrayType(ArrayType t) {
            super.visitArrayType(t);
            arrayType = t;
        }

        @Override
        public void visitPrimitiveType(PrimitiveType t) {
            super.visitPrimitiveType(t);
            primitiveType = t;
        }

        public String getQualifiedName() {
            if (primitiveType != null) {
                return pickQualifiedName(primitiveType);
            } else if (arrayType != null) {
                return pickQualifiedName(arrayType);
            } else if (enumType != null) {
                return pickQualifiedName(enumType);
            } else if (declaredType != null) {
                return pickQualifiedName(declaredType);
            } else {
                return null;
            }
        }

    }

    public GenDbHandlerAnnotationProcessor(AnnotationProcessorEnvironment env) {
        _env = env;
    }

    public void process() {
        Messager messager = _env.getMessager();
        Collection<TypeDeclaration> tds = this.getEnvironment().getTypeDeclarations();
        for (TypeDeclaration td : tds) {
            GenDbHandler genDbHandler = td.getAnnotation(GenDbHandler.class);
            if (genDbHandler == null) {
                continue;
            }
            String packageName = td.getPackage().getQualifiedName();
            String className = td.getSimpleName();
            String cprPackageName = packageName + ".handler";
            String cprClassName = className + "Handler";

            EnvironmentBundle bundle = new EnvironmentBundle();
            {
                bundle.genDbHandler = genDbHandler;
                bundle.fieldEntries = pickFieldDeclaration(td, messager, genDbHandler);
                { // fieldEntryMap:name->fieldEntyrの作成
                    Map<String, FieldEntry> feMap = new HashMap<String, FieldEntry>();
                    for (FieldEntry fe : bundle.fieldEntries) {
                        feMap.put(fe.name, fe);
                    }
                    bundle.fieldEntryMap = feMap;
                }
                bundle.findList = createFindEntries(td.getPosition(), genDbHandler.find(), bundle,
                        messager);
                bundle.uniqueList = createUniqueEntries(td.getPosition(), genDbHandler.unique(),
                        bundle, messager);
            }

            try {
                Filer f = getEnvironment().getFiler();
                PrintWriter pw = f.createSourceFile(cprPackageName + "." + cprClassName);
                pw.println("package " + cprPackageName + ";");
                pw.println("import net.cattaka.util.gendbhandler.Accessor;");
                if (genDbHandler.genDbFunc() || genDbHandler.genContentResolverFunc()) {
                    pw.println("import android.content.ContentValues;");
                    pw.println("import android.database.Cursor;");
                }
                if (genDbHandler.genDbFunc()) {
                    pw.println("import android.database.sqlite.SQLiteDatabase;");
                }
                if (genDbHandler.genContentResolverFunc()) {
                    pw.println("import android.content.ContentResolver;");
                    pw.println("import android.net.Uri;");
                }
                if (genDbHandler.genParcelFunc()) {
                    pw.println("import android.os.Parcel;");
                    pw.println("import android.os.Parcelable;");
                }
                pw.println("import " + packageName + "." + className + ";");
                pw.println();
                pw.println("public class " + cprClassName + " {");

                if (genDbHandler.genDbFunc() || genDbHandler.genContentResolverFunc()) {
                    writeCursorFunc(messager, pw, bundle, td, genDbHandler, className);
                }
                if (genDbHandler.genDbFunc()) {
                    writeDbFunc(messager, pw, bundle, td, genDbHandler, className);
                }
                if (genDbHandler.genContentResolverFunc()) {
                    writeContentResolverFunc(messager, pw, bundle, td, genDbHandler, className);
                }
                if (genDbHandler.genParcelFunc()) {
                    writeParcelFunc(messager, pw, bundle, td, genDbHandler, className);
                }

                pw.println("}");
                pw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public void writeCursorFunc(Messager messager, PrintWriter pw, EnvironmentBundle bundle,
            TypeDeclaration td, GenDbHandler genDbHandler, String className) throws IOException {
        String tableName = convertName(genDbHandler.tableNamingConventions(), className);

        List<FindEntriesPerVersion> findEntriesPerVersions;
        {
            Map<Long, FindEntriesPerVersion> findEntriesPerVersionMap = new TreeMap<Long, FindEntriesPerVersion>();
            for (FieldEntry fe : bundle.fieldEntries) {
                FindEntriesPerVersion item = findEntriesPerVersionMap.get(fe.version);
                if (item == null) {
                    item = new FindEntriesPerVersion();
                    item.version = fe.version;
                    item.fieldEntries = new ArrayList<FieldEntry>();
                    findEntriesPerVersionMap.put(fe.version, item);
                }
                item.fieldEntries.add(fe);
            }
            findEntriesPerVersions = new ArrayList<FindEntriesPerVersion>(
                    findEntriesPerVersionMap.values());
        }
        pw.println("    public static final String SQL_CREATE_TABLE = \""
                + createCreateStatement(tableName, bundle) + "\";");
        if (findEntriesPerVersions.size() > 1) {
            FindEntriesPerVersion lastItem = findEntriesPerVersions.get(0);
            for (int i = 1; i < findEntriesPerVersions.size(); i++) {
                FindEntriesPerVersion nextItem = findEntriesPerVersions.get(i);
                List<String> sqls = createAlterTableStatements(tableName, nextItem);
                pw.println("    public static final String[] SQL_ALTER_TABLE_" + lastItem.version
                        + "_TO_" + nextItem.version + " = new String[] {");
                for (String sql : sqls) {
                    pw.println("        \"" + sql + "\",");
                }
                pw.println("    };");
                lastItem = nextItem;
            }
        }
        pw.println("    public static final String TABLE_NAME = \"" + tableName + "\";");
        pw.println("    public static final String COLUMNS = \"" + createColumns(bundle) + "\";");
        pw.println("    public static final String[] COLUMNS_ARRAY = new String[] {"
                + createColumnsArray(bundle) + "};");

        if (genDbHandler.columnIndexConstants()) {
            int index = 0;
            for (FieldEntry fe : bundle.fieldEntries) {
                pw.println("    public static final int COL_INDEX_" + fe.constantsColumnName
                        + " = " + index + ";");
                index++;
            }
        }
        if (genDbHandler.columnNameConstants()) {
            for (FieldEntry fe : bundle.fieldEntries) {
                pw.println("    public static final String COL_NAME_" + fe.constantsColumnName
                        + " = \"" + fe.columnName + "\";");
            }
        }
        {
            pw.println("    public static void readCursorByIndex(Cursor cursor, " + className
                    + " dest) {");
            {
                int index = 0;
                for (FieldEntry fe : bundle.fieldEntries) {
                    String readFromCursor = String.format(fe.fieldType.readFromCursor, "cursor",
                            String.valueOf(index), fe.nullValue, fe.fieldClass);
                    pw.println("        "
                            + String.format(fe.fieldType.setterBlock, "dest",
                                    convertCap(fe.name, true), readFromCursor) + ";");
                    index++;
                }
            }
            pw.println("    }");
        }
        {
            pw.println("    public static " + className + " readCursorByIndex(Cursor cursor) {");
            pw.println("        " + className + " result = new " + className + "();");
            pw.println("        readCursorByIndex(cursor, result);");
            pw.println("        return result;");
            pw.println("    }");
        }
        {
            pw.println("    public static void readCursorByName(Cursor cursor, " + className
                    + " dest) {");
            pw.println("        int idx;");
            {
                // int index = 0;
                for (FieldEntry fe : bundle.fieldEntries) {
                    pw.println("        idx = cursor.getColumnIndex(\"" + fe.columnName + "\");");
                    String readFromCursor = String.format(fe.fieldType.readFromCursor, "cursor",
                            "idx", fe.nullValue, fe.fieldClass);
                    pw.println("        "
                            + String.format(fe.fieldType.setterBlock, "dest",
                                    convertCap(fe.name, true), readFromCursor) + ";");

                    // index++;
                }
            }
            pw.println("    }");
        }
        {
            pw.println("    public static " + className + " readCursorByName(Cursor cursor) {");
            pw.println("        " + className + " result = new " + className + "();");
            pw.println("        readCursorByName(cursor, result);");
            pw.println("        return result;");
            pw.println("    }");
            pw.println("    public static String toStringValue(Object arg) {");
            pw.println("        return (arg != null) ? arg.toString() : null;");
            pw.println("    }");
        }
    }

    public void writeDbFunc(Messager messager, PrintWriter pw, EnvironmentBundle bundle,
            TypeDeclaration td, GenDbHandler genDbHandler, String className) throws IOException {
        FieldEntry keyFieldEntry = null;
        { // Check existence of PRIMARY KEY (Only 1 key is supported.)
            int primaryKeyCount = 0;
            for (FieldEntry fe : bundle.fieldEntries) {
                if (fe.primaryKey) {
                    primaryKeyCount++;
                    keyFieldEntry = fe;
                }
            }
            if (primaryKeyCount == 0) {
                messager.printError(td.getPosition(),
                        "At least one primary key is required. put @Attribute(primaryKey=true) to field of key");
            } else if (primaryKeyCount > 1) {
                messager.printError(td.getPosition(), "Only single primary key is supported.");
            }
        }

        {// Insert
            pw.println("    public static long insert(SQLiteDatabase db, " + className
                    + " model) {");
            pw.println("        ContentValues values = new ContentValues();");
            for (FieldEntry fe : bundle.fieldEntries) {
                if (genDbHandler.autoinclement() && fe.primaryKey) {
                    continue;
                }
                String getterBlock = String.format(fe.fieldType.getterBlock, "model",
                        convertCap(fe.name, true));
                pw.println("        "
                        + String.format(fe.fieldType.putToContentValue, "values", "\""
                                + fe.columnName + "\"", getterBlock) + ";");
            }
            pw.println("        long key = db.insert(TABLE_NAME, null, values);");
            String cast = (!"long".equals(keyFieldEntry.fieldClass)) ? ("("
                    + keyFieldEntry.fieldClass + ")") : "";
            if (genDbHandler.autoinclement()) {
                pw.println("        "
                        + String.format(keyFieldEntry.fieldType.setterBlock, "model",
                                convertCap(keyFieldEntry.name, true), cast + "key") + ";");
            }
            pw.println("        return key;");
            pw.println("    }");
        }
        {// Update
            pw.println("    public static int update(SQLiteDatabase db, " + className + " model) {");
            pw.println("        ContentValues values = new ContentValues();");
            pw.println("        String whereClause = \"" + keyFieldEntry.columnName + "=?\";");
            pw.println("        String[] whereArgs = new String[]{String.valueOf("
                    + String.format(keyFieldEntry.fieldType.getterBlock, "model",
                            convertCap(keyFieldEntry.name, true)) + ")};");
            for (FieldEntry fe : bundle.fieldEntries) {
                if (!fe.primaryKey) {
                    String getterBlock = String.format(fe.fieldType.getterBlock, "model",
                            convertCap(fe.name, true));
                    pw.println("        "
                            + String.format(fe.fieldType.putToContentValue, "values", "\""
                                    + fe.columnName + "\"", getterBlock) + ";");
                }
            }
            pw.println("        return db.update(TABLE_NAME, values, whereClause, whereArgs);");
            pw.println("    }");
        }
        {// Delete
            pw.println("    public static int delete(SQLiteDatabase db, "
                    + keyFieldEntry.fieldClass + " key) {");
            pw.println("        String whereClause = \"" + keyFieldEntry.columnName + "=?\";");
            pw.println("        String[] whereArgs = new String[]{String.valueOf(key)};");
            pw.println("        return db.delete(TABLE_NAME, whereClause, whereArgs);");
            pw.println("    }");
        }
        {// Find
            for (FindEntry findEntry : bundle.findList) {
                if (checkUnique(findEntry, bundle.uniqueList)) {
                    pw.println("    public static " + className + " "
                            + createMethodName(findEntry, false) + "(SQLiteDatabase db"
                            + createMethodArg(findEntry, true) + ") {");
                    pw.println("        Cursor cursor = " + createMethodName(findEntry, true)
                            + "(db" + createMethodArg(findEntry, false) + ");");
                    pw.println("        " + className
                            + " model = (cursor.moveToNext()) ? readCursorByIndex(cursor) : null;");
                    pw.println("        cursor.close();");
                    pw.println("        return model;");
                    pw.println("    }");
                } else {
                    pw.println("    public static java.util.List<" + className + "> "
                            + createMethodName(findEntry, false) + "(SQLiteDatabase db, int limit"
                            + createMethodArg(findEntry, true) + ") {");
                    pw.println("        Cursor cursor = " + createMethodName(findEntry, true)
                            + "(db, limit" + createMethodArg(findEntry, false) + ");");
                    pw.println("        java.util.List<" + className
                            + "> result = new java.util.ArrayList<" + className + ">();");
                    pw.println("        while (cursor.moveToNext()) {");
                    pw.println("            result.add(readCursorByIndex(cursor));");
                    pw.println("        }");
                    pw.println("        cursor.close();");
                    pw.println("        return result;");
                    pw.println("    }");
                }
            }
            for (FindEntry findEntry : bundle.findList) {
                if (checkUnique(findEntry, bundle.uniqueList)) {
                    pw.println("    public static Cursor " + createMethodName(findEntry, true)
                            + "(SQLiteDatabase db" + createMethodArg(findEntry, true) + ") {");
                    pw.println("        String selection = \"" + createSelection(findEntry) + "\";");
                    pw.println("        String[] selectionArgs = new String[]{"
                            + createSelectionArgs(findEntry) + "};");
                    pw.println("        return db.query(TABLE_NAME, COLUMNS_ARRAY, selection, selectionArgs, null, null, null);");
                    pw.println("    }");
                } else {
                    String orderBy = createOrderBy(findEntry);
                    pw.println("    public static Cursor " + createMethodName(findEntry, true)
                            + "(SQLiteDatabase db, int limit" + createMethodArg(findEntry, true)
                            + ") {");
                    pw.println("        String selection = \"" + createSelection(findEntry) + "\";");
                    pw.println("        String[] selectionArgs = new String[]{"
                            + createSelectionArgs(findEntry) + "};");
                    pw.println("        String limitStr = (limit > 0) ? String.valueOf(limit) : null;");
                    if (orderBy.length() > 0) {
                        pw.println("        String orderBy = \"" + createOrderBy(findEntry) + "\";");
                        pw.println("        return db.query(TABLE_NAME, COLUMNS_ARRAY, selection, selectionArgs, null, null, orderBy, limitStr);");
                    } else {
                        pw.println("        return db.query(TABLE_NAME, COLUMNS_ARRAY, selection, selectionArgs, null, null, null, limitStr);");
                    }
                    pw.println("    }");
                }
            }
        }
    }

    public void writeContentResolverFunc(Messager messager, PrintWriter pw,
            EnvironmentBundle bundle, TypeDeclaration td, GenDbHandler genDbHandler,
            String className) throws IOException {
        FieldEntry keyFieldEntry = null;
        { // Check existence of PRIMARY KEY (Only 1 key is supported.)
            int primaryKeyCount = 0;
            for (FieldEntry fe : bundle.fieldEntries) {
                if (fe.primaryKey) {
                    primaryKeyCount++;
                    keyFieldEntry = fe;
                }
            }
            if (primaryKeyCount == 0) {
                messager.printError(td.getPosition(),
                        "At least one primary key is required. put @Attribute(primaryKey=true) to field of key");
            } else if (primaryKeyCount > 1) {
                messager.printError(td.getPosition(), "Only single primary key is supported.");
            }
        }

        {// Insert
            pw.println("    public static Uri insert(ContentResolver resolver, Uri uri, "
                    + className + " model) {");
            pw.println("        ContentValues values = new ContentValues();");
            for (FieldEntry fe : bundle.fieldEntries) {
                if (genDbHandler.autoinclement() && fe.primaryKey) {
                    continue;
                }
                String getterBlock = String.format(fe.fieldType.getterBlock, "model",
                        convertCap(fe.name, true));
                pw.println("        "
                        + String.format(fe.fieldType.putToContentValue, "values", "\""
                                + fe.columnName + "\"", getterBlock) + ";");
            }
            pw.println("        return resolver.insert(uri, values);");
            pw.println("    }");
        }
        {// Update
            pw.println("    public static int update(ContentResolver resolver, Uri uri, "
                    + className + " model) {");
            pw.println("        ContentValues values = new ContentValues();");
            pw.println("        String whereClause = \"" + keyFieldEntry.columnName + "=?\";");
            pw.println("        String[] whereArgs = new String[]{String.valueOf("
                    + String.format(keyFieldEntry.fieldType.getterBlock, "model",
                            convertCap(keyFieldEntry.name, true)) + ")};");
            for (FieldEntry fe : bundle.fieldEntries) {
                if (!fe.primaryKey) {
                    String getterBlock = String.format(fe.fieldType.getterBlock, "model",
                            convertCap(fe.name, true));
                    pw.println("        "
                            + String.format(fe.fieldType.putToContentValue, "values", "\""
                                    + fe.columnName + "\"", getterBlock) + ";");
                }
            }
            pw.println("        return resolver.update(uri, values, whereClause, whereArgs);");
            pw.println("    }");
        }
        {// Delete
            pw.println("    public static int delete(ContentResolver resolver, Uri uri, "
                    + keyFieldEntry.fieldClass + " key) {");
            pw.println("        String whereClause = \"" + keyFieldEntry.columnName + "=?\";");
            pw.println("        String[] whereArgs = new String[]{String.valueOf(key)};");
            pw.println("        return resolver.delete(uri, whereClause, whereArgs);");
            pw.println("    }");
        }
        {// Find
            for (FindEntry findEntry : bundle.findList) {
                if (checkUnique(findEntry, bundle.uniqueList)) {
                    pw.println("    public static " + className + " "
                            + createMethodName(findEntry, false)
                            + "(ContentResolver resolver, Uri uri"
                            + createMethodArg(findEntry, true) + ") {");
                    pw.println("        Cursor cursor = " + createMethodName(findEntry, true)
                            + "(resolver, uri" + createMethodArg(findEntry, false) + ");");
                    pw.println("        " + className
                            + " model = (cursor.moveToNext()) ? readCursorByIndex(cursor) : null;");
                    pw.println("        cursor.close();");
                    pw.println("        return model;");
                    pw.println("    }");
                } else {
                    pw.println("    public static java.util.List<" + className + "> "
                            + createMethodName(findEntry, false)
                            + "(ContentResolver resolver, Uri uri"
                            + createMethodArg(findEntry, true) + ") {");
                    pw.println("        Cursor cursor = " + createMethodName(findEntry, true)
                            + "(resolver, uri" + createMethodArg(findEntry, false) + ");");
                    pw.println("        java.util.List<" + className
                            + "> result = new java.util.ArrayList<" + className + ">();");
                    pw.println("        while (cursor.moveToNext()) {");
                    pw.println("            result.add(readCursorByIndex(cursor));");
                    pw.println("        }");
                    pw.println("        cursor.close();");
                    pw.println("        return result;");
                    pw.println("    }");
                }
            }
            for (FindEntry findEntry : bundle.findList) {
                if (checkUnique(findEntry, bundle.uniqueList)) {
                    pw.println("    public static Cursor " + createMethodName(findEntry, true)
                            + "(ContentResolver resolver, Uri uri"
                            + createMethodArg(findEntry, true) + ") {");
                    pw.println("        String selection = \"" + createSelection(findEntry) + "\";");
                    pw.println("        String[] selectionArgs = new String[]{"
                            + createSelectionArgs(findEntry) + "};");
                    pw.println("        return resolver.query(uri, COLUMNS_ARRAY, selection, selectionArgs, null);");
                    pw.println("    }");
                } else {
                    String orderBy = createOrderBy(findEntry);
                    pw.println("    public static Cursor " + createMethodName(findEntry, true)
                            + "(ContentResolver resolver, Uri uri"
                            + createMethodArg(findEntry, true) + ") {");
                    pw.println("        String selection = \"" + createSelection(findEntry) + "\";");
                    pw.println("        String[] selectionArgs = new String[]{"
                            + createSelectionArgs(findEntry) + "};");
                    if (orderBy.length() > 0) {
                        pw.println("        String orderBy = \"" + createOrderBy(findEntry) + "\";");
                        pw.println("        return resolver.query(uri, COLUMNS_ARRAY, selection, selectionArgs, orderBy);");
                    } else {
                        pw.println("        return resolver.query(uri, COLUMNS_ARRAY, selection, selectionArgs, null);");
                    }
                    pw.println("    }");
                }
            }
        }
    }

    public void writeParcelFunc(Messager messager, PrintWriter pw, EnvironmentBundle bundle,
            TypeDeclaration td, GenDbHandler genDbHandler, String className) {
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
        pw.println("            return new " + className + "[size];");
        pw.println("        }");
        pw.println("    };");

        pw.println("    public static void readFromParcel(" + className + " dest, Parcel in) {");
        for (FieldEntry fe : bundle.fieldEntries) {
            String readFromParcel = String.format(Locale.ROOT, fe.fieldType.readFromParcel, "in",
                    fe.fieldClass);
            pw.print("        ");
            pw.println("        "
                    + String.format(fe.fieldType.setterBlock, "dest", convertCap(fe.name, true),
                            readFromParcel) + ";");

        }
        pw.println("    }");

        pw.println("    public static void writeToParcel(" + className
                + " src, Parcel out, int flags) {");
        for (FieldEntry fe : bundle.fieldEntries) {
            String getterBlock = String.format(fe.fieldType.getterBlock, "src",
                    convertCap(fe.name, true));
            pw.println("        " + String.format(fe.fieldType.writeToParcel, "out", getterBlock)
                    + ";");
        }
        pw.println("    }");
    }

    public AnnotationProcessorEnvironment getEnvironment() {
        return _env;
    }

    AnnotationProcessorEnvironment _env;

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
            sb.append(fe.fieldType.dbType);
            if (fe.primaryKey) {
                sb.append(" PRIMARY KEY");
                if (bundle.genDbHandler.autoinclement()) {
                    sb.append(" AUTOINCREMENT");
                }
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

    public List<String> createAlterTableStatements(String tableName,
            FindEntriesPerVersion findEntriesPerVersion) {
        List<String> result = new ArrayList<String>();
        for (FieldEntry fe : findEntriesPerVersion.fieldEntries) {
            StringBuilder sb = new StringBuilder();
            sb.append("ALTER TABLE ");
            sb.append(tableName);
            sb.append(" ADD COLUMN ");
            sb.append(fe.columnName);
            sb.append(" ");
            sb.append(fe.fieldType.dbType);
            result.add(sb.toString());
        }
        return result;
    }

    private static String createMethodName(FindEntry findEntry, boolean cursor) {
        StringBuilder sb = new StringBuilder();
        if (cursor) {
            sb.append("findCursor");
        } else {
            sb.append("find");
        }
        {
            boolean firstFlag = true;
            for (FieldEntry fe : findEntry.columns) {
                if (firstFlag) {
                    sb.append("By");
                    firstFlag = false;
                } else {
                    sb.append("And");
                }
                sb.append(convertCap(fe.name, true));
            }
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

    private static String createMethodArg(FindEntry findEntry, boolean withType) {
        StringBuilder sb = new StringBuilder();
        for (FieldEntry fe : findEntry.columns) {
            sb.append(", ");
            if (withType) {
                sb.append(fe.fieldClass);
                sb.append(" ");
            }
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
            sb.append('"' + fe.columnName + '"');
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
            sb.append(String.format(fe.fieldType.toStringBlock, fe.name));
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
        if (findEntry.columns.size() == 0) {
            return false;
        }
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

    private static List<UniqueEntry> createUniqueEntries(SourcePosition sp, String[] srcs,
            EnvironmentBundle bundle, Messager messager) {
        List<UniqueEntry> uniqueEntries = new ArrayList<UniqueEntry>();
        for (String src : srcs) {
            UniqueEntry entry = new UniqueEntry();
            entry.columns = createFieldList(sp, src, bundle, messager);
            uniqueEntries.add(entry);
        }
        return uniqueEntries;
    }

    private static List<FindEntry> createFindEntries(SourcePosition sp, String[] srcs,
            EnvironmentBundle bundle, Messager messager) {
        List<FindEntry> findEntries = new ArrayList<FindEntry>();
        for (String src : srcs) {
            String[] src2 = src.split(":");
            FindEntry entry = new FindEntry();
            entry.columns = createFieldList(sp, src2[0], bundle, messager);
            entry.orderBy = (src2.length > 1) ? createOrderByList(sp, src2[1], bundle, messager)
                    : new ArrayList<OrderByEntry>();
            findEntries.add(entry);
        }
        return findEntries;
    }

    private static List<FieldEntry> createFieldList(SourcePosition sp, String src,
            EnvironmentBundle bundle, Messager messager) {
        List<FieldEntry> fieldEntries = new ArrayList<FieldEntry>();
        if (src != null && src.length() > 0) {
            String[] names = src.split(",");
            for (String name : names) {
                name = name.trim();
                FieldEntry fe = bundle.fieldEntryMap.get(name);
                if (fe != null) {
                    fieldEntries.add(fe);
                } else {
                    messager.printError(sp, "Field '" + name + "' in fields is not found.");
                }
            }
        }
        return fieldEntries;
    }

    private static List<OrderByEntry> createOrderByList(SourcePosition sp, String src,
            EnvironmentBundle bundle, Messager messager) {
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

    private static List<FieldEntry> pickFieldDeclaration(TypeDeclaration td, Messager messager,
            GenDbHandler genDbHandler) {
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
                Attribute attribute = fd.getAnnotation(Attribute.class);
                if (attribute != null) {
                    fe.persistent = attribute.persistent();
                    fe.primaryKey = attribute.primaryKey();
                    fe.customDataType = attribute.customDataType();
                    fe.version = attribute.version();
                    fe.nullValue = attribute.nullValue();
                    try {
                        fe.customParser = attribute.customCoder().getName();
                    } catch (MirroredTypeException mte) {
                        fe.customParser = mte.getTypeMirror().toString();
                    }
                }
            }
            {
                MyTypeVisitor myTypeVisitor = new MyTypeVisitor();
                TypeMirror typeMirror = fd.getType();
                typeMirror.accept(myTypeVisitor);
                fe.fieldClass = myTypeVisitor.getQualifiedName();
                if (fe.persistent) {
                    findFieldEntry(myTypeVisitor, fe);
                    if (fe.fieldType == null) {
                        messager.printError(fd.getPosition(),
                                "Data type is not supported. set persistent=false, or use @customCoder and @customDataType");
                    }
                }
            }
            {
                if (fe.primaryKey && genDbHandler.autoinclement() && !fe.fieldType.isInteger) {
                    messager.printError(fd.getPosition(),
                            "use autoinclement=false, or use number type.");
                }
                if (fe.fieldType.isPrimitive
                        && (fe.nullValue == null || fe.nullValue.length() == 0)) {
                    fe.nullValue = fe.fieldType.defaultNullValue;
                } else if (!fe.fieldType.isPrimitive && fe.nullValue != null
                        && fe.nullValue.length() > 0) {
                    messager.printError(fd.getPosition(), "nullValue is only for primitive type.");
                }
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

    private static void findFieldEntry(MyTypeVisitor myTypeVisitor, FieldEntry fe) {
        if (fe.customParser != null && !Object.class.getCanonicalName().equals(fe.customParser)) {
            if (fe.customDataType == FieldType.BLOB) {
                fe.fieldType = InnerFieldType.BLOB;
            } else if (fe.customDataType == FieldType.BOOLEAN) {
                fe.fieldType = InnerFieldType.BOOLEAN;
            } else if (fe.customDataType == FieldType.BYTE) {
                fe.fieldType = InnerFieldType.BYTE;
            } else if (fe.customDataType == FieldType.CHAR) {
                fe.fieldType = InnerFieldType.BLOB;
            } else if (fe.customDataType == FieldType.DOUBLE) {
                fe.fieldType = InnerFieldType.DOUBLE;
            } else if (fe.customDataType == FieldType.FLOAT) {
                fe.fieldType = InnerFieldType.FLOAT;
            } else if (fe.customDataType == FieldType.INTEGER) {
                fe.fieldType = InnerFieldType.INTEGER;
            } else if (fe.customDataType == FieldType.LONG) {
                fe.fieldType = InnerFieldType.LONG;
            } else if (fe.customDataType == FieldType.P_BOOLEAN) {
                fe.fieldType = InnerFieldType.P_BOOLEAN;
            } else if (fe.customDataType == FieldType.P_BYTE) {
                fe.fieldType = InnerFieldType.P_BYTE;
            } else if (fe.customDataType == FieldType.P_CHAR) {
                fe.fieldType = InnerFieldType.P_CHAR;
            } else if (fe.customDataType == FieldType.P_DOUBLE) {
                fe.fieldType = InnerFieldType.P_DOUBLE;
            } else if (fe.customDataType == FieldType.P_FLOAT) {
                fe.fieldType = InnerFieldType.P_FLOAT;
            } else if (fe.customDataType == FieldType.P_INT) {
                fe.fieldType = InnerFieldType.P_INT;
            } else if (fe.customDataType == FieldType.P_LONG) {
                fe.fieldType = InnerFieldType.P_LONG;
            } else if (fe.customDataType == FieldType.P_SHORT) {
                fe.fieldType = InnerFieldType.P_SHORT;
            } else if (fe.customDataType == FieldType.PARCELABLE) {
                fe.fieldType = InnerFieldType.PARCELABLE;
            } else if (fe.customDataType == FieldType.SERIALIZABLE) {
                fe.fieldType = InnerFieldType.SERIALIZABLE;
            } else if (fe.customDataType == FieldType.SHORT) {
                fe.fieldType = InnerFieldType.SHORT;
            } else if (fe.customDataType == FieldType.STRING) {
                fe.fieldType = InnerFieldType.STRING;
            }
            fe.fieldType = new InnerFieldType(fe.fieldType.isInteger, //
                    false, //
                    fe.fieldType.dbType, //
                    fe.fieldType.defaultNullValue, //
                    fe.fieldType.readFromParcel, //
                    fe.fieldType.writeToParcel, //
                    fe.fieldType.readFromCursor, //
                    fe.fieldType.putToContentValue, //
                    String.format(fe.fieldType.toStringBlock, fe.customParser + ".encode(%1$s)"), //
                    fe.customParser + ".encode(%1$s.get%2$s())", "%1$s.set%2$s(" + fe.customParser
                            + ".decode(%3$s))" //
            );
        } else if (myTypeVisitor.arrayType != null) {
            if (myTypeVisitor.arrayType.getComponentType() instanceof PrimitiveType) {
                PrimitiveType pt = (PrimitiveType)myTypeVisitor.arrayType.getComponentType();
                if (pt.getKind() == Kind.BYTE) {
                    fe.fieldType = InnerFieldType.BLOB;
                } else {
                    // TODO
                }
            }
        } else if (myTypeVisitor.primitiveType != null) {

            if (myTypeVisitor.primitiveType.getKind() == Kind.BOOLEAN) {
                fe.fieldType = InnerFieldType.P_BOOLEAN;
            } else if (myTypeVisitor.primitiveType.getKind() == Kind.BYTE) {
                fe.fieldType = InnerFieldType.P_BYTE;
            } else if (myTypeVisitor.primitiveType.getKind() == Kind.CHAR) {
                fe.fieldType = InnerFieldType.P_CHAR;
            } else if (myTypeVisitor.primitiveType.getKind() == Kind.INT) {
                fe.fieldType = InnerFieldType.P_INT;
            } else if (myTypeVisitor.primitiveType.getKind() == Kind.SHORT) {
                fe.fieldType = InnerFieldType.P_SHORT;
            } else if (myTypeVisitor.primitiveType.getKind() == Kind.LONG) {
                fe.fieldType = InnerFieldType.P_LONG;
            } else if (myTypeVisitor.primitiveType.getKind() == Kind.FLOAT) {
                fe.fieldType = InnerFieldType.P_FLOAT;
            } else if (myTypeVisitor.primitiveType.getKind() == Kind.DOUBLE) {
                fe.fieldType = InnerFieldType.P_DOUBLE;
            }
        } else if (myTypeVisitor.enumType != null) {
            fe.fieldType = InnerFieldType.ENUM_NAME;
        } else if (myTypeVisitor.declaredType != null) {
            String qualifiedName = myTypeVisitor.declaredType.getDeclaration().getQualifiedName();
            if (Boolean.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.BOOLEAN;
            } else if (Byte.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.BYTE;
            } else if (Character.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.CHAR;
            } else if (Integer.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.INTEGER;
            } else if (Short.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.SHORT;
            } else if (Long.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.LONG;
            } else if (Float.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.FLOAT;
            } else if (Double.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.DOUBLE;
            } else if (String.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.STRING;
            } else if (byte[].class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.BLOB;
            } else if (Date.class.getName().equals(qualifiedName)) {
                fe.fieldType = InnerFieldType.DATE;
            } else {
                for (TypeMirror tm : myTypeVisitor.declaredType.getSuperinterfaces()) {
                    InnerFieldType origFt = null;
                    if (Serializable.class.getCanonicalName().equals(pickQualifiedName(tm))) {
                        origFt = InnerFieldType.SERIALIZABLE;
                    } else if ("android.os.Parcelable".equals(pickQualifiedName(tm))) {
                        origFt = InnerFieldType.PARCELABLE;
                    }
                    if (origFt != null) {
                        fe.fieldType = new InnerFieldType(origFt.isInteger, false, //
                                origFt.dbType, //
                                origFt.defaultNullValue, //
                                "((" + qualifiedName + ")" + origFt.readFromParcel + ")", //
                                origFt.writeToParcel, //
                                "((" + qualifiedName + ")" + origFt.readFromCursor + ")", //
                                origFt.putToContentValue, //
                                origFt.toStringBlock, //
                                origFt.getterBlock, origFt.setterBlock);
                        break;
                    }
                }
            }
        }
    }

    private static String pickQualifiedName(TypeMirror src) {
        if (src instanceof PrimitiveType) {
            return ((PrimitiveType)src).getKind().name().toLowerCase();
        } else if (src instanceof EnumType) {
            return ((EnumType)src).getDeclaration().getQualifiedName();
        } else if (src instanceof ArrayType) {
            return pickQualifiedName(((ArrayType)src).getComponentType()) + "[]";
        } else if (src instanceof DeclaredType) {
            return ((DeclaredType)src).getDeclaration().getQualifiedName();
        }
        return null;
    }
}
