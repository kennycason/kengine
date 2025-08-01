package com.kengine.hooks.reducer

import com.kengine.hooks.state.State

data class Reducer<State, Action>(
    private val initialState: State,
    private val reducer: (State, Action) -> State
) {
    private val currentState = State(initialState)

    fun getState(): State = currentState.get()

    fun dispatch(action: Action) {
        val newState = reducer(currentState.get(), action)
        currentState.set(newState)
    }
}