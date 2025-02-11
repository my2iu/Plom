package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor position in some code. This needs to be abstracted out because
 * I'm not sure about how the code will be represented internally yet. 
 */
public class CodePosition
{
  public static final int PARAMTOK_POS_EXPRS = 1;
  public static final int PARAMTOK_POS_TEXTS = 0;
  
  public static final int EXPRBLOCK_POS_BLOCK = 2;
  public static final int EXPRBLOCK_POS_EXPR = 1;
  public static final int EXPRBLOCK_POS_START = 0;

//  private static final int EXPRBLOCK_POS_END = 3;

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
   public boolean hasOffset(int level)
   {
      if (level >= offsets.size()) return false;
      if (offsets.get(level) < 0) return false;
      return true;
   }
   public void setMaxOffset(int level)
   {
      for (int n = level; n < offsets.size(); n++)
         offsets.set(n, -1);
   }
   
   public boolean isBefore(CodePosition other)
   {
     for (int level = 0; ; level++)
     {
       boolean hasNext = hasOffset(level);
       boolean otherHasNext = other.hasOffset(level);
       if (!hasNext && !otherHasNext) return false;
       if (otherHasNext && !hasNext) return true;
       if (hasNext && !otherHasNext) return false;
       if (getOffset(level) < other.getOffset(level)) return true;
       if (getOffset(level) > other.getOffset(level)) return false;
     }
   }

   // Checks if this position is between two positions (returns false
   // if either of the positions is null)
   public boolean isBetweenNullable(CodePosition pos1, CodePosition pos2)
   {
     if (pos1 == null || pos2 == null) 
       return false;
     CodePosition startPos, endPos;
     if (pos1.isBefore(pos2))
     {
       startPos = pos1;
       endPos = pos2;
     }
     else
     {
       startPos = pos2;
       endPos = pos1;
     }
     return !this.isBefore(startPos) && this.isBefore(endPos);
   }
   
   public boolean equalUpToLevel(CodePosition other, int level)
   {
     for (int n = 0; n <= level; n++)
     {
       if (getOffset(n) != other.getOffset(n))
         return false;
     }
     return true;
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
  @Override
  public String toString()
  {
    String toReturn = "[";
    boolean isFirst = true;
    for (int offset: offsets)
    {
      if (offset < 0) break;
      if (!isFirst)
        toReturn += ", ";
      isFirst = false;
      toReturn += offset;
    }
    toReturn += "]";
    return toReturn;
  }
  public static CodePosition fromString(String str)
  {
    if (str == null) return null;
    String [] splits = str.split("[,]");
    int [] offsets = new int[splits.length - 2];
    for (int n = 1; n < splits.length - 1; n++)
      offsets[n-1] = Integer.parseInt(splits[n]);
    return CodePosition.fromOffsets(offsets);
  }
  public CodePosition clone()
  {
    CodePosition pos = new CodePosition();
    pos.offsets = new ArrayList<>(offsets);
    return pos;
  }
}
