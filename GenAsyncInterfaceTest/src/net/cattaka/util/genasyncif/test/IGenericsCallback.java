
package net.cattaka.util.genasyncif.test;

import net.cattaka.util.genasyncif.GenAsyncInterface;

@GenAsyncInterface
public interface IGenericsCallback<T extends Number> {
    public T add(T a, T b);

    public void put(String key, T number);

    public T get(String key);
}
