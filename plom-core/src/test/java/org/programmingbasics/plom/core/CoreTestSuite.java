package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.LL1ParserTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class CoreTestSuite extends TestSuite
{
   // GWT Maven plugin requires tests to be organized in test suites
   public static Test suite() 
   {
      TestSuite suite = new TestSuite();
      suite.addTestSuite(LL1ParserTest.class);
      return suite;
   }

}
