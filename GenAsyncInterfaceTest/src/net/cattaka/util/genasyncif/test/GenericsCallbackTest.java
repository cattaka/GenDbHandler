
package net.cattaka.util.genasyncif.test;

import java.util.HashMap;
import java.util.Map;

import net.cattaka.util.genasyncif.test.async.IGenericsCallbackAsync;
import android.os.Looper;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;

public class GenericsCallbackTest extends InstrumentationTestCase {
    /**
     * A implementation of IGenericsCallback. Every methods are only allowed
     * calling from main thread.
     */
    public static class GenericsCallbackImpl implements IGenericsCallback<Double> {
        private Map<String, Double> values = new HashMap<String, Double>();

        @Override
        public Double add(Double a, Double b) {
            return a + b;
        }

        @Override
        public void put(String key, Double number) {
            values.put(key, number);
        }

        @Override
        public Double get(String key) {
            return values.get(key);
        }
    }

    /**
     * Testing put/get methods.
     */
    public void testPutGetInt() {
        // At first, this is not running on main thread.
        assertNotSame(Looper.getMainLooper().getThread(), Thread.currentThread());

        // Create normal callback and asyncCallback.
        GenericsCallbackImpl callback = new GenericsCallbackImpl();
        IGenericsCallback<Double> asyncCallback = new IGenericsCallbackAsync<Double>(callback);

        // Call standard methods. It runs successfully.
        asyncCallback.put("CAT", 3.0);
        assertEquals(3.0, asyncCallback.get("CAT"));
        assertNull(asyncCallback.get("DOG"));
    }

    /**
     * simple Stress test 1
     */
    public void testSomeAdd() throws Throwable {
        // Debug.startMethodTracing("testSomeAdd");
        final int count = 10000;
        final GenericsCallbackImpl callback = new GenericsCallbackImpl();
        IGenericsCallback<Double> asyncCallback = new IGenericsCallbackAsync<Double>(callback);
        {
            long start = SystemClock.elapsedRealtime();
            for (int i = 0; i < count; i++) {
                asyncCallback.add((double)i, (double)i);
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
                        callback.add((double)i, (double)i);
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
        final GenericsCallbackImpl callback = new GenericsCallbackImpl();
        IGenericsCallback<Double> asyncCallback = new IGenericsCallbackAsync<Double>(callback);
        {
            long start = SystemClock.elapsedRealtime();
            for (int i = 0; i < count; i++) {
                asyncCallback.put("key", (double)i);
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
                        callback.put("key", (double)i);
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
