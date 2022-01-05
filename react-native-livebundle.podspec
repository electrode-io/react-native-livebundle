require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-livebundle"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  react-native-livebundle
                   DESC
  s.homepage     = "https://github.com/electrode-io/react-native-livebundle"
  s.license      = { :type => "Apache-2.0", :file => "LICENSE" }
  s.authors      = { "Your Name" => "yourname@email.com" }
  s.platforms    = { :ios => "9.0" }
  s.source       = { :git => "https://github.com/electrode-io/react-native-livebundle.git", :tag => "v#{s.version}" }

  s.source_files = "ios/**/*.{h,c,m,swift}"
  s.requires_arc = true

  s.dependency "React"
  s.dependency "SSZipArchive", "~> 2.2.3"
end
