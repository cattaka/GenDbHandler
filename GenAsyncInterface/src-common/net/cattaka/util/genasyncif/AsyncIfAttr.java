
package net.cattaka.util.genasyncif;

public @interface AsyncIfAttr {
    boolean forceSync() default false;

    boolean ignore() default false;
}
