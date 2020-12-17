package org.programmingbasics.plom.core;

public class Clipboard
{
  public static Clipboard instance = new Clipboard();
  
  String clippedCodeFragment;
  
  public void putCodeFragment(String fragment)
  {
    clippedCodeFragment = fragment;
  }
  
  public String getCodeFragment()
  {
    return clippedCodeFragment;
  }
}
