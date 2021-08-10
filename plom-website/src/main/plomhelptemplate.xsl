<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat"/> 

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
		}
		.plomarg {
			border-bottom: 1px solid black;
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
</xsl:template>

<!-- Sets up the JavaScript needed to start each tutorial code panel -->
<xsl:template match="plom-tutorial-code-panel" mode="js">
	var repo = makeRepositoryWithStdLib();
	var codePanel = new org.programmingbasics.plom.core.CodePanel(document.getElementById("<xsl:value-of select="generate-id()"/>"), true);
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
	/*
    if (code != null)
      codePanel.setCode(code);
    else
	  */
    codePanel.setCode(new org.programmingbasics.plom.core.ast.StatementContainer());
	window.addEventListener('resize', function() {
		codePanel.updateAfterResize();
	});
    
    
]]>	  
</xsl:template>

<!-- Sets up the CSS needed for each tutorial code panel -->
<xsl:template match="plom-tutorial-code-panel" mode="css">
	<style>
		#<xsl:value-of select="generate-id()"/> {
			max-width: 30em;
		}
		#<xsl:value-of select="generate-id()"/> .codemain {
			height: auto;
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
	</style>
</xsl:template>


</xsl:stylesheet>
