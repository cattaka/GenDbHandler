
package net.cattaka.util.genasyncif.test;

import net.cattaka.util.genasyncif.GenAsyncInterface;

@GenAsyncInterface
public interface IMyCallback {

    public void put(String key, int number);

    public void put(String key, String number) throws NumberFormatException;

    public String getAsString(String key);

    public int getAsInt(String key) throws NumberFormatException;

    public int add(int a, int b);
}
