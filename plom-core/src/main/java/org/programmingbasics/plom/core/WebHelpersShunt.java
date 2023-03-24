package org.programmingbasics.plom.core;


import com.google.gwt.core.shared.GWT;
import com.google.gwt.core.shared.GwtIncompatible;

import elemental.client.Browser;
import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.util.SettableInt;

/**
 * Shunt that redirects to a Js or Java implementation of an
 * interface depending on whether we're running tests in Java
 * or running Js code
 */
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

  public static Uint8Array uint8ArrayforSize(int size)
  {
    return new ByteArrayUint8Array(size);
  }
}
