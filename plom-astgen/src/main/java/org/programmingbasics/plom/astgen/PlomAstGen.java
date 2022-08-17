package org.programmingbasics.plom.astgen;

import static org.programmingbasics.plom.astgen.Symbol.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
      rule(FullStatement, StatementOrEmpty, EndStatement),
      rule(StatementOrEmpty, Statement),
      rule(StatementOrEmpty),
      rule(Statement, AssignmentExpression),
      rule(Statement, VarStatement),
      rule(Statement, ReturnStatement),
      rule(Statement, PrimitivePassthrough),
      rule(Statement, WideStatement, StatementOrEmpty),
      rule(WideStatement, COMPOUND_IF, AfterIf),
      rule(WideStatement, COMPOUND_WHILE),
      rule(WideStatement, COMPOUND_FOR),
      rule(WideStatement, DUMMY_COMMENT),
      rule(ExpressionOnly, Expression, EndStatement),
      rule(AssignmentExpression, Expression, AssignmentExpressionMore),
      rule(AssignmentExpressionMore, Assignment, Expression),
      rule(AssignmentExpressionMore),
      rule(Expression, OrExpression),
      rule(OrExpression, AndExpression, OrExpressionMore),
      rule(OrExpressionMore, Or, AndExpression, OrExpressionMore),
      rule(OrExpressionMore),
      rule(AndExpression, RelationalExpression, AndExpressionMore),
      rule(AndExpressionMore, And, RelationalExpression, AndExpressionMore),
      rule(AndExpressionMore),
      rule(RelationalExpression, AdditiveExpression, RelationalExpressionMore),
      rule(RelationalExpressionMore, Retype, Type, RelationalExpressionMore),
      rule(RelationalExpressionMore, Gt, AdditiveExpression, RelationalExpressionMore),
      rule(RelationalExpressionMore, Ge, AdditiveExpression, RelationalExpressionMore),
      rule(RelationalExpressionMore, Lt, AdditiveExpression, RelationalExpressionMore),
      rule(RelationalExpressionMore, Le, AdditiveExpression, RelationalExpressionMore),
      rule(RelationalExpressionMore, Eq, AdditiveExpression, RelationalExpressionMore),
      rule(RelationalExpressionMore, Ne, AdditiveExpression, RelationalExpressionMore),
      rule(RelationalExpressionMore, Is, Type, RelationalExpressionMore),
      rule(RelationalExpressionMore),
      rule(AdditiveExpression, MultiplicativeExpression, AdditiveExpressionMore),
      rule(AdditiveExpressionMore, Plus, MultiplicativeExpression, AdditiveExpressionMore),
      rule(AdditiveExpressionMore, Minus, MultiplicativeExpression, AdditiveExpressionMore),
      rule(AdditiveExpressionMore),
      rule(MultiplicativeExpression, MemberExpression, MultiplicativeExpressionMore),
      rule(MultiplicativeExpressionMore, Multiply, MemberExpression, MultiplicativeExpressionMore),
      rule(MultiplicativeExpressionMore, Divide, MemberExpression, MultiplicativeExpressionMore),
      rule(MultiplicativeExpressionMore),
      rule(MemberExpression, ParenthesisExpression, MemberExpressionMore),
      rule(MemberExpressionMore, DotMember, MemberExpressionMore),
      rule(MemberExpressionMore, As, Type, MemberExpressionMore),
      rule(MemberExpressionMore),
      rule(ParenthesisExpression, OpenParenthesis, Expression, ClosedParenthesis),
      rule(ParenthesisExpression, ValueExpression),
      rule(ValueExpression, This),
      rule(ValueExpression, SuperCallExpression),
      rule(ValueExpression, NullLiteral),
      rule(ValueExpression, TrueLiteral),
      rule(ValueExpression, FalseLiteral),
      rule(ValueExpression, Number),
      rule(ValueExpression, FunctionLiteral),
      rule(ValueExpression, StaticMethodCallExpression),
      rule(ValueExpression, DotVariable),
      rule(ValueExpression, String),
      rule(StaticMethodCallExpression, AtType, DotMember),
      rule(SuperCallExpression, Super, DotSuperMember),
      rule(AfterIf, COMPOUND_ELSEIF, AfterIf),
      rule(AfterIf, COMPOUND_ELSE),
      rule(AfterIf),
      rule(VarStatement, Var, DotDeclareIdentifier, VarType, VarAssignment),
      rule(VarType, Type), 
      rule(VarType),
      rule(TypeOnly, Type, EndStatement),
      rule(Type, AtType),
      rule(Type, FunctionType),
      rule(FunctionType, FunctionTypeName, Returns, ParameterFieldOptionalName),
      rule(DotDeclareIdentifier, DotVariable),
      rule(DotMember, DotVariable),
      rule(DotSuperMember, DotVariable),
      rule(VarAssignment, Assignment, Expression),
      rule(VarAssignment),
      rule(ReturnStatement, Return, Expression),
      rule(ForExpression, DotDeclareIdentifier, VarType, In, Expression),
      rule(ForExpressionOnly, ForExpression, EndStatement),
      rule(FunctionLiteralExpression, FunctionType),
      rule(FunctionLiteralExpressionOnly, FunctionLiteralExpression, EndStatement),

      rule(FullVariableDeclaration, VariableDeclarationOrEmpty, EndStatement),
      rule(VariableDeclarationOrEmpty, VariableDeclaration),
      rule(VariableDeclarationOrEmpty),
      rule(VariableDeclaration, VarDeclarationStatement),
      rule(VariableDeclaration, DUMMY_COMMENT, VariableDeclarationOrEmpty),
      rule(VarDeclarationStatement, Var, DotDeclareIdentifier, VarType),

      rule(ReturnTypeField, Type),
      rule(ReturnTypeFieldOnly, ReturnTypeField, EndStatement),
      rule(ParameterField, DotDeclareIdentifier, Type),
      rule(ParameterFieldOnly, ParameterField, EndStatement),
      rule(ParameterFieldOptionalName, DotDeclareIdentifier, Type),
      rule(ParameterFieldOptionalName, Type),
      rule(ParameterFieldOptionalNameOnly, ParameterFieldOptionalName, EndStatement),
      
//      rule(IfMore, COMPOUND_ELSEIF, OptionalComm ent, AfterIf),
//      rule(IfMore, COMPOUND_ELSE),
//      rule(OptionalComment),
//      rule(OptionalComment, DUMMY_COMMENT, OptionalComment)
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
          if (p.to.get(n) == Symbol.IfMore)
            System.out.println("Statement");
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
          if (p.to.get(p.to.size()-1) == Symbol.IfMore)
            System.out.println("Statement");
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
    if (parsingTable.get(from).get(token) == p)
      return;
    
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
      out.println(";");
      generateSymbolBooleanTest(out, "isTerminal", sym -> sym.isTerminal);
      generateSymbolBooleanTest(out, "isWide", sym -> sym.isWide());
      generateSymbolBooleanTest(out, "isInline", sym -> sym.isInline());
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
      List<Symbol> sortedParsingSymbols = new ArrayList<>(parsingTable.keySet());
      sortedParsingSymbols.sort(Comparator.comparing(Symbol::name));
      for (Symbol stackTop: sortedParsingSymbols)
      {
        Map<Symbol, Production> nextTokenProduction = parsingTable.get(stackTop);
        out.println("\t\tparsingTable.put(Symbol." + stackTop.name() + ", new HashMap<>());");
        List<Symbol> sortedNextTokens = new ArrayList<>(nextTokenProduction.keySet());
        sortedNextTokens.sort(Comparator.comparing(Symbol::name));
        for (Symbol nextToken: sortedNextTokens)
        {
          Production expansion = nextTokenProduction.get(nextToken);
          out.print("\t\tparsingTable.get(Symbol." + stackTop.name() + ").put(Symbol." + nextToken.name() + ", new Symbol[] {");
          for (Symbol to: expansion.to)
          {
            out.print("Symbol." + to.name() +", ");
          }
          out.println("});");
        }
      }
      out.println("\t}");
      
      out.println("}");
    }

    try (PrintWriter out = new PrintWriter(new File(dir, "Rule.java"), "UTF-8"))
    {
      out.println("package org.programmingbasics.plom.core.ast.gen;");
      out.println("");
      out.println("import java.util.Arrays;"); 
      out.println("import java.util.List;"); 
      out.println();
      out.println("public class Rule");
      out.println("{");
      for (Production p: grammar)
      {
        out.print("\tpublic final static List<Symbol> ");
        out.print(p.from.name());
        for (Symbol s: p.to)
          out.print("_" + s.name());
        out.print(" = Arrays.asList(");
        out.print("Symbol." + p.from.name());
        for (Symbol s: p.to)
          out.print(", Symbol." + s.name());
        out.println(");");
      }
      for (Symbol s: Symbol.values())
      {
        if (!s.isTerminal) continue;
        out.print("\tpublic final static List<Symbol> ");
        out.print(s.name());
        out.print(" = Arrays.asList(");
        out.print("Symbol." + s.name());
        out.println(");");
      }
      out.println("}");
    }

  }
  
  private void generateSymbolBooleanTest(PrintWriter out, String fnName, Function<Symbol, Boolean> test)
  {
    out.println("\tpublic boolean " + fnName + "()");
    out.println("\t{");
    out.println("\t\tswitch(this) {");
    for (Symbol sym: Symbol.values())
    {
      if (test.apply(sym))
        out.println("\t\tcase " + sym.name() + ":");
    }
    out.println("\t\t\treturn true;");
    out.println("\t\tdefault:");
    out.println("\t\t\treturn false;");
    out.println("\t\t}");
    out.println("\t}");
  }

  public void go(File dir) throws IOException
  {
    calculateFirsts();
    calculateFollows();
//    printBigMap(followsTerminals);
    createLLParsingTable();
    generateFiles(dir);
  }

  private void printBigMap(Map<Symbol, Set<Symbol>> map)
  {
    System.out.println("{");
    for (Map.Entry<Symbol, Set<Symbol>> entry: map.entrySet())
      System.out.println("  " + entry.getKey() + " : " + entry.getValue());
    System.out.println("}");
  }

  public static void main(String [] args) throws IOException
  {
    new PlomAstGen().go(new File("../plom-core/src/main/java/org/programmingbasics/plom/core/ast/gen"));
  }
}