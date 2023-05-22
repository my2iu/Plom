package org.programmingbasics.plom.core;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.google.gwt.core.shared.GWT;

import elemental.html.ArrayBuffer;
import elemental.html.ArrayBufferView;
import elemental.html.Uint8Array;
import elemental.json.JsonValue;
import elemental.util.ArrayOf;
import elemental.util.SettableInt;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;


/**
 * Holds various helper classes for accessing various HTML/js functionality
 * that isn't available in the ancient version of elemental that I use
 */
public class WebHelpers
{
  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  public static interface TextDecoder
  {
    String decode();
    String decode(ArrayBuffer buf);
    String decode(ArrayBufferView buf);
//    @JsOverlay static TextDecoder create() { return Js.asConstructorFn(TextDecoder.class).construct(); }
  }
  @JsType(isNative = true, name = "TextDecoder", namespace = JsPackage.GLOBAL)
  public static class TextDecoderClass implements TextDecoder
  {
    public TextDecoderClass() {}
    @Override public native String decode();
    @Override public native String decode(ArrayBuffer buf);
    @Override public native String decode(ArrayBufferView buf);
  }

  static TextDecoder decoder = new TextDecoderClass();
  
  public static class Base64EncoderDecoder
  {
     static char[] encodeLookup = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
     };
     
     // encodes up to 3 bytes into 4 characters
     static int encodeTriple(byte[] input, int numBytes, char[] output)
     {
        // zero out the output
        Arrays.fill(output, (char)0);
        
        // zero out unused input
        switch(numBytes)
        {
        case 0:
           input[0] = 0;
        case 1:
           input[1] = 0;
        case 2:
           input[2] = 0;
        }
        
        // encode the bytes that we have available
        switch (numBytes)
        {
        case 3:
           output[3] = encodeLookup[((int)input[2] & 0x3f)];
        case 2:
           output[2] = encodeLookup[(((int)input[1] & 0xf) << 2) | (((int)input[2] & 192) >> 6)];
        case 1:
           output[1] = encodeLookup[(((int)input[0] & 3) << 4) | (((int)input[1] & 0xf0) >> 4)];
           output[0] = encodeLookup[((int)input[0] & 252) >> 2];
        case 0:
        default:
        }
        
        // return number of characters encoded
        switch(numBytes)
        {
        case 3: return 4;
        case 2: return 3;
        case 1: return 2;
        case 0: default: return 0;
        }
     }
     public static String encodeDataURIBase64(String mimeType, Uint8Array data)
     {
         return "data:" + mimeType + ";base64," + encodeForDataURI(data);
     }
     static char [] hexLookup = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'}; 
     static boolean [] validURLBytes = new boolean[256];
     static {
      for (char n = 'A'; n <= 'Z'; n++) validURLBytes[n+128] = true;
      for (char n = 'a'; n <= 'z'; n++) validURLBytes[n+128] = true;
      for (char n = '0'; n <= '9'; n++) validURLBytes[n+128] = true;
      validURLBytes['_'+128] = true;
      validURLBytes['-'+128] = true;
      validURLBytes['.'+128] = true;
      validURLBytes['~'+128] = true;
     }
     
     public static String guessMimeType(String filename)
     {
         String lowerFile = filename.toLowerCase();
         if (lowerFile.endsWith(".png"))
             return "image/png";
         else if (lowerFile.endsWith(".jpg"))
             return "image/jpeg";
         else if (lowerFile.endsWith(".jpeg"))
             return "image/jpeg";
         else if (lowerFile.endsWith(".gif"))
             return "image/gif";
         return "application/x-octet-stream";
     }
     public static String encodeDataURIText(String mimeType, String stringData)
     {
        try {
            byte [] data = stringData.getBytes("UTF-8");
          StringBuffer sb = new StringBuffer(data.length * 3);
          for (int n = 0; n < data.length; n++)
          {
              int val = data[n];
              if (validURLBytes[val+128])
                  sb.append((char)val);
              else
              {
                  sb.append('%');
                  sb.append(hexLookup[(val >> 4) & 0xf]);
                  sb.append(hexLookup[val & 0xf]);
              }
          }
            return "data:" + mimeType + "," + sb.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
      return null;
     }

     public static String encodeForDataURI(Uint8Array data)
     {
        return encodeToString(data, true);  // Chrome needs the padding
     }
     public static int getNumCharsForEncoding(int numBytes)
     {
       int numChars = (int)(numBytes / 3) * 4;
       int tightChars = numChars;
       if ((numBytes % 3) > 0)
       {
          tightChars += (numBytes % 3) + 1;
       }
       return tightChars;
     }
     public static int getNumCharsWithPaddingForEncoding(int numBytes)
     {
       int numChars = (int)(numBytes / 3) * 4;
       if ((numBytes % 3) > 0)
       {
          numChars += 4;
       }
       return numChars;
     }
     
     public static void encode(Uint8Array encoded, Uint8Array uint8Data, boolean usePadding)
     {
        int numBytes = uint8Data.getByteLength();
        int tightChars = getNumCharsForEncoding(numBytes);
        int numChars = usePadding ? getNumCharsWithPaddingForEncoding(numBytes) : tightChars;
        SettableInt encodedVals = (SettableInt)encoded;
        
        // Encode all 3-byte triples into 4 characters
        int charpos = 0;
        int bytepos = 0;
        for (; bytepos < numBytes - (numBytes % 3); bytepos+=3)
        {
           encodedVals.setAt(charpos + 3, encodeLookup[(uint8Data.intAt(bytepos+2) & 0x3f)]);
           encodedVals.setAt(charpos + 2, encodeLookup[((uint8Data.intAt(bytepos+1) & 0xf) << 2) | ((uint8Data.intAt(bytepos+2) & 192) >> 6)]);
           encodedVals.setAt(charpos + 1, encodeLookup[((uint8Data.intAt(bytepos) & 3) << 4) | ((uint8Data.intAt(bytepos+1) & 0xf0) >> 4)]);
           encodedVals.setAt(charpos + 0, encodeLookup[(uint8Data.intAt(bytepos) & 252) >> 2]);
           charpos += 4;
        }
        // Encode remaining partial bytes into characters
        int []miniEncode = new int[4];
        for (; bytepos < numBytes; bytepos++)
        {
           switch(bytepos % 3)
           {
           case 0:
              miniEncode[0] += (uint8Data.intAt(bytepos) & 252) >> 2;
              miniEncode[1] += ((uint8Data.intAt(bytepos) & 3) << 4);
              break;
           case 1:
              miniEncode[1] += ((uint8Data.intAt(bytepos) & 0xf0) >> 4);
              miniEncode[2] += ((uint8Data.intAt(bytepos) & 0xf) << 2);
           case 2:
              miniEncode[2] += ((uint8Data.intAt(bytepos) & 192) >> 6);
              miniEncode[3] += (uint8Data.intAt(bytepos) & 0x3f);
           }
        }
        for (;charpos < tightChars; charpos++)
        {
           encodedVals.setAt(charpos, encodeLookup[miniEncode[charpos % 4]]);
        }
        if (usePadding)
        {
           for (;charpos < numChars; charpos++)
           {
             encodedVals.setAt(charpos, '=');
           }
        }
     }

     public static String encodeToString(Uint8Array data, boolean usePadding)
     {
       int numChars = usePadding ? getNumCharsWithPaddingForEncoding(data.getByteLength()) : getNumCharsForEncoding(data.getByteLength());
       Uint8Array chars = WebHelpersShunt.uint8ArrayforSize(numChars);
       encode(chars, data, usePadding);
       if (GWT.isClient())
       {
         return decoder.decode(chars);
       }
       else
       {
         String toReturn = "";
         for (int n = 0; n < chars.getLength(); n++)
         {
           toReturn += Character.toString((char)chars.intAt(n));
         }
         return toReturn;
       }
     }

     static int[] decodeLookup = new int[256];
     static {
      decodeLookup = new int[256];
      for (int n = 0; n < 256; n++) decodeLookup[n] = -1;
      for (int n = 0; n < encodeLookup.length; n++)
          decodeLookup[encodeLookup[n]] = n;
     }
     public static Uint8Array decodeBase64ToUint8Array(String str)
     {
       Uint8Array data = WebHelpersShunt.uint8ArrayforSize(str.length()); // we'll overestimate the size for now
       SettableInt dataSetter = (SettableInt)data;
       int charIdx = 0;
       int idx = 0;
       for (; charIdx < str.length(); charIdx++)
       {
         int c = str.charAt(charIdx);
         if (c > 255) break;
         int val = decodeLookup[c];
         if (val < 0) continue;

         int triple = idx / 4;
         int tripleIdx = idx % 4;
         switch (tripleIdx)
         {
         case 0: dataSetter.setAt(triple * 3, dataSetter.intAt(triple * 3)| (val << 2)); break;
         case 1: dataSetter.setAt(triple*3,  dataSetter.intAt(triple*3) | ((val >> 4) & 3)); dataSetter.setAt(triple*3+1, dataSetter.intAt(triple*3+1) | ((val << 4) & 0xf0)); break;
         case 2: dataSetter.setAt(triple*3+1, dataSetter.intAt(triple*3+1) | ((val >> 2) & 0xf)); dataSetter.setAt(triple*3+2, dataSetter.intAt(triple*3+2) | ((val << 6) & 192)); break;
         case 3: dataSetter.setAt(triple*3+2, dataSetter.intAt(triple*3+2) | (val & 0x3f)); break;
         }
         idx++;
       }

       // Shrink the size of the array to the actual number of bytes read
       int lastCharRead = idx;
       int byteSize = lastCharRead / 4 * 3;
       switch (lastCharRead % 4)
       {
       case 0: break;
       case 1: break;  // impossible
       case 2: byteSize += 1; break;
       case 3: byteSize += 2; break;
       }
       return data.subarray(0, byteSize);
     }
  }


  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  public static interface Promise<T>
  {
//    @JsOverlay static <U> Promise<U> create(PromiseConstructorFunction<U> createCallback) { return (Promise<U>)Js.asConstructorFn(Promise.class).construct(createCallback); }
    @JsFunction
    public static interface Consumer<U>
    {
      public void accept(U val);
    }
    @JsFunction
    public static interface PromiseConstructorFunction<U>
    {
      public void call(Consumer<U> resolve, Consumer<Object> reject);
    }
    
    <U> Promise<U> then(Then<T, Promise<U>> fn);
    @JsMethod(name="then")
    <U> Promise<U> thenNow(Then<T, U> fn);
    @JsFunction
    public static interface Then<U, V>
    {
      public V call(U val);
    }

    @JsFunction 
    public static interface All
    {
      public <U> Promise<ArrayOf<U>> all(ArrayOf<Promise<U>> promises);
      
    }
//    @JsOverlay public static All all()
//    {
//      return (All)Js.asPropertyMap(Js.global().get("Promise")).get("all");
//    }
    
  }
  
  @JsType(isNative = true, name = "Promise", namespace = JsPackage.GLOBAL)
  public static class PromiseClass<T> implements Promise<T>
  {
    public PromiseClass(PromiseConstructorFunction<T> createCallback) {}

    @Override public native <U> Promise<U> then(Then<T, Promise<U>> fn);
    @Override public native <U> Promise<U> thenNow(Then<T, U> fn);
    
    public static native <U> Promise<U> resolve(U val);
  }
  
  // Abstracts out the creation of promises so that promises can be
  // mocked by Java tests
  static interface PromiseCreator
  {
    <U> Promise<U> create(Promise.PromiseConstructorFunction<U> createCallback);
  }

//  @JsFunction 
//  public static interface PromiseAll<T>
//  {
//    public Promise<ArrayOf<T>> all(ArrayOf<Promise<T>> promises);
//    
//  }
  @JsMethod(name = "Promise.all", namespace = JsPackage.GLOBAL)
  public static native <U> Promise<ArrayOf<U>> promiseAll(ArrayOf<Promise<U>> promises);
  
  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  public static interface Response
  {
    Promise<ArrayBuffer> arrayBuffer();
    Promise<JsonValue> json();
    Promise<String> text();
  }
  
  
  @JsMethod(name = "fetch", namespace = JsPackage.GLOBAL)
  public static native Promise<Response> fetch(String url);

  // The GWT Elemental FileReader uses the iframe's FileReader instead of
  // $wnd's FileReader, which can cause failures with instanceof, so we 
  // need to define our own FileReader which will be correctly instantiated
  // using the $wnd's version
  @JsType(isNative = true, name = "FileReader", namespace = JsPackage.GLOBAL)
  public static class FileReader
  {
    public FileReader() {}
  }

  
  // Bindings to jszip
  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  public static class JSZip
  {
    public JSZip() {}
    public native JSZip file(String name, String data);
    @JsMethod(name = "file") public native JSZip filePromiseString(String name, Promise<String> data);
    public native JSZip file(String name, ArrayBuffer data);
    @JsMethod(name = "file") public native JSZip filePromiseArrayBuffer(String name, Promise<ArrayBuffer> data);
    public native JSZip file(String name, Uint8Array data);
    @JsMethod(name = "file") public native JSZip filePromiseUint8Array(String name, Promise<Uint8Array> data);
    public native JSZip folder(String name);
    public native Promise<Object> generateAsync(JSZipGenerateAsyncOptions options);
  }
  
  @JsType(isNative = true)
  public static interface JSZipGenerateAsyncOptions
  {
    @JsProperty(name = "type") void setType(String type);
    @JsOverlay default void setTypeBlob() { setType("blob"); }
    @JsOverlay default void setTypeArrayBuffer() { setType("arraybuffer"); }
    @JsProperty(name = "compression") void setCompression(String compression);
    @JsOverlay default void setCompressionStore() { setCompression("STORE"); }
    @JsOverlay default void setCompressionDeflate() { setCompression("DEFLATE"); }
  }
}
