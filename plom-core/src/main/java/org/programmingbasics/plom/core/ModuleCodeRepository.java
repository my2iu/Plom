package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
import org.programmingbasics.plom.core.interpreter.ExecutableFunction;
import org.programmingbasics.plom.core.interpreter.Type;

public class ModuleCodeRepository
{
  public static class FunctionSignature
  {
    public List<String> nameParts = new ArrayList<>();
    public List<String> argNames = new ArrayList<>();
    public List<Token.ParameterToken> argTypes = new ArrayList<>();
    public Token.ParameterToken returnType;
    public String getLookupName()
    {
      String name = "";
      for (int n = 0; n < nameParts.size(); n++)
      {
        name += nameParts.get(n);
        if (!argNames.isEmpty())
          name += ":";
      }
      return name;
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name)
    {
      return from(returnType, Arrays.asList(name.split(":")), new ArrayList<>(), new ArrayList<>());
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name), Arrays.asList(arg1Type));
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type, String arg2Name, Token.ParameterToken arg2Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name, arg2Name), Arrays.asList(arg1Type, arg2Type));
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type, String arg2Name, Token.ParameterToken arg2Type, String arg3Name, Token.ParameterToken arg3Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name, arg2Name, arg3Name), Arrays.asList(arg1Type, arg2Type, arg3Type));
    }
    public static FunctionSignature from(Token.ParameterToken returnType, List<String> nameParts, List<String> argNames, List<Token.ParameterToken> argTypes)
    {
      FunctionSignature sig = new FunctionSignature();
      sig.returnType = returnType;
      sig.nameParts = nameParts;
      sig.argNames = argNames;
      sig.argTypes = argTypes;
      return sig;
    }
  }
  
  public static class FunctionDescription
  {
    public FunctionDescription(FunctionSignature sig, StatementContainer code)
    {
      this.sig = sig;
      this.code = code;
    }
    public FunctionSignature sig;
    public StatementContainer code;
  }
  
  private Map<String, FunctionDescription> functions = new HashMap<>(); 
  
  public ModuleCodeRepository()
  {
    // Create a basic main function that can be filled in
    FunctionDescription func = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "main"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("var", Symbol.Var),
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken(":", Symbol.Colon),
                Token.ParameterToken.fromContents("@string", Symbol.AtType)
                ),
            new TokenContainer(
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken(":=", Symbol.Assignment),
                Token.ParameterToken.fromContents(".input:", Symbol.DotVariable,
                    new TokenContainer(new Token.SimpleToken("\"Guess a number between 1 and 10\"", Symbol.String)))
                ),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken("=", Symbol.Eq),
                        new Token.SimpleToken("\"8\"", Symbol.String)
                        ),
                    new StatementContainer(
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                                new TokenContainer(
                                    new Token.SimpleToken("\"You guessed correctly\"", Symbol.String)
                                    ))
                            ))
                    ),
                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
                    new StatementContainer(
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                                new TokenContainer(
                                    new Token.SimpleToken("\"Incorrect\"", Symbol.String)
                                    ))
                            ))
                    )
                )
            )
        );
    functions.put(func.sig.getLookupName(), func);
    
    FunctionDescription testParamFunc = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@number", Symbol.AtType), Arrays.asList("test"), Arrays.asList("arg1"), Arrays.asList(Token.ParameterToken.fromContents("@number", Symbol.AtType))),
        new StatementContainer());
    functions.put(testParamFunc.sig.getLookupName(), testParamFunc);
    
    FunctionDescription printStringPrimitive = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "print string:", "value", Token.ParameterToken.fromContents("@string", Symbol.AtType)),
        new StatementContainer(
            new TokenContainer(
                new Token.WideToken("// Prints a string to the screen", Symbol.DUMMY_COMMENT),
                new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))
            ));
    functions.put(printStringPrimitive.sig.getLookupName(), printStringPrimitive);

    FunctionDescription printPrimitive = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "print:", "value", Token.ParameterToken.fromContents("@object", Symbol.AtType)),
        new StatementContainer(
            new TokenContainer(
                new Token.WideToken("// Prints a value to the screen", Symbol.DUMMY_COMMENT),
                Token.ParameterToken.fromContents(".print string:", Symbol.DotVariable, 
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".value", Symbol.DotVariable),
                        Token.ParameterToken.fromContents(".to string", Symbol.DotVariable))
                    )
            )));
    functions.put(printPrimitive.sig.getLookupName(), printPrimitive);

    FunctionDescription inputPrimitive = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@string", Symbol.AtType), "input:", "prompt", Token.ParameterToken.fromContents("@string", Symbol.AtType)),
        new StatementContainer(
            new TokenContainer(
                new Token.WideToken("// Displays a prompt asking for input and returns the value entered by the user", Symbol.DUMMY_COMMENT),
                new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))
            ));
    functions.put(inputPrimitive.sig.getLookupName(), inputPrimitive);

  }
  
  public FunctionDescription getFunctionDescription(String name)
  {
    return functions.get(name);
  }
  
  public boolean hasFunctionWithName(String name)
  {
    return functions.containsKey(name);
  }
  
  public void addFunction(FunctionDescription func)
  {
    functions.put(func.sig.getLookupName(), func);
  }
  
  public List<String> getAllFunctions()
  {
    List<String> names = new ArrayList<>(functions.keySet());
    names.sort(Comparator.naturalOrder());
    return names;
  }
  
  public void changeFunctionSignature(FunctionSignature newSig, FunctionSignature oldSig)
  {
    FunctionDescription func = functions.remove(oldSig.getLookupName());
    func.sig = newSig;
    functions.put(func.sig.getLookupName(), func);
  }
}
