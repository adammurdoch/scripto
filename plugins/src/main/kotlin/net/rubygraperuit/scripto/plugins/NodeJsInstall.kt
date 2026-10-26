package net.rubygraperuit.scripto.plugins

abstract class NodeJsInstall : Download() {
    override fun url(): String {
        return "https://nodejs.org/dist/v24.22.1/node-v24.22.1-darwin-arm64.tar.gz"
    }
}