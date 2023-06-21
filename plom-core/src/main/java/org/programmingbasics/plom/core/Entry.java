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
- if no constructor is defined on a class, inherit the parent's constructors 
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
- when editing methods, automatic advance to next field when pressing enter
- when editing methods, name validation
- when editing methods, should have a menu with delete and insert, not just a delete button
- when editing methods, have button to clear an input field?
- when editing methods, there should be a better indication that input fields are editable?
- when editing methods, too much space between part name and ":"
- when editing methods, not handling blur of type fields properly
- when editing methods, type fields should use new svg rendering
- when editing methods, have a quick erase button for auto-resizing text input?


TODO list from TOJam 2023
- [ ] New project in external folder doesn’t copy external files or other files on iOS
- [ ] iOS showing stuff outside web/ folder in extra files
- [ ] Open project directories straight from web version
- [ ] Send logging messages and crash messages back to show in console or elsewhere
- [ ] Clear console messages
- [ ] On iOS, you possibly have to double tap to get copy and erase buttons of selections to register
- [ ] Should throw an error if you don’t chain your constructors—especially to @object.new
- [ ] Wkwebview not being cleaned when exiting (after exiting and reentering to save a few times, there will be a lot of WKWebViews hanging around that the Safari debugger can find)
- [ ] Run error shows the line but not the file
- [ ] Escape the HTML put in strings
- [ ] Warning when nothing returned from method when something expected
- [ ] Auto fill of lambda function args strips out the leading . from arg name
- [ ] If there is a problem with a method signature (e.g. unknown return type because of unknown class name) then flag it in UI
- [ ] If there is a problem with a variable declaration (e.g. unknown type due to unknown class name) then flag it in UI
- [ ] If there is a parse error in ParseToAst, especially deep in a lambda, there is no way to report where in the code the error was found and then to propagate that onwards
- [ ] Don’t need people to click on OK after changing method signatures
- [ ] RepositoryScope unboundtypetotype should log errors when constructing types
- [ ] Are event handlers (jsobject from function) logging errors?
- [ ] Are methods listed alphabetically in suggestions?
- [ ] Android version should filter out .git folders in extra files (or any dir starting with .)?
- [ ] Method return type and extends should use proper code widget
- [ ] When hooking in project with an existing dir in Android (possibly empty project), it copies in the wrong stuff
- [ ] No warning when using member variables before calling super constructor in constructor
- [ ] Comment out code
- [ ] Code suggestions should be case insensitive
- [ ] Generic  JS object wrap and unwrap Plom objects
- [ ] Super doesn’t work for calling non-static methods
- [ ] No suggestions for instance methods after calling a constructor
- [ ] Better handling of big tablets where it’s a pain to reach around the screen to press buttons
- [ ] In method signature editor, when editing a name part, pressing enter should move to the argument part
- [ ] Line after an if with lambdas might have line height too short maybe if I can find an example again
- [ ] Lambdas with a long function signature part sometimes has wrapping problems where the return type of the function signature wraps to an incorrect location
- [ ] Allow extra blank lines in saved Plom files in case of hand editing
- [ ] List of method suggestions doesn’t list everything available in a class if there are a lot of methods available
- [ ] When typing, suggestion list should scroll back to top if you’ve scrolled it a bit before looking for something
- [ ] Undo support
- [ ] Specify UTF8 in index.html to ensure that .js files also default to UTF8?
- [ ] Possible problem with resources loading in out of order in compiled .js (if something is delayed, then it doesn’t load correctly?)
- [ ] Slow performance on slow Android phones maybe with larger projects?
- [ ] Need break and continue commands
- [ ] Iterator for @array
- [ ] Need an iOS icon, maybe a phone with code on it (curly braces? Or 5 lines with some indented)
- [ ] Add meta viewport tag to default index.html
- [ ] Empty return in @void method not allowed
- [ ] Increase size of minimize web output button so that you don’t accidentally keep hitting the hamburger menu instead
- [ ] A separate @JSObject.from function: and @JSObject.from top level event handler: so that debugging hooks can be inserted properly
- [ ] Back button in upper-left for android

- SimpleInterpreter should allow a method be given as the entrypoint (or special entrypoint code that invokes main) so that function names are preserved for debugging
- Remove the VariableDescription stuff since it's no longer used and it's confusing

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