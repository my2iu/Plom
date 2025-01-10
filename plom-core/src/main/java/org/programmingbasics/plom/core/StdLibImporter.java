package org.programmingbasics.plom.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomTextScanner;
import org.programmingbasics.plom.core.ast.PlomTextReader.StringTextReader;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;

import com.google.gwt.core.shared.GwtIncompatible;

/**
 * Utility for reading the code for the standard library and merging it into a single
 * file.
 */

@GwtIncompatible
public class StdLibImporter
{
  public static void main(String [] args)
  {
    Path stdlibBasePath = Paths.get("../plom-stdlib/src");
    CodeRepositoryClient repo = new CodeRepositoryClient();
    
    try {
      // Load in the main module describing the global variables and functions
      repo.loadModule(lexerForFile(stdlibBasePath.resolve("program.plom")));

      // Load in all the code from the various files for different classes
      Files.list(stdlibBasePath).forEach((subpath) -> {
        if (subpath.endsWith("program.plom")) return;
        try
        {
          repo.loadClassIntoModule(lexerForFile(subpath));
        }
        catch (PlomReadException e)
        {
          throw new IllegalArgumentException("Problem reading class file", e);
        }
        catch (IOException e)
        {
          throw new IllegalArgumentException("Problem reading class file", e);
        }
      });
      
      // Fix up @object so that it doesn't have a parent
      for (ClassDescription cls: repo.getAllClassesSorted())
      {
        if (cls.getName().equals("object"))
          cls.parent = null;
      }
      
      // Write out the code in a single file
      StringBuilder singleFileText = new StringBuilder();
      PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(singleFileText);
      repo.saveModule(out, true);
      Files.write(Paths.get("src/main/resources/org/programmingbasics/plom/core/stdlib.plom"), singleFileText.toString().getBytes(StandardCharsets.UTF_8));
    } 
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (PlomReadException e)
    {
      e.printStackTrace();
    }
  }
  
  private static PlomTextScanner lexerForFile(Path file) throws IOException
  {
    return new PlomTextScanner(new StringTextReader(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
  }
  
}
