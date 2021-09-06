package org.programmingbasics.plom.core;

import com.google.gwt.core.client.EntryPoint;

import jsinterop.annotations.JsType;

/*
TODO:
- functions
- number constants (change inputmode to numeric)
- string constants (allow multi-line strings?)
- valign to middle
- keyboard movement
- keyboard entry
- adding a newline in the middle of a function call
- type checking for errors
- null and Null type
- when first creating a string/number, the cursor position should be shown as being after the token or on the token, not before it
- keep track of names of functions so that it can show up in stack traces
- default to a name of empty string to the first piece of code executed
- store function name when an error is thrown
- properly handle focus and loss of focus on type entry fields of the method panel
- do type checking of arguments of a function
- type checking of return type of a function
- object super
- constructor chaining
- separate constructor list
- call constructor from instance method
- constructors cannot return a value
- autogeneration of getter and setters for instance data members
- calls to the repository will eventually be asynchronous on iOS 
- generics
- js native API
- better formatting of saved code
- export + import projects
- github integration
- debugger
- standard library
- Android + iOS apps
- web lessons
- tutorial mode
- export as html5 program
- external images, external html
- run in a separate browser window
- transpile to JavaScript
- standardize value stuff so that functions always return and pass around direct pointers, but you are expected to copy the contents (unless you specify explicitly something else)?
- retype
- restrict grammar so that boolean expressions normally aren't allowed so as to avoid confusion between := and =
- svg render word wrap -- fix up hit testing
- svg render top of parameter in parameter token should extend to top of line
- svg render add extra space between lines
- svg render have an "active" token (not the same as selection) so you know you can edit it
- svg render for type fields
- distinguish between cursor positions that are at the end of a line or beginning of next wrapped line 
- svg render better scrolling when the keyboard deploys
- sidebar of suggestions should overlay instead of shrinking code view
- fix up scrolling when deleting classes or modifying code
- improve the breadcrumbs
- improve the class/method view
- method view and input focus when adding new args 
- deleting args doesn't close suggestions (is this a problem elsewhere?) 
- sidebar of suggestions in method view shifts text around (should absolute position itself)
- refine the hamburger menu (should automatically close itself)
- change the back button to match the platform style?
- saving should save out functions and methods in alphabetical order
- when editing methods, automatic advance to next field when pressing enter
- when editing methods, name validation
- when editing methods, should have a menu with delete and insert, not just a delete button
- when editing methods, have button to clear an input field?
- when editing methods, there should be a better indication that input fields are editable?
- when editing methods, too much space between part name and ":"
- when editing methods, not handling blur of type fields properly
- <auto-resizing-input> should have an attribute that styles things transparently 
 */

public class Entry implements EntryPoint
{
  @Override
  public void onModuleLoad()
  {
    checkGwtOnLoad();
  }

  private static native void checkGwtOnLoad() /*-{
    function executeOnLoadCode()
    {
      if ($wnd.$gwtOnLoad) {
        for (var n = 0; n < $wnd.$gwtOnLoad.length; n++)
        {
          $wnd.$gwtOnLoad[n]();
        }
        $wnd.$gwtOnLoad = [];
      }
      // Change the preloader to immediately execute any code
      $wnd.addGwtOnLoad = function(fn) {
        fn();
      }
    }
    
    // Check if we're fully loaded yet
    if ($doc.readyState == 'complete')
    {
      // Execute any onload code
      executeOnLoadCode();
    }
    else
    {
      // Wait until we're fully loaded before running any onload code
      $doc.addEventListener('readystatechange', function(e) {
        if ($doc.readyState == 'complete') 
        {
          // Execute any onload code
          executeOnLoadCode();
        }          
      });
    }
  }-*/;
}