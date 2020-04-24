package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

import elemental.dom.Element;

public class RenderedHitBox
{
   Element el;
   RenderedHitBox() {} 

   RenderedHitBox(Element el) 
   {
      this.el = el;
   }
   
   public static RenderedHitBox withChildren()
   {
     RenderedHitBox toReturn = new RenderedHitBox();
     toReturn.children = new ArrayList<>();
     return toReturn;
   }
//   int x;
//   int y;
//   int w;
//   int h;
   List<RenderedHitBox> children;
}
