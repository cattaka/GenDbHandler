package net.cattaka.util.gendbhandler;

public @interface Attribute {
	public enum FieldType {
		INTEGER,
		SHORT,
		LONG,
		FLOAT,
		DOUBLE,
		STRING,
		BLOB,
		DATE,
		ENUM,
		CUSTOM,
	}
	
	
	boolean persistent() default true;
	boolean primaryKey() default false;
	long version() default 1;
	Class<?> customCoder() default Object.class;
	FieldType customDataType() default FieldType.STRING;
}