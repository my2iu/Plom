//
//  ProjectListViewController.swift
//  plom-ios
//

import UIKit

class ProjectListViewController: ViewController, UITableViewDataSource, UITableViewDelegate {
    
    @IBOutlet weak var navigationBar: UINavigationBar!
    @IBOutlet weak var tableView: UITableView!
    
    override func viewDidLoad() {
        navigationBar.prefersLargeTitles = true
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
        print("hi")
    }
}

