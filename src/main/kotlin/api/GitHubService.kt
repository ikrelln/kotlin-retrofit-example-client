package api

import http.Service
import http.TargetService
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Call

object GithubResponses {
    data class UserInfo(val login: String, val repos_url: String)
    data class Repo(val id: Int, val name: String)
}

interface GitHubService {

    @TargetService(Service.Github)
    @GET("users/{user}")
    fun getUser(@Path("user") user: String): Call<GithubResponses.UserInfo>

    @TargetService(Service.Github)
    @GET("users/{user}/repos")
    fun getUserRepos(@Path("user") user: String): Call<List<GithubResponses.Repo>>

    companion object {
        fun create(baseUrl: String): GitHubService {
            return ApiService.create(GitHubService::class.java, baseUrl)
        }

    }
}