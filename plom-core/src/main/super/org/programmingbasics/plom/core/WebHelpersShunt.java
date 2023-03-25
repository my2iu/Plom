package org.programmingbasics.plom.core;

import java.util.concurrent.CompletableFuture;

import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.WebHelpers.Promise.PromiseConstructorFunction;
import org.programmingbasics.plom.core.WebHelpersShunt.JsEmulatedPromise;

import com.google.gwt.core.shared.GWT;

import elemental.client.Browser;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;

/**
 * Shunt that redirects to a Js or Java implementation of an
 * interface depending on whether we're running tests in Java
 * or running Js code
 */
public class WebHelpersShunt
{
  public static Uint8Array uint8ArrayforSize(int size)
  {
    return Browser.getWindow().newUint8Array(size);
  }
  
  public static <U> Promise<U> promiseResolve(U val)
  {
    return WebHelpers.PromiseClass.resolve(val);
  }
  
  public static <U> Promise<U> newPromise(PromiseConstructorFunction<U> createCallback) 
  {
    return new WebHelpers.PromiseClass<>(createCallback);
  }

  public static <U> Promise<ArrayOf<U>> promiseAll(ArrayOf<Promise<U>> promises)
  {
    return WebHelpers.promiseAll(promises);
  }
}
