/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation.compose

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavGraphBuilder
import androidx.navigation.get
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.TestNavigator
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCurrentBackStackEntrySetGraph() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        composeTestRule.setContent {
            val navController = TestNavHostController(AmbientContext.current)

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.arguments?.getString(KEY_ROUTE))
            .isEqualTo(FIRST_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryNavigate() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = TestNavHostController(AmbientContext.current)

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.arguments?.getString(KEY_ROUTE))
            .isEqualTo(FIRST_DESTINATION)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
        }

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.arguments?.getString(KEY_ROUTE))
            .isEqualTo(SECOND_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryPop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(AmbientContext.current)

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        composeTestRule.runOnUiThread {
            navController.setCurrentDestination(SECOND_DESTINATION)
            navController.popBackStack()
        }

        assertWithMessage("the currentBackStackEntry should return to first destination after pop")
            .that(currentBackStackEntry.value?.arguments?.getString(KEY_ROUTE))
            .isEqualTo(FIRST_DESTINATION)
    }

    @Test
    fun testNavigateThenNavigateWithPop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        val navigator = TestNavigator()
        composeTestRule.setContent {
            navController = TestNavHostController(AmbientContext.current)
            navController.navigatorProvider.addNavigator(navigator)

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.arguments?.getString(KEY_ROUTE))
            .isEqualTo(FIRST_DESTINATION)
        assertThat(navigator.backStack.size).isEqualTo(1)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION) {
                popUpTo("first") { inclusive = true }
            }
        }

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.arguments?.getString(KEY_ROUTE))
            .isEqualTo(SECOND_DESTINATION)
        assertWithMessage("the second destination should be the only one on the back stack")
            .that(navigator.backStack.size)
            .isEqualTo(1)
    }

    @Test
    fun testNavigateOptionSingleTop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        val navigator = TestNavigator()
        composeTestRule.setContent {
            navController = TestNavHostController(AmbientContext.current)
            navController.navigatorProvider.addNavigator(navigator)

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.arguments?.getString(KEY_ROUTE))
            .isEqualTo(FIRST_DESTINATION)
        assertThat(navigator.backStack.size).isEqualTo(1)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
        }

        assertWithMessage("there should be 2 destinations on the back stack after navigate")
            .that(navigator.backStack.size)
            .isEqualTo(2)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION) {
                launchSingleTop = true
            }
        }

        assertWithMessage("there should be 2 destination on back stack when using singleTop")
            .that(navigator.backStack.size)
            .isEqualTo(2)
    }
}

internal inline fun NavGraphBuilder.test(
    route: String,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit = { deepLink(createRoute(route)) }
) = destination(
    NavDestinationBuilder<NavDestination>(
        provider["test"],
        createRoute(route).hashCode()
    ).apply(builder).apply { argument(KEY_ROUTE) { defaultValue = route } }
)

internal fun TestNavHostController.setCurrentDestination(
    route: String
) = setCurrentDestination(createRoute(route).hashCode())

private const val FIRST_DESTINATION = "first"
private const val SECOND_DESTINATION = "second"
