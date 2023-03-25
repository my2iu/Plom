package org.programmingbasics.plom.core;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.WebHelpers.Promise.PromiseConstructorFunction;
import org.programmingbasics.plom.core.WebHelpers.Promise.Then;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.core.shared.GwtIncompatible;

import elemental.client.Browser;
import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import elemental.util.SettableInt;

/**
 * Shunt that redirects to a Js or Java implementation of an
 * interface depending on whether we're running tests in Java
 * or running Js code
 */
@GwtIncompatible
public class WebHelpersShunt
{
  /**
   * Emulated a Uint8Array for Java using a byte[] array. But when
   * compiled into JavaScript, it will simply return a normal Uint8Array().
   * This is useful for testing some JS code in Java
   */
  @GwtIncompatible
  public static class ByteArrayUint8Array implements Uint8Array, SettableInt, ArrayBuffer
  {
    final byte[] data;
    ByteArrayUint8Array(int size)
    {
      data = new byte[size];
    }
    
    @Override
    public int intAt(int index)
    {
      return ((int)data[index]) & 0xff;
    }

    @Override
    public int length()
    {
      return data.length;
    }

    @Override
    public double numberAt(int index)
    {
      return intAt(index);
    }

    @Override
    public void setAt(int index, int value)
    {
      data[index] = (byte)value;
    }

    @Override
    public ArrayBuffer getBuffer()
    {
      return this;
    }

    @Override
    public int getByteLength()
    {
      return data.length;
    }

    @Override
    public int getByteOffset()
    {
      return 0;
    }

    @Override
    public int getLength()
    {
      return data.length;
    }

    @Override
    public void setElements(Object array)
    {
      throw new IllegalArgumentException("Not implemented in Java");
    }

    @Override
    public void setElements(Object array, int offset)
    {
      throw new IllegalArgumentException("Not implemented in Java");
    }

    @Override
    public Uint8Array subarray(int start)
    {
      return subarray(start, length());
    }

    @Override
    public Uint8Array subarray(int start, int end)
    {
      ByteArrayUint8Array sub = new ByteArrayUint8Array(end - start);
      System.arraycopy(data, start, sub.data, 0, end - start);
      return sub;
    }

    public static Uint8Array fromByteArray(byte[] data)
    {
      if (GWT.isClient())
      {
        Uint8Array newArr = forSize(data.length);
        for (int n = 0; n < data.length; n++)
        {
          ((SettableInt)newArr).setAt(n, data[n]);
        }
        return newArr;
      }
      else
      {
        ByteArrayUint8Array newArr = new ByteArrayUint8Array(data.length);
        System.arraycopy(data, 0, newArr.data, 0, data.length);
        return newArr;
      }
    }
    
    public static Uint8Array forSize(int size)
    {
      if (GWT.isClient())
        return Browser.getWindow().newUint8Array(size);
      else
        return new ByteArrayUint8Array(size);
    }

    @Override
    public ArrayBuffer slice(int begin)
    {
      throw new IllegalArgumentException("Not implemented in Java");
    }

    @Override
    public ArrayBuffer slice(int begin, int end)
    {
      throw new IllegalArgumentException("Not implemented in Java");
    }
  }

  /**
   * A partial implementation of JS promises in Java that can be
   * used to mock JS functionality in Java tests
   */
  @GwtIncompatible
  public static class JsEmulatedPromise<T> implements WebHelpers.Promise<T>
  {
    CompletableFuture<T> future;
    JsEmulatedPromise() {}
    JsEmulatedPromise(CompletableFuture<T> toWrap) { future = toWrap; }
    JsEmulatedPromise(PromiseConstructorFunction<T> createCallback)
    {
      future = new CompletableFuture<T>();
      ForkJoinPool.commonPool().execute(() -> {
        createCallback.call(resolvedValue -> {
          future.complete(resolvedValue);
        }, 
        errVal -> {
          // Unhandled
        });
      });
    }
    static <U> CompletableFuture<U> promiseToFuture(WebHelpers.Promise<U> toWrap) {
      CompletableFuture<U> future = new CompletableFuture<U>();
      toWrap.thenNow(val -> {
        future.complete(val); 
        return null;
      });
      return future;
    }
    @Override
    public <U> WebHelpers.Promise<U> then(Then<T, WebHelpers.Promise<U>> fn)
    {
      return new JsEmulatedPromise<>(future.thenComposeAsync((T val) -> {
        try {
          return promiseToFuture(fn.call(val));
        } catch (Throwable e)
        {
          e.printStackTrace();
          throw e;
        }
      } ));
    }

    @Override
    public <U> WebHelpers.Promise<U> thenNow(Then<T, U> fn)
    {
      return new JsEmulatedPromise<U>(future.<U>thenApplyAsync((T val) -> { return fn.call(val); }));
    }
    public static <U> Promise<ArrayOf<U>> promiseAll(ArrayOf<Promise<U>> promises)
    {
      return gatherPromiseAll(promises, 0, Collections.arrayOf());
    }
    private static <U> Promise<ArrayOf<U>> gatherPromiseAll(ArrayOf<Promise<U>> promises, int idx, ArrayOf<U> gatheredPromises)
    {
      if (idx >= promises.length())
        return new JsEmulatedPromise<>(CompletableFuture.completedFuture(gatheredPromises));
      return promises.get(idx).then(val -> {
        gatheredPromises.push(val);
        return gatherPromiseAll(promises, idx + 1, gatheredPromises);
      });
    }
  }

  
  public static Uint8Array uint8ArrayforSize(int size)
  {
    return new ByteArrayUint8Array(size);
  }
  
  public static <U> Promise<U> promiseResolve(U val)
  {
    return new JsEmulatedPromise<U>(CompletableFuture.completedFuture(val));
  }
  
  public static <U> Promise<U> newPromise(PromiseConstructorFunction<U> createCallback) 
  {
    return new JsEmulatedPromise<>(createCallback);
  }

  public static <U> Promise<ArrayOf<U>> promiseAll(ArrayOf<Promise<U>> promises)
  {
    return JsEmulatedPromise.promiseAll(promises);
  }

}
