import SwiftUI
import ComposeApp

@main
struct SimpMusicApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeRoot().ignoresSafeArea()
                .onOpenURL { IosDeepLinksKt.handleIosUrl(url: $0.absoluteString) }
        }
    }
}

struct ComposeRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController()
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: "--playback-smoke-test"), index + 1 < arguments.count {
            IosPlaybackSmokeTestKt.startIosPlaybackSmokeTest(url: arguments[index + 1])
        }
        #endif
        return controller
    }
    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
