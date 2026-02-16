package org.example.bass.bassController

sealed class trackSource {

    data class album(val albumKey: String) : trackSource()

    data class playlist(val playlistId: Long) : trackSource()

    companion object {

        fun fromString(value: String): trackSource? {

            return when {

                value.startsWith("_album:") -> {
                    // value = "_album:AlbumName::Year"
                    album(value)
                }

                value.startsWith("_playlist:") -> {
                    // value = "_playlist:Name::123"
                    val id = value.substringAfterLast("::")
                        .toLongOrNull()
                        ?: return null

                    playlist(id)
                }

                else -> null
            }
        }


    }
}
