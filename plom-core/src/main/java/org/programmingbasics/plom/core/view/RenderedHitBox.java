package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elemental.dom.Element;
import elemental.html.ClientRect;
import elemental.html.ClientRectList;

public class RenderedHitBox
{
   Element el;
   RenderedHitBox() {} 

   public RenderedHitBox(Element el) 
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

   public List<Rect> getClientRects() 
   {
     ClientRect base = el.getOffsetParent().getBoundingClientRect();
     double scrollX = el.getOffsetParent().getScrollLeft();
     double scrollY = el.getOffsetParent().getScrollTop();
     List<Rect> rects = new ArrayList<>();
     ClientRectList list = el.getClientRects();
     for (int n = 0; n < list.getLength(); n++)
     {
       ClientRect r = list.item(n);
       rects.add(new Rect(r.getLeft() - base.getLeft() + scrollX, r.getTop() - base.getTop() + scrollY, r.getWidth(), r.getHeight()));
     }
     return rects;
   }
   
   public static class Rect
   {
     Rect(double x, double y, double w, double h) { this.x = x; this.y = y; this.w = w; this.h = h; }
     double x, y, w, h;
     double getX() { return x; }
     double getY() { return y; }
     double getWidth() { return w; }
     double getHeight() { return h; }
     double getLeft() { return x; }
     double getRight() { return x + w; }  // Assumes w > 0
     double getTop() { return y; }
     double getBottom() { return y + h; }  // Assumes h > 0
   }
   
   public static RenderedHitBox withChildren()
   {
     RenderedHitBox toReturn = new RenderedHitBox();
     toReturn.children = new ArrayList<>();
     return toReturn;
   }
   
   public static RenderedHitBox withChildren(Element el)
   {
     RenderedHitBox toReturn = RenderedHitBox.withChildren();
     toReturn.el = el;
     return toReturn;
   }
   
   public static RenderedHitBox forRectangle(double x, double y, double width, double height)
   {
     List<Rect> rects = Arrays.asList(
         new Rect(x, y, width, height)
         );
     return new RenderedHitBox() {
       @Override public int getOffsetLeft() { return (int)x; }
       @Override public int getOffsetTop() { return (int)y; }
       @Override public int getOffsetWidth() { return (int)width; }
       @Override public int getOffsetHeight() { return (int)height; }
       @Override public List<Rect> getClientRects() { return rects; }
     };
   }

   public String getTestData()
   {
     String toReturn = "(";
     if (el == null)
       toReturn += "-1, -1, -1, -1";
     else
       toReturn += getOffsetLeft() + ", " + getOffsetTop() + ", " + getOffsetWidth() + ", " + getOffsetHeight();
     if (children != null)
     {
       for (RenderedHitBox child: children)
       {
         toReturn += ", ";
         if (child == null)
           toReturn += "null";
         else
           toReturn += child.getTestData();
       }
     }
     toReturn += ")";
     return toReturn;
   }
//   int x;
//   int y;
//   int w;
//   int h;
   public List<RenderedHitBox> children;
}
