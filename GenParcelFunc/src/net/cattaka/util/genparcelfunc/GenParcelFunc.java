
package net.cattaka.util.genparcelfunc;

public @interface GenParcelFunc {
    enum NamingConventions {
        LOWER_CAMEL_CASE, // lowerCamelCase
        UPPER_CAMEL_CASE, // UpperCamelCase
        LOWER_COMPOSITE, // lower_composite
        UPPER_COMPOSITE, // UPPER_COMPOSITE
    }

    NamingConventions fieldNamingConventions() default NamingConventions.LOWER_CAMEL_CASE;
}
