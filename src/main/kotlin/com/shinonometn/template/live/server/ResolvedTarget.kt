package com.shinonometn.template.live.server

sealed class ResolvedTarget(val pathString: String) {
    class Template(pathString: String) : ResolvedTarget(pathString)
    class File(pathString: String) : ResolvedTarget(pathString)
}