package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.LL1ParserTest;
import org.programmingbasics.plom.core.ast.LineNumberTrackerTest;
import org.programmingbasics.plom.core.ast.ParseToAstTest;
import org.programmingbasics.plom.core.ast.PlomTextReaderTest;
import org.programmingbasics.plom.core.ast.PlomTextWriterTest;
import org.programmingbasics.plom.core.interpreter.ExpressionEvaluatorTest;
import org.programmingbasics.plom.core.interpreter.MachineContextTest;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreterTest;
import org.programmingbasics.plom.core.interpreter.StandardLibraryTest;
import org.programmingbasics.plom.core.view.CodeFragmentExtractorTest;
import org.programmingbasics.plom.core.view.CodeNestingCounterTest;
import org.programmingbasics.plom.core.view.CodePositionTest;
import org.programmingbasics.plom.core.view.EraseLeftTest;
import org.programmingbasics.plom.core.view.EraseSelectionTest;
import org.programmingbasics.plom.core.view.GatherCodeCompletionInfoTest;
import org.programmingbasics.plom.core.view.HitDetectTest;
import org.programmingbasics.plom.core.view.InsertNewLineTest;
import org.programmingbasics.plom.core.view.InsertTokenTest;
import org.programmingbasics.plom.core.view.LineForPositionTest;
import org.programmingbasics.plom.core.view.NextPositionTest;
import org.programmingbasics.plom.core.view.ParseContextTest;
import org.programmingbasics.plom.core.view.RenderedCursorPositionTest;
import org.programmingbasics.plom.core.view.SvgCodeRendererTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class CoreTestSuite extends TestSuite
{
   // GWT Maven plugin requires tests to be organized in test suites
   public static Test suite() 
   {
      TestSuite suite = new TestSuite();
      suite.addTestSuite(ModuleCodeRepositoryTest.class);
      suite.addTestSuite(LL1ParserTest.class);
      suite.addTestSuite(ParseToAstTest.class);
      suite.addTestSuite(LineNumberTrackerTest.class);
      suite.addTestSuite(PlomTextReaderTest.class);
      suite.addTestSuite(PlomTextWriterTest.class);
      suite.addTestSuite(MachineContextTest.class);
      suite.addTestSuite(ExpressionEvaluatorTest.class);
      suite.addTestSuite(SimpleInterpreterTest.class);
      suite.addTestSuite(StandardLibraryTest.class);
      suite.addTestSuite(InsertNewLineTest.class);
      suite.addTestSuite(InsertTokenTest.class);
      suite.addTestSuite(ParseContextTest.class);
      suite.addTestSuite(EraseLeftTest.class);
      suite.addTestSuite(HitDetectTest.class);
      suite.addTestSuite(NextPositionTest.class);
      suite.addTestSuite(GatherCodeCompletionInfoTest.class);
      suite.addTestSuite(CodeNestingCounterTest.class);
      suite.addTestSuite(LineForPositionTest.class);
      suite.addTestSuite(CodePositionTest.class);
      suite.addTestSuite(RenderedCursorPositionTest.class);
      suite.addTestSuite(CodeFragmentExtractorTest.class);
      suite.addTestSuite(EraseSelectionTest.class);
      suite.addTestSuite(SvgCodeRendererTest.class);
      return suite;
   }

}
