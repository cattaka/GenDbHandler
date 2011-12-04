package net.cattaka.util.gendbhandler;

public @interface Attribute {
	enum FieldType {
		TYPE_INTEGER,
		TYPE_SHORT,
		TYPE_LONG,
		TYPE_FLOAT,
		TYPE_DOUBLE,
		TYPE_STRING,
		TYPE_BLOB,
		TYPE_DATE,
	}
	
	boolean persistent() default true;
	boolean primaryKey() default false;
	long version() default 1;
}
