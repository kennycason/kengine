package com.kengine.hooks.reducer

import com.kengine.hooks.state.State

fun <S, A> useReducer(
    initialState: S,
    reducer: (S, A) -> S
): Pair<State<S>, (A) -> Unit> {
    val currentState = State(initialState)
    val dispatch: (A) -> Unit = { action: A ->
        val newState = reducer(currentState.get(), action)
        currentState.set(newState)
    }
    return Pair(currentState, dispatch)
}