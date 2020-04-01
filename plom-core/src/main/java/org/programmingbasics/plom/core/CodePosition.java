package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor position in some code. This needs to be abstracted out because
 * I'm not sure about how the code will be represented internally yet. 
 */
public class CodePosition
{
   List<Integer> offsets = new ArrayList<>();
   public int getOffset(int level)
   {
      if (level >= offsets.size()) return 0;
      return offsets.get(level);
   }
   public void setOffset(int level, int val)
   {
      while (level >= offsets.size())
         offsets.add(0);
      offsets.set(level, val);
   }
//   int line;
//   int token;
}
