package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onUserEnter
import furhatos.flow.kotlin.onUserLeave
import furhatos.flow.kotlin.state

val Idle: State = state {
    onEntry {
        furhat.attendNobody()
    }

    onUserEnter {
        furhat.attend(it)
        goto(Attending)
    }
}

val Attending = state {
    onEntry {
        goto(StartConversation)
    }

    onUserLeave {
        goto(EndConversation)
    }
}
