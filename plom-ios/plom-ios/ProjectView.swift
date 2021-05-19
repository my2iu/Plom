//
//  ProjectView.swift
//  plom-ios
//

import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationView {
                ProjectView()
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

struct ProjectView: View {
    var body: some View {
        Text("Hello, World!")
        .navigationBarTitle("Plom Projects")
    }
}

struct ProjectView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
