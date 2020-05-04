package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor position in some code. This needs to be abstracted out because
 * I'm not sure about how the code will be represented internally yet. 
 */
public class CodePosition
{
  public static CodePosition fromOffsets(int...vals)
  {
    CodePosition toReturn = new CodePosition();
    for (int n = 0; n < vals.length; n++)
    {
      toReturn.setOffset(n, vals[n]);
    }
    return toReturn;
  }
   private List<Integer> offsets = new ArrayList<>();
   public int getOffset(int level)
   {
      if (level >= offsets.size()) return 0;
      if (offsets.get(level) < 0) return 0;
      return offsets.get(level);
   }
   public void setOffset(int level, int val)
   {
      while (level >= offsets.size())
         offsets.add(0);
      offsets.set(level, val);
   }
   boolean hasOffset(int level)
   {
      if (level >= offsets.size()) return false;
      if (offsets.get(level) < 0) return false;
      return true;
   }
   void setMaxOffset(int level)
   {
      for (int n = level; n < offsets.size(); n++)
         offsets.set(n, -1);
   }
//   int line;
//   int token;
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    CodePosition other = (CodePosition) obj;
    if (offsets == null)
    {
      if (other.offsets != null) return false;
    }
    for (int n = 0; n < Math.max(offsets.size(), other.offsets.size()); n++)
    {
      if (hasOffset(n) != other.hasOffset(n)) return false;
      if (getOffset(n) != other.getOffset(n)) return false;
    }
    return true;
  }
}
