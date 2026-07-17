package com.liukscot.reminders.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

// Holds the current screen's "new reminder" action so the app can draw the FAB once, fixed, instead
// of each screen drawing its own inside the sliding page — which made the buttons swipe off only to
// have identical ones swipe back in. `owner` guards against the outgoing screen clearing the action
// the incoming one just set during a transition: release only clears if the caller still holds it.
class FabState {
    var onAdd by mutableStateOf<(() -> Unit)?>(null)
        private set
    private var owner: Any? = null

    fun claim(owner: Any, action: () -> Unit) {
        this.owner = owner
        onAdd = action
    }

    fun release(owner: Any) {
        if (this.owner === owner) {
            this.owner = null
            onAdd = null
        }
    }
}

val LocalFabState = staticCompositionLocalOf<FabState> { error("No FabState provided") }

// A screen calls this to say "show the add FAB, and this is what it does here". The FAB is drawn by
// the app scaffold, not here — this only registers the action, and unregisters when the screen
// leaves. rememberUpdatedState keeps the action current without re-running the effect.
@Composable
fun ScreenFab(onAdd: () -> Unit) {
    val fab = LocalFabState.current
    val latestOnAdd by rememberUpdatedState(onAdd)
    val owner = remember { Any() }
    DisposableEffect(fab) {
        fab.claim(owner) { latestOnAdd() }
        onDispose { fab.release(owner) }
    }
}
