package org.programmingbasics.plom.core;

import elemental.js.events.JsMouseEvent;

public class JsPointerEvent extends JsMouseEvent implements PointerEvent
{
   protected JsPointerEvent() {}
   
   public final native int getPointerId() /*-{
      return this.pointerId;
    }-*/;
   public final native double getWidth() /*-{
      return this.width;
    }-*/;
   public final native double getHeight() /*-{
      return this.height;
    }-*/;
   public final native float getPressure() /*-{
      return this.pressure;
    }-*/;
   public final native int getTiltX() /*-{
      return this.tiltX;
    }-*/;
   public final native int getTiltY() /*-{
      return this.tiltY;
    }-*/;
   public final native String getPointerType() /*-{
      return this.pointerType;
    }-*/;
   public final native boolean getIsPrimary() /*-{
      return this.isPrimary;
    }-*/;
}
