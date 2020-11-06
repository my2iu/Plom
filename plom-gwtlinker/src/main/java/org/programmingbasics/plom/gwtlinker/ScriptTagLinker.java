/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.programmingbasics.plom.gwtlinker;

import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SoftPermutation;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.core.linker.CrossSiteIframeLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

/**
 * Microsoft Edge seems to have problems debugging JS imported using
 * GWT's usual linker, so this is a more straight-forward linker that
 * uses a script tag to import things.
 * 
 * This linker uses an iframe to hold the code and a script tag to download the
 * code. It can download code cross-site, because it uses a script tag to
 * download it and because it never uses XHR. The iframe, meanwhile, makes it
 * trivial to install additional code as the app runs.
 */
@LinkerOrder(Order.PRIMARY)
@Shardable
public class ScriptTagLinker extends CrossSiteIframeLinker {
  @Override
  public String getDescription() {
    return "ScriptTag";
  }

  static class CompilationResultWrapper extends CompilationResult
  {
    CompilationResultWrapper(CompilationResult wrapped) {
      super(wrapped.getLinker());
      this.wrapped = wrapped;
      strongName = wrapped.getStrongName();
    }
    CompilationResult wrapped;
    String strongName;
    @Override public String[] getJavaScript() { return wrapped.getJavaScript(); }
    @Override public int getPermutationId() { return wrapped.getPermutationId(); }
    @Override public SortedSet<SortedMap<SelectionProperty, String>> getPropertyMap() { return wrapped.getPropertyMap(); }
    @Override public SoftPermutation[] getSoftPermutations() { return wrapped.getSoftPermutations(); }
    @Override public String getStrongName() { return strongName; }
    @Override public SymbolData[] getSymbolMap() { return wrapped.getSymbolMap(); }
    @Override public StatementRanges[] getStatementRanges() { return wrapped.getStatementRanges(); }
  }
  @Override
  protected Collection<Artifact<?>> doEmitCompilation(TreeLogger logger,
      LinkerContext context, CompilationResult result, ArtifactSet artifacts)
      throws UnableToCompleteException {
    CompilationResultWrapper wrapper = new CompilationResultWrapper(result);
    // wrapper.strongName = "0";
    return super.doEmitCompilation(logger, context, wrapper, artifacts);
  }

  /**
   * Returns the name of the {@code ComputeScriptBase} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/computeScriptBase.js"}.
   *
   * @param context a LinkerContext
   */
//  protected String getJsComputeScriptBase(LinkerContext context) {
//    return getStringConfigurationProperty(context, "computeScriptBaseJs",
//        "com/wobastic/gwt/linker/scripttag/EmptyTemplate.js");
//  }


  /**
   * Returns the name of the {@code JsInstallLocation} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/installLocationIframe.js"}.
   *
   * @param context a LinkerContext
   */
//  protected String getJsInstallLocation(LinkerContext context) {
//    return "com/wobastic/gwt/linker/scripttag/EmptyTemplate.js";
//  }

  /**
   * Returns the name of the {@code JsInstallScript} script.  The default is chosen
   * based on the value of {@link #shouldInstallCode}.
   *
   * <p> If you override this, verify that {@link #shouldInstallCode} is
   * set consistently or fragment loading won't work.
   */
//  protected String getJsInstallScript(LinkerContext context) {
//    return "com/wobastic/gwt/linker/scripttag/EmptyTemplate.js";
//  }


  /**
   * Returns the name of the {@code JsLoadExternalStylesheets} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/loadExternalStylesheets.js"}.
   *
   * @param context a LinkerContext
   */
//  protected String getJsLoadExternalStylesheets(LinkerContext context) {
//    return "com/wobastic/gwt/linker/scripttag/EmptyTemplate.js";
//  }


  /**
   * Returns the name of the {@code JsProcessMetas} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/processMetas.js"}.
   *
   * @param context a LinkerContext
   */
//  protected String getJsProcessMetas(LinkerContext context) {
//    return "com/wobastic/gwt/linker/scripttag/EmptyTemplate.js";
//  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context, String strongName)
    throws UnableToCompleteException {
    TextOutput out = new DefaultTextOutput(context.isOutputCompact());

    // Note: this code is included in both the primary fragment and devmode.js

    // $wnd is the main window that the GWT code will affect and also the
    // location where the bootstrap function was defined. In iframe-based linkers,
    // $wnd is set to window.parent. Usually, in others, $wnd = window.
    // By default, $wnd is not set when the module starts, but a replacement for
    // installLocationIframe.js may set it.

    out.print("(function() {");
    out.print("var $wnd = $wnd || window.parent;");
    out.newlineOpt();
    out.print("var __gwtModuleFunction = $wnd." + context.getModuleFunctionName() + ";");
    out.newlineOpt();
    out.print("if (!__gwtModuleFunction) {");
    out.newlineOpt();
    out.print("__gwtModuleFunction = {");
    out.newlineOpt();
    out.print("__sendStats:function(a,b){},");
    out.newlineOpt();
    out.print("__moduleStartupDone:function(a){}");
    out.newlineOpt();
    out.print("};");
    out.newlineOpt();
    out.print("}");
    out.newlineOpt();
    out.print("var $sendStats = __gwtModuleFunction.__sendStats;");
    out.newlineOpt();
    out.print("$sendStats('moduleStartup', 'moduleEvalStart');");
    out.newlineOpt();
    out.print("var $gwt_version = \"" + About.getGwtVersionNum() + "\";");
    out.newlineOpt();
    out.print("var $strongName = '" + strongName + "';");
    out.newlineOpt();
    out.print("var $gwt = {};");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();

    // The functions for runAsync are set up in the bootstrap script so they
    // can be overridden in the same way as other bootstrap code is, however
    // they will be called from, and expected to run in the scope of the GWT code
    // (usually an iframe) so, here we set up those pointers.
    out.print("function __gwtStartLoadingFragment(frag) {");
    out.newlineOpt();
    String fragDir = getFragmentSubdir(logger, context) + '/';
    out.print("var fragFile = '" + fragDir + "' + $strongName + '/' + frag + '" + FRAGMENT_EXTENSION + "';");
    out.newlineOpt();
    out.print("return __gwtModuleFunction.__startLoadingFragment(fragFile);");
    out.newlineOpt();
    out.print("}");
    out.newlineOpt();
    out.print("function __gwtInstallCode(code) {return __gwtModuleFunction.__installRunAsyncCode(code);}");
    out.newlineOpt();

    // The functions for property access are set up in the bootstrap script however
    // they will be called from, and expected to run in the scope of the GWT code
    // (usually an iframe) so, here we set up those pointers.
    out.print("function __gwt_isKnownPropertyValue(propName, propValue) {");
    out.newlineOpt();
    out.print("return __gwtModuleFunction.__gwt_isKnownPropertyValue(propName, propValue);");
    out.newlineOpt();
    out.print("}");
    out.newlineOpt();
    out.print("function __gwt_getMetaProperty(name) {");
    out.newlineOpt();
    out.print("return __gwtModuleFunction.__gwt_getMetaProperty(name);");
    out.newlineOpt();
    out.print("}");
    out.newlineOpt();

    // Even though we call the $sendStats function in the code written in this
    // linker, some of the compilation code still needs the $stats and
    // $sessionId
    // variables to be available.
    out.print("var $stats = $wnd.__gwtStatsEvent ? function(a) {");
    out.newlineOpt();
    out.print("return $wnd.__gwtStatsEvent && $wnd.__gwtStatsEvent(a);");
    out.newlineOpt();
    out.print("} : null;");
    out.newlineOpt();
    out.print("var $sessionId = $wnd.__gwtStatsSessionId ? $wnd.__gwtStatsSessionId : null;");
    out.newlineOpt();

    return out.toString();
  }

  @Override
  protected String getModuleSuffix2(TreeLogger logger, LinkerContext context,
      String strongName) {

    // Note: this method won't be called if getModuleSuffix() is overridden and returns non-null.
    // Note: this code is included in both the primary fragment and devmode.js.

    DefaultTextOutput out = new DefaultTextOutput(context.isOutputCompact());

    out.print("$sendStats('moduleStartup', 'moduleEvalEnd');");
    out.newlineOpt();
    out.print("gwtOnLoad(" + "__gwtModuleFunction.__errFn, " + "__gwtModuleFunction.__moduleName, "
        + "__gwtModuleFunction.__moduleBase, " + "__gwtModuleFunction.__softPermutationId,"
        + "__gwtModuleFunction.__computePropValue);");
    out.newlineOpt();
    out.print("$sendStats('moduleStartup', 'end');");
    out.newlineOpt();
    out.print("$gwt && $gwt.permProps && __gwtModuleFunction.__moduleStartupDone($gwt.permProps);");
    out.newlineOpt();
    out.print("})();");
    
    writeMagicComments(out, context, 0, strongName);
    return out.toString();
  }

  private void writeMagicComments(DefaultTextOutput out, LinkerContext context, int fragmentId,
      String strongName) {
    String sourceMapUrl = getSourceMapUrl(context, strongName, fragmentId);
    if (sourceMapUrl != null) {
      // This magic comment determines where a browser debugger looks for a sourcemap,
      // except that it may be overridden by a "SourceMap" header in the HTTP response when
      // loading the JavaScript.
      // (Note: even if you're using the HTTP header, you still have to set this to an arbitrary
      // value, or Chrome won't enable sourcemaps.)
      out.print("\n//# sourceMappingURL=" + sourceMapUrl + " ");
    }

    // This magic comment determines the name of the JavaScript fragment in a browser debugger.
    // (In Chrome it typically shows up under "(no domain)".)
    // We need to set it explicitly because the JavaScript code may be installed via an "eval"
    // statement and even if we're not using an eval, the filename contains the strongname which
    // isn't stable across recompiles.
    out.print("\n//# sourceURL=" + context.getModuleName() + "-" + fragmentId + ".js\n");
  }

//  @Override
//  protected String getSelectionScriptTemplate(TreeLogger logger, LinkerContext context) {
//    return "com/wobastic/gwt/linker/scripttag/ScriptTagTemplate.js";
//  }

  /**
   * Determines the strategy for installing JavaScript code into the iframe.
   * If set to false, a &lt;script&gt; tag pointing to the js file is added
   * directly to the iframe. Otherwise, GWT downloads the JavaScript code
   * as a list of strings and then adds it to the iframe.
   */
  protected boolean shouldInstallCode(LinkerContext context) {
    return false;
  }

  /**
   * Returns whether to use "self" for $wnd and $doc references. Defaults to false.
   * Useful for worker threads.
   */
  protected boolean shouldUseSelfForWindowAndDocument(LinkerContext context) {
    return false;
  }
}