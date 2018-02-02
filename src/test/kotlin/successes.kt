import api.GitHubService
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import org.testng.annotations.AfterClass
import org.apache.logging.log4j.ThreadContext
import tests.Step
import tests.TestBase
import java.util.*

class SuccessfulTests: TestBase() {

    @BeforeClass(alwaysRun = true)
    fun testSetUp() {
        ThreadContext.put("testName", "Before Class")
    }

    @AfterClass(alwaysRun = true)
    fun testTearDown() {
        ThreadContext.put("testName", "After Class")
    }

    @Test
    fun canGetUserFromGithub() {
        val userResponse = Step("call github to get user").focus {
            GitHubService.create("https://api.github.com/").getUser("ikrelln").execute()
        }
        Step("intermediate check result").verification {
            assert(userResponse.code() == 200)
            assert(userResponse.body()?.login == "ikrelln")
        }
        val repoResponse = Step("call github to get user's repos").focus {
            GitHubService.create("https://api.github.com/").getUserRepos("ikrelln").execute()
        }
        Step("check number of repositories of user").verification {
            assert(repoResponse.code() == 200)
            assert((repoResponse.body()?.count() ?: 0) > 1)
        }
    }

    @Test
    fun canAddOddNumbers() {
        val a = Step("getting a random odd number").setup {
            val randValue = Random().nextInt(50000)
            randValue * 2 + 1
        }
        val b = Step("getting a random odd number").setup {
            val randValue = Random().nextInt(50000)
            randValue * 2 + 1
        }

        val result = Step("adding both odd numbers").focus { a + b }

        Step("verify that result is even").verification {
            assert(result % 2 == 0)
        }
    }

}

