package org.programmingbasics.plom.core.view;

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
   
   public int getOffsetLeft()
   {
     return el.getOffsetLeft();
   }
   
   public int getOffsetTop()
   {
     return el.getOffsetTop();
   }

   public int getOffsetWidth()
   {
     return el.getOffsetWidth();
   }

   public int getOffsetHeight()
   {
     return el.getOffsetHeight();
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
