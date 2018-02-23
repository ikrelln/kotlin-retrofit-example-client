import api.GitHubService
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import org.testng.annotations.AfterClass
import org.apache.logging.log4j.ThreadContext
import tests.Step
import tests.TestBase
import java.util.*

class RandomTests: TestBase() {

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
        val response = Step("call github").focus {
            GitHubService.create("https://api.github.com/").getUser("hibuvgyjhgvhguhgfy").execute()
        }

        if (Random().nextBoolean()) {
            Step("check result").verification {
                assert(response.code() == 200)
            }
        } else {
            Step("check result").verification {
                assert(response.code() == 404)
            }
        }
    }

    @Test
    fun canAddEvenNumbers() {
        val a = Step("getting a random even number").setup {
            val randValue = Random().nextInt(50000)
            randValue * 2
        }
        val b = Step("getting a random even number").setup {
            val randValue = Random().nextInt(50000)
            randValue * 2
        }

        val result = Step("adding both even numbers").focus { a + b }

        if (Random().nextBoolean()) {
            Step("verify that result is odd").verification {
                assert(result % 2 == 1)
            }
        } else {
            Step("verify that result is even").verification {
                assert(result % 2 == 0)
            }
        }
    }

}

