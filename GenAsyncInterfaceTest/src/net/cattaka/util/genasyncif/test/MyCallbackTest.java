
package net.cattaka.util.genasyncif.test;

import java.util.HashMap;
import java.util.Map;

import net.cattaka.util.genasyncif.test.async.IMyCallbackAsync;
import android.os.Looper;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;

public class MyCallbackTest extends InstrumentationTestCase {
    /**
     * A implementation of IMyCallback. Every methods are only allowed calling
     * from main thread.
     */
    public static class MyCallbackImpl implements IMyCallback {
        private Map<String, String> values = new HashMap<String, String>();

        @Override
        public void put(String key, int number) {
            assertSame(Looper.getMainLooper().getThread(), Thread.currentThread());
            values.put(key, String.valueOf(number));
        }

        @Override
        public void put(String key, String number) throws NumberFormatException {
            assertSame(Looper.getMainLooper().getThread(), Thread.currentThread());
            values.put(key, number);
        }

        @Override
        public String getAsString(String key) {
            assertSame(Looper.getMainLooper().getThread(), Thread.currentThread());
            return values.get(key);
        }

        @Override
        public int getAsInt(String key) throws NumberFormatException {
            assertSame(Looper.getMainLooper().getThread(), Thread.currentThread());
            return Integer.valueOf(values.get(key));
        }

        @Override
        public int add(int a, int b) {
            assertSame(Looper.getMainLooper().getThread(), Thread.currentThread());
            return a + b;
        }
    }

    /**
     * Testing put/get methods.
     */
    public void testPutGetInt() {
        // At first, this is not running on main thread.
        assertNotSame(Looper.getMainLooper().getThread(), Thread.currentThread());

        // Create normal callback and asyncCallback.
        MyCallbackImpl callback = new MyCallbackImpl();
        IMyCallback asyncCallback = new IMyCallbackAsync(callback);

        // Call standard methods. It runs successfully.
        asyncCallback.put("CAT", 3);
        assertEquals("3", asyncCallback.getAsString("CAT"));
        assertEquals(3, asyncCallback.getAsInt("CAT"));

        // Call methods that has throws some exception.
        asyncCallback.put("SEA", "Not number");
        assertEquals("Not number", asyncCallback.getAsString("SEA"));
        try {
            asyncCallback.getAsInt("SEA");
            fail();
        } catch (NumberFormatException e) {
            // OK
        }
    }

    /**
     * simple Stress test 1
     */
    public void testSomeAdd() throws Throwable {
        // Debug.startMethodTracing("testSomeAdd");
        final int count = 10000;
        final MyCallbackImpl callback = new MyCallbackImpl();
        IMyCallback asyncCallback = new IMyCallbackAsync(callback);
        {
            long start = SystemClock.elapsedRealtime();
            for (int i = 0; i < count; i++) {
                asyncCallback.add(i, i);
            }
            long end = SystemClock.elapsedRealtime();
            long time = end - start;
            Log.d("Debug", "asyncCallback#add:" + time + "ms");
        }
        {
            runTestOnUiThread(new Runnable() {

                @Override
                public void run() {
                    long start = SystemClock.elapsedRealtime();
                    for (int i = 0; i < count; i++) {
                        callback.add(i, i);
                    }
                    long end = SystemClock.elapsedRealtime();
                    long time = end - start;
                    Log.d("Debug", "callback#add:" + time + "ms");
                }
            });
        }
        // Debug.stopMethodTracing();
    }

    /**
     * simple Stress test 2
     */
    public void testSomePut() throws Throwable {
        // Debug.startMethodTracing("testSomePut");
        final int count = 10000;
        final MyCallbackImpl callback = new MyCallbackImpl();
        IMyCallback asyncCallback = new IMyCallbackAsync(callback);
        {
            long start = SystemClock.elapsedRealtime();
            for (int i = 0; i < count; i++) {
                asyncCallback.put("key", i);
            }
            long end = SystemClock.elapsedRealtime();
            long time = end - start;
            Log.d("Debug", "asyncCallback#put:" + time + "ms");
        }
        {
            runTestOnUiThread(new Runnable() {

                @Override
                public void run() {
                    long start = SystemClock.elapsedRealtime();
                    for (int i = 0; i < count; i++) {
                        callback.put("key", i);
                    }
                    long end = SystemClock.elapsedRealtime();
                    long time = end - start;
                    Log.d("Debug", "callback#put:" + time + "ms");
                }
            });
        }
        // Debug.stopMethodTracing();
    }
}
