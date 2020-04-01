package org.programmingbasics.plom.core;

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
//   int x;
//   int y;
//   int w;
//   int h;
   List<RenderedHitBox> children;
}
