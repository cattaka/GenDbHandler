
package net.cattaka.util.genparcelfunc;

import java.util.Date;

public @interface ParcelFuncAttr {
    public enum FieldType {
        BOOLEAN(Boolean.class.getName(),
                "(in.readByte() != 0)",
                "%1$s.writeByte(%2$s ? (byte)1 : (byte)0)", //
                "%1$s.set%2$s((in.readByte() != 0) ? (in.readByte() != 0) : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeByte(%1$s.get%2$s() ? (byte)1 : (byte)0); } else { out.writeByte((byte)0); }"), //
        BYTE(Integer.class.getName(), "in.readByte()",
                "%1$s.writeByte(%2$s)", //
                "%1$s.set%2$s((in.readByte() != 0) ? in.readByte() : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeByte(%1$s.get%2$s()); } else { out.writeByte((byte)0); }"), //
        CHAR(Integer.class.getName(), "((char)in.readInt())",
                "%1$s.writeInt(%2$s)", //
                "%1$s.set%2$s((in.readByte() != 0) ? ((char)in.readInt()) : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeInt(%1$s.get%2$s()); } else { out.writeByte((byte)0); }"), //
        INTEGER(Integer.class.getName(), "in.readInt()",
                "%1$s.writeInt(%2$s)", //
                "%1$s.set%2$s((in.readByte() != 0) ? in.readInt() : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeInt(%1$s.get%2$s()); } else { out.writeByte((byte)0); }"), //
        SHORT(Short.class.getName(), "(short)in.readInt()",
                "%1$s.writeInt(%2$s)", //
                "%1$s.set%2$s((in.readByte() != 0) ? (short)in.readInt() : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeInt(%1$s.get%2$s()); } else { out.writeByte((byte)0); }"), //
        LONG(Long.class.getName(), "in.readLong()",
                "%1$s.writeLong(%2$s)",//
                "%1$s.set%2$s((in.readByte() != 0) ? in.readLong() : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeLong(%1$s.get%2$s()); } else { out.writeByte((byte)0); }"), //
        FLOAT(Float.class.getName(), "in.readFloat()",
                "%1$s.writeFloat(%2$s)", //
                "%1$s.set%2$s((in.readByte() != 0) ? in.readFloat() : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeFloat(%1$s.get%2$s()); } else { out.writeByte((byte)0); }"), //
        DOUBLE(Double.class.getName(), "in.readDouble()",
                "%1$s.writeDouble(%2$s)", //
                "%1$s.set%2$s((in.readByte() != 0) ? in.readDouble() : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeDouble(%1$s.get%2$s()); } else { out.writeByte((byte)0); }"), //
        STRING(String.class.getName(), "in.readString()", "%1$s.writeString(%2$s)", //
                "%1$s.set%2$s(in.readString());", //
                "out.writeString(%1$s.get%2$s());"), //
        BLOB(
                String.class.getName(),
                "readByteArray(in)",
                "writeByteArray(%1$s, %2$s)", //
                "{int r=in.readInt(); if(r>=0){byte[] blob = new byte[r]; in.readByteArray(blob); %1$s.set%2$s(blob); } else { %1$s.set%2$s(null); }}", //
                "{if(%1$s.get%2$s()!=null){out.writeInt(%1$s.get%2$s().length);out.writeByteArray(%1$s.get%2$s());} else { out.writeInt(-1); }}"), //
        DATE(Date.class.getName(), "(notSupported)",
                "(notSupported(%1$s,%2$s))", //
                "%1$s.set%2$s((in.readByte() != 0) ? new java.util.Date(in.readLong()) : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeLong(%1$s.get%2$s().getTime()); } else { out.writeByte((byte)0); }"), //
        ENUM(Enum.class.getName(), "(notSupported)",
                "(notSupported(%1$s,%2$s))", //
                "%1$s.set%2$s((in.readByte() != 0) ? %3$s.valueOf(in.readString()) : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); out.writeString(%1$s.get%2$s().name()); } else { out.writeByte((byte)0); }"), //
        CUSTOM("", "(notSupported)", "(notSupported(%1$s,%2$s))", //
                "%1$s.set%2$s((in.readByte() != 0) ? %3$s.decode(%4$s) : null);", //
                "if (%1$s.get%2$s() != null) { out.writeByte((byte)1); %4$s; } else { out.writeByte((byte)0); }"), //
        ;

        public final String javaClassName;

        public final String parcelReadFragment;

        public final String parcelWriteFragment;

        public final String readFormat;

        public final String writeFormat;

        private FieldType(String javaClassName, String parcelReadFragment,
                String parcelWriteFragment, String readFormat, String writeFormat) {
            this.javaClassName = javaClassName;
            this.parcelReadFragment = parcelReadFragment;
            this.parcelWriteFragment = parcelWriteFragment;
            this.readFormat = readFormat;
            this.writeFormat = writeFormat;
        }

    }

    boolean persistent() default true;

    Class<?> customCoder() default Object.class;

    FieldType customDataType() default FieldType.STRING;
}
