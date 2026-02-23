package org.example.ui.screens.searchingAlgorithm

import org.example.audioindex.ScannedAudio
import org.example.folderGetter.Playlist
import org.example.similarity

fun matchesQuery(query: String, album: ScannedAudio): Boolean {
    if (query.isBlank()) return true

    val words = query
        .lowercase()
        .split(" ")
        .filter { it.isNotBlank() }

    val fields = listOf(
        album.album,
        album.artist,
        album.year
    ).map { it.lowercase() }

    return words.all { word ->
        fields.any { field -> field.contains(word) }
    }
}

fun albumScore(query: String, album: ScannedAudio): Float {
    val q = query.lowercase()
    return maxOf(
        similarity(q, album.album.lowercase()),
        similarity(q, album.artist.lowercase())
    )
}

fun matchesQueryPlaylist(query: String, playlist: Playlist): Boolean {
    if (query.isBlank()) return true

    val words = query
        .lowercase()
        .split(" ")
        .filter { it.isNotBlank() }

    val fields = listOf(
        playlist.name
    ).map { it.lowercase() }

    return words.all { word ->
        fields.any { field -> field.contains(word) }
    }
}