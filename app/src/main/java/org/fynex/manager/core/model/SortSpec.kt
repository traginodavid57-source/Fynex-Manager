package org.fynex.manager.core.model

enum class SortField(val displayName: String) {
    NAME("Nome"),
    DATE("Data"),
    SIZE("Tamanho"),
    TYPE("Tipo")
}

data class SortSpec(
    val field: SortField = SortField.NAME,
    val ascending: Boolean = true,
    val foldersFirst: Boolean = true
) {
    fun sort(items: List<FileItem>): List<FileItem> {
        val comparator = Comparator<FileItem> { a, b ->
            if (foldersFirst && a.isDirectory != b.isDirectory) {
                return@Comparator if (a.isDirectory) -1 else 1
            }
            val res = when (field) {
                SortField.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                SortField.DATE -> a.lastModified.compareTo(b.lastModified)
                SortField.SIZE -> a.size.compareTo(b.size)
                SortField.TYPE -> a.extension.compareTo(b.extension, ignoreCase = true)
            }
            if (ascending) res else -res
        }
        return items.sortedWith(comparator)
    }
}
