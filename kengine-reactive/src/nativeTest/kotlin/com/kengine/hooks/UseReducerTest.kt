package com.kengine.hooks

import com.kengine.hooks.reducer.useReducer
import com.kengine.test.expectThat
import kotlin.test.Test

class UseReducerTest {

    @Test
    fun `useReducer handles basic state transitions`() {
        val (count, dispatch) = useReducer(0) { state: Int, action: String ->
            when (action) {
                "increment" -> state + 1
                "decrement" -> state - 1
                else -> state
            }
        }

        expectThat(count.get()).isEqualTo(0)

        dispatch("increment")
        expectThat(count.get()).isEqualTo(1)

        dispatch("decrement")
        expectThat(count.get()).isEqualTo(0)
    }

    @Test
    fun `useReducer supports complex actions and state`() {
        data class User(val name: String, val age: Int)
        abstract class UserAction
        data class UpdateName(val name: String) : UserAction()
        data class IncrementAge(val by: Int) : UserAction()

        val initialUser = User("John", 25)
        val (user, dispatch) = useReducer(initialUser) { state: User, action: UserAction ->
            when (action) {
                is UpdateName -> state.copy(name = action.name)
                is IncrementAge -> state.copy(age = state.age + action.by)
                else -> throw IllegalStateException()
            }
        }

        expectThat(user.get().name).isEqualTo("John")
        expectThat(user.get().age).isEqualTo(25)

        dispatch(UpdateName("Jane"))
        expectThat(user.get().name).isEqualTo("Jane")

        dispatch(IncrementAge(5))
        expectThat(user.get().age).isEqualTo(30)
    }
}