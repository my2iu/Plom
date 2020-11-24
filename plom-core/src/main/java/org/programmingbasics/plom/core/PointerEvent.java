package org.programmingbasics.plom.core;

import elemental.events.MouseEvent;

public interface PointerEvent extends MouseEvent
{
   int getPointerId();
   double getWidth();
   double getHeight();
   float getPressure();
   int getTiltX();
   int getTiltY();
   String getPointerType();
   boolean getIsPrimary();
   
   public interface PointerType {
      public static final String MOUSE = "mouse";
      public static final String PEN_STYLUS = "pen";
      public static final String TOUCH = "touch";
    }
   
   public interface PointerEventType {
      public static final String POINTERDOWN = "pointerdown";
      public static final String POINTERUP = "pointerup";
      public static final String POINTERCANCEL = "pointercancel";
      public static final String POINTERMOVE = "pointermove";
      public static final String POINTEROVER = "pointerover";
      public static final String POINTEROUT = "pointerout";
      public static final String GOTPOINTERCAPTURE = "gotpointercapture";
      public static final String LOSTPOINTERCAPTURE = "lostpointercapture";

   }

}
