package org.programmingbasics.plom.core;

import com.google.gwt.core.shared.GWT;

import elemental.client.Browser;
import elemental.html.Uint8Array;

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
}
