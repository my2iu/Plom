package org.programmingbasics.plom.astgen;

import static org.programmingbasics.plom.astgen.Symbol.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlomAstGen
{
  PlomAstGen()
  {

  }

  Map<Symbol, Set<Symbol>> firstTerminals = new HashMap<>();
  Map<Symbol, Set<Symbol>> followsTerminals = new HashMap<>();
  Map<Symbol, Map<Symbol, Production>> parsingTable = new HashMap<>();
  //   Map<String, Set<String>> noAcceptTokenException = new HashMap<String, Set<String>>();

  Production[] grammar = new Production[] {
      rule(Statement, Expression, EndStatement),
      rule(Statement, EndStatement),
      rule(Expression, AdditiveExpression),
      rule(AdditiveExpression, ValueExpression, AdditiveExpressionMore),
      rule(AdditiveExpressionMore, Plus, AdditiveExpression),
      rule(AdditiveExpressionMore, Minus, AdditiveExpression),
      rule(AdditiveExpressionMore),
      rule(ValueExpression, Number),
      rule(ValueExpression, String)
  };

  static Production rule(Symbol from, Symbol ... to)
  {
    return rule("", from, to);
  }

  static Production rule(String name, Symbol from, Symbol ... to)
  {
    return new Production(name, from, to);
  }

  private void calculateFirsts()
  {
    // Initialize firstTerminals map
    for (Production rule : grammar)
    {
      if (!firstTerminals.containsKey(rule.from))
        firstTerminals.put(rule.from, new HashSet<Symbol>());
    }

    boolean isChanged = true;
    while (isChanged)
    {
      isChanged = false;
      for (Production p: grammar)
      {
        Set<Symbol> firsts = firstTerminals.get(p.from);
        if (p.to.isEmpty())
        {
          firsts.add(EMPTY);
          continue;
        }
        boolean expandsToEmpty = false;
        for (Symbol token: p.to)
        {
          // Terminals are simply added as is.
          if (token.isTerminal)
          {
            if (!firsts.contains(token))
            {
              firsts.add(token);
              isChanged = true;
            }
            expandsToEmpty = false;
            break;
          }
          // For non-terminals, we consider them without EMPTY first
          Set<Symbol> tokenFirsts = new HashSet<>(firstTerminals.get(token));
          boolean hasEmpty = tokenFirsts.contains(EMPTY);
          if (hasEmpty)
          {
            tokenFirsts = new HashSet<>(tokenFirsts);
            tokenFirsts.remove(EMPTY);
          }
          if (!firsts.containsAll(tokenFirsts))
          {
            firsts.addAll(tokenFirsts);
            isChanged = true;
          }
          // If we have an epsilon, then we need to gather the possible firsts from the
          // next token as well. Otherwise, move on to the next rule.
          if (!hasEmpty) 
          {
            expandsToEmpty = false;
            break;
          }
        }
        if (expandsToEmpty)
        {
          // We went through all the tokens, and it's possible for the non-terminal to
          // expand to nothing.
          // TODO: I'm not 100% sure what to do here.
          if (!firsts.contains(EMPTY))
          {
            isChanged = true;
            firsts.add(EMPTY);
          }
        }
      }
    }
  }

  private void calculateFollows()
  {
    // Initialize followsTerminals map
    for (Production rule : grammar)
    {
      if (!followsTerminals.containsKey(rule.from))
        followsTerminals.put(rule.from, new HashSet<Symbol>());
    }

    boolean isChanged = true;
    while (isChanged)
    {
      isChanged = false;
      for (Production p: grammar)
      {
        for (int n = 0; n < p.to.size() - 1; n++)
        {
          if (p.to.get(n).isTerminal) continue;
          Set<Symbol> follows = followsTerminals.get(p.to.get(n));
          for (int i = n+1; i < p.to.size(); i++)
          {
            Symbol next = p.to.get(i);
            if (next.isTerminal)
            {
              if (!follows.contains(next)) isChanged = true;
              follows.add(next);
              break;
            }
            // Non-terminal
            boolean hasEmpty = false;
            for (Symbol token: firstTerminals.get(next))
            {
              if (token.equals(EMPTY)) 
              {
                hasEmpty = true;
                // TODO: This is different from Wikipedia. Are you sure?
              } 
              else 
              {
                if (!follows.contains(token)) isChanged = true;
                follows.add(token);
              }
            }
            if (!hasEmpty)
              break;
            // Everything after the non-terminal can expand into EMPTY
            if (i == p.to.size() - 1)
            {
              if (!follows.containsAll(followsTerminals.get(p.from)))
              {
                isChanged = true;
                follows.addAll(followsTerminals.get(p.from));
              }
            }
            // Move on to the next token since its first symbols might
            // apply to this one too.
          }

        }
        // If the last token is a non-terminal, it simply takes the following
        // of the parent.
        if (!p.to.isEmpty() && !p.to.get(p.to.size()-1).isTerminal) 
        {
          Set<Symbol> follows = followsTerminals.get(p.to.get(p.to.size()-1));
          if (!follows.containsAll(followsTerminals.get(p.from)))
          {
            isChanged = true;
            follows.addAll(followsTerminals.get(p.from));
          }
        }
      }
    }
  }

  private void createLLParsingTable()
  {
    // Initialize parser map
    for (Production rule : grammar)
    {
      if (!parsingTable.containsKey(rule.from))
        parsingTable.put(rule.from, new HashMap<Symbol, Production>());
    }
    
    for (Production p: grammar)
    {
      boolean hasEmptyPath = true;
      for (Symbol token: p.to)
      {
        if (token.isTerminal)
        {
          hasEmptyPath = false;
          addParsingRule(p.from, token, p);
          break;
        }
        for (Symbol first : firstTerminals.get(token))
        {
          if (first.equals(EMPTY)) continue;
//          if (isNoAcceptException(p.from, first)) continue;
          addParsingRule(p.from, first, p);
        }
        if (!firstTerminals.get(token).contains(EMPTY))
        {
          hasEmptyPath = false;
          break;
        }
      }
      if (hasEmptyPath)
      {
        // It's possible to go through all the rules getting an EMPTY
        for (Symbol follow : followsTerminals.get(p.from))
        {
          addParsingRule(p.from, follow, p);
        }
      }
    }
  }

  private void addParsingRule(Symbol from, Symbol token, Production p)
  {
    if (parsingTable.get(from).containsKey(token))
    {
      // We have a conflict. See if we have a preferred rule.
//      if (parsingTable.get(from).get(token).isPreferred && !p.isPreferred)
//      {
//        // Existing rule is preferred, go with that.
//        return;
//      }
//      if (!parsingTable.get(from).get(token).isPreferred && p.isPreferred)
//      {
//        // New rule is preferred, replace it.
//        parsingTable.get(from).put(token, p);
//        return;
//      }
      System.err.println("LL parsing conflict with token " + token + " with rules ");
      System.err.println("  " + p);
      System.err.println("  " + parsingTable.get(from).get(token));
    }
    parsingTable.get(from).put(token, p);
  }
  
  void generateFiles(File dir) throws IOException
  {
    dir.mkdirs();
    try (PrintWriter out = new PrintWriter(new File(dir, "Symbol.java"), "UTF-8"))
    {
      out.println("package org.programmingbasics.plom.core.ast.gen;");
      out.println("");
      out.println("public enum Symbol");
      out.println("{");
      boolean isFirst = true;
      for (Symbol sym: Symbol.values())
      {
        if (!isFirst) out.println(",");
        isFirst = false;
        out.print("\t" + sym.name());
      }
      out.println();
      out.println("}");
    }
    
    try (PrintWriter out = new PrintWriter(new File(dir, "Parser.java"), "UTF-8"))
    {
      out.println("package org.programmingbasics.plom.core.ast.gen;");
      out.println("");
      out.println("import java.util.Map;"); 
      out.println("import java.util.HashMap;"); 
      out.println();
      out.println("public class Parser");
      out.println("{");
      out.println("\tpublic Map<Symbol, Map<Symbol, Symbol[]>> parsingTable = new HashMap<>();");
      out.println("\t{");
      for (Map.Entry<Symbol, Map<Symbol, Production>> stackTop: parsingTable.entrySet())
      {
        out.println("\t\tparsingTable.put(Symbol." + stackTop.getKey().name() + ", new HashMap<>());");
        for (Map.Entry<Symbol, Production> expansion: stackTop.getValue().entrySet())
        {
          out.print("\t\tparsingTable.get(Symbol." + stackTop.getKey().name() + ").put(Symbol." + expansion.getKey().name() + ", new Symbol[] {");
          for (Symbol to: expansion.getValue().to)
          {
            out.print("Symbol." + to.name() +", ");
          }
          out.println("});");
        }
      }
      out.println("\t}");
      
      out.println("}");
    }

  }

  public void go(File dir) throws IOException
  {
    calculateFirsts();
    calculateFollows();
    createLLParsingTable();
    generateFiles(dir);
  }

  public static void main(String [] args) throws IOException
  {
    new PlomAstGen().go(new File("../plom-core/src/main/java/org/programmingbasics/plom/core/ast/gen"));
  }
}