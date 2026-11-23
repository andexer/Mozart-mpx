package com.mozartmpx.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TrackTechInfo(
    val id: String,
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val channels: Int
)

@Dao
interface TrackDao {
    @Query(
        """
        SELECT * FROM tracks
        WHERE id NOT IN (SELECT trackId FROM hidden_tracks)
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE isFavorite = 1 AND id NOT IN (SELECT trackId FROM hidden_tracks)
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun observeFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT id FROM tracks WHERE isFavorite = 1")
    suspend fun getFavoriteTrackIds(): List<String>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("UPDATE tracks SET sampleRateHz = :sampleRateHz, bitrateKbps = :bitrateKbps, channels = :channels WHERE id = :trackId")
    suspend fun updateTechMetadata(trackId: String, sampleRateHz: Int, bitrateKbps: Int, channels: Int)

    @Query("SELECT id, sampleRateHz, bitrateKbps, channels FROM tracks WHERE sourceType = 'mediastore'")
    suspend fun getMediaStoreTech(): List<TrackTechInfo>

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: String)

    @Query("DELETE FROM tracks WHERE sourceType = 'mediastore'")
    suspend fun clearMediaStoreTracks()

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: String, isFavorite: Boolean)

    // Permanently hidden tracks management
    @Query("SELECT * FROM hidden_tracks ORDER BY hiddenAt DESC")
    fun observeHiddenTracks(): Flow<List<HiddenTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideTrack(entry: HiddenTrackEntity)

    @Query("DELETE FROM hidden_tracks WHERE trackId = :trackId")
    suspend fun unhideTrack(trackId: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId ASC, position ASC")
    fun observeAllPlaylistRefs(): Flow<List<PlaylistTrackCrossRef>>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
          AND t.id NOT IN (SELECT trackId FROM hidden_tracks)
        ORDER BY pt.position ASC
        """
    )
    fun observeTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, description = :description WHERE id = :playlistId")
    suspend fun updatePlaylist(playlistId: Long, name: String, description: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE trackId = :trackId")
    suspend fun removeTrackFromAllPlaylists(trackId: String)
}

@Dao
interface HistoryAffinityDao {
    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN (
            SELECT trackId, MAX(playedAt) as latestPlay
            FROM play_history
            GROUP BY trackId
        ) h ON t.id = h.trackId
        WHERE t.id NOT IN (SELECT trackId FROM hidden_tracks)
        ORDER BY h.latestPlay DESC
        LIMIT 20
        """
    )
    fun observeRecentlyPlayed(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN (
            SELECT trackId, COUNT(*) as playCount
            FROM play_history
            WHERE completed = 1
            GROUP BY trackId
        ) h ON t.id = h.trackId
        WHERE t.id NOT IN (SELECT trackId FROM hidden_tracks)
        ORDER BY h.playCount DESC
        LIMIT 30
        """
    )
    fun observeMostPlayed(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: PlayHistoryEntity)

    @Query("DELETE FROM play_history WHERE trackId = :trackId")
    suspend fun deleteHistoryForTrack(trackId: String)

    @Query("DELETE FROM play_history WHERE playedAt < :before")
    suspend fun pruneHistory(before: Long)

    @Query("SELECT * FROM taste_affinity")
    fun observeAllAffinities(): Flow<List<TasteAffinityEntity>>

    @Query("SELECT * FROM taste_affinity WHERE `key` = :key LIMIT 1")
    suspend fun getAffinity(key: String): TasteAffinityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAffinity(entity: TasteAffinityEntity)
}

@Dao
interface EqualizerDao {
    @Query("SELECT * FROM custom_eq_presets")
    fun observeCustomPresets(): Flow<List<CustomEqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomPreset(preset: CustomEqPresetEntity)
}
