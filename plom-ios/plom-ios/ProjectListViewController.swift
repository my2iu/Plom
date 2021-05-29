//
//  ProjectListViewController.swift
//  plom-ios
//

import UIKit
import MobileCoreServices

class ProjectListViewController: ViewController, UITableViewDataSource, UITableViewDelegate, CreateNewProjectProtocol {
    
    @IBOutlet weak var tableView: UITableView!
    
    var projectNamesList : [String] = []
    var projectBookmarksList : [Data] = []
    
    let PROJECT_NAMES_KEY = "project names"
    let PROJECT_BOOKMARKS_KEY = "project bookmarks"
    
    override func viewDidLoad() {
        navigationController?.navigationBar.prefersLargeTitles = true
        
        projectNamesList = UserDefaults.standard.array(forKey: PROJECT_NAMES_KEY) as? [String] ?? []
        projectBookmarksList = UserDefaults.standard.array(forKey: PROJECT_BOOKMARKS_KEY) as? [Data] ?? []
    }
    
    // For filling the table view
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return projectNamesList.count + 1;
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        var cell: UITableViewCell
        if indexPath.item == 0 {
            cell = tableView.dequeueReusableCell(withIdentifier: "AddButtonTableItem", for: indexPath)
        } else {
            cell = tableView.dequeueReusableCell(withIdentifier: "ProjectTableItem", for: indexPath)
            cell.textLabel?.text = projectNamesList[indexPath.item - 1]
        }
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        if indexPath.item > 0 {
            do {
                // Move the project to the head of the list
                let name = projectNamesList.remove(at:indexPath.item - 1)
                let bookmark = projectBookmarksList.remove(at:indexPath.item - 1)
                projectNamesList.insert(name, at: 0)
                projectBookmarksList.insert(bookmark, at: 0)
                saveProjectListToUserDefaults()
                tableView.reloadData()
                
                // Show the project
                projectNameForSegue = name
                var isStale = true
                projectUrlForSegue = try URL(resolvingBookmarkData: bookmark, bookmarkDataIsStale: &isStale)
                if !isStale {
                    performSegue(withIdentifier: "PlomView", sender: self)
                } else {
                    // Should remove the project since the URL is not valid
                }
               
            } catch {
                
            }
        }
    }

    var projectNameForSegue: String?
    var projectUrlForSegue: URL?
    
    @IBAction func addNewProjectPressed() {
        performSegue(withIdentifier: "ShowNewProjectDialog", sender: self)
    }
    
    func createProject(name: String, url: URL) {
        do {
            // Check if a project of the same name already exists
            if projectNamesList.contains(where: {$0.lowercased() == name.lowercased()}) {
                let alert = UIAlertController(title: "Project already exists", message: "A project with the same name already exists", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                alert.popoverPresentationController?.sourceView = tableView
                self.present(alert, animated: true, completion: nil)
                return
            }
            
            // Add the project to the list of projects
            let bookmark = try url.bookmarkData(options: .withSecurityScope, includingResourceValuesForKeys: nil, relativeTo: nil)
            projectNamesList.insert(name, at:0)
            projectBookmarksList.insert(bookmark, at:0)
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
        UserDefaults.standard.set(projectNamesList, forKey: PROJECT_NAMES_KEY)
        UserDefaults.standard.set(projectBookmarksList, forKey: PROJECT_BOOKMARKS_KEY)
    }
}

protocol CreateNewProjectProtocol {
    func createProject(name: String, url: URL)
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
        if nameField.text?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true {
            let alert = UIAlertController(title: "Missing project name", message: "Please provide a name for the project", preferredStyle: .actionSheet)
            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
            alert.popoverPresentationController?.sourceView = nameField
            self.present(alert, animated: true, completion: nil)
            return
        }
        let url : URL
        if externalProjectFolder == nil {
            url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("projects").appendingPathComponent(nameField.text!)
            // Make sure the project doesn't already exist
//            if FileManager.default.fileExists(atPath: url.path) {
//                let alert = UIAlertController(title: "Project name already in use", message: "Please provide a different name for the project", preferredStyle: .actionSheet)
//                alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
//                alert.popoverPresentationController?.sourceView = nameField
//                self.present(alert, animated: true, completion: nil)
//                return
//            }
            // Create a project directory
            do {
                try FileManager.default.createDirectory(at: url, withIntermediateDirectories: true, attributes: nil)
            } catch {
                self.dismiss(animated: true, completion: nil)
            }
        } else {
            url = externalProjectFolder!
        }
        self.dismiss(animated: true, completion: nil)
        
        newProjectCallback?.createProject(name: nameField.text!, url: url)
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
