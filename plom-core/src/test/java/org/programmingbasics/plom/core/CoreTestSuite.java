package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.LL1ParserTest;
import org.programmingbasics.plom.core.view.EraseLeftTest;
import org.programmingbasics.plom.core.view.HitDetectTest;
import org.programmingbasics.plom.core.view.InsertNewLineTest;
import org.programmingbasics.plom.core.view.InsertTokenTest;
import org.programmingbasics.plom.core.view.ParseContextTest;

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
      suite.addTestSuite(ParseContextTest.class);
      suite.addTestSuite(EraseLeftTest.class);
      suite.addTestSuite(HitDetectTest.class);
      return suite;
   }

}
