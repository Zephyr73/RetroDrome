package code.name.monkey.retromusic.network.subsonic

import code.name.monkey.retromusic.network.subsonic.model.SubsonicResponseWrapper
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface SubsonicApi {
    @GET("rest/ping.view")
    suspend fun ping(): SubsonicResponseWrapper

    @GET("rest/getArtists.view")
    suspend fun getArtists(): SubsonicResponseWrapper

    @GET("rest/getArtist.view")
    suspend fun getArtist(
        @Query("id") id: String
    ): SubsonicResponseWrapper

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("id") id: String
    ): SubsonicResponseWrapper

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String = "alphabeticalByName",
        @Query("size") size: Int = 500,
        @Query("offset") offset: Int = 0
    ): SubsonicResponseWrapper

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 50
    ): SubsonicResponseWrapper

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(): SubsonicResponseWrapper

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(
        @Query("id") id: String
    ): SubsonicResponseWrapper

    @GET("rest/getRandomSongs.view")
    suspend fun getRandomSongs(
        @Query("size") size: Int = 500
    ): SubsonicResponseWrapper

    @GET("rest/getGenres.view")
    suspend fun getGenres(): SubsonicResponseWrapper

    @GET("rest/getSongsByGenre.view")
    suspend fun getSongsByGenre(
        @Query("genre") genre: String,
        @Query("count") count: Int = 500,
        @Query("offset") offset: Int = 0
    ): SubsonicResponseWrapper

    @GET("rest/getStarred2.view")
    suspend fun getStarred2(): SubsonicResponseWrapper

    @GET("rest/star.view")
    suspend fun star(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null
    ): SubsonicResponseWrapper

    @GET("rest/unstar.view")
    suspend fun unstar(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null
    ): SubsonicResponseWrapper

    @GET("rest/scrobble.view")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("submission") submission: Boolean = true
    ): SubsonicResponseWrapper

    @GET("rest/getSong.view")
    suspend fun getSong(
        @Query("id") id: String
    ): SubsonicResponseWrapper
}
