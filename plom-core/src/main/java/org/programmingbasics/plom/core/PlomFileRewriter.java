package org.programmingbasics.plom.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.programmingbasics.plom.core.codestore.ModuleCodeRepository;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomTextScanner;
import org.programmingbasics.plom.core.ast.PlomTextReader.StringTextReader;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;

import com.google.gwt.core.shared.GwtIncompatible;

/**
 * When the file format for Plom files change, I need to load all the
 * Plom files in the old format and rewrite them in the new format
 *
 */
@GwtIncompatible
public class PlomFileRewriter
{
  private static void rewriteFile(File f) throws IOException, PlomReadException
  {
    // Recurse
    if (f.isDirectory())
    {
      File []subfiles = f.listFiles();
      for (File sub: subfiles)
      {
        if (sub.isDirectory())
          rewriteFile(sub);
        else if (sub.isFile() && sub.getName().endsWith(".plom"))
          rewriteFile(sub);
      }
      return;
    }
    // Rewrite any files
    if (f.getName().equals("program.plom"))
    {
      // Assume it's a module
      System.out.println("Rewriting " + f);
      ModuleCodeRepository repo = new ModuleCodeRepository();
      repo.loadModulePlain(lexerForFile(f), null);
      StringBuilder text = new StringBuilder();
      PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(text);
      repo.saveModule(out, true);
      Files.write(f.toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
    }
    else
    {
      System.out.println("Rewriting " + f);
      // Assume it's a class
      ClassDescription c = ModuleCodeRepository.loadClass(lexerForFile(f));
      StringBuilder text = new StringBuilder();
      PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(text);
      ModuleCodeRepository.saveClass(out, c);
      Files.write(f.toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  private static PlomTextScanner lexerForFile(File file) throws IOException
  {
    return new PlomTextScanner(new StringTextReader(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)));
  }

  private static void rewriteFiles(File... files) throws IOException, PlomReadException
  {
    for (File f: files)
      rewriteFile(f);
  }
  
  public static void main(String [] args) throws IOException, PlomReadException
  {
    rewriteFiles(
//        new File("../plom-android/app/src/main/assets/templates"),
//        new File("../plom-ios/html/templates"),
        new File("../plom-stdlib/src")
        );
  }
}
