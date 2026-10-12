/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */

package code.name.monkey.retromusic.dialogs

import android.app.Dialog
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.text.Spanned
import android.util.Log
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.EXTRA_SONG
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.DialogFileDetailsBinding
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper
import code.name.monkey.retromusic.network.subsonic.model.SubsonicSong
import code.name.monkey.retromusic.util.MusicUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import java.io.File

class SongDetailDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context: Context = requireContext()
        val binding = DialogFileDetailsBinding.inflate(layoutInflater)

        val song = BundleCompat.getParcelable(requireArguments(), EXTRA_SONG, Song::class.java)
        with(binding) {
            fileName.text = makeTextWithTitle(context, R.string.label_file_name, "-")
            filePath.text = makeTextWithTitle(context, R.string.label_file_path, "-")
            fileSize.text = makeTextWithTitle(context, R.string.label_file_size, "-")
            fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, "-")
            trackLength.text = makeTextWithTitle(context, R.string.label_track_length, "-")
            bitrate.text = makeTextWithTitle(context, R.string.label_bit_rate, "-")
            samplingRate.text = makeTextWithTitle(context, R.string.label_sampling_rate, "-")
        }

        if (song != null) {
            val songFile = File(song.data)
            if (songFile.exists()) {
                binding.fileName.text =
                    makeTextWithTitle(context, R.string.label_file_name, songFile.name)
                binding.filePath.text =
                    makeTextWithTitle(context, R.string.label_file_path, songFile.absolutePath)

                binding.dateModified.text = makeTextWithTitle(
                    context, R.string.label_last_modified,
                    MusicUtil.getDateModifiedString(songFile.lastModified())
                )

                binding.fileSize.text =
                    makeTextWithTitle(
                        context,
                        R.string.label_file_size,
                        getFileSizeString(songFile.length())
                    )
                try {

                    if (songFile.extension.equals("opus", ignoreCase = true)) {

                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(songFile.absolutePath)

                            val bitrate =
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                                    ?.toIntOrNull() ?: 0
                            val sampleRate = if (VersionUtils.hasS()) {
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                                    ?.toIntOrNull() ?: 0
                            } else 0
                            val duration =
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    ?.toLongOrNull() ?: 0L

                            binding.fileFormat.text =
                                makeTextWithTitle(context, R.string.label_file_format, "Opus")
                            binding.trackLength.text = makeTextWithTitle(
                                context,
                                R.string.label_track_length,
                                MusicUtil.getReadableDurationString(duration)
                            )
                            binding.bitrate.text = makeTextWithTitle(
                                context,
                                R.string.label_bit_rate,
                                "${bitrate / 1000} kb/s"
                            )
                            binding.samplingRate.text =
                                makeTextWithTitle(
                                    context,
                                    R.string.label_sampling_rate,
                                    "$sampleRate Hz"
                                )
                        } finally {
                            retriever.release()
                        }
                    } else {
                        // Use JAudioTagger for other formats
                        val audioFile = AudioFileIO.read(songFile)
                        val audioHeader = audioFile.audioHeader

                        binding.fileFormat.text =
                            makeTextWithTitle(context, R.string.label_file_format, audioHeader.format)
                        binding.trackLength.text = makeTextWithTitle(
                            context,
                            R.string.label_track_length,
                            MusicUtil.getReadableDurationString((audioHeader.trackLength * 1000).toLong())
                        )
                        binding.bitrate.text = makeTextWithTitle(
                            context,
                            R.string.label_bit_rate,
                            audioHeader.bitRate + " kb/s"
                        )
                        binding.samplingRate.text =
                            makeTextWithTitle(
                                context,
                                R.string.label_sampling_rate,
                                audioHeader.sampleRate + " Hz"
                            )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "error while reading the song file", e)
                    // fallback
                    binding.trackLength.text = makeTextWithTitle(
                        context,
                        R.string.label_track_length,
                        MusicUtil.getReadableDurationString(song.duration)
                    )
                }
            } else if (SubsonicMapper.isSubsonicId(song.id)) {
                val cached = SubsonicMapper.getCachedSong(song.id)
                bindServerSongDetails(binding, context, song, cached)
                lifecycleScope.launch(Dispatchers.IO) {
                    val fullSong = SubsonicMapper.getSongDetails(song.id)
                    if (fullSong != null && fullSong != cached) {
                        withContext(Dispatchers.Main) {
                            if (isAdded && !isDetached) {
                                bindServerSongDetails(binding, context, song, fullSong)
                            }
                        }
                    }
                }
            } else {
                // fallback
                binding.fileName.text =
                    makeTextWithTitle(context, R.string.label_file_name, song.title)
                binding.trackLength.text = makeTextWithTitle(
                    context,
                    R.string.label_track_length,
                    MusicUtil.getReadableDurationString(song.duration)
                )
            }
        }
        return materialDialog(R.string.action_details)
            .setPositiveButton(android.R.string.ok, null)
            .setView(binding.root)
            .create()
            .colorButtons()
    }

    private fun bindServerSongDetails(
        binding: DialogFileDetailsBinding,
        context: Context,
        song: Song,
        subSong: SubsonicSong?
    ) {
        val resolvedName = subSong?.path?.substringAfterLast('/')?.substringAfterLast('\\')
            ?: if (!subSong?.suffix.isNullOrBlank()) "${song.title}.${subSong?.suffix}" else song.title
        binding.fileName.text = makeTextWithTitle(context, R.string.label_file_name, resolvedName)

        val resolvedPath = subSong?.path
            ?: if (song.artistName.isNotBlank() && song.albumName.isNotBlank()) {
                "${song.artistName}/${song.albumName}/$resolvedName"
            } else {
                resolvedName
            }
        binding.filePath.text = makeTextWithTitle(context, R.string.label_file_path, resolvedPath)

        if (subSong?.size != null && subSong.size > 0) {
            binding.fileSize.text = makeTextWithTitle(
                context,
                R.string.label_file_size,
                getFileSizeString(subSong.size)
            )
        }

        val dateString = parseDateCreated(subSong?.created) ?: if (song.dateModified > 0) {
            MusicUtil.getDateModifiedString(song.dateModified * 1000L)
        } else null
        if (dateString != null) {
            binding.dateModified.text = makeTextWithTitle(
                context,
                R.string.label_last_modified,
                dateString
            )
        }

        val format = (subSong?.suffix ?: subSong?.contentType?.substringAfter('/') ?: "").uppercase()
        if (format.isNotBlank()) {
            binding.fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, format)
        }

        binding.trackLength.text = makeTextWithTitle(
            context,
            R.string.label_track_length,
            MusicUtil.getReadableDurationString(song.duration)
        )

        val bitRate = subSong?.effectiveBitRate
        if (bitRate != null && bitRate > 0) {
            binding.bitrate.text = makeTextWithTitle(
                context,
                R.string.label_bit_rate,
                "$bitRate kb/s"
            )
        }

        val sampleRate = subSong?.effectiveSampleRate
        if (sampleRate != null && sampleRate > 0) {
            val bitDepthStr = if (subSong.bitDepth != null && subSong.bitDepth > 0) " (${subSong.bitDepth} bit)" else ""
            binding.samplingRate.text = makeTextWithTitle(
                context,
                R.string.label_sampling_rate,
                "$sampleRate Hz$bitDepthStr"
            )
        }
    }

    private fun parseDateCreated(created: String?): String? {
        if (created.isNullOrBlank()) return null
        return try {
            val instant = java.time.Instant.parse(created)
            MusicUtil.getDateModifiedString(instant.toEpochMilli())
        } catch (e: Exception) {
            try {
                val zdt = java.time.ZonedDateTime.parse(created)
                MusicUtil.getDateModifiedString(zdt.toInstant().toEpochMilli())
            } catch (e2: Exception) {
                created.substringBefore('T')
            }
        }
    }

    companion object {

        val TAG: String = SongDetailDialog::class.java.simpleName

        fun create(song: Song): SongDetailDialog {
            return SongDetailDialog().apply {
                arguments = bundleOf(
                    EXTRA_SONG to song
                )
            }
        }

        private fun makeTextWithTitle(context: Context, titleResId: Int, text: String?): Spanned {
            return ("<b>" + context.resources.getString(titleResId) + ": " + "</b>" + text)
                .parseAsHtml()
        }

        private fun getFileSizeString(sizeInBytes: Long): String {
            val fileSizeInMB = sizeInBytes.toDouble() / (1024 * 1024)
            return if (fileSizeInMB >= 0.1) {
                String.format(java.util.Locale.getDefault(), "%.2f MB", fileSizeInMB)
            } else {
                "${sizeInBytes / 1024} KB"
            }
        }
    }
}