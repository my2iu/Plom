<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat" indent="no"/> 

<xsl:include href="basetemplate.xsl"/>

<xsl:template match="plombutton"><span class="plombutton"><xsl:apply-templates/></span></xsl:template>

<xsl:template match="plomsnippet"><span class="plomsnippet"><xsl:apply-templates/></span></xsl:template>

<xsl:template match="plomsnippet//arg"><span class="plomarg"><xsl:apply-templates/></span></xsl:template>

<xsl:template match="plomcode"><div class="plomcode"><xsl:apply-templates/></div></xsl:template>

<xsl:template match="plomcode//arg"><span class="plomarg"><xsl:apply-templates/></span></xsl:template>

<xsl:template match="plom-tutorial-code-panel"><div id="{generate-id()}"><xsl:apply-templates/></div></xsl:template>

<xsl:template match="plom-ide-head-imports">
	<link href="https://fonts.googleapis.com/css2?family=Source+Sans+Pro:wght@400;700&amp;display=swap" rel="stylesheet"/>
	<link type="text/css" rel="stylesheet" href="ide/ide.css"/>
<!-- <link type="text/css" rel="stylesheet" href="app.css">  -->
    <script type="text/javascript" language="javascript" src="ide/gwtPreload.js"></script>
    <script type="text/javascript" language="javascript" src="ide/plomcore/plomdirect.js"></script>
    <script type="text/javascript" language="javascript" src="ide/plomUi.js"></script>
</xsl:template>

<xsl:template match="plom-tutorial-code-panel-css">
	<style>
		.plombutton {
			border: 1px solid #bbb;
    		border-radius: 0.2em;
    		background: linear-gradient(0deg, #ddd, #fff);;
    		padding: 0.05em;
		}
		.plomsnippet {
			font-family: 'Source Sans Pro', sans-serif;
			font-weight: bold;
		}
		.plomcode {
			padding: 0.5em;
			background-color: #eee;
			margin-left: 1em;
			font-family: 'Source Sans Pro', sans-serif;
			line-height: 1.6;
			white-space: pre;
		}
		.plomarg {
			border-bottom: 1px solid black;
		}
		.plomtip {
			display: flow-root;
			padding: 0.5em;
			margin-left: 1em;
			margin-right: 1em;
			background: #d2f5ff;
		}
	</style>
	<xsl:apply-templates select="//plom-tutorial-code-panel" mode="css"/>
</xsl:template>

<xsl:template match="plom-tutorial-code-panel-js">
	<script type="text/javascript" language="javascript"><![CDATA[
		addGwtOnLoad(function() {
			setupPlomUi();
			]]>
			<xsl:apply-templates select="//plom-tutorial-code-panel" mode="js"/>
		<![CDATA[
			/*
			var main = new org.programmingbasics.plom.core.Main();
			initRepository(main);
			//main.go();
    		hookRun(main);
    		hookLoadSave(main);
    		main.hookSubject();
    		window.addEventListener('resize', function() {
    			main.updateAfterResize();
    		});
    		main.loadFunctionCodeView("main");
			*/
		});

	]]></script>
	<xsl:apply-templates select="//plom-tutorial-code-panel" mode="codefragments"/>
</xsl:template>

<!-- Sets up the JavaScript needed to start each tutorial code panel -->
<xsl:template match="plom-tutorial-code-panel" mode="js">
	var repo = makeRepositoryWithStdLib();
	var codePanel = new org.programmingbasics.plom.core.CodePanel.forFullScreen(document.getElementById("<xsl:value-of select="generate-id()"/>"), true);
    codePanel.setVariableContextConfigurator(
        function(scope, coreTypes) {
          org.programmingbasics.plom.core.interpreter.StandardLibrary.createGlobals(null, scope, coreTypes);
          scope.setParent(new org.programmingbasics.plom.core.RepositoryScope(repo, coreTypes));
        },
        function(context) {
			// Assume the code panel is not in the context of a function or method, so there's
			// no class member variables or argument variables that need to be made available
			return;
        });
    <xsl:if test="@cutpaste='false'">codePanel.setHasCutAndPaste(false);</xsl:if>
    <xsl:for-each select="token-filter">
   		<xsl:if test="@exclude='control flow' or @exclude='statements' or @exclude='all' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["COMPOUND_IF", "COMPOUND_ELSEIF", "COMPOUND_WHILE", "COMPOUND_FOR"]);</xsl:if>
   		<xsl:if test="@include='control flow' or @include='statements' or @include='all' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["COMPOUND_IF", "COMPOUND_ELSEIF", "COMPOUND_WHILE", "COMPOUND_FOR"]);</xsl:if>
   		<xsl:if test="@exclude='statements' or @exclude='all' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["DUMMY_COMMENT", "Return", "Var"]);</xsl:if>
   		<xsl:if test="@include='statements' or @include='all' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["DUMMY_COMMENT", "Return", "Var"]);</xsl:if>
   		<xsl:if test="@exclude='math operators' or @exclude='all' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["Plus", "Minus", "Multiply", "Divide"]);</xsl:if>
   		<xsl:if test="@include='math operators' or @include='all' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["Plus", "Minus", "Multiply", "Divide"]);</xsl:if>
   		<xsl:if test="@exclude='boolean operators' or @exclude='all' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["Lt", "Gt", "Le", "Ge", "Eq", "Ne", "Or", "And"]);</xsl:if>
   		<xsl:if test="@include='boolean operators' or @include='all' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["Lt", "Gt", "Le", "Ge", "Eq", "Ne", "Or", "And"]);</xsl:if>
   		<xsl:if test="@exclude='simple literals' or @exclude='all' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["String", "Number"]);</xsl:if>
   		<xsl:if test="@include='simple literals' or @include='all' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["String", "Number"]);</xsl:if>
   		<xsl:if test="@exclude='boolean literals' or @exclude='all' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["TrueLiteral", "FalseLiteral"]);</xsl:if>
   		<xsl:if test="@include='boolean literals' or @include='all' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["TrueLiteral", "FalseLiteral"]);</xsl:if>
   		<xsl:if test="@exclude='all' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["AtType", "This", "Super", "NullLiteral", "Is", "As", "Retype", "OpenParenthesis", "DotVariable", "ClosedParenthesis", "Assignment", "FunctionLiteral", "FunctionTypeName"]);</xsl:if>
   		<xsl:if test="@include='all' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["AtType", "This", "Super", "NullLiteral", "Is", "As", "Retype", "OpenParenthesis", "DotVariable", "ClosedParenthesis", "Assignment", "FunctionLiteral", "FunctionTypeName"]);</xsl:if>
   		<xsl:if test="@exclude='variable identifiers' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["DotVariable"]);</xsl:if>
   		<xsl:if test="@include='variable identifiers' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["DotVariable"]);</xsl:if>
   		<xsl:if test="@exclude='identifiers' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["AtType", "DotVariable"]);</xsl:if>
   		<xsl:if test="@include='identifiers' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["AtType", "DotVariable"]);</xsl:if>
		<xsl:if test="@exclude='assign' ">org.programmingbasics.plom.core.Main.jsCodePanelMoreExcludedTokens(codePanel, ["Assignment"]);</xsl:if>
   		<xsl:if test="@include='assign' ">org.programmingbasics.plom.core.Main.jsCodePanelFewerExcludedTokens(codePanel, ["Assignment"]);</xsl:if>

    </xsl:for-each>
    var QuickSuggestion = org.programmingbasics.plom.core.CodeWidgetBase.QuickSuggestion;
    codePanel.setQuickSuggestions(org.programmingbasics.plom.core.Main.jsMakeListFromArray([
	    <xsl:for-each select="suggestion">
	    	new QuickSuggestion("<xsl:value-of select="@token"/>", "<xsl:value-of select="@token"/>")<xsl:if test="count(following-sibling::suggestion) &gt; 0">,</xsl:if>
	    </xsl:for-each>
    ]));
<![CDATA[
    codePanel.setListener(function(isCodeChanged) {
	  /*
      if (isCodeChanged)
      {
        // Update error list
        codePanel.codeErrors.clear();
        try {
          ParseToAst.parseStatementContainer(codePanel.codeList, codePanel.codeErrors);
        }
        catch (Exception e)
        {
          // No errors should be thrown
        }
        // Update line numbers
        lineNumbers.calculateLineNumbersForStatements(codePanel.codeList, 1);
      }
      if (codePanel.cursorPos != null)
      {
        int lineNo = LineForPosition.inCode(codePanel.codeList, codePanel.cursorPos, lineNumbers);
       Element lineEl = Browser.getDocument().querySelector(".lineIndicator");
        lineEl.setTextContent("L" + lineNo);
      }
	  */
    });
]]>
    <xsl:choose>
    	<xsl:when test="code">
			var PlomTextReader = org.programmingbasics.plom.core.ast.PlomTextReader;
    		var inStream = new PlomTextReader.StringTextReader(document.getElementById('plom-code-fragment-<xsl:value-of select="generate-id()"/>').textContent);
			var lexer = new PlomTextReader.PlomTextScanner(inStream);
			codePanel.setCode(PlomTextReader.readStatementContainer(lexer));
    	</xsl:when>
    	<xsl:otherwise>
			codePanel.setCode(new org.programmingbasics.plom.core.ast.StatementContainer());
    	</xsl:otherwise>
    </xsl:choose>
    
	window.addEventListener('resize', function() {
		codePanel.updateAfterResize();
	});
</xsl:template>

<!-- Sets up the CSS needed for each tutorial code panel -->
<xsl:template match="plom-tutorial-code-panel" mode="css">
	<style>
		#<xsl:value-of select="generate-id()"/> {
			max-width: 30em;
		}
		#<xsl:value-of select="generate-id()"/> .codemain {
			height: auto;
			max-height: 80vh;
		}
		#<xsl:value-of select="generate-id()"/> .codemain .codesidesplit {
			<xsl:choose>
				<xsl:when test="@lines">
					height: <xsl:value-of select="1.5 + 1.5 * @lines"/>em;
				</xsl:when>
				<xsl:otherwise>
					min-height: 10em;
					height: 50vh;
				</xsl:otherwise>
			</xsl:choose>
		}
		#<xsl:value-of select="generate-id()"/> .codemain .choices {
			overflow: auto;
			<xsl:choose>
				<xsl:when test="@choicelines">
					height: <xsl:value-of select="2 + 2 * @choicelines"/>em;
				</xsl:when>
				<xsl:otherwise>
					height: auto;
					max-height: 10em;
				</xsl:otherwise>
			</xsl:choose>
		}
		#<xsl:value-of select="generate-id()"/> .codemain .simpleentry {
			<xsl:choose>
				<xsl:when test="@choicelines">
					height: <xsl:value-of select="2 + 2 * @choicelines"/>em;
				</xsl:when>
				<xsl:otherwise></xsl:otherwise>
			</xsl:choose>
		}
		#<xsl:value-of select="generate-id()"/> .codesvg svg {
			<xsl:choose>
				<xsl:when test="@choicelines">
					padding-bottom: <xsl:value-of select="1.5 + 1.5 * @lines"/>em;
				</xsl:when>
				<xsl:otherwise></xsl:otherwise>
			</xsl:choose>
		}
	</style>
</xsl:template>

<xsl:template match="plom-tutorial-code-panel[code]" mode="codefragments">
	<script type="plom" id="plom-code-fragment-{generate-id()}"><xsl:value-of select="code"/></script>
</xsl:template>

<xsl:template match="plom-tip">
	<!-- Better to use a different icon? &#128161; lightbulb, &#129300; thinking face, &#128173; thought balloon, &#128466; spiral notepad -->
	<div class="plomtip"><div style="float: left; font-size: 200%; vertical-align: top; line-height: 1; padding: 0 0.25em 0.25em 0;">&#128161;</div><xsl:apply-templates /></div>
</xsl:template>


</xsl:stylesheet>
