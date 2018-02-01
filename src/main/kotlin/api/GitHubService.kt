package api

import http.Service
import http.TargetService
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Call

object GithubResponses {
    data class Result(val login: String)
}

interface GitHubService {

    @TargetService(Service.Github)
    @GET("users/{user}")
    fun getUser(@Path("user") user: String): Call<GithubResponses.Result>

    companion object {
        fun create(baseUrl: String): GitHubService {
            return ApiService.create(GitHubService::class.java, baseUrl)
        }

    }
}