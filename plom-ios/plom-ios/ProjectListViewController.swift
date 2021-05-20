//
//  ProjectListViewController.swift
//  plom-ios
//

import UIKit
import MobileCoreServices

class ProjectListViewController: ViewController, UITableViewDataSource, UITableViewDelegate, CreateNewProjectProtocol {
    
    @IBOutlet weak var tableView: UITableView!
    
    override func viewDidLoad() {
        navigationController?.navigationBar.prefersLargeTitles = true

    }
    
    // For filling the table view
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 3;
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        var cell: UITableViewCell
        if indexPath.item == 0 {
            cell = tableView.dequeueReusableCell(withIdentifier: "AddButtonTableItem", for: indexPath)
        } else {
            cell = tableView.dequeueReusableCell(withIdentifier: "ProjectTableItem", for: indexPath)
        }
//        if fontFileNames.isEmpty {
//            cell.textLabel!.text = "No external fonts have been added"
//            cell.detailTextLabel!.text = ""
//            return cell;
//        }
//        cell.textLabel!.text = fontFileNames[indexPath.item]
//        let metadata: [NSDictionary] = fontFileMetadata[fontBookmarkData[indexPath.item]]!
//        let fontsFound = metadata.map( {$0.object(forKey: "fullName") as! String})
//        cell.detailTextLabel!.text = fontsFound.joined(separator: ", ")
        return cell
    }

    var projectNameForSegue: String?
    var projectUrlForSegue: URL?
    
    @IBAction func addNewProjectPressed() {
        performSegue(withIdentifier: "ShowNewProjectDialog", sender: self)
    }
    
    func createProject(name: String, url: URL) {
        projectNameForSegue = name
        projectUrlForSegue = url
        performSegue(withIdentifier: "PlomView", sender: self)
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
