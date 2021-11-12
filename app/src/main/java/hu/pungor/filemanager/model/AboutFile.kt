package hu.pungor.filemanager.model

class AboutFile(
    var name: String,
    val info: String,
    val path: String,
    val mimeType: String,
    var selected: Boolean
)