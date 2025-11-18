package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onNoResponse
import furhatos.flow.kotlin.onResponse
import furhatos.flow.kotlin.state
import furhatos.flow.kotlin.users
import furhatos.gestures.Gestures
import furhatos.nlu.Intent
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
        furhat.ask("How should I call you?")
    }

    onResponse {
        val userName = it.text
        users.current.put("name", userName)
        furhat.say("Nice to meet you, $userName!")
        var confirm = furhat.askYN("Do you want to start this session?")

        if(confirm) {
            furhat.gesture(Gestures.Nod, async = false)
            goto(ValidatingEmotion)
        } else {
            goto(EndConversation)
        }
    }

    onNoResponse { // Catches silence
        furhat.say("I didn't hear anything")
        val confirmExit = furhat.askYN("Do you want to stop the session instead?")

        if(confirmExit) {
            goto(EndConversation)
        } else {
            reentry()
        }
    }
}

