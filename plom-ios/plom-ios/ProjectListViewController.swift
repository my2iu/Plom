//
//  ProjectListViewController.swift
//  plom-ios
//

import UIKit

class ProjectListViewController: ViewController, UITableViewDataSource, UITableViewDelegate, CreateNewProjectProtocol {
    
    @IBOutlet weak var tableView: UITableView!
    
    override func viewDidLoad() {
//        navigationItem.largeTitleDisplayMode = .always
//        navigationItem.title = "Plom Projects"
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

    @IBAction func addNewProjectPressed() {
        performSegue(withIdentifier: "ShowNewProjectDialog", sender: self)
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "ShowNewProjectDialog" {
            (segue.destination as! NewProjectViewController).newProjectCallback = self
        }
    }
}

protocol CreateNewProjectProtocol {
    
}

class NewProjectViewController : UIViewController {
    @IBOutlet weak var navigationBar: UINavigationBar!
    var newProjectCallback : CreateNewProjectProtocol?
    
    @objc func cancelPressed(sender: UIBarButtonItem) {
        self.dismiss(animated: true, completion: nil)
    }
    
    override func viewDidLoad() {
        navigationBar.items = [navigationItem]
        let backButton = UIBarButtonItem.init(title: "Cancel", style: .plain, target: self, action: #selector(cancelPressed))
        navigationItem.leftBarButtonItem = backButton
        
        
    }
}
