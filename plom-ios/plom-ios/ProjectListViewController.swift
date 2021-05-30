//
//  ProjectListViewController.swift
//  plom-ios
//

import UIKit
import MobileCoreServices

class ProjectListViewController: ViewController, UITableViewDataSource, UITableViewDelegate, CreateNewProjectProtocol {
    
    @IBOutlet weak var tableView: UITableView!
    
    var projects : [ProjectDescription] = []
    
    let PROJECT_PREFS_KEY = "project list"
    
    override func viewDidLoad() {
        navigationController?.navigationBar.prefersLargeTitles = true
        
        let savedProjects = UserDefaults.standard.array(forKey: PROJECT_PREFS_KEY) ?? []
        for entry in savedProjects {
            let proj = entry as! [String: Any]
            let name = proj["name"] as! String
            let bookmark = proj["url"] as! Data
            let managed = proj["managed"] as! Int
            projects.append(ProjectDescription(name: name, urlBookmark: bookmark, isManagedByPlom: managed != 0))
        }
    }
    
    // For filling the table view
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return projects.count + 1;
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        var cell: UITableViewCell
        if indexPath.item == 0 {
            cell = tableView.dequeueReusableCell(withIdentifier: "AddButtonTableItem", for: indexPath)
        } else {
            let projectCel = tableView.dequeueReusableCell(withIdentifier: "ProjectTableItem", for: indexPath) as! ProjectTableItem
            projectCel.titleLabel?.text = projects[indexPath.item - 1].name
            projectCel.moreButton.tag = indexPath.item - 1
            cell = projectCel
        }
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        if indexPath.item > 0 {
            do {
                // Move the project to the head of the list
                let proj = projects.remove(at:indexPath.item - 1)
                projects.insert(proj, at: 0)
                saveProjectListToUserDefaults()
                tableView.reloadData()
                
                // Show the project
                projectNameForSegue = proj.name
                var isStale = true
                projectUrlForSegue = try URL(resolvingBookmarkData: proj.urlBookmark, bookmarkDataIsStale: &isStale)
                if !isStale {
                    performSegue(withIdentifier: "PlomView", sender: self)
                } else {
                    // Should remove the project since the URL is not valid
                }
               
            } catch {
                
            }
        }
    }

    @IBAction func projectItemMorePressed(_ sender: UIButton) {
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        let index = sender.tag
        let proj = projects[index]
        let bookmark = proj.urlBookmark
        var isStale = true
        let url = try? URL(resolvingBookmarkData: bookmark, bookmarkDataIsStale: &isStale)
        if proj.isManagedByPlom {
            alert.addAction(UIAlertAction(title: "Delete", style: .default, handler: {_ in
                do {
                    try FileManager.default.removeItem(atPath: url!.path)
                } catch {
                    // Swallow the error
                }
                self.projects.remove(at: index)
                self.saveProjectListToUserDefaults()
                self.tableView.reloadData()
            } ))
        } else {
            alert.addAction(UIAlertAction(title: "Remove", style: .default, handler: {_ in
                self.projects.remove(at: index)
                self.saveProjectListToUserDefaults()
                self.tableView.reloadData()
            }))
        }
        alert.popoverPresentationController?.sourceView = sender
        self.present(alert, animated: true, completion: nil)
    }
    
    var projectNameForSegue: String?
    var projectUrlForSegue: URL?
    
    @IBAction func addNewProjectPressed() {
        performSegue(withIdentifier: "ShowNewProjectDialog", sender: self)
    }
    
    func createProject(name: String, url: URL, isExternal: Bool) {
        do {
            // Check if a project of the same name already exists
            if projects.contains(where: {$0.name.lowercased() == name.lowercased()}) {
                let alert = UIAlertController(title: "Project already exists", message: "A project with the same name already exists", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                alert.popoverPresentationController?.sourceView = tableView
                self.present(alert, animated: true, completion: nil)
                return
            }
            
            // Add the project to the list of projects
#if os(iOS)
            let bookmark = try url.bookmarkData(options: .minimalBookmark, includingResourceValuesForKeys: nil, relativeTo: nil)
#else
            let bookmark = try url.bookmarkData(options: .withSecurityScope, includingResourceValuesForKeys: nil, relativeTo: nil)
#endif
            projects.insert(ProjectDescription(name: name, urlBookmark: bookmark, isManagedByPlom: !isExternal), at: 0)
            saveProjectListToUserDefaults()
            tableView.reloadData()

            // Show the project view
            projectNameForSegue = name
            projectUrlForSegue = url
            performSegue(withIdentifier: "PlomView", sender: self)
        } catch {
            
        }
        
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "ShowNewProjectDialog" {
            (segue.destination as! NewProjectViewController).newProjectCallback = self
        } else if segue.identifier == "PlomView" {
            let plomView = (segue.destination as! PlomViewController)
            plomView.projectName = projectNameForSegue!
            plomView.projectUrl = projectUrlForSegue!
        }
    }
    
    func saveProjectListToUserDefaults() {
        var savedProjects: [Any] = []
        for proj in projects {
            var entry : [String:Any] = [:]
            entry["name"] = proj.name
            entry["url"] = proj.urlBookmark
            entry["managed"] = (proj.isManagedByPlom ? 1 : 0)
            savedProjects.append(entry)
        }
        UserDefaults.standard.set(savedProjects, forKey: PROJECT_PREFS_KEY)
    }
}

class ProjectTableItem : UITableViewCell {
    @IBOutlet weak var titleLabel : UILabel!
    @IBOutlet weak var moreButton : UIButton!
}

struct ProjectDescription {
//    init(name: String, url: URL, isManagedByPlom: Bool)
    let name: String
    let urlBookmark: Data
    let isManagedByPlom: Bool
}

protocol CreateNewProjectProtocol {
    func createProject(name: String, url: URL, isExternal: Bool)
}

class NewProjectViewController : UIViewController, UIDocumentPickerDelegate {
    @IBOutlet weak var navigationBar: UINavigationBar!
    @IBOutlet weak var nameField: UITextField!
    @IBOutlet weak var useExistingFolderSwitch: UISwitch!
    @IBOutlet weak var existingFolderView: UIView!
    @IBOutlet weak var existingFolderName: UILabel!

    var doneButton: UIBarButtonItem?;
    
    var newProjectCallback : CreateNewProjectProtocol?
    
    override func viewDidLoad() {
        navigationBar.items = [navigationItem]
        let backButton = UIBarButtonItem.init(title: "Cancel", style: .plain, target: self, action: #selector(cancelPressed))
        navigationItem.leftBarButtonItem = backButton
        
        doneButton = UIBarButtonItem.init(title: "Done", style: .plain, target: self, action: #selector(donePressed))
        navigationItem.rightBarButtonItem = doneButton
        update()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        nameField.becomeFirstResponder()
    }

    func update() {
        if let url = externalProjectFolder {
            existingFolderName.text = url.lastPathComponent
            existingFolderView.isHidden = false
        } else {
            existingFolderView.isHidden = true
        }
    }
    

    @objc func cancelPressed(sender: UIBarButtonItem) {
        self.dismiss(animated: true, completion: nil)
    }

    @objc func donePressed(sender: UIBarButtonItem) {
        // Make sure the name field isn't blank
        let name = nameField.text?.trimmingCharacters(in: .whitespacesAndNewlines)
        if name?.isEmpty ?? true {
            let alert = UIAlertController(title: "Missing project name", message: "Please provide a name for the project", preferredStyle: .actionSheet)
            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
            alert.popoverPresentationController?.sourceView = nameField
            self.present(alert, animated: true, completion: nil)
            return
        }
        let url : URL
        if externalProjectFolder == nil {
            url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("projects").appendingPathComponent(name!)
            // Create a project directory
            do {
                try FileManager.default.createDirectory(at: url, withIntermediateDirectories: true, attributes: nil)
            } catch {
                self.dismiss(animated: true, completion: nil)
                return
            }
        } else {
            url = externalProjectFolder!
        }
        self.dismiss(animated: true, completion: nil)
        
        newProjectCallback?.createProject(name: name!, url: url, isExternal: externalProjectFolder != nil)
    }
    
    var externalProjectFolder : URL?
    
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        externalProjectFolder = urls[0]
        self.useExistingFolderSwitch.setOn(true, animated: false)
        update()
    }
    
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        externalProjectFolder = nil
        self.useExistingFolderSwitch.setOn(false, animated: false)
        update()
    }
    
    @IBAction func useExistingFolderSwitchToggled() {
        if useExistingFolderSwitch.isOn {
            let documentPicker =
                UIDocumentPickerViewController(documentTypes: [kUTTypeFolder as String], in: .open)
            documentPicker.delegate = self
            documentPicker.modalPresentationStyle = .popover
            documentPicker.popoverPresentationController?.sourceView = useExistingFolderSwitch
            present(documentPicker, animated: true, completion: nil)
            
        } else {
            externalProjectFolder = nil
            update()
        }
        
    }
}
