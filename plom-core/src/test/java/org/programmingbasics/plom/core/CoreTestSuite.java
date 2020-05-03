package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.LL1ParserTest;
import org.programmingbasics.plom.core.view.InsertNewLineTest;
import org.programmingbasics.plom.core.view.InsertTokenTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class CoreTestSuite extends TestSuite
{
   // GWT Maven plugin requires tests to be organized in test suites
   public static Test suite() 
   {
      TestSuite suite = new TestSuite();
      suite.addTestSuite(LL1ParserTest.class);
      suite.addTestSuite(InsertNewLineTest.class);
      suite.addTestSuite(InsertTokenTest.class);
      return suite;
   }

}
