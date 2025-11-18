package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onResponse
import furhatos.flow.kotlin.state
import furhatos.gestures.Gestures
import furhatos.nlu.Intent
import furhatos.nlu.common.No
import furhatos.nlu.common.Yes
import furhatos.util.Language


class GreetReply : Intent() {
    override fun getExamples(lang: Language) = listOf(
        "Hi",
        "Hello",
        "Howdy",
        "Nice to meet you",
        "Likewise"
    )
}

val StartConversation: State = state {
    onEntry {
        furhat.gesture(Gestures.BigSmile, async = true)
        furhat.ask({
            random {
                +"Hi"
                +"Hello"
            }
            +"there, nice to meet you"})
    }

    onResponse<GreetReply> {
        furhat.ask("Do you want to start this session?")
    }

    onResponse<Yes> {
        furhat.gesture(Gestures.Nod, async = false)
        goto(ValidatingEmotion)
    }

    onResponse<No> {
        goto(EndConversation)
    }
}

