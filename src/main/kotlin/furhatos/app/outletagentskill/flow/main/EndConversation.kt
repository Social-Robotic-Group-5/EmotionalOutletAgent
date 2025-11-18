package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.state

val EndConversation: State= state{
    onEntry {
        furhat.say("Bye")
        goto(Idle)
    }
}