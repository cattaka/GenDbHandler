
package net.cattaka.util.genasyncif;

public @interface GenAsyncInterface {
    int poolSize() default 10;

    String prefix() default "";

    String suffix() default "Async";
}
